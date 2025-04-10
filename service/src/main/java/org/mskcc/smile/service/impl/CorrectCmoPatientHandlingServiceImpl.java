package org.mskcc.smile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
import org.mskcc.smile.model.PatientAlias;
import org.mskcc.smile.model.SampleMetadata;
import org.mskcc.smile.model.SmilePatient;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.Status;
import org.mskcc.smile.service.CorrectCmoPatientHandlingService;
import org.mskcc.smile.service.CrdbMappingService;
import org.mskcc.smile.service.SmilePatientService;
import org.mskcc.smile.service.SmileSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CorrectCmoPatientHandlingServiceImpl implements CorrectCmoPatientHandlingService {
    @Value("${smile.correct_cmoptid_topic}")
    private String CORRECT_CMOPTID_TOPIC;

    @Value("${smile.cmo_sample_update_topic}")
    private String CMO_SAMPLE_UPDATE_TOPIC;

    @Value("${request_reply.cmo_label_generator_topic}")
    private String CMO_LABEL_GENERATOR_REQREPLY_TOPIC;

    @Value("${num.new_request_handler_threads}")
    private int NUM_NEW_REQUEST_HANDLERS;

    @Autowired
    private CrdbMappingService crdbMappingService;

    @Autowired
    private SmilePatientService patientService;

    @Autowired
    private SmileSampleService sampleService;

    private static Gateway messagingGateway;
    private static final Log LOG = LogFactory.getLog(CorrectCmoPatientHandlingServiceImpl.class);
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
            initializeMessageHandlers();
            initialized = true;
        } else {
            LOG.error("Messaging Handler Service has already been initialized, ignoring request.");
        }
    }

    private void initializeMessageHandlers() throws Exception {
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

                        // start with updating/merging old patient aliases to the new patient
                        // that we are swapping to
                        // before deleting it forever though we will check for aliases that are not-CMO ID
                        // and merge them to the patient to swap to....
                        SmilePatient patientByOldId =
                                patientService.getPatientByCmoPatientId(oldCmoPtId);
                        SmilePatient patientByNewId =
                                patientService.getPatientByCmoPatientId(newCmoPtId);
                        if (patientByNewId != null) {
                            for (PatientAlias pa : patientByOldId.getPatientAliases()) {
                                if (pa.getNamespace().equalsIgnoreCase("cmoId")) {
                                    continue;
                                }
                                // if patient by new id doesn't already have current alias
                                // then go ahead and add as a new instance of PatientAlias so as to
                                // not recycle node ids.
                                if (!patientByNewId.hasPatientAlias(pa.getNamespace())) {
                                    patientByNewId.addPatientAlias(new PatientAlias(pa.getValue(),
                                            pa.getNamespace()));
                                }
                            }
                            patientService.savePatientMetadata(patientByNewId);
                        }

                        // grab samples by old cmo patient id and new cmo patient id that
                        // we are correcting or swapping to
                        List<SmileSample> samplesByOldCmoPatient =
                                sampleService.getSamplesByCmoPatientId(oldCmoPtId);
                        List<SmileSample> samplesByNewCmoPatient =
                                sampleService.getSamplesByCmoPatientId(newCmoPtId);

                        // update the cmo patient id for each sample linked to the "old" patient
                        // and the metadata as well
                        for (SmileSample sample : samplesByOldCmoPatient) {
                            SampleMetadata updatedMetadata = sample.getLatestSampleMetadata();
                            updatedMetadata.setCmoPatientId(newCmoPtId);
                            updatedMetadata.setStatus(new Status());

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
                            // update the sample with the new metadata which should now reference
                            // the cmo patient id we are swapping to
                            sample.updateSampleMetadata(updatedMetadata);
                            sampleService.saveSmileSample(sample);
                        }

                        // sample service is handling patient swapping now so we can simply rely on deleting
                        // this old patient node that shouldn't have any samples attached to it anymore
                        patientService.deletePatient(patientByOldId);

                        // sanity check the counts before and after the swaps
                        Integer expectedCount = samplesByOldCmoPatient.size()
                                + samplesByNewCmoPatient.size();
                        List<SmileSample> samplesAfterSwap =
                                sampleService.getSamplesByCmoPatientId(newCmoPtId);
                        if (expectedCount != samplesAfterSwap.size()) {
                            LOG.error("Expected sample count after patient ID swap does not match actual"
                                    + " count: " + expectedCount + " != " + samplesAfterSwap.size());
                        } else {
                            // collect unique set of sample smile ids that we can skip publishing updates for
                            Set<UUID> samplesToSkipPublishingUpdatesFor = new HashSet<>();
                            for (SmileSample s : samplesByOldCmoPatient) {
                                samplesToSkipPublishingUpdatesFor.add(s.getSmileSampleId());
                            }

                            // publish sample updates only for samples that actually got patient swapped
                            List<SmileSample> samplesByNewCmoPatientAfterCorrection =
                                    sampleService.getSamplesByCmoPatientId(newCmoPtId);
                            for (SmileSample sample : samplesByNewCmoPatientAfterCorrection) {
                                if (samplesToSkipPublishingUpdatesFor.contains(sample.getSmileSampleId())) {
                                    continue;
                                }
                                // publish sampleMetadata history to CMO_SAMPLE_UPDATE_TOPIC
                                LOG.info("Publishing sample-level metadata history for sample: "
                                        + sample.getLatestSampleMetadata().getPrimaryId());
                                messagingGateway.publish(CMO_SAMPLE_UPDATE_TOPIC,
                                        mapper.writeValueAsString(sample.getSampleMetadataList()));
                            }
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
            CorrectCmoPatientHandlingService correctCmoPatientHandlingService) throws Exception {
        gateway.subscribe(CORRECT_CMOPTID_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                LOG.info("Received message on topic: " + CORRECT_CMOPTID_TOPIC);
                Map<String, String> incomingDataMap = new HashMap<>();
                String oldCmoPatientId = null;
                String newCmoPatientId = null;
                try {
                    String incomingDataString = mapper.readValue(
                            new String(msg.getData(), StandardCharsets.UTF_8),
                            String.class);
                    incomingDataMap = mapper.readValue(incomingDataString, Map.class);
                    oldCmoPatientId = crdbMappingService.getCmoPatientIdByInputId(
                                incomingDataMap.get("oldId"));
                    newCmoPatientId = crdbMappingService.getCmoPatientIdByInputId(
                                incomingDataMap.get("newId"));
                } catch (Exception e) {
                    LOG.error("Error parsing the incoming data map: ", e);
                }
                if (incomingDataMap.isEmpty()) {
                    LOG.error("Was not able to deserialize incoming message as instance of Map.class - "
                            + "please confirm manually that the expected message contents were published");
                } else {
                    Boolean crdbMappingStatus = Boolean.TRUE;
                    // verify that old and new ids resolve to a valid cmo patient id in crdb service
                    if (oldCmoPatientId == null || oldCmoPatientId.isEmpty()) {
                        StringBuilder logMessage = new StringBuilder();
                        logMessage.append("Could not resolve 'old' provided patient "
                                + "ID to a CMO patient ID - ");
                        if (incomingDataMap.get("oldId").startsWith("C-")) {
                            logMessage.append("using provided ID as is since it has a "
                                    + "CMO patient ID prefix");
                            LOG.warn(logMessage.toString());
                            oldCmoPatientId = incomingDataMap.get("oldId");
                        } else {
                            logMessage.append("please manually check the incoming message "
                                    + "contents to verify contents");
                            LOG.error(logMessage.toString());
                            crdbMappingStatus = Boolean.FALSE;
                        }
                    }
                    if (newCmoPatientId == null || newCmoPatientId.isEmpty()) {
                        StringBuilder logMessage = new StringBuilder();
                        logMessage.append("Could not resolve 'new' provided patient "
                                + "ID to a CMO patient ID - ");
                        if (incomingDataMap.get("newId").startsWith("C-")) {
                            logMessage.append("using provided ID as is since it has a "
                                    + "CMO patient ID prefix");
                            LOG.warn(logMessage.toString());
                            newCmoPatientId = incomingDataMap.get("newId");
                        } else {
                            logMessage.append("please manually check the incoming message "
                                    + "contents to verify contents");
                            LOG.error(logMessage.toString());
                            crdbMappingStatus = Boolean.FALSE;
                        }
                    }
                    if (crdbMappingStatus) {
                        // if crdb mapping succeeded then update the incoming data map and
                        // proceed to process message by message handler
                        incomingDataMap.put("oldId", oldCmoPatientId);
                        incomingDataMap.put("newId", newCmoPatientId);
                        try {
                            correctCmoPatientHandlingService.correctCmoPatientIdHandler(incomingDataMap);
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
