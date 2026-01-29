package org.mskcc.smile.persistence.neo4j;

import java.util.List;
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
           RETURN DISTINCT c, hcc, cc
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
           MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId})
           MATCH (c: Cohort {cohortId: $cohortId})
           MERGE (c)-[hcs:HAS_COHORT_SAMPLE]->(s)
           """)
    void addCohortSampleRelationship(@Param("cohortId") String cohortId,
            @Param("primaryId") String primaryId);
}
