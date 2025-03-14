package org.mskcc.smile.service.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.smile.service.CBioPortalMessageSchedulingService;
import org.springframework.stereotype.Component;

/**
 *
 * @author ochoaa
 */
@Component
public class CBioPortalMessageSchedulingServiceImpl implements CBioPortalMessageSchedulingService {
    private static Gateway messagingGateway;
    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;
    private static final ExecutorService exec = Executors.newCachedThreadPool();
    private static final Log LOG = LogFactory.getLog(CBioPortalMessageSchedulingServiceImpl.class);
    private ScheduledExecutorService scheduler;

    @Override
    public void initialize(Gateway gateway) throws Exception {
        if (!initialized) {
            messagingGateway = gateway;
            initialized = true;
            LOG.info("CBioPortal Message Scheduling Service initialized");
            scheduleTask();
        } else {
            LOG.error("Messaging Scheduler Service has already been initialized, ignoring request.\n");
        }
    }

    @Override
    public void shutdown() throws Exception {
        if (initialized && scheduler != null) {
            LOG.info("Shutting down CBioPortal Message Scheduling Service...");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            initialized = false;
        }
    }

    @Override
    public void scheduleTask() throws Exception {
        if (initialized) {
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                LOG.info("Hello world");
            }, 0, 5, TimeUnit.SECONDS);
            LOG.info("Scheduled task to run every 5 seconds");
        } else {
            LOG.error("Cannot schedule task - service not initialized");
        }
    }
}
