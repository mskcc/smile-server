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

    @Value("${mdb_admin.correct_cmoptid_topic}")
    private String ADMIN_CORRECT_CMOPTID_TOPIC;
    
    @Value("${mdb_admin.cmo_sample_label_update_topic}")
    private String CMO_SAMPLE_LABEL_UPDATE;

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

    private class ReplyInfo {
        String requestMessage;
        String replyTo;

        ReplyInfo(String requestMessage, String replyTo) {
            this.requestMessage = requestMessage;
            this.replyTo = replyTo;
        }

        String getRequestMessage() {
            return requestMessage;
        }

        String getReplyTo() {
            return replyTo;
        }
    }

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
                        List<SampleMetadata> sampleMetadataList = sampleService
                                .getSampleMetadataListByCmoPatientId(oldCmoPatientId);
                        MetadbPatient updatedPatient = patientService.updateCmoPatientId(
                                oldCmoPatientId, newCmoPatientId);
                        
                        for (SampleMetadata sample: sampleMetadataList) {
                            sample.setCmoPatientId(newCmoPatientId);
                            LOG.info("Publishing updated sample metadata: "
                                    + sample.getPrimaryId());
                            //check if sample is research or clinical, then publish to their respective topics
                            // Topic to push updated research samples is yet to be set up
                            // maybe CMO_SAMPLE_LABEL_UPDATE_TOPIC?
                            messagingGateway.publish(CMO_SAMPLE_LABEL_UPDATE,
                                    mapper.writeValueAsString(sample));
                        }
                        
                        LOG.info("Old cmo sample label is updated to the new label,"
                                + " and should be persisted in the database shortly");
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

    private void setupCorrectCmoPatientIdHandler(Gateway gateway,
            AdminMessageHandlingService adminMsgHandlingService) throws Exception {
        gateway.subscribe(ADMIN_CORRECT_CMOPTID_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                LOG.info("Received message on topic: " + ADMIN_CORRECT_CMOPTID_TOPIC);
                Map<String, String> incomingDataMap = new HashMap<>();
                try {
                    incomingDataMap = mapper.readValue(
                            new String(msg.getData(), StandardCharsets.UTF_8), Map.class);
                    // do the crdb mapping service information exchange here
                } catch (JsonProcessingException e) {
                    LOG.error("Error processing the incoming data mao ... more details?");
                    // do not log output of json processing exception due
                    // to potentially sensitive information
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
