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
public interface TempoRepository extends Neo4jRepository<Tempo, UUID> {
    @Query("MATCH (s: Sample {smileSampleId: $smileSampleId})-[:HAS_TEMPO]->(t: Tempo) "
            + "RETURN DISTINCT t")
    Tempo findTempoBySmileSampleId(@Param("smileSampleId") UUID smileSampleId);

    @Query("MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId}) "
            + "MATCH (s)-[:HAS_TEMPO]->(t:Tempo) RETURN DISTINCT t")
    Tempo findTempoBySamplePrimaryId(@Param("primaryId") String primaryId);

    @Query("MATCH (t: Tempo) WHERE t.smileTempoId = $smileTempoId MATCH (t)-[:HAS_EVENT]->(bc: BamComplete) "
            + "RETURN DISTINCT bc")
    List<BamComplete> findBamCompleteEventsByTempoId(@Param("smileTempoId") UUID smileTempoId);

    @Query("MATCH (t: Tempo) WHERE t.smileTempoId = $smileTempoId MATCH (t)-[:HAS_EVENT]->(qc: QcComplete) "
            + "RETURN DISTINCT qc")
    List<QcComplete> findQcCompleteEventsByTempoId(@Param("smileTempoId") UUID smileTempoId);
    @Query("MATCH (t: Tempo) WHERE t.smileTempoId = $smileTempoId MATCH (t)-[:HAS_EVENT]->(mc: MafComplete) "
            + "RETURN DISTINCT mc")
    List<MafComplete> findMafCompleteEventsByTempoId(@Param("smileTempoId") UUID smileTempoId);

    @Query("MATCH (t:Tempo) WHERE t.smileTempoId = $smileTempoId MATCH (t)-[:HAS_EVENT]->(bc: BamComplete) "
            + "RETURN DISTINCT bc ORDER BY bc.date DESC LIMIT 1")
    BamComplete findLatestBamCompleteEventByTempoId(@Param("smileTempoId") UUID smileTempoId);

    @Query("MATCH (t:Tempo) WHERE t.smileTempoId = $smileTempoId MATCH (t)-[:HAS_EVENT]->(mc: MafComplete) "
            + "RETURN DISTINCT mc ORDER BY mc.date DESC LIMIT 1")
    MafComplete findLatestMafCompleteEventByTempoId(@Param("smileTempoId") UUID smileTempoId);

    @Query("MATCH (t:Tempo) WHERE t.smileTempoId = $smileTempoId MATCH (t)-[:HAS_EVENT]->(qc: QcComplete) "
            + "RETURN DISTINCT qc ORDER BY qc.date DESC LIMIT 1")
    QcComplete findLatestQcCompleteEventByTempoId(@Param("smileTempoId") UUID smileTempoId);

    @Query("MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId}) "
            + "MERGE (s)-[ht:HAS_TEMPO]->(t: Tempo) WITH s,t "
            + "MERGE (t)-[he:HAS_EVENT]->(bc: BamComplete {date: $bcEvent.date, "
            + "status: $bcEvent.status}) WITH s,t,bc RETURN DISTINCT t")
    Tempo mergeBamCompleteEventBySamplePrimaryId(@Param("primaryId") String primaryId,
            @Param("bcEvent") BamComplete bcEvent);

    @Query("MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId}) "
            + "MERGE (s)-[ht:HAS_TEMPO]->(t: Tempo) WITH s,t "
            + "MERGE (t)-[he:HAS_EVENT]->(qc: QcComplete {date: $qcEvent.date, result: $qcEvent.result, "
            + "reason: $qcEvent.reason, status: $qcEvent.status}) WITH s,t,qc RETURN DISTINCT t")
    Tempo mergeQcCompleteEventBySamplePrimaryId(@Param("primaryId") String primaryId,
            @Param("qcEvent") QcComplete qcEvent);

    @Query("MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId}) "
            + "MERGE (s)-[ht:HAS_TEMPO]->(t: Tempo) WITH s,t "
            + "MERGE (t)-[he:HAS_EVENT]->(mc: MafComplete {date: $mcEvent.date, "
            + "normalPrimaryId: $mcEvent.normalPrimaryId, status: $mcEvent.status}) "
            + "WITH s,t,mc RETURN DISTINCT t")
    Tempo mergeMafCompleteEventBySamplePrimaryId(@Param("primaryId") String primaryId,
            @Param("mcEvent") MafComplete mcEvent);

    @Query("MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $billing.primaryId}) "
            + "MATCH (s)-[:HAS_TEMPO]->(t: Tempo) "
            + "SET t.billed = $billing.billed, t.billedBy = $billing.billedBy, "
            + "t.costCenter = $billing.costCenter, t.custodianInformation = $billing.custodianInformation, "
            + "t.accessLevel = $billing.accessLevel")
    void updateSampleBilling(@Param("billing") SampleBillingJson billing);

    @Query("MATCH (cc:CohortComplete)<-[:HAS_COHORT_COMPLETE]-(c:Cohort)-[:HAS_COHORT_SAMPLE]"
            + "->(s:Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId}) "
            + "RETURN DISTINCT cc.date ORDER BY cc.date ASC LIMIT 1")
    String findInitialPipelineRunDateBySamplePrimaryId(@Param("primaryId") String primaryId);
}
