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
import org.mskcc.smile.model.tempo.MafComplete;
import org.mskcc.smile.model.tempo.QcComplete;
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

    @Value("${tempo.wes_qc_complete_topic}")
    private String TEMPO_WES_QC_COMPLETE_TOPIC;

    @Value("${tempo.wes_maf_complete_topic}")
    private String TEMPO_WES_MAF_COMPLETE_TOPIC;

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
    private static final BlockingQueue<Map.Entry<String, QcComplete>> qcCompleteQueue =
            new LinkedBlockingQueue<Map.Entry<String, QcComplete>>();
    private static final BlockingQueue<Map.Entry<String, MafComplete>> mafCompleteQueue =
            new LinkedBlockingQueue<Map.Entry<String, MafComplete>>();

    private static CountDownLatch bamCompleteHandlerShutdownLatch;
    private static CountDownLatch qcCompleteHandlerShutdownLatch;
    private static CountDownLatch mafCompleteHandlerShutdownLatch;

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
                        // first determine if sample exists by the provided primary id
                        String primaryId = bcEvent.getKey();
                        BamComplete bamComplete = bcEvent.getValue();
                        if (sampleService.sampleExistsByPrimaryId(primaryId)) {
                            // merge and/or create tempo bam complete event to sample
                            Tempo tempo = tempoService.getTempoDataBySamplePrimaryId(primaryId);
                            if (tempo == null
                                    || !tempo.hasBamCompleteEvent(bamComplete)) {
                                tempoService.mergeBamCompleteEventBySamplePrimaryId(primaryId,
                                        bamComplete);
                            }
                        } else {
                            LOG.error("Sample with primary id " + primaryId + " does not exist");
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

    private class QcCompleteHandler implements Runnable {
        final Phaser phaser;
        boolean interrupted = false;

        QcCompleteHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    Entry<String, QcComplete> qcEvent = qcCompleteQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (qcEvent != null) {
                        // first determine if sample exists by the provided primary id
                        String primaryId = qcEvent.getKey();
                        QcComplete qcComplete = qcEvent.getValue();
                        if (sampleService.sampleExistsByPrimaryId(primaryId)) {
                            // merge and/or create tempo qc complete event to sample
                            Tempo tempo = tempoService.getTempoDataBySamplePrimaryId(primaryId);
                            if (tempo == null
                                    || !tempo.hasQcCompleteEvent(qcComplete)) {
                                tempoService.mergeQcCompleteEventBySamplePrimaryId(primaryId,
                                        qcComplete);
                            }
                        } else {
                            LOG.error("Sample with primary id: " + primaryId + " does not exist");
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

    private class MafCompleteHandler implements Runnable {
        final Phaser phaser;
        boolean interrupted = false;

        MafCompleteHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    Entry<String, MafComplete> mcEvent = mafCompleteQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (mcEvent != null) {
                        // first determine if sample exists by the provided primary id
                        String primaryId = mcEvent.getKey();
                        MafComplete mafComplete = mcEvent.getValue();
                        if (sampleService.sampleExistsByPrimaryId(primaryId)) {
                            // merge and/or create tempo maf complete event to sample
                            Tempo tempo = tempoService.getTempoDataBySamplePrimaryId(primaryId);
                            if (tempo == null
                                    || !tempo.hasMafCompleteEvent(mafComplete)) {
                                tempoService.mergeMafCompleteEventBySamplePrimaryId(primaryId,
                                        mafComplete);
                            }
                        } else {
                            LOG.error("Sample with primary id " + primaryId + " does not exist");
                        }
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during handling of MAF complete event", e);
                }
            }
        }

    }

    @Override
    public void intialize(Gateway gateway) throws Exception {
        if (!initialized) {
            messagingGateway = gateway;
            setupBamCompleteHandler(messagingGateway, this);
            setupQcCompleteHandler(messagingGateway, this);
            setupMafCompleteHandler(messagingGateway, this);
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
    public void qcCompleteHandler(Map.Entry<String, QcComplete> qcEvent) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            qcCompleteQueue.put(qcEvent);
        } else {
            LOG.error("Shutdown initiated, not accepting QC event: " + qcEvent);
            throw new IllegalStateException("Shutdown initiated, not handling any more TEMPO events");
        }
    }

    @Override
    public void mafCompleteHandler(Map.Entry<String, MafComplete> mcEvent) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            mafCompleteQueue.put(mcEvent);
        } else {
            LOG.error("Shutdown initiated, not accepting MAF event: " + mcEvent);
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
        qcCompleteHandlerShutdownLatch.await();
        mafCompleteHandlerShutdownLatch.await();
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

        // qc complete handler
        qcCompleteHandlerShutdownLatch = new CountDownLatch(NUM_TEMPO_MSG_HANDLERS);
        final Phaser qcCompletePhaser = new Phaser();
        qcCompletePhaser.register();
        for (int lc = 0; lc < NUM_TEMPO_MSG_HANDLERS; lc++) {
            qcCompletePhaser.register();
            exec.execute(new QcCompleteHandler(qcCompletePhaser));
        }
        qcCompletePhaser.arriveAndAwaitAdvance();
        
        // maf complete handler
        mafCompleteHandlerShutdownLatch = new CountDownLatch(NUM_TEMPO_MSG_HANDLERS);
        final Phaser mafCompletePhaser = new Phaser();
        mafCompletePhaser.register();
        for (int lc = 0; lc < NUM_TEMPO_MSG_HANDLERS; lc++) {
            mafCompletePhaser.register();
            exec.execute(new MafCompleteHandler(mafCompletePhaser));
        }
        mafCompletePhaser.arriveAndAwaitAdvance();
    }

    private void setupBamCompleteHandler(Gateway gateway,
            TempoMessageHandlingService tempoMessageHandlingService) throws Exception {
        gateway.subscribe(TEMPO_WES_BAM_COMPLETE_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                try {
                    String bamCompleteJson = mapper.readValue(
                        new String(msg.getData(), StandardCharsets.UTF_8),
                        String.class);
                    Map<String, String> bamCompleteMap = mapper.readValue(bamCompleteJson, Map.class);
                    BamComplete bamComplete = new BamComplete(bamCompleteMap.get("date"),
                            bamCompleteMap.get("status"));
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

    private void setupQcCompleteHandler(Gateway gateway,
            TempoMessageHandlingService tempoMessageHandlingService) throws Exception {
        gateway.subscribe(TEMPO_WES_QC_COMPLETE_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                try {
                    String qcCompleteJson = mapper.readValue(
                        new String(msg.getData(), StandardCharsets.UTF_8),
                        String.class);
                        Map<String, String> qcCompleteMap = mapper.readValue(qcCompleteJson, Map.class);
                        QcComplete qcComplete = new QcComplete(qcCompleteMap.get("date"),
                                qcCompleteMap.get("result"), qcCompleteMap.get("reason"),
                                qcCompleteMap.get("status"));
                    String primaryId = qcCompleteMap.get("primaryId");
                    Map.Entry<String, QcComplete> eventData =
                            new AbstractMap.SimpleImmutableEntry<>(primaryId, qcComplete);
                    tempoMessageHandlingService.qcCompleteHandler(eventData);
                } catch (Exception e) {
                    LOG.error("Exception occurred during processing of QC complete event: "
                            + TEMPO_WES_QC_COMPLETE_TOPIC, e);
                }
            }
        });
    }
            
    
    private void setupMafCompleteHandler(Gateway gateway,
            TempoMessageHandlingService tempoMessageHandlingService) throws Exception {
        gateway.subscribe(TEMPO_WES_MAF_COMPLETE_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                try {
                    String mafCompleteJson = mapper.readValue(
                        new String(msg.getData(), StandardCharsets.UTF_8),
                        String.class);
                    Map<String, String> mafCompleteMap = mapper.readValue(mafCompleteJson, Map.class);
                    MafComplete mafComplete = new MafComplete(mafCompleteMap.get("date"),
                            mafCompleteMap.get("normalPrimaryId"),
                            mafCompleteMap.get("status"));
                    String primaryId = mafCompleteMap.get("primaryId");
                    Map.Entry<String, MafComplete> eventData =
                            new AbstractMap.SimpleImmutableEntry<>(primaryId, mafComplete);
                    tempoMessageHandlingService.mafCompleteHandler(eventData);
                } catch (Exception e) {
                    LOG.error("Exception occurred during processing of MAF complete event: "
                            + TEMPO_WES_MAF_COMPLETE_TOPIC, e);
                }
            }
        });
    }
}
