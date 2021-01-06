package org.mskcc.cmo.metadb.service.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.messaging.MessageConsumer;
import org.mskcc.cmo.metadb.service.MessageHandlingService;
import org.mskcc.cmo.metadb.service.RequestService;
import org.mskcc.cmo.shared.neo4j.CmoRequestEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MessageHandlingServiceImpl implements MessageHandlingService {

    @Value("${igo.new_request_topic}")
    private String IGO_NEW_REQUEST;

    //@Value("${num.new.request.handler.threads}")
    //private int NUM_NEW_HANDLERS;
    
    @Autowired
    private RequestService requestService;

    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;

    private static final ExecutorService exec = Executors.newCachedThreadPool();
    private static final BlockingQueue<CmoRequestEntity> newRequestQueue =
            new LinkedBlockingQueue<CmoRequestEntity>();
    private static CountDownLatch newRequestHandlerShutdownLatch;

    private class NewRequestHandler implements Runnable {

        final Phaser phaser;
        boolean interrupted = false;

        NewRequestHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    CmoRequestEntity request = newRequestQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (request != null) {
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
    public void newRequestHandler(CmoRequestEntity request) throws Exception {
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
        //newRequestHandlerShutdownLatch = new CountDownLatch(NUM_NEW_HANDLERS);
        final Phaser newRequestPhaser = new Phaser();
        newRequestPhaser.register();
        //for (int lc = 0; lc < NUM_NEW_HANDLERS; lc++) {
        //    newRequestPhaser.register();
        //    exec.execute(new NewRequestHandler(newRequestPhaser));
        //}
        newRequestPhaser.arriveAndAwaitAdvance();
    }
    
    
    private void setupIgoNewRequestHandler(Gateway gateway, MessageHandlingService messageHandlingService)
            throws Exception {
            gateway.subscribe(IGO_NEW_REQUEST, CmoRequestEntity.class, new MessageConsumer() {
                public void onMessage(Object message) {
                    try {
                        messageHandlingService.newRequestHandler((CmoRequestEntity)message);
                    } catch (Exception e) {
                        System.err.printf("Cannot process IGO_NEW_REQUEST:\n%s\n", message);
                        System.err.printf("Exception during processing:\n%s\n", e.getMessage());
                    }
                }
            });
        }
}
