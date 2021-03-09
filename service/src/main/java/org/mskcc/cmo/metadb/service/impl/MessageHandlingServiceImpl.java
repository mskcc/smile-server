package org.mskcc.cmo.metadb.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.messaging.MessageConsumer;
import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.service.MessageHandlingService;
import org.mskcc.cmo.metadb.service.MetaDbRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MessageHandlingServiceImpl implements MessageHandlingService {

    @Value("${igo.new_request_topic}")
    private String IGO_NEW_REQUEST_TOPIC;

    @Value("${cmo.new_request_topic}")
    private String CMO_NEW_REQUEST_TOPIC;

    @Value("${num.new_request_handler_threads}")
    private int NUM_NEW_REQUEST_HANDLERS;

    @Autowired
    private MetaDbRequestService requestService;

    private final ObjectMapper mapper = new ObjectMapper();
    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;
    private static final ExecutorService exec = Executors.newCachedThreadPool();
    private static final BlockingQueue<MetaDbRequest> newRequestQueue =
        new LinkedBlockingQueue<MetaDbRequest>();
    private static CountDownLatch newRequestHandlerShutdownLatch;
    private static Gateway messagingGateway;

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
                    MetaDbRequest request = newRequestQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (request != null) {
                        requestService.saveRequest(request);
                        messagingGateway.publish(CMO_NEW_REQUEST_TOPIC,
                                mapper.writeValueAsString(
                                        requestService.getMetaDbRequest(request.getRequestId())));
                    }
                    if (interrupted && newRequestQueue.isEmpty()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    System.err.printf("Error during request handling: %s\n", e.getMessage());
                    e.printStackTrace();
                }
            }
            newRequestHandlerShutdownLatch.countDown();
        }
    }

    @Override
    public void initialize(Gateway gateway) throws Exception {
        if (!initialized) {
            messagingGateway = gateway;
            setupIgoNewRequestHandler(messagingGateway, this);
            initializeNewRequestHandlers();
            initialized = true;
        } else {
            System.err.printf("Messaging Handler Service has already been initialized, ignoring request.\n");
        }
    }

    @Override
    public void newRequestHandler(MetaDbRequest request) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            newRequestQueue.put(request);
        } else {
            System.err.printf("Shutdown initiated, not accepting request: %s\n", request);
            throw new IllegalStateException("Shutdown initiated, not handling any more requests");
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

    private void initializeNewRequestHandlers() throws Exception {
        newRequestHandlerShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser newSamplePhaser = new Phaser();
        newSamplePhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            newSamplePhaser.register();
            exec.execute(new NewIgoRequestHandler(newSamplePhaser));
        }
        newSamplePhaser.arriveAndAwaitAdvance();
    }

    private void setupIgoNewRequestHandler(Gateway gateway, MessageHandlingService messageHandlingService)
        throws Exception {
        gateway.subscribe(IGO_NEW_REQUEST_TOPIC, Object.class, new MessageConsumer() {
            public void onMessage(Object message) {
                try {
                    MetaDbRequest metaDbRequest = mapper.readValue(message.toString(),
                            MetaDbRequest.class);
                    metaDbRequest.setMetaDbSampleList(extractMetaDbSamplesFromIgoResponse(message));
                    metaDbRequest.setNamespace("igo");
                    messageHandlingService.newRequestHandler(metaDbRequest);
                } catch (Exception e) {
                    System.err.printf("Cannot process IGO_NEW_REQUEST:\n%s\n", message);
                    System.err.printf("Exception during processing:\n%s\n", e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private List<MetaDbSample> extractMetaDbSamplesFromIgoResponse(Object message)
            throws JsonProcessingException, IOException {
        Map<String, Object> map = mapper.readValue(message.toString(), Map.class);
        ObjectMapper mapper = new ObjectMapper();
        SampleMetadata[] sampleList = mapper.convertValue(map.get("samples"),
                SampleMetadata[].class);
        List<MetaDbSample> metaDbSampleList = new ArrayList<>();
        for (SampleMetadata sample: sampleList) {
            // update import date here since we are parsing from json
            sample.setImportDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

            MetaDbSample metaDbSample = new MetaDbSample();
            metaDbSample.addSampleMetadata(sample);
            metaDbSampleList.add(metaDbSample);
        }
        return metaDbSampleList;
    }
}
