package org.mskcc.smile.persistence.neo4j;

import java.util.UUID;
import org.mskcc.smile.model.DbGap;
import org.mskcc.smile.model.json.DbGapJson;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DbGapRepository extends Neo4jRepository<DbGap, UUID> {
    @Query("MATCH (s: Sample {smileSampleId: $smileSampleId})-[:HAS_DBGAP]->(d: DbGap) "
            + "RETURN DISTINCT d")
    DbGap findDbGapBySmileSampleId(@Param("smileSampleId") UUID smileSampleId);

    @Query("MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId}) "
            + "MATCH (s)-[:HAS_DBGAP]->(d: DbGap) RETURN DISTINCT d")
    DbGap findDbGapBySamplePrimaryId(@Param("primaryId") String primaryId);

    @Query("MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $dbGap.primaryId}) "
            + "MERGE (s)-[:HAS_DBGAP]->(d: DbGap) "
            + "SET d.dbGapStudy = $dbGap.dbGapStudy, d.smileDbGapId = $smileDbGapId "
            + "RETURN DISTINCT d")
    DbGap updateDbGap(@Param("smileDbGapId") UUID smileDbGapId, @Param("dbGap") DbGapJson dbGap);
}
