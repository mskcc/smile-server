package org.mskcc.smile.persistence.neo4j;

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
public interface CohortCompleteRepository extends Neo4jRepository<CohortComplete, Long> {
    @Query("""
           MERGE (c: Cohort {cohortId: $cohortId})
           WITH c
           MATCH (cc: CohortComplete) WHERE id(cc) = $cohortCompleteId
           MERGE (c)-[:HAS_COHORT_COMPLETE]->(cc)
           """)
    void mergeCohortCompleteEvent(@Param("cohortId") String cohortId,
            @Param("cohortCompleteId") Long cohortCompleteId);
}
