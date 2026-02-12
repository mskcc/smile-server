package org.mskcc.smile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.smile.commons.JsonComparator;
import org.mskcc.smile.model.tempo.Cohort;
import org.mskcc.smile.persistence.neo4j.CohortCompleteRepository;
import org.mskcc.smile.service.CohortCompleteService;
import org.mskcc.smile.service.SmileSampleService;
import org.mskcc.smile.service.TempoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired @Lazy // prevents circular dependencies and initializes when component is first needed
    private TempoService tempoService;

    private ObjectMapper mapper = new ObjectMapper();

    private static final Log LOG = LogFactory.getLog(CohortCompleteServiceImpl.class);

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void saveCohort(Cohort cohort, Set<String> sampleIds) throws Exception {
        // persist new cohort complete event to the db
        cohortCompleteRepository.save(cohort);
        if (sampleIds == null) {
            LOG.error("No samples saved for cohort: " + cohort.getCohortId());
        }

        Map<String, Object> result
                = sampleService.getMatchedAndUnmatchedInputSampleIds(new ArrayList<>(sampleIds));
        if (result.isEmpty()) {
            LOG.error("None of the samples provided in the cohort sample list are known to SMILE: "
                    + mapper.writeValueAsString(result));
            throw new RuntimeException("Cohort does not have any known samples in SMILE"
                    + " - check data before reattempting.");
        }

        // merge cohort-samples
        List<String> primaryIds = (List<String>) result.get("matchedPrimaryIds");
        LOG.info("Adding cohort-sample edges in database for " + primaryIds.size() + " samples...");
        cohortCompleteRepository.addCohortSampleRelationship(cohort.getCohortId(), primaryIds);
        LOG.info("Done.");

        // create tempo nodes for samples that do not already have tempo data in smile
        Map<String, Object> samplesByTempoStatus = tempoService.sortSamplesByTempoStatus(primaryIds);
        if (samplesByTempoStatus.containsKey("false")) {
            LOG.info("Creating TEMPO nodes for cohort samples...");
            List<String> samplesMissingTempoData = (List<String>) samplesByTempoStatus.get("false");
            Integer actual = tempoService.batchCreateTempoNodesForSamplePrimaryIds(samplesMissingTempoData,
                    cohort.getLatestCohortComplete().getDate());
            if (actual != samplesMissingTempoData.size()) {
                LOG.error("Actual number of TEMPO nodes created does not match expected. "
                        + "Actual = " + actual + ", expected = " + samplesMissingTempoData.size());
            } else {
                LOG.info("Number of TEMPO nodes created = " + samplesMissingTempoData.size());
            }
            LOG.info("Done");
        }

        // re-calculate the intiial pipeline rundate embargo date, and access level for samples
        // that already have tempo data in smile
        if (samplesByTempoStatus.containsKey("true")) {
            LOG.info("Updating TEMPO nodes for cohort samples...");
            List<String> samplesWithTempoData = (List<String>) samplesByTempoStatus.get("true");
            tempoService.batchUpdateTempoDataForSamplePrimaryIds(samplesWithTempoData);
            LOG.info("Done. Number of TEMPO nodes updated = " + samplesWithTempoData.size());
        }

        // log and report unknown samples for reference
        List<String> unknownSamples = (List<String>) result.get("unmatchedIds");
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
    @Transactional(rollbackFor = {Exception.class})
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
