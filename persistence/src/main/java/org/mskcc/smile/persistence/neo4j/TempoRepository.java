package org.mskcc.smile.persistence.neo4j;

import java.util.List;
import java.util.UUID;
import org.mskcc.smile.model.tempo.BamComplete;
import org.mskcc.smile.model.tempo.Tempo;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */
@Repository
public interface TempoRepository extends Neo4jRepository<Tempo, Long> {
    @Query("MATCH (s: Sample {smileSampleId: $smileSampleId})-[:HAS_TEMPO]->(t: Tempo) "
            + "RETURN t")
    Tempo findTempoBySmileSampleId(@Param("smileSampleId") UUID smileSampleId);

    @Query("MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId}) "
            + "MATCH (s)-[:HAS_TEMPO]->(t:Tempo) RETURN t")
    Tempo findTempoBySamplePrimaryId(@Param("primaryId") String primaryId);

    @Query("MATCH (t: Tempo) WHERE ID(t) = $tempoId MATCH (t)-[:HAS_EVENT]->(bc: BamComplete) "
            + "RETURN bc")
    List<BamComplete> findBamCompleteEventsByTempoId(@Param("tempoId") Long tempoId);

    @Query("MATCH (t:Tempo) WHERE ID(t) = $tempoId MATCH (t)-[:HAS_EVENT]->(bc: BamComplete) "
            + "RETURN bc ORDER BY bc.timestamp DESC LIMIT 1")
    BamComplete findLatestBamCompleteEventByTempoId(@Param("tempoId") Long tempoId);

    @Query("MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId}) "
            + "MERGE (s)-[:HAS_TEMPO]->(t: Tempo) WITH s,t "
            + "MERGE (t)-[:HAS_EVENT]->(bc: BamComplete {timestamp: $bcEvent.timestamp, "
            + "status: $bcEvent.status}) WITH s,t,bc RETURN t")
    Tempo mergeBamCompleteEventBySamplePrimaryId(@Param("primaryId") String primaryId,
            @Param("bcEvent") BamComplete bcEvent);
}
