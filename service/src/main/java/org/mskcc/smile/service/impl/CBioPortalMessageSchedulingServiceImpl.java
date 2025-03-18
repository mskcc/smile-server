package org.mskcc.smile.service.impl;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.smile.service.CBioPortalMessageSchedulingService;
import org.mskcc.smile.service.SmileSampleService;
import org.mskcc.smile.service.TempoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 *
 * @author ochoaa
 */
@Component
public class CBioPortalMessageSchedulingServiceImpl implements CBioPortalMessageSchedulingService {
    @Autowired
    private TempoService tempoService;

    @Autowired
    private SmileSampleService smileSampleService;

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
     * TODO
     */
    @Scheduled(fixedRate = 1000 * 30) // 30 seconds
    // @Scheduled(cron = "0 0 0 * * ?") // every day at midnight
    public void checkEmbargoStatusChangeAndSendUpdates() {
        if (initialized) {
            try {
                LOG.info("Checking for embargoed Tempo samples that are now public...");
                List<String> tempoIdsNoLongerEmbargoed = tempoService.getTempoIdsNoLongerEmbargoed();
                if (tempoIdsNoLongerEmbargoed.isEmpty()) {
                    return;
                }

                LOG.info("Found " + tempoIdsNoLongerEmbargoed.size() + " Tempo samples that are no longer embargoed.");
                LOG.info("Updating Access Level to '" + TempoServiceImpl.ACCESS_LEVEL_PUBLIC + "' for these samples...");
                tempoService.updateTempoAccessLevel(tempoIdsNoLongerEmbargoed, TempoServiceImpl.ACCESS_LEVEL_PUBLIC);

                List<String> samplePrimaryIds = smileSampleService.getSamplePrimaryIdsBySmileTempoIds(tempoIdsNoLongerEmbargoed);
            } catch (Exception e) {
                LOG.error("Error running the daily job checkEmbargoStatusChangeAndSendUpdates: " + e.getMessage());
            }
        }
    }
}
