package org.mskcc.smile.service;

import java.util.List;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 *
 * @author qu8n
 */
@Service
public class TempoEmbargoStatusScheduler {
    @Autowired
    private TempoService tempoService;

    @Autowired
    private SmileSampleService sampleService;

    @Autowired
    private TempoMessageHandlingService tempoMessageHandlingService;

    private static final Log LOG = LogFactory.getLog(TempoEmbargoStatusScheduler.class);

    // @Scheduled(cron = "0 0 0 * * ?") // every day at midnight
    @Scheduled(cron = "*/2 * * * * ?") // testing - every 2 sec
    public void checkEmbargoStatusChangesDaily() {
        try {
            LOG.info("Checking for Tempo records that are no longer embargoed...");
            List<UUID> tempoIdsNoLongerEmbargoed = tempoService.getTempoIdsNoLongerEmbargoed();
            if (!tempoIdsNoLongerEmbargoed.isEmpty()) {
                // add to tempo messaging queue
                List<String> samplePrimaryIds = sampleService.getSamplePrimaryIdsBySmileTempoIds(
                        tempoIdsNoLongerEmbargoed);
                tempoMessageHandlingService.tempoEmbargoStatusHandler(samplePrimaryIds);
            }
        } catch (Exception e) {
            LOG.error("Error running the daily job checkEmbargoStatusChangesDaily: " + e.getMessage());
        }
    }
}
