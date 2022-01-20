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
import org.mskcc.cmo.metadb.service.AdminMessageHandlingService;
import org.mskcc.cmo.metadb.service.CrdbMappingService;
import org.mskcc.cmo.metadb.service.MetadbPatientService;
import org.mskcc.cmo.metadb.service.MetadbSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author ochoaa
 */
@Component
public class AdminMessageHandlingServiceImpl implements AdminMessageHandlingService {

    @Value("${metadb.correct_cmoptid_topic}")
    private String ADMIN_CORRECT_CMOPTID_TOPIC;

    @Value("${metadb.cmo_sample_label_update_topic}")
    private String CMO_SAMPLE_LABEL_UPDATE;

    @Value("${num.new_request_handler_threads}")
    private int NUM_NEW_REQUEST_HANDLERS;

    @Autowired
    private CrdbMappingService crdbMappingService;

    @Autowired
    private MetadbPatientService patientService;

    @Autowired
    private MetadbSampleService sampleService;

    private final ObjectMapper mapper = new ObjectMapper();
    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;
    private static final ExecutorService exec = Executors.newCachedThreadPool();
    private static final BlockingQueue<Map<String, String>> correctCmoPatientIdQueue =
            new LinkedBlockingQueue<Map<String, String>>();
    private static CountDownLatch correctCmoPatientIdShutdownLatch;
    private static Gateway messagingGateway;

    private static final Log LOG = LogFactory.getLog(AdminMessageHandlingServiceImpl.class);

    private class CorrectCmoPatientIdReqReplyHandler implements Runnable {
        final Phaser phaser;
        boolean interrupted = false;

        CorrectCmoPatientIdReqReplyHandler(Phaser phaser) {
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
                        String oldCmoPatientId = idCorrectionMap.get("oldId");
                        String newCmoPatientId = idCorrectionMap.get("newId");

                        // get samples by old cmo patient id before updating the
                        // cmo patient id for the given patient alias/patient node
                        List<MetadbSample> samples =
                                sampleService.getMetadbSampleListByCmoPatientId(oldCmoPatientId);
                        MetadbPatient updatedPatient = patientService.updateCmoPatientId(
                                oldCmoPatientId, newCmoPatientId);

                        for (MetadbSample sample: samples) {
                            // TODO: add support for clinical sample updates
                            // publish research samples only to label generator
                            if (sample.getSampleCategory().equals("research")) {
                                SampleMetadata latestMetadata = sample.getLatestSampleMetadata();
                                latestMetadata.setCmoPatientId(newCmoPatientId);
                                LOG.info("Publishing sample with updated CMO patient ID to "
                                        + "label-generator service (CMO_SAMPLE_LABEL_UPDATE)");
                                messagingGateway.publish(CMO_SAMPLE_LABEL_UPDATE,
                                        mapper.writeValueAsString(latestMetadata));
                            } else if (sample.getSampleCategory().equals("clinical")) {
                                LOG.info("CLINICAL SAMPLE UPDATES NOT SUPPORTED YET");
                            }
                        }
                        LOG.info("CMO sample label should be updated shortly through the "
                                + "IGO_SAMPLE_UPDATE topic");
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
    public void shutdown() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        exec.shutdownNow();
        correctCmoPatientIdShutdownLatch.await();
        shutdownInitiated = true;
    }

    private void initializeMessageHandlers() throws Exception {
        correctCmoPatientIdShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser correctCmoPtIdPhaser = new Phaser();
        correctCmoPtIdPhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            correctCmoPtIdPhaser.register();
            exec.execute(new CorrectCmoPatientIdReqReplyHandler(correctCmoPtIdPhaser));
        }
        correctCmoPtIdPhaser.arriveAndAwaitAdvance();
    }

    private void setupCorrectCmoPatientIdHandler(Gateway gateway,
            AdminMessageHandlingService adminMsgHandlingService) throws Exception {
        gateway.subscribe(ADMIN_CORRECT_CMOPTID_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                LOG.info("Received message on topic: " + ADMIN_CORRECT_CMOPTID_TOPIC);
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
                            adminMsgHandlingService.correctCmoPatientIdHandler(incomingDataMap);
                        } catch (Exception e) {
                            LOG.error("Error occurred while adding the CMO Patient ID "
                                    + "correction data to message handler queue");
                        }
                    }
                }
            }
        });
    }
}
