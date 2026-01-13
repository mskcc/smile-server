package org.mskcc.smile.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.messaging.MessageConsumer;
import org.mskcc.smile.commons.generated.Smile.TempoCohortUpdate;
import org.mskcc.smile.commons.generated.Smile.TempoSample;
import org.mskcc.smile.commons.generated.Smile.TempoSampleUpdateMessage;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.tempo.BamComplete;
import org.mskcc.smile.model.tempo.Cohort;
import org.mskcc.smile.model.tempo.MafComplete;
import org.mskcc.smile.model.tempo.QcComplete;
import org.mskcc.smile.model.tempo.Tempo;
import org.mskcc.smile.model.tempo.json.CohortCompleteJson;
import org.mskcc.smile.model.tempo.json.SampleBillingJson;
import org.mskcc.smile.service.AwsS3Service;
import org.mskcc.smile.service.CohortCompleteService;
import org.mskcc.smile.service.SmileSampleService;
import org.mskcc.smile.service.TempoMessageHandlingService;
import org.mskcc.smile.service.TempoService;
import org.mskcc.smile.service.util.NatsMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Component;

/**
 *
 * @author ochoaa
 */
@Component
public class TempoMessageHandlingServiceImpl implements TempoMessageHandlingService {
    @Value("${tempo.wes_bam_complete_topic:}")
    private String TEMPO_WES_BAM_COMPLETE_TOPIC;

    @Value("${tempo.wes_qc_complete_topic:}")
    private String TEMPO_WES_QC_COMPLETE_TOPIC;

    @Value("${tempo.wes_maf_complete_topic:}")
    private String TEMPO_WES_MAF_COMPLETE_TOPIC;

    @Value("${tempo.wes_cohort_complete_topic:}")
    private String TEMPO_WES_COHORT_COMPLETE_TOPIC;

    @Value("${tempo.sample_billing_topic:}")
    private String TEMPO_SAMPLE_BILLING_TOPIC;

    @Value("${tempo.update_samples_embargo_topic:}")
    private String TEMPO_UPDATE_SAMPLES_EMBARGO_TOPIC;

    @Value("${tempo.upload_samples_to_s3_topic:}")
    private String TEMPO_UPLOAD_SAMPLES_TO_S3_TOPIC;

    @Value("${tempo.update_cohort_sub_topic:}")
    private String TEMPO_UPDATE_COHORT_SUB_TOPIC;

    @Value("${tempo.update_cohort_pub_topic:}")
    private String TEMPO_UPDATE_COHORT_PUB_TOPIC;

    @Value("${num.tempo_msg_handler_threads:1}")
    private int NUM_TEMPO_MSG_HANDLERS;

    @Autowired
    private SmileSampleService sampleService;

    @Autowired
    private TempoService tempoService;

    @Autowired
    private CohortCompleteService cohortCompleteService;

    @Autowired
    private AwsS3Service awsS3Service;

