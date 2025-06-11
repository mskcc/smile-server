package org.mskcc.smile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.smile.commons.JsonComparator;
import org.mskcc.smile.model.SmileSample;
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
    public Cohort saveCohort(Cohort cohort, Set<String> sampleIds) throws Exception {
        // persist new cohort complete event to the db
        cohortCompleteRepository.save(cohort);
        Set<String> unknownSamples = new HashSet<>(); // tracks unknown samples in smile
        // create cohort-sample relationships
        LOG.info("Adding cohort-sample edges in database for " + sampleIds.size() + " samples...");
        for (String sampleId : sampleIds) {
            // confirm sample exists by primary id and then link to cohort
            if (sampleService.sampleExistsByInputId(sampleId)) {
                String primaryId = sampleService.getSamplePrimaryIdBySampleInputId(sampleId);
                // init default tempo data for sample if sample does not already have tempo data
                if (tempoService.getTempoDataBySamplePrimaryId(primaryId) == null) {
                    tempoService.initAndSaveDefaultTempoData(primaryId, cohort.getLatestCohortComplete().getDate());
                }
                // if tempo node does not have initial pipeline run date/embargo date then calculate now and update values
                
                
                cohortCompleteRepository.addCohortSampleRelationship(cohort.getCohortId(), primaryId);
            } else {
                unknownSamples.add(sampleId);
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
