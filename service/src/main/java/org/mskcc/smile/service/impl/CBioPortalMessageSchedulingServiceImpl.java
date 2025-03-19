package org.mskcc.smile.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.smile.commons.generated.Smile.TempoSample;
import org.mskcc.smile.commons.generated.Smile.TempoSampleUpdateMessage;
import org.mskcc.smile.model.SampleMetadata;
import org.mskcc.smile.model.SmilePatient;
import org.mskcc.smile.model.tempo.Tempo;
import org.mskcc.smile.service.CBioPortalMessageSchedulingService;
import org.mskcc.smile.service.SmilePatientService;
import org.mskcc.smile.service.SmileSampleService;
import org.mskcc.smile.service.TempoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 *
 * @author qu8n
 */
@Component
public class CBioPortalMessageSchedulingServiceImpl implements CBioPortalMessageSchedulingService {
    @Autowired
    private TempoService tempoService;

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
     * TODO: add function description
     * Note for Angelica: for testing, we can change the cron expression to run more frequently
     * (e.g. every 5 minutes) by changing the cron expression or setting a fixedDelay
     */
    @Scheduled(cron = "0 0 0 * * ?") // every day at midnight
    public void checkEmbargoStatusChangesDaily() {
        if (initialized) {
            try {
                LOG.info("Checking for Tempo records that are no longer embargoed...");
                List<UUID> tempoIdsNoLongerEmbargoed = tempoService.getTempoIdsNoLongerEmbargoed();
                if (tempoIdsNoLongerEmbargoed.isEmpty()) {
                    LOG.info("No Tempo records need to be updated.");
                    return;
                }
                // Notes for Angelica: these are methods I wrote that you might find useful for your PR
                // (Please modify them as needed and delete them if you don't end up using them)
                //
                // 1. To update the access level of a list of Tempo IDs to MSK Public
                // tempoService.updateTempoAccessLevel(
                //     tempoIdsNoLongerEmbargoed, TempoServiceImpl.ACCESS_LEVEL_PUBLIC);
                //
                // 2. To get a list of sample primary IDs from a list of Tempo IDs
                // List<String> samplePrimaryIds = sampleService.getSamplePrimaryIdsBySmileTempoIds(
                //     tempoIdsNoLongerEmbargoed);
            } catch (Exception e) {
                LOG.error("Error running the daily job checkEmbargoStatusChangesDaily: " + e.getMessage());
            }
        }
    }
}
