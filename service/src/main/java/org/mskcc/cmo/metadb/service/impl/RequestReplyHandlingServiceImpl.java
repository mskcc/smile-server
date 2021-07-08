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
import org.mskcc.cmo.metadb.service.RequestReplyHandlingService;
import org.mskcc.cmo.metadb.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RequestReplyHandlingServiceImpl implements RequestReplyHandlingService {
    
    @Value("${request_reply.patient_samples_topic}")
    private String REQUEST_REPLY_TOPIC;

    @Value("${num.new_request_handler_threads}")
    private int NUM_NEW_REQUEST_HANDLERS;

    @Autowired
    private SampleService sampleService;

    private final ObjectMapper mapper = new ObjectMapper();
    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;
    private static final ExecutorService exec = Executors.newCachedThreadPool();
    private static final BlockingQueue<ReplyInfo> newRequestReplyQueue =
        new LinkedBlockingQueue<ReplyInfo>();
    private static CountDownLatch newRequestHandlerShutdownLatch;
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
    
    private class NewRequestReplyHandler implements Runnable {

        final Phaser phaser;
        boolean interrupted = false;

        NewRequestReplyHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    ReplyInfo replyInfo = newRequestReplyQueue.poll(100, TimeUnit.MILLISECONDS);
                    if ((replyInfo != null)) {
                        List<SampleMetadata> sampleList =
                                sampleService.getSampleMetadataListByCmoPatientId(
                                        replyInfo.getRequestMessage());
                        messagingGateway.replyPublish(replyInfo.getReplyTo(),
                                mapper.writeValueAsString(sampleList));
                    } 
                    if (interrupted && newRequestReplyQueue.isEmpty()) {
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


    @Override
    public void initialize(Gateway gateway) throws Exception {
        if (!initialized) {
            messagingGateway = gateway;
            setupPatientSamplesHandler(messagingGateway, this);
            initializePatientSamplesHandlers();
            initialized = true;
        } else {
            LOG.error("Messaging Handler Service has already been initialized, ignoring patientId.\n");
        }        
    }

    @Override
    public void newPatientSamplesHandler(String patientId, String replyTo) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            newRequestReplyQueue.put(new ReplyInfo(patientId, replyTo));

        } else {
            LOG.error("Shutdown initiated, not accepting PatientIds: " + patientId);
            throw new IllegalStateException("Shutdown initiated, not handling any more patientIds");
        }
        
    }

    @Override
    public void shutdown() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        exec.shutdownNow();
        newRequestHandlerShutdownLatch.await();
        shutdownInitiated = true;        
    }
    
    private void setupPatientSamplesHandler(Gateway gateway,
            RequestReplyHandlingServiceImpl requestReplyHandlingServiceImpl)
            throws Exception {
        gateway.replySub(REQUEST_REPLY_TOPIC, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                LOG.info("Received message on topic: " + REQUEST_REPLY_TOPIC);
                try {
                    requestReplyHandlingServiceImpl.newPatientSamplesHandler(
                            new String(msg.getData()), msg.getReplyTo());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });    
    }
    
    private void initializePatientSamplesHandlers() throws Exception {
        newRequestHandlerShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser newSamplePhaser = new Phaser();
        newSamplePhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            newSamplePhaser.register();
            exec.execute(new NewRequestReplyHandler(newSamplePhaser));
        }
        newSamplePhaser.arriveAndAwaitAdvance();
    }
    
}
