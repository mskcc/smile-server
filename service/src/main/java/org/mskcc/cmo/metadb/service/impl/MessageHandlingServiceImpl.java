package org.mskcc.cmo.metadb.service.impl;

import com.google.gson.Gson;
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
import org.mskcc.cmo.metadb.model.neo4j.MetaDbRequest;
import org.mskcc.cmo.metadb.model.neo4j.MetaDbSample;
import org.mskcc.cmo.metadb.model.neo4j.SampleManifestEntity;
import org.mskcc.cmo.metadb.service.CmoRequestService;
import org.mskcc.cmo.metadb.service.MessageHandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MessageHandlingServiceImpl implements MessageHandlingService {

    @Value("${igo.new_request_topic}")
    private String IGO_NEW_REQUEST_TOPIC;

    @Value("${num.new_request_handler_threads}")
    private int NUM_NEW_REQUEST_HANDLERS;

    @Autowired
    private CmoRequestService requestService;

    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;

    private static final ExecutorService exec = Executors.newCachedThreadPool();
    private static final BlockingQueue<MetaDbRequest> newRequestQueue =
        new LinkedBlockingQueue<MetaDbRequest>();
    private static CountDownLatch newRequestHandlerShutdownLatch;

    private class NewCmoRequestHandler implements Runnable {

        final Phaser phaser;
        boolean interrupted = false;

        NewCmoRequestHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    MetaDbRequest request = newRequestQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (request != null) {
                        // validate
                        // id gen
                        // persist
                        Gson gson = new Gson();
                        System.out.println("This is where we would persist the request to neo4j...");
                        System.out.println(gson.toJson(request).toString());
                        requestService.saveRequest(request);
                        // pass to aggregate
                    }
                    if (interrupted && newRequestQueue.isEmpty()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    // TBD requeue?
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
            setupIgoNewRequestHandler(gateway, this);
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
            exec.execute(new NewCmoRequestHandler(newSamplePhaser));
        }
        newSamplePhaser.arriveAndAwaitAdvance();
    }

    private void setupIgoNewRequestHandler(Gateway gateway, MessageHandlingService messageHandlingService)
        throws Exception {
        gateway.subscribe(IGO_NEW_REQUEST_TOPIC, Object.class, new MessageConsumer() {
            public void onMessage(Object message) {
                try {
                    Gson gson = new Gson();
                    MetaDbRequest metaDbRequest = gson.fromJson(message.toString(),
                            MetaDbRequest.class);
                    metaDbRequest.setMetaDbSampleList(extractMetaDbSamplesFromIgoResponse(message));
                    metaDbRequest.setIdSource("igo");
                    messageHandlingService.newRequestHandler(metaDbRequest);
                } catch (Exception e) {
                    System.err.printf("Cannot process IGO_NEW_REQUEST:\n%s\n", message);
                    System.err.printf("Exception during processing:\n%s\n", e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
    
    private List<MetaDbSample> extractMetaDbSamplesFromIgoResponse(Object message) {
        Gson gson = new Gson();
        Map<String, Object> map = gson.fromJson(message.toString(), Map.class);
        SampleManifestEntity[] sampleList = gson.fromJson(gson.toJson(
                map.get("sampleManifestList")), SampleManifestEntity[].class);
        List<MetaDbSample> metaDbSampleList = new ArrayList<>();
        for (SampleManifestEntity sample: sampleList) {
            MetaDbSample metaDbSample = new MetaDbSample();
            metaDbSample.addSampleManifest(sample);
            metaDbSampleList.add(metaDbSample);
        }
        return metaDbSampleList;
    }
}
