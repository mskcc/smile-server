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
import org.mskcc.smile.model.json.DbGapJson;
import org.mskcc.smile.service.DbGapMessageHandlingService;
import org.mskcc.smile.service.DbGapService;
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

    private static Gateway messagingGateway;
    private static final Log LOG = LogFactory.getLog(DbGapMessageHandlingServiceImpl.class);

    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;
    private static final ExecutorService exec = Executors.newCachedThreadPool();

    private static final BlockingQueue<DbGapJson> dbgapUpdateQueue =
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
                    DbGapJson dbGapJson = dbgapUpdateQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (dbGapJson != null) {
                        try {
                            dbGapService.updateDbGap(dbGapJson);
                        } catch (Exception e) {
                            LOG.error("[DBGAP UPDATE ERROR] Encountered error while persisting "
                                    + "DbGap update to database: " + dbGapJson.toString(), e);
                        }
                    }
                    if (interrupted || shutdownInitiated) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during handling of DbGap update event", e);
                }
                if (interrupted || shutdownInitiated) {
                    break;
                }
            }
            dbgapUpdateHandlerShutdownLatch.countDown();
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
            dbgapUpdateQueue.put(dbGapJson);
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
                    String dbGapJsonStr = NatsMsgUtil.extractNatsJsonString(msg);
                    if (dbGapJsonStr == null) {
                        LOG.error("Exception occurred during processing of NATS message data");
                        return;
                    }
                    DbGapJson dbGapJson = (DbGapJson) NatsMsgUtil.convertObjectFromString(
                            dbGapJsonStr, new TypeReference<DbGapJson>() {});
                    LOG.info("Received DbGap update event: " + dbGapJson);
                    dbGapMessageHandlingService.dbGapUpdateHandler(dbGapJson);
                } catch (Exception e) {
                    LOG.error("Error during handling of DbGap update", e);
                }
            }
        });
    }
}
