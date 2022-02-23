package org.mskcc.cmo.metadb.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.messaging.MessageConsumer;
import org.mskcc.cmo.metadb.model.MetadbPatient;
import org.mskcc.cmo.metadb.model.MetadbRequest;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.RequestMetadata;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.model.dmp.DmpSampleMetadata;
import org.mskcc.cmo.metadb.service.CrdbMappingService;
import org.mskcc.cmo.metadb.service.MessageHandlingService;
import org.mskcc.cmo.metadb.service.MetadbPatientService;
import org.mskcc.cmo.metadb.service.MetadbRequestService;
import org.mskcc.cmo.metadb.service.MetadbSampleService;
import org.mskcc.cmo.metadb.service.util.RequestDataFactory;
import org.mskcc.cmo.metadb.service.util.SampleDataFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MessageHandlingServiceImpl implements MessageHandlingService {
    @Value("${igo.new_request_topic}")
    private String IGO_NEW_REQUEST_TOPIC;

    @Value("${consistency_check.new_request_topic}")
    private String CONSISTENCY_CHECK_NEW_REQUEST;

    @Value("${metadb.igo_request_update_topic}")
    private String IGO_REQUEST_UPDATE_TOPIC;

    @Value("${metadb.igo_sample_update_topic}")
    private String IGO_SAMPLE_UPDATE_TOPIC;

    @Value("${metadb.dmp_new_sample_topic}")
    private String NEW_DMP_SAMPLE_TOPIC;

    @Value("${metadb.dmp_sample_update_topic}")
    private String DMP_SAMPLE_UPDATE_TOPIC;

    @Value("${metadb.cmo_request_update_topic}")
    private String CMO_REQUEST_UPDATE_TOPIC;

    @Value("${metadb.cmo_sample_update_topic}")
    private String CMO_SAMPLE_UPDATE_TOPIC;

    @Value("${metadb.correct_cmoptid_topic}")
    private String CORRECT_CMOPTID_TOPIC;

    @Value("${request_reply.cmo_label_generator_topic}")
    private String CMO_LABEL_GENERATOR_REQREPLY_TOPIC;

    @Value("${num.new_request_handler_threads}")
    private int NUM_NEW_REQUEST_HANDLERS;

    @Autowired
    private CrdbMappingService crdbMappingService;

    @Autowired
    private MetadbPatientService patientService;

    @Autowired
    private MetadbRequestService requestService;

    @Autowired
    private MetadbSampleService sampleService;

    private final ObjectMapper mapper = new ObjectMapper();
    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;
    private static final ExecutorService exec = Executors.newCachedThreadPool();
    private static final BlockingQueue<MetadbRequest> newRequestQueue =
        new LinkedBlockingQueue<MetadbRequest>();
    private static final BlockingQueue<RequestMetadata> requestUpdateQueue =
            new LinkedBlockingQueue<RequestMetadata>();
    private static final BlockingQueue<SampleMetadata> researchSampleUpdateQueue =
            new LinkedBlockingQueue<SampleMetadata>();
    private static final BlockingQueue<Map<String, String>> correctCmoPatientIdQueue =
            new LinkedBlockingQueue<Map<String, String>>();
    private static final BlockingQueue<MetadbSample> newClinicalSampleQueue =
            new LinkedBlockingQueue<MetadbSample>();
    private static final BlockingQueue<MetadbSample> clinicalSampleUpdateQueue =
            new LinkedBlockingQueue<MetadbSample>();

    private static CountDownLatch newRequestHandlerShutdownLatch;
    private static CountDownLatch requestUpdateHandlerShutdownLatch;
    private static CountDownLatch researchSampleUpdateHandlerShutdownLatch;
    private static CountDownLatch newClinicalSampleHandlerShutdownLatch;
    private static CountDownLatch clinicalSampleUpdateHandlerShutdownLatch;
    private static CountDownLatch correctCmoPatientIdShutdownLatch;

    private static Gateway messagingGateway;

    private static final Log LOG = LogFactory.getLog(MessageHandlingServiceImpl.class);

    private class CorrectCmoPatientIdHandler implements Runnable {
        final Phaser phaser;
        boolean interrupted = false;

        CorrectCmoPatientIdHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    Map<String, String> idCorrectionMap =
                            correctCmoPatientIdQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (idCorrectionMap != null) {
                        String oldCmoPtId = idCorrectionMap.get("oldId");
                        String newCmoPtId = idCorrectionMap.get("newId");

                        List<MetadbSample> samplesByOldCmoPatient =
                                sampleService.getSamplesByCmoPatientId(oldCmoPtId);
                        List<MetadbSample> samplesByNewCmoPatient =
                                sampleService.getSamplesByCmoPatientId(newCmoPtId);
                        MetadbPatient patientByNewId = patientService.getPatientByCmoPatientId(newCmoPtId);

                        // update the cmo patient id for each sample linked to the "old" patient
                        // and the metadata as well
                        for (MetadbSample sample : samplesByOldCmoPatient) {
                            SampleMetadata updatedMetadata = sample.getLatestSampleMetadata();
                            updatedMetadata.setCmoPatientId(newCmoPtId);

                            // research samples need a new label as well
                            if (sample.getSampleCategory().equals("research")) {
                                LOG.info("Requesting new CMO sample label for sample: "
                                        + updatedMetadata.getPrimaryId());
                                Message reply = messagingGateway.request(CMO_LABEL_GENERATOR_REQREPLY_TOPIC,
                                        mapper.writeValueAsString(updatedMetadata));
                                String newCmoSampleLabel = new String(reply.getData(),
                                        StandardCharsets.UTF_8);
                                updatedMetadata.setCmoSampleName(newCmoSampleLabel);
                            }
                            // now update sample with the target patient we want to swap to
                            sample.updateSampleMetadata(updatedMetadata);

                            // update the sample-to-patient relationship if swapping to a different
                            // patient node. if still using the same node the samples are already linked
                            // to then there's no need to override the patient currently set for the sample
                            if (patientByNewId != null) {
                                sample.setPatient(patientByNewId);
                                sampleService.updateSamplePatientRelationship(sample.getMetaDbSampleId(),
                                        patientByNewId.getMetaDbPatientId());
                            }
                            sampleService.saveMetadbSample(sample);
                        }

                        // delete old patient node if we are swapping to an existing patient node
                        // otherwise simply update the existing patient node with the new cmo id
                        if (patientByNewId != null) {
                            LOG.info("Deleting Patient node (and its relationships) for old ID: "
                                + oldCmoPtId);
                            MetadbPatient patientByOldId =
                                    patientService.getPatientByCmoPatientId(oldCmoPtId);
                            patientService.deletePatient(patientByOldId);
                        } else {
                            patientService.updateCmoPatientId(oldCmoPtId, newCmoPtId);
                        }

                        // sanity check the counts before and after the swaps
                        Integer expectedCount = samplesByOldCmoPatient.size()
                                + samplesByNewCmoPatient.size();
                        List<MetadbSample> samplesAfterSwap =
                                sampleService.getSamplesByCmoPatientId(newCmoPtId);
                        if (expectedCount != samplesAfterSwap.size()) {
                            LOG.error("Expected sample count after patient ID swap does not match actual"
                                    + " count: " + expectedCount + " != " + samplesAfterSwap.size());
                        }
                    }
                    if (interrupted && correctCmoPatientIdQueue.isEmpty()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during request handling", e);
                }
            }
            correctCmoPatientIdShutdownLatch.countDown();
        }
    }

    private class NewIgoRequestHandler implements Runnable {

        final Phaser phaser;
        boolean interrupted = false;

        NewIgoRequestHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    MetadbRequest request = newRequestQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (request != null) {
                        MetadbRequest existingRequest =
                                requestService.getMetadbRequestById(request.getIgoRequestId());

                        // persist new request to database
                        if (existingRequest == null) {
                            LOG.info("Received new request with id: " + request.getIgoRequestId());
                            requestService.saveRequest(request);
                            messagingGateway.publish(request.getIgoRequestId(),
                                    CONSISTENCY_CHECK_NEW_REQUEST,
                                    mapper.writeValueAsString(
                                            requestService.getPublishedMetadbRequestById(
                                                    request.getIgoRequestId())));
                        } else if (requestService.requestHasUpdates(existingRequest, request)) {
                            // make call to update the requestJson member of existingRequest in the
                            // database to reflect the latest version of the raw json string that we got
                            // directly from IGO LIMS

                            // message handlers will check if there are updates to persist or not
                            requestUpdateQueue.add(request.getLatestRequestMetadata());
                            for (MetadbSample sample : request.getMetaDbSampleList()) {
                                researchSampleUpdateQueue.add(sample.getLatestSampleMetadata());
                            }
                        } else {
                            LOG.warn("Request already in database - it will not be saved: "
                                    + request.getIgoRequestId());
                        }
                    }
                    if (interrupted && newRequestQueue.isEmpty()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during request handling", e);
                }
            }
            newRequestHandlerShutdownLatch.countDown();
        }
    }

    private class RequestMetadataUpdateHandler implements Runnable {
        final Phaser phaser;
        boolean interrupted = false;

        RequestMetadataUpdateHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    RequestMetadata requestMetadata = requestUpdateQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (requestMetadata != null) {
                        MetadbRequest existingRequest =
                                requestService.getMetadbRequestById(requestMetadata.getIgoRequestId());
                        if (existingRequest == null) {
                            LOG.warn("Request does not already exist in the database: "
                                    + requestMetadata.getIgoRequestId()
                                    + " - will not be persisting updates.");
                        } else if (requestService.requestHasMetadataUpdates(
                                existingRequest.getLatestRequestMetadata(), requestMetadata)) {
                            // persist request-level metadata updates to database
                            LOG.info("Found updates in request metadata: " + requestMetadata.getIgoRequestId()
                                    + " - persisting to database");
                            existingRequest.updateRequestMetadataByMetadata(requestMetadata);
                            if (requestService.saveRequestMetadata(existingRequest)) {
                                LOG.info("Publishing Request-level Metadata updates "
                                        + "to " + CMO_REQUEST_UPDATE_TOPIC);
                                // publish request-level metadata history to CMO_REQUEST_UPDATE_TOPIC
                                messagingGateway.publish(existingRequest.getIgoRequestId(),
                                        CMO_REQUEST_UPDATE_TOPIC,
                                        mapper.writeValueAsString(
                                              existingRequest.getRequestMetadataList()));
                            } else {
                                LOG.error("Failed to update the request metadata for request: "
                                        + existingRequest.getIgoRequestId());
                            }
                        } else {
                            LOG.warn("There are no request-level metadata updates to persist: "
                                    + requestMetadata.getIgoRequestId());
                        }
                    }
                    if (interrupted && requestUpdateQueue.isEmpty()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during handling of request metadata update", e);
                }
            }
            requestUpdateHandlerShutdownLatch.countDown();
        }
    }

    private class ResearchSampleMetadataUpdateHandler implements Runnable {

        final Phaser phaser;
        boolean interrupted = false;

        ResearchSampleMetadataUpdateHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    SampleMetadata sampleMetadata = researchSampleUpdateQueue.poll(
                            100, TimeUnit.MILLISECONDS);
                    if (sampleMetadata != null) {
                        MetadbSample existingSample = sampleService.getResearchSampleByRequestAndIgoId(
                                sampleMetadata.getIgoRequestId(), sampleMetadata.getPrimaryId());
                        if (existingSample == null) {
                            LOG.info("research Sample metadata does not already exist - persisting to db: "
                                    + sampleMetadata.getPrimaryId());
                            // handle and persist new sample received
                            MetadbSample sample = SampleDataFactory
                                    .buildNewResearchSampleFromMetadata(sampleMetadata.getIgoRequestId(),
                                            sampleMetadata);
                            sampleService.saveMetadbSample(sample);
                            LOG.info("Publishing metadata history for new research sample: "
                                    + sampleMetadata.getPrimaryId());
                            messagingGateway.publish(CMO_SAMPLE_UPDATE_TOPIC,
                                    mapper.writeValueAsString(sample.getSampleMetadataList()));
                        } else if (sampleService.sampleHasMetadataUpdates(
                                existingSample.getLatestSampleMetadata(), sampleMetadata)
                                || (!sampleService.sampleHasMetadataUpdates(
                                        existingSample.getLatestSampleMetadata(), sampleMetadata))
                                && !existingSample.getLatestSampleMetadata().getCmoSampleName()
                                        .equals(sampleMetadata.getCmoSampleName())) {
                            // logic checks if 1. comparator detects changes or 2. comparator does not
                            // detect changes because 'cmoSampleName' is ignored but the 'cmoSampleName'
                            // for existing and current sample metadata clearly differ then proceed with
                            // persisting updates for sample
                            LOG.info("Found updates for research sample - persisting to database: "
                                    + sampleMetadata.getPrimaryId());
                            // persist sample level updates to database and publish
                            // sample metadata history to CMO_SAMPLE_METADATA_UPDATE
                            existingSample.updateSampleMetadata(sampleMetadata);
                            sampleService.saveMetadbSample(existingSample);
                            LOG.info("Publishing sample-level metadata history for research sample: "
                                    + sampleMetadata.getPrimaryId());
                            messagingGateway.publish(CMO_SAMPLE_UPDATE_TOPIC,
                                    mapper.writeValueAsString(existingSample.getSampleMetadataList()));
                        } else {
                            LOG.info("There are no updates to persist for research sample: "
                                    + sampleMetadata.getPrimaryId());
                        }
                    }
                    if (interrupted && researchSampleUpdateQueue.isEmpty()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during handling of research sample metadata update", e);
                }
            }
            researchSampleUpdateHandlerShutdownLatch.countDown();
        }
    }

    private class NewClinicalSampleMetadataHandler implements Runnable {

        final Phaser phaser;
        boolean interrupted = false;

        NewClinicalSampleMetadataHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    MetadbSample metadbSample = newClinicalSampleQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (metadbSample != null) {
                        MetadbSample existingSample = sampleService.getClinicalSampleByDmpId(
                                metadbSample.getPrimarySampleAlias());
                        if (existingSample == null) {
                            LOG.info("Clinical sample does not already exist - persisting to db: "
                                    + metadbSample.getPrimarySampleAlias());

                            sampleService.saveMetadbSample(metadbSample);
                            LOG.info("Publishing metadata history for new sample: "
                                    + metadbSample.getPrimarySampleAlias());
                            // TODO: PUBLISH HERE TO APPRORPIATE CLINICAL DATA PROCESSING TOPICS ONCE
                            // ACTUALLY SUPPORTED
                        } else if (sampleService.sampleHasMetadataUpdates(
                                existingSample.getLatestSampleMetadata(),
                                metadbSample.getLatestSampleMetadata())) {
                            LOG.info("Found updates for sample - persisting to database: "
                                    + metadbSample.getPrimarySampleAlias());
                            existingSample.updateSampleMetadata(metadbSample.getLatestSampleMetadata());
                            sampleService.saveMetadbSample(existingSample);
                            // TODO: pUBLISH TO APPROPRIATE CLINICAL DATA PROCESSING TOPIC
                            // ONCE SUPPORTED
                        } else {
                            LOG.info("There are no updates to persist for clincial sample: "
                                    + metadbSample.getPrimarySampleAlias());
                        }
                    }
                    if (interrupted && newClinicalSampleQueue.isEmpty()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during handling of new clinical sample metadata", e);
                }
            }
            newClinicalSampleHandlerShutdownLatch.countDown();
        }
    }

    private class ClinicalSampleMetadataUpdateHandler implements Runnable {

        final Phaser phaser;
        boolean interrupted = false;

        ClinicalSampleMetadataUpdateHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    MetadbSample metadbSample = clinicalSampleUpdateQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (metadbSample != null) {
                        MetadbSample existingSample = sampleService.getClinicalSampleByDmpId(
                                metadbSample.getPrimarySampleAlias());
                        if (existingSample == null) {
                            LOG.info("Clinical sample does not already exist - persisting to db: "
                                    + metadbSample.getPrimarySampleAlias());

                            sampleService.saveMetadbSample(metadbSample);
                            LOG.info("Publishing metadata history for new sample: "
                                    + metadbSample.getPrimarySampleAlias());
                            // TODO: PUBLISH HERE TO APPRORPIATE CLINICAL DATA PROCESSING TOPICS ONCE
                            // ACTUALLY SUPPORTED
                        } else if (sampleService.sampleHasMetadataUpdates(
                                existingSample.getLatestSampleMetadata(),
                                metadbSample.getLatestSampleMetadata())) {
                            LOG.info("Found updates for sample - persisting to database: "
                                    + metadbSample.getPrimarySampleAlias());
                            existingSample.updateSampleMetadata(metadbSample.getLatestSampleMetadata());
                            sampleService.saveMetadbSample(existingSample);
                            // TODO: PUBLISH TO APPROPRIATE CLINICAL DATA PROCESSING TOPIC
                            // ONCE SUPPORTED
                        } else {
                            LOG.info("There are no updates to persist for clincial sample: "
                                    + metadbSample.getPrimarySampleAlias());
                        }
                    }
                    if (interrupted && clinicalSampleUpdateQueue.isEmpty()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during handling of clinical sample metadata update", e);
                }
            }
            clinicalSampleUpdateHandlerShutdownLatch.countDown();
        }
    }

    @Override
    public void initialize(Gateway gateway) throws Exception {
        if (!initialized) {
            messagingGateway = gateway;
            setupIgoNewRequestHandler(messagingGateway, this);
            setupRequestUpdateHandler(messagingGateway, this);
            setupResearchSampleUpdateHandler(messagingGateway, this);
            setupCorrectCmoPatientIdHandler(messagingGateway, this);
            setupNewClinicalSampleHandler(messagingGateway, this);
            setupClinicalSampleUpdateHandler(messagingGateway, this);
            initializeNewRequestHandlers();
            initialized = true;
        } else {
            LOG.error("Messaging Handler Service has already been initialized, ignoring request.\n");
        }
    }

    @Override
    public void correctCmoPatientIdHandler(Map<String, String> idCorrectionMap) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            correctCmoPatientIdQueue.put(idCorrectionMap);
        } else {
            throw new IllegalStateException("Shutdown intiated, not accepting "
                    + "new CMO patient ID correction messages");
        }
    }

    @Override
    public void newRequestHandler(MetadbRequest request) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            newRequestQueue.put(request);
        } else {
            LOG.error("Shutdown initiated, not accepting request: " + request);
            throw new IllegalStateException("Shutdown initiated, not handling any more requests");
        }
    }

    @Override
    public void newClinicalSampleHandler(MetadbSample metadbSample) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            newClinicalSampleQueue.put(metadbSample);
        } else {
            LOG.error("Shutdown initiated, not accepting clinical sample: " + metadbSample);
            throw new IllegalStateException("Shutdown initiated, not handling any more clinical samples");
        }
    }

    @Override
    public void clinicalSampleUpdateHandler(MetadbSample metadbSample) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            clinicalSampleUpdateQueue.put(metadbSample);
        } else {
            LOG.error("Shutdown initiated, not accepting clinical sample: " + metadbSample);
            throw new IllegalStateException("Shutdown initiated, not handling any more clinical samples");
        }
    }

    @Override
    public void requestUpdateHandler(RequestMetadata requestMetadata) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            requestUpdateQueue.put(requestMetadata);
        } else {
            LOG.error("Shutdown initiated, not accepting request update: " + requestMetadata);
            throw new IllegalStateException("Shutdown initiated, not handling any more requests");
        }
    }

    @Override
    public void researchSampleUpdateHandler(SampleMetadata sampleMetadata) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            researchSampleUpdateQueue.put(sampleMetadata);
        } else {
            LOG.error("Shutdown initiated, not accepting research sample update: " + sampleMetadata);
            throw new IllegalStateException("Shutdown initiated, not handling any more samples");
        }
    }

    @Override
    public void shutdown() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        exec.shutdownNow();
        newRequestHandlerShutdownLatch.await();
        requestUpdateHandlerShutdownLatch.await();
        researchSampleUpdateHandlerShutdownLatch.await();
        correctCmoPatientIdShutdownLatch.await();
        newClinicalSampleHandlerShutdownLatch.await();
        clinicalSampleUpdateHandlerShutdownLatch.await();
        shutdownInitiated = true;
    }

    private void initializeNewRequestHandlers() throws Exception {
        // new request handler
        newRequestHandlerShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser newRequestPhaser = new Phaser();
        newRequestPhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            newRequestPhaser.register();
            exec.execute(new NewIgoRequestHandler(newRequestPhaser));
        }
        newRequestPhaser.arriveAndAwaitAdvance();

        // request update handler
        requestUpdateHandlerShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser requestUpdatePhaser = new Phaser();
        requestUpdatePhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            requestUpdatePhaser.register();
            exec.execute(new RequestMetadataUpdateHandler(requestUpdatePhaser));
        }
        requestUpdatePhaser.arriveAndAwaitAdvance();

        // research sample update handler
        researchSampleUpdateHandlerShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser researchSampleUpdatePhaser = new Phaser();
        researchSampleUpdatePhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            researchSampleUpdatePhaser.register();
            exec.execute(new ResearchSampleMetadataUpdateHandler(researchSampleUpdatePhaser));
        }
        researchSampleUpdatePhaser.arriveAndAwaitAdvance();

        correctCmoPatientIdShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser correctCmoPtIdPhaser = new Phaser();
        correctCmoPtIdPhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            correctCmoPtIdPhaser.register();
            exec.execute(new CorrectCmoPatientIdHandler(correctCmoPtIdPhaser));
        }
        correctCmoPtIdPhaser.arriveAndAwaitAdvance();

        // new clinical sample handler
        newClinicalSampleHandlerShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser newClinicalsamplePhaser = new Phaser();
        newClinicalsamplePhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            newClinicalsamplePhaser.register();
            exec.execute(new NewClinicalSampleMetadataHandler(newClinicalsamplePhaser));
        }
        newClinicalsamplePhaser.arriveAndAwaitAdvance();

        // clinical sample update handler
        clinicalSampleUpdateHandlerShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser clinicalsampleUpdatePhaser = new Phaser();
        clinicalsampleUpdatePhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            clinicalsampleUpdatePhaser.register();
            exec.execute(new ClinicalSampleMetadataUpdateHandler(clinicalsampleUpdatePhaser));
        }
        clinicalsampleUpdatePhaser.arriveAndAwaitAdvance();
    }

    private void setupIgoNewRequestHandler(Gateway gateway, MessageHandlingService messageHandlingService)
        throws Exception {
        gateway.subscribe(IGO_NEW_REQUEST_TOPIC, Object.class, new MessageConsumer() {
            public void onMessage(Message msg, Object message) {
                try {
                    String requestJson = mapper.readValue(
                            new String(msg.getData(), StandardCharsets.UTF_8),
                            String.class);
                    MetadbRequest request = RequestDataFactory.buildNewLimsRequestFromJson(requestJson);
                    LOG.info("Received message on topic: " + IGO_NEW_REQUEST_TOPIC + " and request id: "
                            + request.getIgoRequestId());
                    messageHandlingService.newRequestHandler(request);
                } catch (Exception e) {
                    LOG.error("Exception during processing of request on topic: " + IGO_NEW_REQUEST_TOPIC, e);
                }
            }
        });
    }

    private void setupRequestUpdateHandler(Gateway gateway, MessageHandlingService messageHandlingService)
            throws Exception {
        gateway.subscribe(IGO_REQUEST_UPDATE_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                try {
                    String requestMetadataJson = mapper.readValue(
                            new String(msg.getData(), StandardCharsets.UTF_8), String.class);
                    RequestMetadata requestMetadata =
                            RequestDataFactory.buildNewRequestMetadataFromMetadata(requestMetadataJson);
                    LOG.info("Received message on topic: "  + IGO_REQUEST_UPDATE_TOPIC + " and request id: "
                            + requestMetadata.getIgoRequestId());
                    messageHandlingService.requestUpdateHandler(requestMetadata);
                } catch (Exception e) {
                    LOG.error("Exception during processing of request update on topic: "
                            + IGO_REQUEST_UPDATE_TOPIC, e);
                }
            }
        });
    }

    private void setupResearchSampleUpdateHandler(Gateway gateway,
            MessageHandlingService messageHandlingService)
            throws Exception {
        gateway.subscribe(IGO_SAMPLE_UPDATE_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                LOG.info("Received message on topic: " + IGO_SAMPLE_UPDATE_TOPIC);
                try {
                    String sampleMetadataJson = mapper.readValue(
                            new String(msg.getData(), StandardCharsets.UTF_8), String.class);
                    SampleMetadata sampleMetadata =
                            SampleDataFactory.buildNewSampleMetadatafromJson(sampleMetadataJson);
                    messageHandlingService.researchSampleUpdateHandler(sampleMetadata);
                } catch (Exception e) {
                    LOG.error("Exception during processing of research sample update on topic: "
                            + IGO_REQUEST_UPDATE_TOPIC, e);
                }
            }
        });
    }

    private void setupCorrectCmoPatientIdHandler(Gateway gateway,
            MessageHandlingService messageHandlingService) throws Exception {
        gateway.subscribe(CORRECT_CMOPTID_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                LOG.info("Received message on topic: " + CORRECT_CMOPTID_TOPIC);
                Map<String, String> incomingDataMap = new HashMap<>();
                try {
                    // do not log contents of incoming message
                    String incomingDataString = mapper.readValue(
                            new String(msg.getData(), StandardCharsets.UTF_8),
                            String.class);
                    incomingDataMap = mapper.readValue(incomingDataString, Map.class);
                } catch (JsonProcessingException e) {
                    LOG.error("Error processing the incoming data map. "
                            + "Refer to NATS logs for more details.");
                }
                if (incomingDataMap.isEmpty()) {
                    LOG.error("Was not able to deserialize incoming message as instance of Map.class - "
                            + "please confirm manually that the expected message contents were published");
                } else {
                    String oldCmoPatientId = crdbMappingService.getCmoPatientIdByInputId(
                            incomingDataMap.get("oldId"));
                    String newCmoPatientId = crdbMappingService.getCmoPatientIdByInputId(
                            incomingDataMap.get("newId"));
                    Boolean crdbMappingStatus = Boolean.TRUE;

                    // verify that old and new ids resolve to a valid cmo patient id in crdb service
                    if (oldCmoPatientId == null || oldCmoPatientId.isEmpty()) {
                        LOG.error("Could not resolve 'old' provided patient ID to a CMO patient ID - "
                                + "please manually check the incoming message contents to verify contents");
                        crdbMappingStatus = Boolean.FALSE;
                    }
                    if (newCmoPatientId == null || newCmoPatientId.isEmpty()) {
                        LOG.error("Could not resolve 'new' provided patient ID to a CMO patient ID - "
                                + "please manually check the incoming message contents to verify contents");
                        crdbMappingStatus = Boolean.FALSE;
                    }
                    if (crdbMappingStatus) {
                        // if crdb mapping succeeded then update the incoming data map and
                        // proceed to process message by message handler
                        incomingDataMap.put("oldId", oldCmoPatientId);
                        incomingDataMap.put("newId", newCmoPatientId);
                        try {
                            messageHandlingService.correctCmoPatientIdHandler(incomingDataMap);
                        } catch (Exception e) {
                            LOG.error("Error occurred while adding the CMO Patient ID "
                                    + "correction data to message handler queue");
                        }
                    }
                }
            }
        });
    }

    private void setupNewClinicalSampleHandler(Gateway gateway, MessageHandlingService messageHandlingService)
            throws Exception {
        gateway.subscribe(NEW_DMP_SAMPLE_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                LOG.info("Received message on topic: " + NEW_DMP_SAMPLE_TOPIC);
                try {
                    String clinicalSampleJson = mapper.readValue(
                            new String(msg.getData(), StandardCharsets.UTF_8), String.class);
                    DmpSampleMetadata dmpSample = mapper.readValue(clinicalSampleJson,
                            DmpSampleMetadata.class);
                    String cmoPatientId = crdbMappingService.getCmoPatientIdbyDmpId(
                            dmpSample.getDmpPatientId());
                    MetadbSample sample = SampleDataFactory.buildNewClinicalSampleFromMetadata(
                            cmoPatientId, dmpSample);
                    messageHandlingService.newClinicalSampleHandler(sample);
                } catch (Exception e) {
                    LOG.error("Exception during processing of new clinical sample on topic: "
                            + NEW_DMP_SAMPLE_TOPIC, e);
                }
            }
        });
    }

    private void setupClinicalSampleUpdateHandler(Gateway gateway,
            MessageHandlingService messageHandlingService) throws Exception {
        gateway.subscribe(DMP_SAMPLE_UPDATE_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                LOG.info("Received message on topic: " + NEW_DMP_SAMPLE_TOPIC);
                try {
                    String clinicalSampleJson = mapper.readValue(
                            new String(msg.getData(), StandardCharsets.UTF_8), String.class);
                    DmpSampleMetadata dmpSample = mapper.readValue(clinicalSampleJson,
                            DmpSampleMetadata.class);
                    String cmoPatientId = crdbMappingService.getCmoPatientIdbyDmpId(
                            dmpSample.getDmpPatientId());
                    MetadbSample sample = SampleDataFactory.buildNewClinicalSampleFromMetadata(
                            cmoPatientId, dmpSample);
                    messageHandlingService.clinicalSampleUpdateHandler(sample);
                } catch (Exception e) {
                    LOG.error("Exception during processing of clinical sample updates on topic: "
                            + NEW_DMP_SAMPLE_TOPIC, e);
                }
            }
        });
    }
}
