package org.mskcc.smile.persistence.neo4j;

import java.util.List;
import java.util.UUID;
import org.mskcc.smile.model.tempo.BamComplete;
import org.mskcc.smile.model.tempo.MafComplete;
import org.mskcc.smile.model.tempo.QcComplete;
import org.mskcc.smile.model.tempo.Tempo;
import org.mskcc.smile.model.tempo.json.SampleBillingJson;
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

    @Query("MATCH (t: Tempo) WHERE ID(t) = $tempoId MATCH (t)-[:HAS_EVENT]->(qc: QcComplete) "
            + "RETURN qc")
    List<QcComplete> findQcCompleteEventsByTempoId(@Param("tempoId") Long tempoId);
    @Query("MATCH (t: Tempo) WHERE ID(t) = $tempoId MATCH (t)-[:HAS_EVENT]->(mc: MafComplete) "
            + "RETURN mc")
    List<MafComplete> findMafCompleteEventsByTempoId(@Param("tempoId") Long tempoId);

    @Query("MATCH (t:Tempo) WHERE ID(t) = $tempoId MATCH (t)-[:HAS_EVENT]->(bc: BamComplete) "
            + "RETURN bc ORDER BY bc.date DESC LIMIT 1")
    BamComplete findLatestBamCompleteEventByTempoId(@Param("tempoId") Long tempoId);

    @Query("MATCH (t:Tempo) WHERE ID(t) = $tempoId MATCH (t)-[:HAS_EVENT]->(mc: MafComplete) "
            + "RETURN mc ORDER BY mc.date DESC LIMIT 1")
    MafComplete findLatestMafCompleteEventByTempoId(@Param("tempoId") Long tempoId);

    @Query("MATCH (t:Tempo) WHERE ID(t) = $tempoId MATCH (t)-[:HAS_EVENT]->(qc: QcComplete) "
            + "RETURN qc ORDER BY qc.date DESC LIMIT 1")
    QcComplete findLatestQcCompleteEventByTempoId(@Param("tempoId") Long tempoId);

    @Query("MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId}) "
            + "MERGE (s)-[:HAS_TEMPO]->(t: Tempo) WITH s,t "
            + "MERGE (t)-[:HAS_EVENT]->(bc: BamComplete {date: $bcEvent.date, "
            + "status: $bcEvent.status}) WITH s,t,bc RETURN t")
    Tempo mergeBamCompleteEventBySamplePrimaryId(@Param("primaryId") String primaryId,
            @Param("bcEvent") BamComplete bcEvent);

    @Query("MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId}) "
            + "MERGE (s)-[:HAS_TEMPO]->(t: Tempo) WITH s,t "
            + "MERGE (t)-[:HAS_EVENT]->(qc: QcComplete {date: $qcEvent.date, result: $qcEvent.result, "
            + "reason: $qcEvent.reason, status: $qcEvent.status}) WITH s,t,qc RETURN t")
    Tempo mergeQcCompleteEventBySamplePrimaryId(@Param("primaryId") String primaryId,
            @Param("qcEvent") QcComplete qcEvent);

    @Query("MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId}) "
            + "MERGE (s)-[:HAS_TEMPO]->(t: Tempo) WITH s,t "
            + "MERGE (t)-[:HAS_EVENT]->(mc: MafComplete {date: $mcEvent.date, "
            + "normalPrimaryId: $mcEvent.normalPrimaryId, status: $mcEvent.status}) "
            + "WITH s,t,mc RETURN t")
    Tempo mergeMafCompleteEventBySamplePrimaryId(@Param("primaryId") String primaryId,
            @Param("mcEvent") MafComplete mcEvent);

    @Query("MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $billing.primaryId}) "
            + "MATCH (s)-[:HAS_TEMPO]->(t: Tempo) "
            + "SET t.billed = $billing.billed, t.billedBy = $billing.billedBy, "
            + "t.costCenter = $billing.costCenter")
    void updateSampleBilling(@Param("billing") SampleBillingJson billing);
}
