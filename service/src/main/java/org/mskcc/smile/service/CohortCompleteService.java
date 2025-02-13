package org.mskcc.smile.service;

import java.util.List;
import java.util.Set;
import org.mskcc.smile.model.tempo.Cohort;

/**
 *
 * @author ochoaa
 */
public interface CohortCompleteService {
    Cohort saveCohort(Cohort cohort, Set<String> samplePrimaryIds) throws Exception;
    Cohort getCohortByCohortId(String cohortId) throws Exception;
    List<Cohort> getCohortsBySamplePrimaryId(String primaryId) throws Exception;
    Boolean hasUpdates(Cohort existingCohort, Cohort cohort) throws Exception;
    Boolean hasCohortCompleteUpdates(Cohort existingCohort, Cohort cohort)
            throws Exception;
    Cohort updateCohort(Cohort cohort) throws Exception;
    public String getInitialPipelineRunDateBySamplePrimaryId(String primaryId) throws Exception;
}
