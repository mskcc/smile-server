package org.mskcc.cmo.metadb.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import java.nio.charset.StandardCharsets;
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
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.dmp.DmpSampleMetadata;
import org.mskcc.cmo.metadb.service.ClinicalMessageHandlingService;
import org.mskcc.cmo.metadb.service.CrdbMappingService;
import org.mskcc.cmo.metadb.service.MetadbSampleService;
import org.mskcc.cmo.metadb.service.util.SampleDataFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClinicalMessageHandlingServiceImpl implements ClinicalMessageHandlingService {
    @Value("${metadb.dmp_new_sample_topic}")
    private String NEW_DMP_SAMPLE_TOPIC;

    @Value("${metadb.dmp_sample_update_topic}")
    private String DMP_SAMPLE_UPDATE_TOPIC;

    @Value("${num.new_request_handler_threads}")
    private int NUM_NEW_REQUEST_HANDLERS;

    private static Gateway messagingGateway;
    private static final Log LOG = LogFactory.getLog(ClinicalMessageHandlingServiceImpl.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;
    private static final ExecutorService exec = Executors.newCachedThreadPool();
    private static final BlockingQueue<MetadbSample> newClinicalSampleQueue =
            new LinkedBlockingQueue<MetadbSample>();
    private static final BlockingQueue<MetadbSample> clinicalSampleUpdateQueue =
            new LinkedBlockingQueue<MetadbSample>();

    private static CountDownLatch newClinicalSampleHandlerShutdownLatch;
    private static CountDownLatch clinicalSampleUpdateHandlerShutdownLatch;

    @Autowired
    private MetadbSampleService sampleService;

    @Autowired
    private CrdbMappingService crdbMappingService;

    @Override
    public void initialize(Gateway gateway) throws Exception {
        if (!initialized) {
            messagingGateway = gateway;
            setupNewClinicalSampleHandler(messagingGateway, this);
            setupClinicalSampleUpdateHandler(messagingGateway, this);
            initializeMessageHandlers();
            initialized = true;
        } else {
            LOG.error("Messaging Handler Service has already been initialized, ignoring request.\n");
        }
    }

    private void initializeMessageHandlers() throws Exception {
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

    private void setupNewClinicalSampleHandler(Gateway gateway, ClinicalMessageHandlingService
            clinicalMessageHandlingService) throws Exception {
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
                    clinicalMessageHandlingService.newClinicalSampleHandler(sample);
                } catch (Exception e) {
                    LOG.error("Exception during processing of new clinical sample on topic: "
                            + NEW_DMP_SAMPLE_TOPIC, e);
                }
            }
        });
    }

    private void setupClinicalSampleUpdateHandler(Gateway gateway,
            ClinicalMessageHandlingService clinicalMessageHandlingService) throws Exception {
        gateway.subscribe(DMP_SAMPLE_UPDATE_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                LOG.info("Received message on topic: " + DMP_SAMPLE_UPDATE_TOPIC);
                try {
                    String clinicalSampleJson = mapper.readValue(
                            new String(msg.getData(), StandardCharsets.UTF_8), String.class);
                    DmpSampleMetadata dmpSample = mapper.readValue(clinicalSampleJson,
                            DmpSampleMetadata.class);
                    String cmoPatientId = crdbMappingService.getCmoPatientIdbyDmpId(
                            dmpSample.getDmpPatientId());
                    MetadbSample sample = SampleDataFactory.buildNewClinicalSampleFromMetadata(
                            cmoPatientId, dmpSample);
                    clinicalMessageHandlingService.clinicalSampleUpdateHandler(sample);
                } catch (Exception e) {
                    LOG.error("Exception during processing of clinical sample updates on topic: "
                            + DMP_SAMPLE_UPDATE_TOPIC, e);
                }
            }
        });
    }

    @Override
    public void shutdown() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        exec.shutdownNow();
        newClinicalSampleHandlerShutdownLatch.await();
        clinicalSampleUpdateHandlerShutdownLatch.await();
        shutdownInitiated = true;
    }
}