    private static Gateway messagingGateway;
    private static final Log LOG = LogFactory.getLog(TempoMessageHandlingServiceImpl.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;
    private static final ExecutorService exec = Executors.newCachedThreadPool();

    private static final BlockingQueue<Map.Entry<String, BamComplete>> bamCompleteQueue =
            new LinkedBlockingQueue<Map.Entry<String, BamComplete>>();
    private static final BlockingQueue<Map.Entry<String, QcComplete>> qcCompleteQueue =
            new LinkedBlockingQueue<Map.Entry<String, QcComplete>>();
    private static final BlockingQueue<Map.Entry<String, MafComplete>> mafCompleteQueue =
            new LinkedBlockingQueue<Map.Entry<String, MafComplete>>();
    private static final BlockingQueue<CohortCompleteJson> cohortCompleteQueue =
            new LinkedBlockingQueue<CohortCompleteJson>();
    private static final BlockingQueue<SampleBillingJson> sampleBillingQueue =
            new LinkedBlockingQueue<SampleBillingJson>();
    private static final BlockingQueue<List<String>> tempoEmbargoStatusQueue =
            new LinkedBlockingQueue<List<String>>();
    private static final BlockingQueue<List<String>> uploadSamplesToS3BucketQueue =
            new LinkedBlockingQueue<List<String>>();
    private static final BlockingQueue<CohortCompleteJson> updateTempoCohortQueue =
            new LinkedBlockingQueue<CohortCompleteJson>();

    private static CountDownLatch bamCompleteHandlerShutdownLatch;
    private static CountDownLatch qcCompleteHandlerShutdownLatch;
    private static CountDownLatch mafCompleteHandlerShutdownLatch;
    private static CountDownLatch cohortCompleteHandlerShutdownLatch;
    private static CountDownLatch sampleBillingHandlerShutdownLatch;
    private static CountDownLatch tempoEmbargoStatusHandlerShutdownLatch;
    private static CountDownLatch uploadSamplesToS3HandlerShutdownLatch;
    private static CountDownLatch updateTempoCohortHandlerShutdownLatch;

    private class BamCompleteHandler implements Runnable {
        final Phaser phaser;
        boolean interrupted = false;

        BamCompleteHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    Entry<String, BamComplete> bcEvent = bamCompleteQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (bcEvent != null) {
                        // first determine if sample exists by the provided primary id
                        String sampleId = bcEvent.getKey();
                        BamComplete bamComplete = bcEvent.getValue();
                        SmileSample sample = sampleService.getDetailedSampleByInputId(sampleId);
                        if (sample != null) {
                            String primaryId = sample.getPrimarySampleAlias();
                            // merge and/or create tempo bam complete event to sample
                            Tempo tempo = tempoService.getTempoDataBySamplePrimaryId(primaryId);
                            if (tempo == null
                                    || !tempo.hasBamCompleteEvent(bamComplete)) {
                                try {
                                    tempoService.mergeBamCompleteEventBySamplePrimaryId(primaryId,
                                        bamComplete);
                                } catch (IncorrectResultSizeDataAccessException e) {
                                    LOG.error("[TEMPO BAM COMPLETE ERROR] Encountered error while "
                                            + "persisting BAM complete event to database: "
                                            + bcEvent.toString(), e);
                                }
                            }
                        } else {
                            LOG.error("[TEMPO BAM COMPLETE ERROR] Sample does not exist by id: " + sampleId);
                        }
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during handling of BAM complete event", e);
                }
                bamCompleteHandlerShutdownLatch.countDown();
            }
        }
    }

    private class QcCompleteHandler implements Runnable {
        final Phaser phaser;
        boolean interrupted = false;

        QcCompleteHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    Entry<String, QcComplete> qcEvent = qcCompleteQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (qcEvent != null) {
                        // first determine if sample exists by the provided primary id
                        String sampleId = qcEvent.getKey();
                        QcComplete qcComplete = qcEvent.getValue();
                        SmileSample sample = sampleService.getDetailedSampleByInputId(sampleId);
                        if (sample != null) {
                            String primaryId = sample.getPrimarySampleAlias();
                            // merge and/or create tempo qc complete event to sample
                            Tempo tempo = tempoService.getTempoDataBySamplePrimaryId(primaryId);
                            if (tempo == null
                                    || !tempo.hasQcCompleteEvent(qcComplete)) {
                                try {
                                    tempoService.mergeQcCompleteEventBySamplePrimaryId(primaryId,
                                            qcComplete);
                                } catch (IncorrectResultSizeDataAccessException e) {
                                    LOG.error("[TEMPO QC COMPLETE ERROR] Encountered error while persisting "
                                            + "QC complete event to database: " + qcEvent.toString(), e);
                                }
                            }
                        } else {
                            LOG.error("[TEMPO QC COMPLETE ERROR] Sample does not exist by id: " + sampleId);
                        }
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during handling of BAM complete event", e);
                }
                qcCompleteHandlerShutdownLatch.countDown();
            }
        }
    }

    private class MafCompleteHandler implements Runnable {
        final Phaser phaser;
        boolean interrupted = false;

        MafCompleteHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    Entry<String, MafComplete> mcEvent = mafCompleteQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (mcEvent != null) {
                        // first determine if sample exists by the provided primary id
                        String sampleId = mcEvent.getKey();
                        MafComplete mafComplete = mcEvent.getValue();
                        SmileSample sample = sampleService.getDetailedSampleByInputId(sampleId);
                        if (sample != null) {
                            String primaryId = sample.getPrimarySampleAlias();

                            // resolve normal primary id since cmo sample id might have been provided
                            // in the incoming NATS message from TEMPO - if sample does not exist then
                            // simply keep the provided input id as is
                            SmileSample normalSample = sampleService
                                    .getDetailedSampleByInputId(mafComplete.getNormalPrimaryId());
                            if (normalSample != null) {
                                mafComplete.setNormalPrimaryId(normalSample.getPrimarySampleAlias());
                            }

                            // merge and/or create tempo maf complete event to sample
                            Tempo tempo = tempoService.getTempoDataBySamplePrimaryId(primaryId);
                            if (tempo == null
                                    || !tempo.hasMafCompleteEvent(mafComplete)) {
                                try {
                                    tempoService.mergeMafCompleteEventBySamplePrimaryId(primaryId,
                                            mafComplete);
                                } catch (IncorrectResultSizeDataAccessException e) {
                                    LOG.error("[TEMPO MAF COMPLETE ERROR] Encountered error while "
                                            + "persisting MAF complete event to database: "
                                            + mcEvent.toString(), e);
                                }
                            }
                        } else {
                            LOG.error("[TEMPO MAF COMPLETE ERROR] Sample does not exist by id: " + sampleId);
                        }
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during handling of MAF complete event", e);
                }
                mafCompleteHandlerShutdownLatch.countDown();
            }
        }
    }

    private class CohortCompleteHandler implements Runnable {
        final Phaser phaser;
        boolean interrupted = false;

        CohortCompleteHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    CohortCompleteJson ccJson = cohortCompleteQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (ccJson != null) {
                        // cohorts are never redelivered. only updates to end users
                        // (access) can change but associated cohort samples do not change
                        Cohort cohort = new Cohort(ccJson);
                        Cohort existingCohort =
                                cohortCompleteService.getCohortByCohortId(ccJson.getCohortId());
                        if (existingCohort == null) {
                            LOG.info("Persisting new cohort: " + ccJson.getCohortId());
                            // tumor-normal pairs are provided as map entries - this block
                            // compiles them into a set list of strings
                            cohortCompleteService.saveCohort(cohort, ccJson.getTumorNormalPairsAsSet());
                            // upload to aws s3 bucket
                            uploadTempoSamplesToAwsS3Bucket(ccJson.getTumorPrimaryIdsAsSet());
                        } else if (cohortCompleteService.hasUpdates(existingCohort,
                                cohort)) {
                            LOG.info("Received updates for cohort: " + ccJson.getCohortId());
                            if (cohortCompleteService.hasCohortCompleteUpdates(existingCohort, cohort)) {
                                existingCohort.addCohortComplete(cohort.getLatestCohortComplete());
                            }

                            // new samples refer to samples that aren't yet linked to the given cohort
                            Set<String> newSamples = new HashSet<>();
                            for (String inputId : ccJson.getTumorNormalPairsAsSet()) {
                                newSamples.add(sampleService.getSamplePrimaryIdBySampleInputId(inputId));
                            }
                            newSamples.removeAll(existingCohort.getCohortSamplePrimaryIds());

                            // persist updates to db and upload to aws s3 bucket
                            cohortCompleteService.saveCohort(existingCohort, newSamples);
                            uploadTempoSamplesToAwsS3Bucket(ccJson.getTumorPrimaryIdsAsSet());
                        } else {
                            LOG.error("Cohort " + ccJson.getCohortId()
                                    + " already exists and no new updates were received.");
                        }
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during handling of Cohort complete event", e);
                }
                cohortCompleteHandlerShutdownLatch.countDown();
            }
        }
    }

    private class SampleBillingHandler implements Runnable {
        final Phaser phaser;
        boolean interrupted = false;

        SampleBillingHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    SampleBillingJson billing = sampleBillingQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (billing != null) {
                        // this message is coming straight from the dashboard and should therefore always
                        // have data for a valid sample that exists in the database
                        // however this check will make extra sure that the primary id received
                        // in the nats message actually exists in the database before conducting
                        // further operations in the db
                        SmileSample sample = sampleService.getDetailedSampleByInputId(billing.getPrimaryId());
                        if (sample != null) {
                            StringBuilder builder = new StringBuilder();
                            builder.append("Updating billing information for sample: ")
                                    .append(sample.getPrimarySampleAlias());
                            if (!billing.getPrimaryId().equalsIgnoreCase(sample.getPrimarySampleAlias())) {
                                builder.append(" (mapped from input id: ")
                                        .append(billing.getPrimaryId())
                                        .append(")");
                            }
                            LOG.info(builder.toString());
                            tempoService.updateSampleBilling(billing);

                            // publish to tempo sample update topic
                            TempoSampleUpdateMessage sampleUpdateMessage = genTempoSampleUpdateMessage(
                                    new HashSet<>(Arrays.asList(billing.getPrimaryId())));
                            if (sampleUpdateMessage != null) {
                                LOG.info("Publishing TEMPO samples to cBioPortal:\n"
                                        + sampleUpdateMessage.toString());
                                messagingGateway.publish(TEMPO_UPDATE_SAMPLES_EMBARGO_TOPIC,
                                        sampleUpdateMessage.toByteArray());
                                LOG.info("Pushing TEMPO samples to AWS s3 bucket for cBioPortal:\n"
                                        + sampleUpdateMessage.toString());
                                awsS3Service.pushTempoSamplesToS3Bucket(sampleUpdateMessage);
                            } else {
                                LOG.warn("There are no valid TEMPO samples to publish to: "
                                        + TEMPO_UPDATE_SAMPLES_EMBARGO_TOPIC);
                            }

                        } else {
                            LOG.error("[TEMPO SAMPLE BILLING ERROR] Cannot update billing information for "
                                    + "sample that does not exist: " + billing.getPrimaryId());
                        }
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during handling of sample billing data", e);
                }
                sampleBillingHandlerShutdownLatch.countDown();
            }
        }
    }

    private class TempoEmbargoStatusHandler implements Runnable {
        final Phaser phaser;
        boolean interrupted = false;

        TempoEmbargoStatusHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    List<String> samplePrimaryIds = tempoEmbargoStatusQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (samplePrimaryIds != null) {
                        LOG.info("Updating access level for " + samplePrimaryIds.size() + " samples...");
                        tempoService.updateTempoAccessLevel(samplePrimaryIds,
                                TempoServiceImpl.ACCESS_LEVEL_PUBLIC);
                        // publish to tempo sample update topic
                        TempoSampleUpdateMessage sampleUpdateMessage = genTempoSampleUpdateMessage(
                                new HashSet<>(samplePrimaryIds));
                        if (sampleUpdateMessage != null) {
                            LOG.info("Publishing TEMPO samples to cBioPortal:\n"
                                    + sampleUpdateMessage.toString());
                            messagingGateway.publish(TEMPO_UPDATE_SAMPLES_EMBARGO_TOPIC,
                                    sampleUpdateMessage.toByteArray());
                            LOG.info("Pushing TEMPO samples to AWS s3 bucket for cBioPortal:\n"
                                        + sampleUpdateMessage.toString());
                            awsS3Service.pushTempoSamplesToS3Bucket(sampleUpdateMessage);
                        } else {
                            LOG.warn("There are no valid TEMPO samples to publish to: "
                                    + TEMPO_UPDATE_SAMPLES_EMBARGO_TOPIC);
                        }
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during handling of sample embargo status update", e);
                }
                tempoEmbargoStatusHandlerShutdownLatch.countDown();
            }
        }
    }

    private class UploadSamplesToS3BucketHandler implements Runnable {
        final Phaser phaser;
        boolean interrupted = false;

        UploadSamplesToS3BucketHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    List<String> samplePrimaryIds
                            = uploadSamplesToS3BucketQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (samplePrimaryIds != null) {
                        // upload samples to s3 bucket
                        LOG.info("Uploading " + samplePrimaryIds.size() + " samples to AWS s3 bucket...");
                        uploadTempoSamplesToAwsS3Bucket(new HashSet<>(samplePrimaryIds));
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during handling of sample billing data", e);
                }
                uploadSamplesToS3HandlerShutdownLatch.countDown();
            }
        }
    }

    private class UpdateTempoCohortHandler implements Runnable {
        final Phaser phaser;
        boolean interrupted = false;

        UpdateTempoCohortHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    CohortCompleteJson ccJson
                            = updateTempoCohortQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (ccJson != null) {
                        Cohort cohort = new Cohort(ccJson);
                        Cohort existingCohort =
                                cohortCompleteService.getCohortByCohortId(ccJson.getCohortId());
                        // check if there are updates to persist
                        if (cohortCompleteService.hasCohortCompleteUpdates(existingCohort,
                                cohort)) {
                            LOG.info("Received updates for cohort: " + ccJson.getCohortId());
                            existingCohort.addCohortComplete(cohort.getLatestCohortComplete());

                            // persist updates to db and publish updates to tempobot
                            // note: passing null as sample ids because this update
                            // is from the smile dashboard
                            cohortCompleteService.saveCohort(existingCohort, null);

                            // use proto type
                            TempoCohortUpdate cohortUpdateMessage = TempoCohortUpdate.newBuilder()
                                    .setCohortId(ccJson.getCohortId())
                                    .setDate(ccJson.getDate())
                                    .setType(ccJson.getType())
                                    .setProjectTitle(ccJson.getProjectTitle())
                                    .setProjectSubtitle(ccJson.getProjectSubtitle())
                                    .addAllEndUsers(ccJson.getEndUsers())
                                    .addAllPmUsers(ccJson.getPmUsers())
                                    .build();
                            LOG.info("Publishing updates to TEMPO bot.");
                            messagingGateway.publish(TEMPO_UPDATE_COHORT_PUB_TOPIC,
                                    cohortUpdateMessage.toByteArray());
                        } else {
                            LOG.error("No new updates to persist for cohort:  " + ccJson.getCohortId());
                        }
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during handling of tempo cohort update", e);
                }
                updateTempoCohortHandlerShutdownLatch.countDown();
            }
        }
    }

    private TempoSampleUpdateMessage genTempoSampleUpdateMessage(Set<String> samplePrimaryIds)
            throws Exception {
        // validate and build tempo samples to publish to cBioPortal
        Set<TempoSample> validTempoSamples = new HashSet<>();
        LOG.info("Assembling TEMPO data per sample for uploading to AWS s3 bucket for cBioPortal");
        for (String primaryId : samplePrimaryIds) {
            try {
                TempoSample tempoSample = tempoService.getTempoSampleDataBySamplePrimaryId(primaryId);

                // confirm tempo data exists by primary id
                if (tempoSample == null) {
                    LOG.error("[TEMPO EMBARGO UPDATE ERROR] Tempo data not found "
                            + "for sample: " + primaryId);
                    continue;
                }
                // validate props before adding tempo sample to set of samples to be published
                if (StringUtils.isBlank(tempoSample.getCmoSampleName())) {
                    LOG.error("[TEMPO EMBARGO UPDATE ERROR] Invalid CMO Sample Name "
                            + "for sample: " + primaryId + ", " + tempoSample.toString());
                    continue;
                }
                if (StringUtils.isBlank(tempoSample.getAccessLevel())) {
                    LOG.error("[TEMPO EMBARGO UPDATE ERROR] Invalid Access Level "
                            + "for sample: " + primaryId + ", " + tempoSample.toString());
                    continue;
                }
                if (StringUtils.isBlank(tempoSample.getCustodianInformation())) {
                    LOG.error("[TEMPO EMBARGO UPDATE ERROR] Invalid Custodian Information "
                            + "for sample: " + primaryId + ", " + tempoSample.toString());
                    continue;
                }
                if (StringUtils.isBlank(tempoSample.getDmpSampleId())) {
                    LOG.error("[TEMPO EMBARGO UPDATE ERROR] Invalid DMP WES ID "
                            + "for sample: " + primaryId + ", " + tempoSample.toString());
                    continue;
                }
                validTempoSamples.add(tempoSample);
            } catch (Exception e) {
                LOG.error("Error building TEMPO data to publish to cBioPortal for sample: " + primaryId, e);
            }
        }
        // bundle together all valid tempo samples and publish to cBioPortal
        if (!validTempoSamples.isEmpty()) {
            LOG.info("Total samples that will be uploaded to AWS s3 bucket = " + validTempoSamples.size());
            TempoSampleUpdateMessage tempoSampleUpdateMessage = TempoSampleUpdateMessage.newBuilder()
                .addAllTempoSamples(validTempoSamples)
                .build();
            return tempoSampleUpdateMessage;
        } else {
            LOG.warn("No valid TEMPO samples to upload to AWS s3 bucket for cBioPortal");
            return null;
        }
    }

    private void uploadTempoSamplesToAwsS3Bucket(Set<String> samplePrimaryIds) throws Exception {
        TempoSampleUpdateMessage tempoSampleUpdateMessage = genTempoSampleUpdateMessage(samplePrimaryIds);
        if (tempoSampleUpdateMessage != null) {
            try {
                LOG.info("Pushing TEMPO samples to AWS s3 bucket for cBioPortal:\n"
                        + tempoSampleUpdateMessage.toString());
                awsS3Service.pushTempoSamplesToS3Bucket(tempoSampleUpdateMessage);
            } catch (Exception e) {
                LOG.error("Error uploading TEMPO samples to AWS s3 bucket for cBioPortal", e);
            }
        } else {
            LOG.warn("There are no valid TEMPO samples to upload to AWS s3 bucket");
        }
    }

    @Override
    public void intialize(Gateway gateway) throws Exception {
        if (!initialized) {
            messagingGateway = gateway;
            setupBamCompleteHandler(messagingGateway, this);
            setupQcCompleteHandler(messagingGateway, this);
            setupMafCompleteHandler(messagingGateway, this);
            setupCohortCompleteHandler(messagingGateway, this);
            setupSampleBillingHandler(messagingGateway, this);
            setupTempoEmbargoStatusHandler(messagingGateway, this);
            setupUploadSamplesToS3BucketHandler(messagingGateway, this);
            setupUpdateTempoCohortHandler(messagingGateway, this);
            initializeMessageHandlers();
            initialized = true;
        } else {
            LOG.error("Messaging Handler Service has already been initialized, ignoring request.");
        }
    }

    @Override
    public void bamCompleteHandler(Map.Entry<String, BamComplete> bcEvent) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            bamCompleteQueue.put(bcEvent);
        } else {
            LOG.error("Shutdown initiated, not accepting BAM event: " + bcEvent);
            throw new IllegalStateException("Shutdown initiated, not handling any more TEMPO events");
        }
    }

    @Override
    public void qcCompleteHandler(Map.Entry<String, QcComplete> qcEvent) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            qcCompleteQueue.put(qcEvent);
        } else {
            LOG.error("Shutdown initiated, not accepting QC event: " + qcEvent);
            throw new IllegalStateException("Shutdown initiated, not handling any more TEMPO events");
        }
    }

    @Override
    public void mafCompleteHandler(Map.Entry<String, MafComplete> mcEvent) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            mafCompleteQueue.put(mcEvent);
        } else {
            LOG.error("Shutdown initiated, not accepting MAF event: " + mcEvent);
            throw new IllegalStateException("Shutdown initiated, not handling any more TEMPO events");
        }
    }

    @Override
    public void cohortCompleteHandler(CohortCompleteJson cohortEvent) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            cohortCompleteQueue.put(cohortEvent);
        } else {
            LOG.error("Shutdown initiated, not accepting Cohort event: " + cohortEvent);
            throw new IllegalStateException("Shutdown initiated, not handling any more TEMPO events");
        }
    }

    @Override
    public void sampleBillingHandler(SampleBillingJson billing) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            sampleBillingQueue.put(billing);
        } else {
            LOG.error("Shutdown initiated, not accepting billing event: " + billing);
            throw new IllegalStateException("Shutdown initiated, not handling any more TEMPO events");
        }
    }

    @Override
    public void tempoEmbargoStatusHandler(List<String> samplePrimaryIds) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            tempoEmbargoStatusQueue.put(samplePrimaryIds);
        } else {
            LOG.error("Shutdown initiated, not accepting TEMPO embargo status update event: "
                    + samplePrimaryIds);
            throw new IllegalStateException("Shutdown initiated, not handling any more TEMPO events");
        }
    }

    @Override
    public void uploadSamplesToS3BucketHandler(List<String> samplePrimaryIds) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            uploadSamplesToS3BucketQueue.put(samplePrimaryIds);
        } else {
            LOG.error("Shutdown initiated, not accepting request to upload samples to s3 bucket: "
                    + samplePrimaryIds);
            throw new IllegalStateException("Shutdown initiated, not handling any more TEMPO events");
        }
    }

    @Override
    public void updateTempoCohortHandler(CohortCompleteJson ccJson) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            updateTempoCohortQueue.put(ccJson);
        } else {
            LOG.error("Shutdown initiated, not accepting request to update TEMPO cohort: "
                    + ccJson);
            throw new IllegalStateException("Shutdown initiated, not handling any more TEMPO events");
        }
    }

    @Override
    public void shutdown() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        exec.shutdownNow();
        bamCompleteHandlerShutdownLatch.await();
        qcCompleteHandlerShutdownLatch.await();
        mafCompleteHandlerShutdownLatch.await();
        cohortCompleteHandlerShutdownLatch.await();
        sampleBillingHandlerShutdownLatch.await();
        tempoEmbargoStatusHandlerShutdownLatch.await();
        uploadSamplesToS3HandlerShutdownLatch.await();
        updateTempoCohortHandlerShutdownLatch.await();
        shutdownInitiated = true;
    }

    private void initializeMessageHandlers() throws Exception {
        // bam complete handler
        bamCompleteHandlerShutdownLatch = new CountDownLatch(NUM_TEMPO_MSG_HANDLERS);
        final Phaser bamCompletePhaser = new Phaser();
        bamCompletePhaser.register();
        for (int lc = 0; lc < NUM_TEMPO_MSG_HANDLERS; lc++) {
            bamCompletePhaser.register();
            exec.execute(new BamCompleteHandler(bamCompletePhaser));
        }
        bamCompletePhaser.arriveAndAwaitAdvance();

        // qc complete handler
        qcCompleteHandlerShutdownLatch = new CountDownLatch(NUM_TEMPO_MSG_HANDLERS);
        final Phaser qcCompletePhaser = new Phaser();
        qcCompletePhaser.register();
        for (int lc = 0; lc < NUM_TEMPO_MSG_HANDLERS; lc++) {
            qcCompletePhaser.register();
            exec.execute(new QcCompleteHandler(qcCompletePhaser));
        }
        qcCompletePhaser.arriveAndAwaitAdvance();

        // maf complete handler
        mafCompleteHandlerShutdownLatch = new CountDownLatch(NUM_TEMPO_MSG_HANDLERS);
        final Phaser mafCompletePhaser = new Phaser();
        mafCompletePhaser.register();
        for (int lc = 0; lc < NUM_TEMPO_MSG_HANDLERS; lc++) {
            mafCompletePhaser.register();
            exec.execute(new MafCompleteHandler(mafCompletePhaser));
        }
        mafCompletePhaser.arriveAndAwaitAdvance();

        // cohort complete handler
        cohortCompleteHandlerShutdownLatch = new CountDownLatch(NUM_TEMPO_MSG_HANDLERS);
        final Phaser cohortCompletePhaser = new Phaser();
        cohortCompletePhaser.register();
        for (int lc = 0; lc < NUM_TEMPO_MSG_HANDLERS; lc++) {
            cohortCompletePhaser.register();
            exec.execute(new CohortCompleteHandler(cohortCompletePhaser));
        }
        cohortCompletePhaser.arriveAndAwaitAdvance();

        // sample billing handler
        sampleBillingHandlerShutdownLatch = new CountDownLatch(NUM_TEMPO_MSG_HANDLERS);
        final Phaser sampleBillingPhaser = new Phaser();
        sampleBillingPhaser.register();
        for (int lc = 0; lc < NUM_TEMPO_MSG_HANDLERS; lc++) {
            sampleBillingPhaser.register();
            exec.execute(new SampleBillingHandler(sampleBillingPhaser));
        }
        sampleBillingPhaser.arriveAndAwaitAdvance();

        // tempo embargo status handler
        tempoEmbargoStatusHandlerShutdownLatch = new CountDownLatch(NUM_TEMPO_MSG_HANDLERS);
        final Phaser tempoEmbargoStatusPhaser = new Phaser();
        tempoEmbargoStatusPhaser.register();
        for (int lc = 0; lc < NUM_TEMPO_MSG_HANDLERS; lc++) {
            tempoEmbargoStatusPhaser.register();
            exec.execute(new TempoEmbargoStatusHandler(tempoEmbargoStatusPhaser));
        }
        tempoEmbargoStatusPhaser.arriveAndAwaitAdvance();

        // upload samples to s3 bucket handler
        uploadSamplesToS3HandlerShutdownLatch = new CountDownLatch(NUM_TEMPO_MSG_HANDLERS);
        final Phaser samplesUploadS3Phaser = new Phaser();
        samplesUploadS3Phaser.register();
        for (int lc = 0; lc < NUM_TEMPO_MSG_HANDLERS; lc++) {
            samplesUploadS3Phaser.register();
            exec.execute(new UploadSamplesToS3BucketHandler(samplesUploadS3Phaser));
        }
        samplesUploadS3Phaser.arriveAndAwaitAdvance();

        // update cohort from smile dashboard handler
        updateTempoCohortHandlerShutdownLatch = new CountDownLatch(NUM_TEMPO_MSG_HANDLERS);
        final Phaser updateTempoCohortPhaser = new Phaser();
        updateTempoCohortPhaser.register();
        for (int lc = 0; lc < NUM_TEMPO_MSG_HANDLERS; lc++) {
            updateTempoCohortPhaser.register();
            exec.execute(new UpdateTempoCohortHandler(updateTempoCohortPhaser));
        }
        updateTempoCohortPhaser.arriveAndAwaitAdvance();
    }

    private void setupBamCompleteHandler(Gateway gateway,
            TempoMessageHandlingService tempoMessageHandlingService) throws Exception {
        gateway.subscribe(TEMPO_WES_BAM_COMPLETE_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                try {
                    LOG.info("Received message on topic: " + TEMPO_WES_BAM_COMPLETE_TOPIC);
                    String bamCompleteJson = NatsMsgUtil.extractNatsJsonString(msg);
                    if (bamCompleteJson == null) {
                        LOG.error("Exception occurred during processing of NATS message data");
                        return;
                    }
                    Map<String, String> bamCompleteMap =
                            (Map<String, String>) NatsMsgUtil.convertObjectFromString(
                                    bamCompleteJson, new TypeReference<Map<String, String>>() {});

                    // resolve sample id, bam complete object
                    String sampleId = ObjectUtils.firstNonNull(bamCompleteMap.get("primaryId"),
                            bamCompleteMap.get("cmoSampleId"));
                    BamComplete bamComplete = new BamComplete(bamCompleteMap.get("date"),
                            bamCompleteMap.get("status"));

                    Map.Entry<String, BamComplete> eventData =
                            new AbstractMap.SimpleImmutableEntry<>(sampleId, bamComplete);
                    tempoMessageHandlingService.bamCompleteHandler(eventData);
                } catch (Exception e) {
                    LOG.error("Exception occurred during processing of BAM complete event: "
                            + TEMPO_WES_BAM_COMPLETE_TOPIC, e);
                }
            }
        });
    }

    private void setupQcCompleteHandler(Gateway gateway,
            TempoMessageHandlingService tempoMessageHandlingService) throws Exception {
        gateway.subscribe(TEMPO_WES_QC_COMPLETE_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                try {
                    LOG.info("Received message on topic: " + TEMPO_WES_QC_COMPLETE_TOPIC);
                    String qcCompleteJson = NatsMsgUtil.extractNatsJsonString(msg);
                    if (qcCompleteJson == null) {
                        LOG.error("Exception occurred during processing of NATS message data");
                        return;
                    }
                    Map<String, String> qcCompleteMap =
                            (Map<String, String>) NatsMsgUtil.convertObjectFromString(
                                    qcCompleteJson, new TypeReference<Map<String, String>>() {});

                        // resolve sample id, qc complete object
                        String sampleId = ObjectUtils.firstNonNull(qcCompleteMap.get("primaryId"),
                            qcCompleteMap.get("cmoSampleId"));
                        QcComplete qcComplete = new QcComplete(qcCompleteMap.get("date"),
                                qcCompleteMap.get("result"), qcCompleteMap.get("reason"),
                                qcCompleteMap.get("status"));

                    Map.Entry<String, QcComplete> eventData =
                            new AbstractMap.SimpleImmutableEntry<>(sampleId, qcComplete);
                    tempoMessageHandlingService.qcCompleteHandler(eventData);
                } catch (Exception e) {
                    LOG.error("Exception occurred during processing of QC complete event: "
                            + TEMPO_WES_QC_COMPLETE_TOPIC, e);
                }
            }
        });
    }

    private void setupMafCompleteHandler(Gateway gateway,
            TempoMessageHandlingService tempoMessageHandlingService) throws Exception {
        gateway.subscribe(TEMPO_WES_MAF_COMPLETE_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                try {
                    LOG.info("Received message on topic: " + TEMPO_WES_MAF_COMPLETE_TOPIC);
                    String mafCompleteJson = NatsMsgUtil.extractNatsJsonString(msg);
                    if (mafCompleteJson == null) {
                        LOG.error("Exception occurred during processing of NATS message data");
                        return;
                    }
                    Map<String, String> mafCompleteMap =
                            (Map<String, String>) NatsMsgUtil.convertObjectFromString(
                                    mafCompleteJson, new TypeReference<Map<String, String>>() {});

                    // resolve sample id, normal ids, and maf complete object
                    String sampleId = ObjectUtils.firstNonNull(mafCompleteMap.get("primaryId"),
                            mafCompleteMap.get("cmoSampleId"));
                    String normalSampleId = ObjectUtils.firstNonNull(mafCompleteMap.get("normalPrimaryId"),
                            mafCompleteMap.get("normalCmoSampleId"));
                    MafComplete mafComplete = new MafComplete(mafCompleteMap.get("date"),
                            normalSampleId,
                            mafCompleteMap.get("status"));

                    Map.Entry<String, MafComplete> eventData =
                            new AbstractMap.SimpleImmutableEntry<>(sampleId, mafComplete);
                    tempoMessageHandlingService.mafCompleteHandler(eventData);
                } catch (Exception e) {
                    LOG.error("Exception occurred during processing of MAF complete event: "
                            + TEMPO_WES_MAF_COMPLETE_TOPIC, e);
                }
            }
        });
    }

    private void setupCohortCompleteHandler(Gateway gateway,
            TempoMessageHandlingService tempoMessageHandlingService) throws Exception {
        gateway.subscribe(TEMPO_WES_COHORT_COMPLETE_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                try {
                    LOG.info("Received message on topic: " + TEMPO_WES_COHORT_COMPLETE_TOPIC);
                    String cohortCompleteJson = NatsMsgUtil.extractNatsJsonString(msg);
                    if (cohortCompleteJson == null) {
                        LOG.error("Exception occurred during processing of NATS message data");
                        return;
                    }
                    CohortCompleteJson cohortCompleteData =
                            (CohortCompleteJson) NatsMsgUtil.convertObjectFromString(
                                    cohortCompleteJson, new TypeReference<CohortCompleteJson>() {});

                    tempoMessageHandlingService.cohortCompleteHandler(cohortCompleteData);
                } catch (Exception e) {
                    LOG.error("Exception occurred during processing of Cohort Complete event: "
                            + TEMPO_WES_COHORT_COMPLETE_TOPIC, e);
                }
            }
        });
    }

    private void setupSampleBillingHandler(Gateway gateway,
            TempoMessageHandlingService tempoMessageHandlingService) throws Exception {
        gateway.subscribe(TEMPO_SAMPLE_BILLING_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                try {
                    LOG.info("Received message on topic: " + TEMPO_SAMPLE_BILLING_TOPIC);
                    String billingJson = NatsMsgUtil.extractNatsJsonString(msg);
                    if (billingJson == null) {
                        LOG.error("Exception occurred during processing of NATS message data");
                        return;
                    }
                    SampleBillingJson billing =
                            (SampleBillingJson) NatsMsgUtil.convertObjectFromString(
                                    billingJson, new TypeReference<SampleBillingJson>() {});

                    tempoMessageHandlingService.sampleBillingHandler(billing);
                } catch (Exception e) {
                    LOG.error("Exception occurred during processing of billing update event: "
                            + TEMPO_SAMPLE_BILLING_TOPIC, e);
                }
            }
        });
    }

    private void setupTempoEmbargoStatusHandler(Gateway gateway,
            TempoMessageHandlingService tempoMessageHandlingService) throws Exception {
        gateway.subscribe(TEMPO_UPDATE_SAMPLES_EMBARGO_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                try {
                    LOG.info("Received message on topic: " + TEMPO_UPDATE_SAMPLES_EMBARGO_TOPIC);
                    String samplePrimaryIdsString = NatsMsgUtil.extractNatsJsonString(msg);
                    if (samplePrimaryIdsString == null) {
                        LOG.error("Exception occurred during processing of NATS message data");
                        return;
                    }
                    List<String> samplePrimaryIds =
                            (List<String>) NatsMsgUtil.convertObjectFromString(
                                    samplePrimaryIdsString, new TypeReference<List<String>>() {});

                    tempoMessageHandlingService.tempoEmbargoStatusHandler(samplePrimaryIds);
                } catch (Exception e) {
                    LOG.error("Exception occurred during processing of TEMPO Embargo Status update event: "
                            + TEMPO_UPDATE_SAMPLES_EMBARGO_TOPIC, e);
                }
            }
        });
    }

    private void setupUploadSamplesToS3BucketHandler(Gateway gateway,
            TempoMessageHandlingService tempoMessageHandlingService) throws Exception {
        gateway.subscribe(TEMPO_UPLOAD_SAMPLES_TO_S3_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                try {
                    LOG.info("Received message on topic: " + TEMPO_UPLOAD_SAMPLES_TO_S3_TOPIC);
                    String samplePrimaryIdsString = NatsMsgUtil.extractNatsJsonString(msg);
                    if (samplePrimaryIdsString == null) {
                        LOG.error("Exception occurred during processing of NATS message data");
                        return;
                    }
                    List<String> samplePrimaryIds =
                            (List<String>) NatsMsgUtil.convertObjectFromString(
                                    samplePrimaryIdsString, new TypeReference<List<String>>() {});
                    tempoMessageHandlingService.uploadSamplesToS3BucketHandler(samplePrimaryIds);
                } catch (Exception e) {
                    LOG.error("Exception occurred during processing of TEMPO "
                            + "samples upload to s3 bucket message: "
                            + TEMPO_UPLOAD_SAMPLES_TO_S3_TOPIC, e);
                }
            }
        });
    }

    private void setupUpdateTempoCohortHandler(Gateway gateway,
            TempoMessageHandlingService tempoMessageHandlingService) throws Exception {
        gateway.subscribe(TEMPO_UPDATE_COHORT_SUB_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                try {
                    LOG.info("Received message on topic: " + TEMPO_UPDATE_COHORT_SUB_TOPIC);
                    String ccJsonString = NatsMsgUtil.extractNatsJsonString(msg);
                    if (ccJsonString == null) {
                        LOG.error("Exception occurred during processing of NATS message data");
                        return;
                    }
                    CohortCompleteJson ccJson = (CohortCompleteJson) NatsMsgUtil.convertObjectFromString(
                                    ccJsonString, new TypeReference<CohortCompleteJson>() {});
                    tempoMessageHandlingService.updateTempoCohortHandler(ccJson);
                } catch (Exception e) {
                    LOG.error("Exception occurred during processing of TEMPO "
                            + "cohort update message: "
                            + TEMPO_UPDATE_COHORT_SUB_TOPIC, e);
                }
            }
        });
    }
}
