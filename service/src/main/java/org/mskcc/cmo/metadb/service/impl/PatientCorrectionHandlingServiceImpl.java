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
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.service.CrdbMappingService;
import org.mskcc.cmo.metadb.service.MetadbPatientService;
import org.mskcc.cmo.metadb.service.MetadbSampleService;
import org.mskcc.cmo.metadb.service.PatientCorrectionHandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PatientCorrectionHandlingServiceImpl implements PatientCorrectionHandlingService {
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
    private MetadbSampleService sampleService;
    
    private static Gateway messagingGateway;
    private static final Log LOG = LogFactory.getLog(PatientCorrectionHandlingServiceImpl.class);
    private final ObjectMapper mapper = new ObjectMapper();
    
    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;
    private static final ExecutorService exec = Executors.newCachedThreadPool();
    private static final BlockingQueue<Map<String, String>> correctCmoPatientIdQueue =
            new LinkedBlockingQueue<Map<String, String>>();
    private static CountDownLatch correctCmoPatientIdShutdownLatch;

    @Override
    public void initialize(Gateway gateway) throws Exception {
        if (!initialized) {
            messagingGateway = gateway;
            setupCorrectCmoPatientIdHandler(messagingGateway, this);
            initializeNewMessageHandlers();
            initialized = true;
        } else {
            LOG.error("Messaging Handler Service has already been initialized, ignoring request.\n");
        }        
    }
    
    private void initializeNewMessageHandlers() throws Exception {
        // Correct CmoPatientId Handler
        correctCmoPatientIdShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser correctCmoPtIdPhaser = new Phaser();
        correctCmoPtIdPhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            correctCmoPtIdPhaser.register();
            exec.execute(new CorrectCmoPatientIdHandler(correctCmoPtIdPhaser));
        }
        correctCmoPtIdPhaser.arriveAndAwaitAdvance();
    }
    
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
    
    private void setupCorrectCmoPatientIdHandler(Gateway gateway,
            PatientCorrectionHandlingService patientCorrectionHandlingService) throws Exception {
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
                            patientCorrectionHandlingService.correctCmoPatientIdHandler(incomingDataMap);
                        } catch (Exception e) {
                            LOG.error("Error occurred while adding the CMO Patient ID "
                                    + "correction data to message handler queue");
                        }
                    }
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
        correctCmoPatientIdShutdownLatch.await();
        shutdownInitiated = true;        
    }
}
