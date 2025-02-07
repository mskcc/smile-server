package org.mskcc.smile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.util.JsonFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.smile.commons.JsonComparator;
import org.mskcc.smile.commons.generated.Smile.TempoSample;
import org.mskcc.smile.commons.generated.Smile.TempoSampleUpdateMessage;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.tempo.Cohort;
import org.mskcc.smile.model.tempo.Tempo;
import org.mskcc.smile.persistence.neo4j.CohortCompleteRepository;
import org.mskcc.smile.service.CohortCompleteService;
import org.mskcc.smile.service.SmileSampleService;
import org.mskcc.smile.service.TempoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author ochoaa
 */
@Component
public class CohortCompleteServiceImpl implements CohortCompleteService {
    @Autowired
    private JsonComparator jsonComparator;

    @Autowired
    private CohortCompleteRepository cohortCompleteRepository;

    @Autowired
    private SmileSampleService sampleService;

    @Autowired
    private TempoService tempoService;

    @Value("${tempo.release-samples}")
    private String TEMPO_RELEASE_SAMPLES_TOPIC;

    private ObjectMapper mapper = new ObjectMapper();
    private static Gateway messagingGateway;
    private static final Log LOG = LogFactory.getLog(CohortCompleteServiceImpl.class);

    @Override
    public Cohort saveCohort(Cohort cohort, Set<String> sampleIds) throws Exception {
        // persist new cohort complete event to the db
        cohortCompleteRepository.save(cohort);
        Set<String> unknownSamples = new HashSet<>(); // tracks unknown samples in smile
        Set<TempoSample> tempoSamples = new HashSet<>(); // tracks known samples for publishing to cBioPortal
        // create cohort-sample relationships
        LOG.info("Adding cohort-sample edges in database for " + sampleIds.size() + " samples...");
        for (String sampleId : sampleIds) {
            // confirm sample exists by primary id and then link to cohort
            SmileSample sample = sampleService.getDetailedSampleByInputId(sampleId);
            if (sample != null) {
                String primaryId = sample.getPrimarySampleAlias();
                Tempo tempo = tempoService.getTempoDataBySamplePrimaryId(primaryId);
                // init default tempo data for sample if sample does not already have tempo data
                if (tempo == null) {
                    tempo = tempoService.initAndSaveDefaultTempoData(primaryId);
                }
                cohortCompleteRepository.addCohortSampleRelationship(cohort.getCohortId(), primaryId);
                // build tempo sample object for publishing to cBioPortal
                TempoSample tempoSample = TempoSample.newBuilder()
                    .setPrimaryId(primaryId)
                    .setCmoSampleName(sample.getLatestSampleMetadata().getCmoSampleName())
                    .setAccessLevel(tempo.getAccessLevel())
                    .setCustodianInformation(tempo.getCustodianInformation())
                    .build();
                tempoSamples.add(tempoSample);
            } else {
                unknownSamples.add(sampleId);
            }
        }
        // build and publish the sample update message to cBioPortal
        if (!tempoSamples.isEmpty()) {
            TempoSampleUpdateMessage tempoSampleUpdateMessage = TempoSampleUpdateMessage.newBuilder()
                .addAllTempoSamples(tempoSamples)
                .build();
            try {
                String messageAsJsonString = JsonFormat.printer().print(tempoSampleUpdateMessage);
                LOG.info("Publishing TEMPO samples to cBioPortal: " + messageAsJsonString);
                messagingGateway.publish(TEMPO_RELEASE_SAMPLES_TOPIC, messageAsJsonString);
            } catch (Exception e) {
                LOG.error("Failed to publish TEMPO samples to cBioPortal: " + e.getMessage());
            }
        }

        // log and report unknown samples for reference
        if (!unknownSamples.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            builder.append("[TEMPO COHORT COMPLETE FAILED SAMPLES] Could not import ")
                    .append(unknownSamples.size())
                    .append(" samples for cohort ")
                    .append(cohort.getCohortId())
                    .append(": ")
                    .append(StringUtils.join(unknownSamples,", "));
            LOG.warn(builder.toString());
        }
        return getCohortByCohortId(cohort.getCohortId());
    }

    @Override
    public Cohort getCohortByCohortId(String cohortId) throws Exception {
        Cohort cohort = cohortCompleteRepository.findCohortByCohortId(cohortId);
        return getDetailedCohortData(cohort);
    }

    @Override
    public List<Cohort> getCohortsBySamplePrimaryId(String primaryId) throws Exception {
        return cohortCompleteRepository.findCohortsBySamplePrimaryId(primaryId);
    }

    @Override
    public Boolean hasUpdates(Cohort existingCohort, Cohort cohort) throws Exception {
        // check cohort complete data for updates first
        Boolean hasUpdates = hasCohortCompleteUpdates(existingCohort, cohort);
        Set<String> newSamples = cohort.getCohortSamplePrimaryIds();
        newSamples.removeAll(existingCohort.getCohortSamplePrimaryIds());
        return (hasUpdates || !newSamples.isEmpty());
    }

    @Override
    public Boolean hasCohortCompleteUpdates(Cohort existingCohort, Cohort cohort) throws Exception {
        String existingCohortComplete = mapper.writeValueAsString(existingCohort.getLatestCohortComplete());
        String currentCohortComplete = mapper.writeValueAsString(cohort.getLatestCohortComplete());
        return !jsonComparator.isConsistentGenericComparison(existingCohortComplete,
                currentCohortComplete);
    }

    @Override
    public Cohort updateCohort(Cohort cohort) throws Exception {
        return cohortCompleteRepository.save(cohort);
    }

    private Cohort getDetailedCohortData(Cohort cohort) throws Exception {
        if (cohort == null || cohort.getId() == null) {
            return null;
        }
        // get cohort samples
        cohort.setCohortSamples(sampleService.getSamplesByCohortId(cohort.getCohortId()));
        return cohort;
    }
}
