package org.mskcc.smile.service;

import java.util.List;
import java.util.Set;
import org.mskcc.smile.model.tempo.Cohort;

/**
 *
 * @author ochoaa
 */
public interface CohortCompleteService {
    void saveCohort(Cohort cohort, Set<String> samplePrimaryIds) throws Exception;
    void saveCohortComplete(Cohort cohort) throws Exception;
    Cohort getCohortByCohortId(String cohortId) throws Exception;
    List<Cohort> getCohortsBySamplePrimaryId(String primaryId) throws Exception;
    Boolean hasUpdates(Cohort existingCohort, Cohort cohort) throws Exception;
    Boolean hasCohortCompleteUpdates(Cohort existingCohort, Cohort cohort)
            throws Exception;
    Boolean hasCohortSampleListUpdates(Set<String> existingSamples, Set<String> incomingSamples)
            throws Exception;
    Boolean updateCohortSamplesList(Cohort cohort, Set<String> sampleIds)
            throws Exception;
    Boolean updateCohortValidationStatus(Cohort cohort) throws Exception;
}
