package org.mskcc.smile.persistence.neo4j;

import java.util.List;
import java.util.Map;
import org.mskcc.smile.model.tempo.Cohort;
import org.mskcc.smile.model.tempo.CohortComplete;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */
@Repository
public interface CohortCompleteRepository extends Neo4jRepository<Cohort, Long> {
    @Query("""
           MATCH (c: Cohort {cohortId: $cohortId})-[hcc:HAS_COHORT_COMPLETE]->(cc: CohortComplete)
           OPTIONAL MATCH (c)-[hs:HAS_STATUS]->(cvs: CohortValidationStatus)
           RETURN DISTINCT c, hcc, cc, hs, cvs
           """)
    Cohort findCohortByCohortId(@Param("cohortId") String cohortId);

    @Query("""
           MATCH (c: Cohort {cohortId: $cohortId})-[:HAS_COHORT_COMPLETE]->(cc: CohortComplete)
           RETURN cc ORDER BY cc.importDate DESC LIMIT 1
           """)
    CohortComplete findLatestCohortCompleteEventByCohortId(@Param("cohortId") String cohortId);

    @Query("""
           MATCH (c: Cohort)-[:HAS_COHORT_SAMPLE]->(s: Sample)-[:HAS_METADATA]->
           (sm: SampleMetadata {primaryId: $primaryId})
           RETURN DISTINCT c
           """)
    List<Cohort> findCohortsBySamplePrimaryId(@Param("primaryId") String primaryId);

    @Query("""
           MATCH (c: Cohort {cohortId: $cohortId})
           MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata)
           WHERE sm.primaryId IN $primaryIds
           WITH s, c
           MERGE (c)-[hcs:HAS_COHORT_SAMPLE]->(s)
           """)
    void addCohortSampleRelationship(@Param("cohortId") String cohortId,
            @Param("primaryIds") List<String> primaryIds);

    @Query("""
           MATCH (c: Cohort {cohortId: $cohortId})-[r:HAS_COHORT_SAMPLE]->(s:Sample) DELETE r
           """)
    void detachExistingCohortSamples(@Param("cohortId") String cohortId);

    @Query("""
           MERGE (c: Cohort {cohortId: $cohortId})
           CREATE (cc: CohortComplete)
           SET cc = $cohortCompleteProps
           CREATE (c)-[:HAS_COHORT_COMPLETE]->(cc)
           """)
    void addCohortCompleteEvent(@Param("cohortId") String cohortId,
            @Param("cohortCompleteProps") Map<String, Object> cohortCompleteProps);

    @Query("""
           MATCH (c: Cohort {cohortId: $cohortId})
           MERGE (c)-[:HAS_STATUS]->(cvs: CohortValidationStatus)
           SET cvs.jsonSchemaValidated = $validationStatus.jsonSchemaValidated,
            cvs.passesAllChecks = $validationStatus.passesAllChecks,
            cvs.invalidEndUsers = $validationStatus.invalidEndUsers,
            cvs.invalidPmUsers = $validationStatus.invalidPmUsers,
            cvs.invalidTempoSamples = $validationStatus.invalidTempoSamples
           """)
    void mergeCohortValidationStatus(@Param("cohortId") String cohortId,
            @Param("validationStatus") Map<String, Object> validationStatus);
}
