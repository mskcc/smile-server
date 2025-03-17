package org.mskcc.smile.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.smile.service.CBioPortalMessageSchedulingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 *
 * @author ochoaa
 */
@Component
public class CBioPortalMessageSchedulingServiceImpl implements CBioPortalMessageSchedulingService {
    private static Gateway messagingGateway;
    private static boolean initialized = false;
    private static final Log LOG = LogFactory.getLog(CBioPortalMessageSchedulingServiceImpl.class);

    @Override
    public void initialize(Gateway gateway) throws Exception {
        if (!initialized) {
            messagingGateway = gateway;
            initialized = true;
            LOG.info("CBioPortal Message Scheduling Service initialized");
        } else {
            LOG.error("Messaging Scheduler Service has already been initialized, ignoring request.\n");
        }
    }

    @Override
    public void shutdown() throws Exception {
        if (initialized) {
            LOG.info("Shutting down CBioPortal Message Scheduling Service...");
            initialized = false;
        }
    }

    /**
     * Automatically logs "Hello world" every 5 seconds
     */
    @Scheduled(fixedRate = 5000) // 5 seconds
    public void printHelloWorld() {
        if (initialized) {
            LOG.info("Hello world");
        }
    }
}
