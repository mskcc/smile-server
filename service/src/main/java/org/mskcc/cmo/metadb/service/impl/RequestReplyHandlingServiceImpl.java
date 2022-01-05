package org.mskcc.cmo.metadb.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import java.util.List;
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
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.service.CrdbMappingService;
import org.mskcc.cmo.metadb.service.MetadbSampleService;
import org.mskcc.cmo.metadb.service.RequestReplyHandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RequestReplyHandlingServiceImpl implements RequestReplyHandlingService {

    @Value("${request_reply.patient_samples_topic}")
    private String PATIENT_SAMPLES_REQREPLY_TOPIC;

    @Value("${request_reply.crdb_mapping_topic}")
    private String CRDB_MAPPING_REQREPLY_TOPIC;

    @Value("${num.new_request_handler_threads}")
    private int NUM_NEW_REQUEST_HANDLERS;

    @Autowired
    private MetadbSampleService sampleService;

    @Autowired
    private CrdbMappingService crdbMappingService;

    private final ObjectMapper mapper = new ObjectMapper();
    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;
    private static final ExecutorService exec = Executors.newCachedThreadPool();
    private static final BlockingQueue<ReplyInfo> patientSamplesReqReplyQueue =
        new LinkedBlockingQueue<ReplyInfo>();
    private static final BlockingQueue<ReplyInfo> crdbMappingReqReplyQueue =
        new LinkedBlockingQueue<ReplyInfo>();
    private static CountDownLatch patientSamplesHandlerShutdownLatch;
    private static CountDownLatch crdbMappingHandlerShutdownLatch;
    private static Gateway messagingGateway;

    private static final Log LOG = LogFactory.getLog(RequestReplyHandlingServiceImpl.class);

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

    private class PatientSamplesReqReplyHandler implements Runnable {

        final Phaser phaser;
        boolean interrupted = false;

        PatientSamplesReqReplyHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    ReplyInfo replyInfo = patientSamplesReqReplyQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (replyInfo != null) {
                        List<SampleMetadata> sampleList =
                                sampleService.getAllSampleMetadataByCmoPatientId(
                                        replyInfo.getRequestMessage());
                        messagingGateway.replyPublish(replyInfo.getReplyTo(),
                                mapper.writeValueAsString(sampleList));
                    }
                    if (interrupted && patientSamplesReqReplyQueue.isEmpty()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during request handling", e);
                }
            }
            patientSamplesHandlerShutdownLatch.countDown();
        }
    }

    private class CrdbMappingReqReplyHandler implements Runnable {

        final Phaser phaser;
        boolean interrupted = false;

        CrdbMappingReqReplyHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    ReplyInfo replyInfo = crdbMappingReqReplyQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (replyInfo != null) {
                        try {
                            String cmoPatientId =
                                    crdbMappingService.getCmoPatientIdbyDmpId(replyInfo.getRequestMessage());
                            messagingGateway.replyPublish(replyInfo.getReplyTo(),
                                    cmoPatientId);
                        } catch (NullPointerException e) {
                            LOG.error("CRDB service returned null for dmp id: "
                                    + replyInfo.getRequestMessage());
                        }
                    }
                    if (interrupted && crdbMappingReqReplyQueue.isEmpty()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during request handling", e);
                }
            }
            crdbMappingHandlerShutdownLatch.countDown();
        }
    }

    @Override
    public void initialize(Gateway gateway) throws Exception {
        if (!initialized) {
            messagingGateway = gateway;
            setupPatientSamplesHandler(messagingGateway, this);
            setupCrdbMappingHandler(messagingGateway, this);
            initializeRequestReplyHandlers();
            initialized = true;
        } else {
            LOG.error("Messaging Handler Service has already been initialized, ignoring patientId.\n");
        }
    }

    @Override
    public void patientSamplesHandler(String patientId, String replyTo) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            patientSamplesReqReplyQueue.put(new ReplyInfo(patientId, replyTo));
        } else {
            LOG.error("Shutdown initiated, not accepting PatientIds: " + patientId);
            throw new IllegalStateException("Shutdown initiated, not handling any more patientIds");
        }
    }

    @Override
    public void crdbMappingHandler(String inputId, String replyTo) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            crdbMappingReqReplyQueue.put(new ReplyInfo(inputId, replyTo));
        } else {
            LOG.error("Shutdown initiated, not accepting CRDB Mapping Service request");
            throw new IllegalStateException("Shutdown initiated, not handling any more patientIds");
        }
    }

    @Override
    public void shutdown() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        exec.shutdownNow();
        patientSamplesHandlerShutdownLatch.await();
        crdbMappingHandlerShutdownLatch.await();
        shutdownInitiated = true;
    }

    private void setupPatientSamplesHandler(Gateway gateway,
            RequestReplyHandlingServiceImpl requestReplyHandlingServiceImpl)
            throws Exception {
        gateway.replySub(PATIENT_SAMPLES_REQREPLY_TOPIC, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                LOG.info("Received message on topic: " + PATIENT_SAMPLES_REQREPLY_TOPIC);
                try {
                    requestReplyHandlingServiceImpl.patientSamplesHandler(
                            new String(msg.getData()), msg.getReplyTo());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setupCrdbMappingHandler(Gateway gateway,
            RequestReplyHandlingServiceImpl requestReplyHandlingServiceImpl)
            throws Exception {
        gateway.replySub(CRDB_MAPPING_REQREPLY_TOPIC, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                LOG.info("Received message on topic: " + CRDB_MAPPING_REQREPLY_TOPIC);
                try {
                    requestReplyHandlingServiceImpl.crdbMappingHandler(
                            new String(msg.getData()), msg.getReplyTo());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initializeRequestReplyHandlers() throws Exception {
        patientSamplesHandlerShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser patientSamplesPhaser = new Phaser();
        patientSamplesPhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            patientSamplesPhaser.register();
            exec.execute(new PatientSamplesReqReplyHandler(patientSamplesPhaser));
        }
        patientSamplesPhaser.arriveAndAwaitAdvance();

        crdbMappingHandlerShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser crdbMappingPhaser = new Phaser();
        crdbMappingPhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            crdbMappingPhaser.register();
            exec.execute(new CrdbMappingReqReplyHandler(crdbMappingPhaser));
        }
        crdbMappingPhaser.arriveAndAwaitAdvance();
    }

}
