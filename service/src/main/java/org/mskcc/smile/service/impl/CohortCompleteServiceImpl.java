package org.mskcc.smile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.smile.commons.JsonComparator;
import org.mskcc.smile.model.converter.ArrayMapConverter;
import org.mskcc.smile.model.converter.ArrayStringConverter;
import org.mskcc.smile.model.tempo.Cohort;
import org.mskcc.smile.model.tempo.CohortComplete;
import org.mskcc.smile.model.tempo.CohortValidationStatus;
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

    private final ArrayMapConverter arrayMapConverter = new ArrayMapConverter();

    private final ArrayStringConverter arrayStringConverter = new ArrayStringConverter();

    private ObjectMapper mapper = new ObjectMapper();

    private static final Log LOG = LogFactory.getLog(CohortCompleteServiceImpl.class);

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void saveCohort(Cohort cohort, Set<String> sampleIds) throws Exception {
        // persist new cohort complete event to the db
        saveCohortComplete(cohort);
        // return early if no new samples to persist
        if (sampleIds == null || sampleIds.isEmpty()) {
            LOG.error("No samples to save for cohort: " + cohort.getCohortId()
                    + " - persisting cohort to SMILE and exiting.");
            return;
        }
        // persist changes to cohort sample list
        updateCohortSampleList(cohort, sampleIds);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void saveCohortComplete(Cohort cohort) throws Exception {
        // persist the new cohort-complete node and its relationship to the cohort via a
        // scoped Cypher query (MERGE/CREATE) rather than the generic repository save(),
        // which uses Neo4j-OGM's default unbounded save depth and would otherwise cascade
        // through every relationship currently populated on the in-memory Cohort object -
        // including its full cohortSamples list, which can be very large for some cohorts
        CohortComplete latestCohortComplete = cohort.getLatestCohortComplete();
        Map<String, Object> cohortCompleteProps = new HashMap<>();
        cohortCompleteProps.put("importDate", latestCohortComplete.getImportDate());
        cohortCompleteProps.put("date", latestCohortComplete.getDate());
        cohortCompleteProps.put("status", latestCohortComplete.getStatus());
        cohortCompleteProps.put("type", latestCohortComplete.getType());
        cohortCompleteProps.put("endUsers",
                arrayStringConverter.toGraphProperty(latestCohortComplete.getEndUsers()));
        cohortCompleteProps.put("pmUsers",
                arrayStringConverter.toGraphProperty(latestCohortComplete.getPmUsers()));
        cohortCompleteProps.put("projectTitle", latestCohortComplete.getProjectTitle());
        cohortCompleteProps.put("projectSubtitle", latestCohortComplete.getProjectSubtitle());
        cohortCompleteProps.put("pipelineVersion", latestCohortComplete.getPipelineVersion());
        cohortCompleteRepository.addCohortCompleteEvent(cohort.getCohortId(), cohortCompleteProps);
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
        if (hasCohortCompleteUpdates(existingCohort, cohort)) {
            return Boolean.TRUE;
        }
        // check for change in status of the latest cohort complete event
        CohortComplete existingLatest = existingCohort.getLatestCohortComplete();
        CohortComplete incomingLatest = cohort.getLatestCohortComplete();
        if (existingLatest != null && incomingLatest != null
                && !StringUtils.isBlank(existingLatest.getStatus())
                && !StringUtils.isBlank(incomingLatest.getStatus())
                && (existingLatest.getStatus().equals("PROVISIONAL")
                && !existingLatest.getStatus().equals(incomingLatest.getStatus()))) {
            return Boolean.TRUE;
        }
        // check for changes to cohort sample list
        return hasCohortSampleListUpdates(existingCohort.getCohortSamplePrimaryIds(),
                cohort.getCohortSamplePrimaryIds());
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
    public Boolean updateCohortSamplesList(Cohort cohort, Set<String> sampleIds)
            throws Exception {
        try {
            cohortCompleteRepository.detachExistingCohortSamples(cohort.getCohortId());
            updateCohortSampleList(cohort, sampleIds);
            return Boolean.TRUE;
        } catch (Exception e) {
            LOG.error("Error updating cohort sample list: " + cohort.getCohortId(), e);
            return Boolean.FALSE;
        }
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Boolean updateCohortValidationStatus(Cohort cohort) throws Exception {
        try {
            CohortValidationStatus validationStatus = cohort.getValidationStatus();
            // invalidTempoSamples is a List<Map<String, String>> which neo4j cannot store
            // directly as a property value - serialize it to JSON via the same converter
            // used for ogm entity persistence before binding it as a query parameter
            Map<String, Object> validationStatusParams = mapper.convertValue(validationStatus, Map.class);
            validationStatusParams.put("invalidTempoSamples",
                    arrayMapConverter.toGraphProperty((List) validationStatus.getInvalidTempoSamples()));
            cohortCompleteRepository.mergeCohortValidationStatus(cohort.getCohortId(),
                    validationStatusParams);
            return Boolean.TRUE;
        } catch (Exception e) {
            LOG.error("Error updating cohort validation status: " + cohort.getCohortId(), e);
            return Boolean.FALSE;
        }
    }

    private Cohort getDetailedCohortData(Cohort cohort) throws Exception {
        if (cohort == null || cohort.getId() == null) {
            return null;
        }
        // get cohort samples
        cohort.setCohortSamples(sampleService.getSamplesByCohortId(cohort.getCohortId()));
        return cohort;
    }

    @Override
    public Boolean hasCohortSampleListUpdates(Set<String> existingSamples,
            Set<String> incomingSamples) throws Exception {
        return !incomingSamples.equals(existingSamples);
    }

    private Boolean updateCohortSampleList(Cohort cohort, Set<String> sampleIds) throws Exception {
        try {
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
                return Boolean.FALSE;
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
            return Boolean.TRUE;
        } catch (Exception e) {
            LOG.error("Error during attempt to update-merge cohort sample list for cohort: "
                    + cohort.getCohortId());
            return Boolean.FALSE;
        }
    }
}
