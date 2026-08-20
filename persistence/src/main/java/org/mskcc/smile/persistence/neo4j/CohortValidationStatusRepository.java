package org.mskcc.smile.persistence.neo4j;

import org.mskcc.smile.model.tempo.CohortValidationStatus;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */
@Repository
public interface CohortValidationStatusRepository extends Neo4jRepository<CohortValidationStatus, Long> {
    @Query("""
           MATCH (c: Cohort {cohortId: $cohortId})
           OPTIONAL MATCH (c)-[:HAS_STATUS]->(existing: CohortValidationStatus)
           WITH c, existing
           MATCH (cvs: CohortValidationStatus) WHERE id(cvs) = $cvsId
           MERGE (c)-[:HAS_STATUS]->(cvs)
           DETACH DELETE existing
           """)
    void mergeCohortValidationStatus(@Param("cohortId") String cohortId,
            @Param("cvsId") Long cvsId);
}
