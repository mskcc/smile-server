package org.mskcc.smile.service.util;

import java.util.List;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.smile.service.SmileSampleService;
import org.mskcc.smile.service.TempoMessageHandlingService;
import org.mskcc.smile.service.TempoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 *
 * @author qu8n
 */
@Component
public class TempoEmbargoStatusScheduler {
    @Autowired
    private TempoService tempoService;

    @Autowired
    private SmileSampleService sampleService;

    @Autowired
    private TempoMessageHandlingService tempoMessageHandlingService;

    private static final Log LOG = LogFactory.getLog(TempoEmbargoStatusScheduler.class);

    /**
     * Checks for changes in TEMPO embargo status.
     */
    @Scheduled(cron = "0 0 0 * * ?") // every day at midnight
    public void checkEmbargoStatusChangesDaily() {
        try {
            LOG.info("Checking for Tempo records that are no longer embargoed...");
            List<UUID> tempoIdsNoLongerEmbargoed = tempoService.getTempoIdsNoLongerEmbargoed();
            if (!tempoIdsNoLongerEmbargoed.isEmpty()) {
                // add to tempo messaging queue
                LOG.info(tempoIdsNoLongerEmbargoed.size() + " TEMPO nodes no longer embargoed.");
                List<String> samplePrimaryIds = sampleService.getSamplePrimaryIdsBySmileTempoIds(
                        tempoIdsNoLongerEmbargoed);

                LOG.info("Updating embargo status for " + samplePrimaryIds.size() + " samples");
                tempoMessageHandlingService.tempoEmbargoStatusHandler(samplePrimaryIds);
            }
        } catch (Exception e) {
            LOG.error("Error running the daily job checkEmbargoStatusChangesDaily: " + e.getMessage());
        }
    }
}
