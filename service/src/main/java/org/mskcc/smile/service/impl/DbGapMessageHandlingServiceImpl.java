package org.mskcc.smile.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import io.nats.client.Message;
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
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.json.DbGapJson;
import org.mskcc.smile.service.DbGapMessageHandlingService;
import org.mskcc.smile.service.DbGapService;
import org.mskcc.smile.service.SmileSampleService;
import org.mskcc.smile.service.util.NatsMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DbGapMessageHandlingServiceImpl implements DbGapMessageHandlingService {
    @Value("${dbgap.sample_update_topic}")
    private String DBGAP_SAMPLE_UPDATE_TOPIC;

    @Value("${num.dbgap_msg_handler_threads}")
    private int NUM_DBGAP_MSG_HANDLERS;

    @Autowired
    private DbGapService dbGapService;

    @Autowired
    private SmileSampleService sampleService;

    private static Gateway messagingGateway;
    private static final Log LOG = LogFactory.getLog(DbGapMessageHandlingServiceImpl.class);

    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;
    private static final ExecutorService exec = Executors.newCachedThreadPool();

    private static final BlockingQueue<DbGapJson> dbGapUpdateQueue =
            new LinkedBlockingQueue<DbGapJson>();

    private static CountDownLatch dbgapUpdateHandlerShutdownLatch;

    private class DbGapUpdateHandler implements Runnable {
        final Phaser phaser;
        boolean interrupted = false;

        DbGapUpdateHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    DbGapJson dbGap = dbGapUpdateQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (dbGap != null) {
                        // this message is coming straight from the dashboard and should therefore always
                        // have data for a valid sample that exists in the database
                        // however this check will make extra sure that the primary id received
                        // in the nats message actually exists in the database before conducting
                        // further operations in the db
                        SmileSample sample = sampleService.getDetailedSampleByInputId(dbGap.getPrimaryId());
                        try {
                        } catch (Exception e) {
                            LOG.error("[DBGAP UPDATE ERROR] Encountered error while persisting "
                                    + "DbGap update to database: " + dbGap.toString(), e);
                        }
                        if (sample != null) {
                            StringBuilder builder = new StringBuilder();
                            builder.append("Updating billing information for sample: ")
                                    .append(sample.getPrimarySampleAlias());
                            if (!dbGap.getPrimaryId().equalsIgnoreCase(sample.getPrimarySampleAlias())) {
                                builder.append(" (mapped from input id: ")
                                        .append(dbGap.getPrimaryId())
                                        .append(")");
                            }
                            LOG.info(builder.toString());
                            dbGapService.updateDbGap(dbGap);
                        } else {
                            LOG.error("[DBGAP UPDATE ERROR] Cannot update DbGap information for "
                                    + "sample that does not exist: " + dbGap.getPrimaryId());
                        }
                    }
                    if (interrupted || shutdownInitiated) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during handling of sample DbGap data", e);
                }
                dbgapUpdateHandlerShutdownLatch.countDown();
            }
        }
    }

    @Override
    public void initialize(Gateway gateway) throws Exception {
        if (!initialized) {
            messagingGateway = gateway;
            setupDbGapUpdateHandler(messagingGateway, this);
            initializeMessageHandlers();
            initialized = true;
        } else {
            LOG.error("DbGap Message Handler Service has already been initialized, ignoring request.\n");
        }
    }

    @Override
    public void dbGapUpdateHandler(DbGapJson dbGapJson) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("DbGap Message Handler Service has not been initialized");
        }
        if (!shutdownInitiated) {
            dbGapUpdateQueue.put(dbGapJson);
        } else {
            LOG.error("DbGap Message Handler Service has been shutdown, cannot handle request: "
                    + dbGapJson);
            throw new IllegalStateException("DbGap Message Handler Service has been shutdown");
        }
    }

    @Override
    public void shutdown() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("DbGap Message Handler Service has not been initialized");
        }

        exec.shutdownNow();
        dbgapUpdateHandlerShutdownLatch.await();
        shutdownInitiated = true;
    }

    private void initializeMessageHandlers() throws Exception {
        dbgapUpdateHandlerShutdownLatch = new CountDownLatch(NUM_DBGAP_MSG_HANDLERS);
        final Phaser dbgapUpdatePhaser = new Phaser();
        dbgapUpdatePhaser.register();
        for (int lc = 0; lc < NUM_DBGAP_MSG_HANDLERS; lc++) {
            dbgapUpdatePhaser.register();
            exec.execute(new DbGapUpdateHandler(dbgapUpdatePhaser));
        }
        dbgapUpdatePhaser.arriveAndAwaitAdvance();
    }

    private void setupDbGapUpdateHandler(Gateway gateway,
            DbGapMessageHandlingService dbGapMessageHandlingService) throws Exception {
        gateway.subscribe(DBGAP_SAMPLE_UPDATE_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                try {
                    LOG.info("Received message on topic: " + DBGAP_SAMPLE_UPDATE_TOPIC);
                    String dbGapJson = NatsMsgUtil.extractNatsJsonString(msg);
                    if (dbGapJson == null) {
                        LOG.error("Exception occurred during processing of NATS message data");
                        return;
                    }
                    DbGapJson dbGap = (DbGapJson) NatsMsgUtil.convertObjectFromString(
                            dbGapJson, new TypeReference<DbGapJson>() {});

                    dbGapMessageHandlingService.dbGapUpdateHandler(dbGap);
                } catch (Exception e) {
                    LOG.error("Exception occurred during processing of DbGap update event: "
                            + DBGAP_SAMPLE_UPDATE_TOPIC, e);
                }
            }
        });
    }
}
