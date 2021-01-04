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
import org.mskcc.cmo.metadb.service.SampleService;
import org.mskcc.cmo.shared.neo4j.SampleManifestEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MessageHandlingServiceImpl implements MessageHandlingService {

    @Value("${igo.new.sample.topic}")
    private String IGO_NEW_SAMPLE;

    @Value("${num.new.sample.handler.threads}")
    private int NUM_NEW_SAMPLE_HANDLERS;

    @Autowired
    private SampleService sampleService;

    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;

    private static final ExecutorService exec = Executors.newCachedThreadPool();
    private static final BlockingQueue<SampleManifestEntity> newSampleQueue =
        new LinkedBlockingQueue<SampleManifestEntity>();
    private static CountDownLatch newSampleHandlerShutdownLatch;

    private class NewSampleHandler implements Runnable {

        final Phaser phaser;
        boolean interrupted = false;

        NewSampleHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    SampleManifestEntity sample = newSampleQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (sample != null) {
                        // validate
                        // id gen
                        if (sampleService.findSampleByIgoId(sample.getIgoId()) != null) {
                            sample = sampleService.updateSampleMetadata(sample);
                        } else {
                            sample = sampleService.insertSampleMetadata(sample);
                        }

                        // pass to aggregate
                    }
                    if (interrupted && newSampleQueue.isEmpty()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    // TBD requeue?
                    System.err.printf("Error during sample handling: %s\n", e.getMessage());
                }
            }
            newSampleHandlerShutdownLatch.countDown();
        }
    }

    @Override
    public void initialize(Gateway gateway) throws Exception {

        if (!initialized) {
            setupIgoNewSampleHandler(gateway, this);
            initializeNewSampleHandlers();
            initialized = true;
        } else {
            System.err.printf("Messaging Handler Service has already been initialized, ignoring request.\n");
        }
    }

    @Override
    public void newSampleHandler(SampleManifestEntity sample) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            newSampleQueue.put(sample);
        } else {
            System.err.printf("Shutdown initiated, not accepting sample: %s\n", sample);
            throw new IllegalStateException("Shutdown initiated, not handling any more samples");
        }
    }

    @Override
    public void shutdown() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        exec.shutdownNow();
        newSampleHandlerShutdownLatch.await();
        shutdownInitiated = true;
    }

    private void initializeNewSampleHandlers() throws Exception {
        newSampleHandlerShutdownLatch = new CountDownLatch(NUM_NEW_SAMPLE_HANDLERS);
        final Phaser newSamplePhaser = new Phaser();
        newSamplePhaser.register();
        for (int lc = 0; lc < NUM_NEW_SAMPLE_HANDLERS; lc++) {
            newSamplePhaser.register();
            exec.execute(new NewSampleHandler(newSamplePhaser));
        }
        newSamplePhaser.arriveAndAwaitAdvance();
    }

    private void setupIgoNewSampleHandler(Gateway gateway, MessageHandlingService messageHandlingService)
        throws Exception {
        gateway.subscribe(IGO_NEW_SAMPLE, SampleManifestEntity.class, new MessageConsumer() {
            public void onMessage(Object message) {
                try {
                    messageHandlingService.newSampleHandler((SampleManifestEntity)message);
                } catch (Exception e) {
                    System.err.printf("Cannot process IGO_NEW_SAMPLE:\n%s\n", message);
                    System.err.printf("Exception during processing:\n%s\n", e.getMessage());
                }
            }
        });
    }
}
