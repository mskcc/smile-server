package org.mskcc.smile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.smile.commons.JsonComparator;
import org.mskcc.smile.model.tempo.Cohort;
import org.mskcc.smile.model.tempo.CohortComplete;
import org.mskcc.smile.persistence.neo4j.CohortCompleteRepository;
import org.mskcc.smile.service.CohortCompleteService;
import org.mskcc.smile.service.SmileSampleService;
import org.springframework.beans.factory.annotation.Autowired;
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

    private ObjectMapper mapper = new ObjectMapper();

    private static final Log LOG = LogFactory.getLog(CohortCompleteServiceImpl.class);

    @Override
    public Cohort saveCohort(Cohort cohort, Set<String> samplePrimaryIds) throws Exception {
        // persist new cohort complete event to the db
        cohortCompleteRepository.save(cohort);
        // create cohort-smaple relationships
        for (String primaryId : samplePrimaryIds) {
            // confirm sample exists by primary id and then link to cohort
            if (sampleService.sampleExistsByPrimaryId(primaryId)) {
                cohortCompleteRepository.addCohortSampleRelationship(cohort.getCohortId(), primaryId);
            }
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
    public Boolean hasUpdates(Cohort cohort, CohortComplete cohortComplete) throws Exception {
        String existingCohortComplete = mapper.writeValueAsString(cohort.getLatestCohortComplete());
        String currentCohortComplete = mapper.writeValueAsString(cohortComplete);
        return !jsonComparator.isConsistentGenericComparison(existingCohortComplete, currentCohortComplete);
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
        // get cohort complete events
        cohort.setCohortCompleteList(
                cohortCompleteRepository.findCohortCompleteEventsByCohortId(cohort.getCohortId()));
        return cohort;
    }


}
