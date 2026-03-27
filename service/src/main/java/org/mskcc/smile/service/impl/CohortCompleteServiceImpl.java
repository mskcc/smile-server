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
        if (sampleIds == null || sampleIds.isEmpty()) {
            LOG.error("No samples saved for cohort: " + cohort.getCohortId()
                    + " - persisting cohort to SMILE and exiting.");
            return;
        }

        // process samples in chunks of 50
        List<String> sampleIdList = new ArrayList<>(sampleIds);
        List<String> primaryIds = new ArrayList<>();
        List<String> unknownSamples = new ArrayList<>();
        int chunkSize = 50;
        boolean anyChunkMatched = false;

        for (int i = 0; i < sampleIdList.size(); i += chunkSize) {
            List<String> chunk = sampleIdList.subList(i, Math.min(i + chunkSize, sampleIdList.size()));
            Map<String, Object> result = sampleService.getMatchedAndUnmatchedInputSampleIds(chunk);
            if (result.isEmpty()) {
                LOG.warn("None of the samples in chunk [" + i + "-"
                        + Math.min(i + chunkSize, sampleIdList.size())
                        + "] are known to SMILE - skipping chunk.");
                unknownSamples.addAll(chunk);
                continue;
            }
            anyChunkMatched = true;
            List<String> chunkMatched = (List<String>) result.get("matchedPrimaryIds");
            if (chunkMatched != null) {
                primaryIds.addAll(chunkMatched);
            }
            List<String> chunkUnmatched = (List<String>) result.get("unmatchedIds");
            if (chunkUnmatched != null) {
                unknownSamples.addAll(chunkUnmatched);
            }
        }

        if (!anyChunkMatched) {
            LOG.error("None of the samples provided in the cohort sample list are known to SMILE.");
            throw new RuntimeException("Cohort does not have any known samples in SMILE"
                    + " - check data before reattempting.");
        }

        // merge cohort-samples in chunks
        LOG.info("Adding cohort-sample edges in database for " + primaryIds.size() + " samples...");
        for (int i = 0; i < primaryIds.size(); i += chunkSize) {
            List<String> chunk = primaryIds.subList(i, Math.min(i + chunkSize, primaryIds.size()));
            cohortCompleteRepository.addCohortSampleRelationship(cohort.getCohortId(), chunk);
        }
        LOG.info("Done.");

        // create tempo nodes for samples that do not already have tempo data in smile
        Map<String, Object> samplesByTempoStatus = tempoService.sortSamplesByTempoStatus(primaryIds);
        if (samplesByTempoStatus.containsKey("false")) {
            LOG.info("Creating TEMPO nodes for cohort samples...");
            List<String> samplesMissingTempoData = (List<String>) samplesByTempoStatus.get("false");
            int actual = 0;
            for (int i = 0; i < samplesMissingTempoData.size(); i += chunkSize) {
                List<String> chunk = samplesMissingTempoData.subList(
                        i, Math.min(i + chunkSize, samplesMissingTempoData.size()));
                actual += tempoService.batchCreateTempoNodesForSamplePrimaryIds(chunk,
                        cohort.getLatestCohortComplete().getDate());
            }
            if (actual != samplesMissingTempoData.size()) {
                LOG.error("Actual number of TEMPO nodes created does not match expected. "
                        + "Actual = " + actual + ", expected = " + samplesMissingTempoData.size());
            } else {
                LOG.info("Number of TEMPO nodes created = " + samplesMissingTempoData.size());
            }
            LOG.info("Done");
        }

        // re-calculate the initial pipeline rundate, embargo date, and access level for samples
        // that already have tempo data in smile
        if (samplesByTempoStatus.containsKey("true")) {
            LOG.info("Updating TEMPO nodes for cohort samples...");
            List<String> samplesWithTempoData = (List<String>) samplesByTempoStatus.get("true");
            tempoService.batchUpdateTempoDataForSamplePrimaryIds(samplesWithTempoData);
            LOG.info("Done. Number of TEMPO nodes updated = " + samplesWithTempoData.size());
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
