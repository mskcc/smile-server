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

    @Autowired
    private SmileSampleService sampleService;

    @Autowired
    private SmilePatientService patientService;

    @Value("${tempo.release_samples_topic}")
    private String TEMPO_RELEASE_SAMPLES_TOPIC;

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
     * Daily job that checks for embargoed Tempo samples that are now public, updates their access level to
     * 'public', and publishes their samples to cBioPortal.
     */
    // @Scheduled(cron = "0 0 0 * * ?") // every day at midnight
    @Scheduled(fixedRate = 1000 * 60) // 60 seconds
    public void checkEmbargoStatusChangeAndSendUpdates() {
        if (initialized) {
            try {
                LOG.info("Checking for embargoed Tempo samples that are now public...");
                List<UUID> tempoIdsNoLongerEmbargoed = tempoService.getTempoIdsNoLongerEmbargoed();
                if (tempoIdsNoLongerEmbargoed.isEmpty()) {
                    return;
                }
                LOG.info("Found " + tempoIdsNoLongerEmbargoed.size()
                    + " Tempo samples that are no longer embargoed.");
                LOG.info("Updating Access Level to '" + TempoServiceImpl.ACCESS_LEVEL_PUBLIC
                    + "' for these samples...");
                tempoService.updateTempoAccessLevel(
                    tempoIdsNoLongerEmbargoed, TempoServiceImpl.ACCESS_LEVEL_PUBLIC);
                List<String> samplePrimaryIds = sampleService.getSamplePrimaryIdsBySmileTempoIds(
                    tempoIdsNoLongerEmbargoed);
                publishTempoSamplesToCBioPortal(samplePrimaryIds);
            } catch (Exception e) {
                LOG.error("Error running the daily job checkEmbargoStatusChangeAndSendUpdates: "
                    + e.getMessage());
            }
        }
    }

    private void publishTempoSamplesToCBioPortal(List<String> samplePrimaryIds) throws Exception {
        // validate and build tempo samples to publish to cBioPortal
        Set<TempoSample> validTempoSamples = new HashSet<>();
        for (String primaryId : samplePrimaryIds) {
            try {
                // confirm tempo data exists by primary id
                Tempo tempo = tempoService.getTempoDataBySamplePrimaryId(primaryId);
                if (tempo == null) {
                    LOG.error("Tempo data not found for sample with Primary ID " + primaryId);
                    continue;
                }
                // validate props before building tempo sample
                SampleMetadata sampleMetadata = sampleService.getLatestSampleMetadataByPrimaryId(primaryId);
                String cmoSampleName = sampleMetadata.getCmoSampleName();
                if (StringUtils.isBlank(cmoSampleName)) {
                    LOG.error("Invalid CMO Sample Name for sample with Primary ID " + primaryId);
                    continue;
                }
                String accessLevel = tempo.getAccessLevel();
                if (StringUtils.isBlank(accessLevel)) {
                    LOG.error("Invalid Access Level for sample with Primary ID " + primaryId);
                    continue;
                }
                String custodianInformation = tempo.getCustodianInformation();
                if (StringUtils.isBlank(custodianInformation)) {
                    LOG.error("Invalid Custodian Information for sample with Primary ID " + primaryId);
                    continue;
                }
                String cmoPatientId = sampleMetadata.getCmoPatientId();
                SmilePatient patient = patientService.getPatientByCmoPatientId(cmoPatientId);
                // build tempo sample object
                TempoSample tempoSample = TempoSample.newBuilder()
                    .setPrimaryId(primaryId)
                    .setCmoSampleName(cmoSampleName)
                    .setAccessLevel(accessLevel)
                    .setCustodianInformation(custodianInformation)
                    .setBaitSet(StringUtils.defaultString(sampleMetadata.getBaitSet(), ""))
                    .setGenePanel(StringUtils.defaultString(sampleMetadata.getGenePanel(), ""))
                    .setOncotreeCode(StringUtils.defaultString(sampleMetadata.getOncotreeCode(), ""))
                    .setCmoPatientId(StringUtils.defaultString(cmoPatientId, ""))
                    .setDmpPatientId(StringUtils.defaultString(patient.getPatientAlias("dmpId"), ""))
                    .setRecapture(sampleService.sampleIsRecapture(sampleMetadata.getInvestigatorSampleId()))
                    .build();
                validTempoSamples.add(tempoSample);
            } catch (Exception e) {
                LOG.error("Error building to publish to cBioPortal of sample: " + primaryId, e);
                continue;
            }
        }
        // bundle together all valid tempo samples and publish to cBioPortal
        if (!validTempoSamples.isEmpty()) {
            TempoSampleUpdateMessage tempoSampleUpdateMessage = TempoSampleUpdateMessage.newBuilder()
                .addAllTempoSamples(validTempoSamples)
                .build();
            try {
                LOG.info("Publishing TEMPO samples to cBioPortal:\n" + tempoSampleUpdateMessage.toString());
                messagingGateway.publish(TEMPO_RELEASE_SAMPLES_TOPIC, tempoSampleUpdateMessage.toByteArray());
            } catch (Exception e) {
                LOG.error("Error publishing TEMPO samples to cBioPortal", e);
            }
        } else {
            LOG.warn("No valid TEMPO samples to publish to cBioPortal");
        }
    }

}
