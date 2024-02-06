package org.mskcc.smile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
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
import org.mskcc.smile.model.tempo.BamComplete;
import org.mskcc.smile.model.tempo.Tempo;
import org.mskcc.smile.service.SmileSampleService;
import org.mskcc.smile.service.TempoMessageHandlingService;
import org.mskcc.smile.service.TempoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author ochoaa
 */
@Component
public class TempoMessageHandlingServiceImpl implements TempoMessageHandlingService {
    @Value("${tempo.wes_bam_complete_topic}")
    private String TEMPO_WES_BAM_COMPLETE_TOPIC;

    @Value("${num.tempo_msg_handler_threads}")
    private int NUM_TEMPO_MSG_HANDLERS;

    @Autowired
    private SmileSampleService sampleService;

    @Autowired
    private TempoService tempoService;

    private static Gateway messagingGateway;
    private static final Log LOG = LogFactory.getLog(TempoMessageHandlingServiceImpl.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;
    private static final ExecutorService exec = Executors.newCachedThreadPool();
    private static final BlockingQueue<Map.Entry<String, BamComplete>> bamCompleteQueue =
            new LinkedBlockingQueue<Map.Entry<String, BamComplete>>();

    private static CountDownLatch bamCompleteHandlerShutdownLatch;

    private class BamCompleteHandler implements Runnable {
        final Phaser phaser;
        boolean interrupted = false;

        BamCompleteHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    Entry<String, BamComplete> bcEvent = bamCompleteQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (bcEvent != null) {
                        LOG.info("Event exists");

                        // first determine if sample exists by the provided primary id
                        String primaryId = bcEvent.getKey();
                        BamComplete bamComplete = bcEvent.getValue();
                        if (sampleService.sampleExistsByPrimaryId(primaryId)) {
                            // merge and/or create tempo bam complete event to sample
                            LOG.info("Sample exists");

                            Tempo tempo = tempoService.getTempoDataBySamplePrimaryId(primaryId);
                            if (tempo == null
                                    || !tempo.hasBamCompleteEvent(bamComplete)) {
                                tempoService.mergeBamCompleteEventBySamplePrimaryId(primaryId,
                                        bamComplete);
                            }
                        } else {
                            LOG.info("Sample does not exist");
                        }
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during handling of BAM complete event", e);
                }
            }
        }

    }

    @Override
    public void intialize(Gateway gateway) throws Exception {
        if (!initialized) {
            messagingGateway = gateway;
            setupBamCompleteHandler(messagingGateway, this);
            initializeMessageHandlers();
            initialized = true;
        } else {
            LOG.error("Messaging Handler Service has already been initialized, ignoring request.\n");
        }
    }

    @Override
    public void bamCompleteHandler(Map.Entry<String, BamComplete> bcEvent) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            bamCompleteQueue.put(bcEvent);
        } else {
            LOG.error("Shutdown initiated, not accepting BAM event: " + bcEvent);
            throw new IllegalStateException("Shutdown initiated, not handling any more TEMPO events");
        }
    }

    @Override
    public void shutdown() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        exec.shutdownNow();
        bamCompleteHandlerShutdownLatch.await();
        shutdownInitiated = true;
    }

    private void initializeMessageHandlers() throws Exception {
        // bam complete handler
        bamCompleteHandlerShutdownLatch = new CountDownLatch(NUM_TEMPO_MSG_HANDLERS);
        final Phaser bamCompletePhaser = new Phaser();
        bamCompletePhaser.register();
        for (int lc = 0; lc < NUM_TEMPO_MSG_HANDLERS; lc++) {
            bamCompletePhaser.register();
            exec.execute(new BamCompleteHandler(bamCompletePhaser));
        }
        bamCompletePhaser.arriveAndAwaitAdvance();
    }

    private void setupBamCompleteHandler(Gateway gateway,
            TempoMessageHandlingService tempoMessageHandlingService) throws Exception {
        gateway.subscribe(TEMPO_WES_BAM_COMPLETE_TOPIC, Object.class, new MessageConsumer() {
            public void onMessage(Message msg, Object message) {
                try {
                    String bamCompleteJson = mapper.readValue(
                        new String(msg.getData(), StandardCharsets.UTF_8),
                        String.class);

                    LOG.info("bamCompleteJson: " + bamCompleteJson);

                        Map<String, String> bamCompleteMap = mapper.readValue(bamCompleteJson, Map.class);
                    BamComplete bamComplete = new BamComplete(bamCompleteMap.get("timestamp"),
                            bamCompleteMap.get("status"));
                    
                    LOG.info("\n\nBAM complete object: " + bamComplete.toString());
                    String primaryId = bamCompleteMap.get("primaryId");
                    Map.Entry<String, BamComplete> eventData =
                            new AbstractMap.SimpleImmutableEntry<>(primaryId, bamComplete);
                    tempoMessageHandlingService.bamCompleteHandler(eventData);
                } catch (Exception e) {
                    LOG.error("Exception occurred during processing of BAM complete event: "
                            + TEMPO_WES_BAM_COMPLETE_TOPIC, e);
                }
            }
        });
    }
}
