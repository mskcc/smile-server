package org.mskcc.cmo.metadb.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
import org.mskcc.cmo.metadb.service.AdminMessageHandlingService;
import org.mskcc.cmo.metadb.service.CrdbMappingService;
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

    @Value("${request_reply.cmo_label_update_topic}")
    private String CMO_LABEL_UPDATE_REQREPLY_TOPIC;

    @Autowired
    private CrdbMappingService crdbMappingService;

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
                    // TODO:
                    // database operations
                    //      1. find patient node matching old id and update with new id
                    //      2. get all RESEARCH samples linked to affected patient
                    // publish each research sample as a sample metadata json to the CMO LABEL UPDATE topic
                    // for label generator to make a new label
                    if (idCorrectionMap != null) {
                        // parse the old cmo patient id and new cmo patient id from map
                        System.out.println("TODO: PARSE OLD AND NEW CMO PATIENT ID FROM REPLYINFO");
                        // make updates to the database
                        // return some sort of indicator that tells us if patient ids were
                        // successfully mapped or not
                        System.out.println("TODO: MAKE CORRECTIONS TO CMO PATIENT ID");
                        // for each RESEARCH sample that was linked to the affected cmo patient node
                        // we need to publish to the CMO_LABEL_UPDATE request-reply topic
                        // and in return we receive an updated cmo label
                        // label generator is also publishing the updated sample metadata to
                        // IGO_SAMPLE_METADATA_UPDATE which will be handled by the existing
                        // metadb sample metadata update subscriber that will persist the updates
                        // to the database - we dont need to do anything with the received
                        // label at this point other than log that a new one has been generated
                        // and that the updated metadata will go through the appropriate
                        // sample update message handling
                        System.out.println("TODO: PUBLISH EACH SAMPLE TO THE CMO LABEL"
                                + "UPDATE TOPIC");
                        // this is a request-reply topic that also publishes to the
                        // sample metadata update topic that is handled by metadb so
                        // what we can do here is simply log that the label change from
                        // "old label" --> "new label" should be persisted shortly
                        System.out.println("TODO: LOG THAT OLD LABEL IS BEING UPDATED TO "
                                + "NEW LABEL AND SHOULD BE PERSISTED IN DB SHORTLY");
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
