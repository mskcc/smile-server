package org.mskcc.smile.persistence.neo4j;

import java.util.List;
import java.util.Map;
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
    @Query("""
           MATCH (s: Sample {smileSampleId: $smileSampleId})-[:HAS_TEMPO]->(t: Tempo)
           RETURN DISTINCT t
           """)
    Tempo findTempoBySmileSampleId(@Param("smileSampleId") UUID smileSampleId);

    @Query("""
           MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId})
           MATCH (s)-[:HAS_TEMPO]->(t:Tempo)
           RETURN DISTINCT t
           """)
    Tempo findTempoBySamplePrimaryId(@Param("primaryId") String primaryId);

    @Query("""
           MATCH (t: Tempo)
           WHERE t.smileTempoId = $smileTempoId
           MATCH (t)-[:HAS_EVENT]->(bc: BamComplete)
           RETURN DISTINCT bc
           """)
    List<BamComplete> findBamCompleteEventsByTempoId(@Param("smileTempoId") UUID smileTempoId);

    @Query("""
           MATCH (t: Tempo)
           WHERE t.smileTempoId = $smileTempoId
           MATCH (t)-[:HAS_EVENT]->(qc: QcComplete)
           RETURN DISTINCT qc
           """)
    List<QcComplete> findQcCompleteEventsByTempoId(@Param("smileTempoId") UUID smileTempoId);

    @Query("""
           MATCH (t: Tempo)
           WHERE t.smileTempoId = $smileTempoId
           MATCH (t)-[:HAS_EVENT]->(mc: MafComplete)
           RETURN DISTINCT mc
           """)
    List<MafComplete> findMafCompleteEventsByTempoId(@Param("smileTempoId") UUID smileTempoId);

    @Query("""
           MATCH (t:Tempo)
           WHERE t.smileTempoId = $smileTempoId
           MATCH (t)-[:HAS_EVENT]->(bc: BamComplete)
           RETURN bc ORDER BY bc.date DESC LIMIT 1
           """)
    BamComplete findLatestBamCompleteEventByTempoId(@Param("smileTempoId") UUID smileTempoId);

    @Query("""
           MATCH (t:Tempo)
           WHERE t.smileTempoId = $smileTempoId
           MATCH (t)-[:HAS_EVENT]->(mc: MafComplete)
           RETURN mc ORDER BY mc.date DESC LIMIT 1
           """)
    MafComplete findLatestMafCompleteEventByTempoId(@Param("smileTempoId") UUID smileTempoId);

    @Query("""
           MATCH (t:Tempo)
           WHERE t.smileTempoId = $smileTempoId
           MATCH (t)-[:HAS_EVENT]->(qc: QcComplete)
           RETURN qc ORDER BY qc.date DESC LIMIT 1
           """)
    QcComplete findLatestQcCompleteEventByTempoId(@Param("smileTempoId") UUID smileTempoId);

    @Query("""
           MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId})
           MERGE (s)-[ht:HAS_TEMPO]->(t: Tempo)
           WITH s, t MERGE (t)-[he:HAS_EVENT]->(bc: BamComplete {
            date: $bcEvent.date, status: $bcEvent.status
           })
           WITH s, t, bc
           RETURN DISTINCT t
           """)
    Tempo mergeBamCompleteEventBySamplePrimaryId(@Param("primaryId") String primaryId,
            @Param("bcEvent") BamComplete bcEvent);

    @Query("""
           MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId})
           MERGE (s)-[ht:HAS_TEMPO]->(t: Tempo)
           WITH s, t MERGE (t)-[he:HAS_EVENT]->(qc: QcComplete {
            date: $qcEvent.date, result: $qcEvent.result, reason: $qcEvent.reason,
            status: $qcEvent.status
           })
           WITH s, t, qc
           RETURN DISTINCT t
           """)
    Tempo mergeQcCompleteEventBySamplePrimaryId(@Param("primaryId") String primaryId,
            @Param("qcEvent") QcComplete qcEvent);

    @Query("""
           MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId})
           MERGE (s)-[ht:HAS_TEMPO]->(t: Tempo)
           WITH s,t
           MERGE (t)-[he:HAS_EVENT]->(mc: MafComplete {
            date: $mcEvent.date, normalPrimaryId: $mcEvent.normalPrimaryId, status: $mcEvent.status
           })
           WITH s, t, mc
           RETURN DISTINCT t
           """)
    Tempo mergeMafCompleteEventBySamplePrimaryId(@Param("primaryId") String primaryId,
            @Param("mcEvent") MafComplete mcEvent);

    @Query("""
           MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $billing.primaryId})
           MATCH (s)-[:HAS_TEMPO]->(t: Tempo)
           SET t.billed = $billing.billed, t.billedBy = $billing.billedBy,
            t.costCenter = $billing.costCenter, t.custodianInformation = $billing.custodianInformation,
            t.accessLevel = $billing.accessLevel
           """)
    void updateSampleBilling(@Param("billing") SampleBillingJson billing);

    @Query("""
           MATCH (cc:CohortComplete)<-[:HAS_COHORT_COMPLETE]-(c:Cohort)-[:HAS_COHORT_SAMPLE]->
           (s:Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId})
           RETURN cc.date ORDER BY cc.date ASC LIMIT 1
           """)
    String findEarliestCohortDeliveryDateBySamplePrimaryId(@Param("primaryId") String primaryId);

    @Query("""
           MATCH (t:Tempo {accessLevel: 'MSK Embargo'})
           WHERE date(t.embargoDate) < date()
           RETURN t.smileTempoId
           """)
    List<UUID> findTempoIdsNoLongerEmbargoed();

    @Query("""
           MATCH (sm: SampleMetadata)<-[:HAS_METADATA]-(s: Sample)-[:HAS_TEMPO]->(t:Tempo)
           WHERE sm.primaryId IN $samplePrimaryIds
           SET t.accessLevel = $accessLevel
           """)
    void updateTempoAccessLevelBySamplePrimaryIds(@Param("samplePrimaryIds") List<String> samplePrimaryIds,
            @Param("accessLevel") String accessLevel);

    @Query("""
           MATCH (s:Sample)-[:HAS_TEMPO]->(t:Tempo)
           WITH s, t, COLLECT {
            MATCH (s)-[:HAS_METADATA]->(sm:SampleMetadata)
            RETURN sm ORDER BY sm.importDate DESC LIMIT 1
           } AS latestSm
           WITH s, t, latestSm[0] as latestSm
           MATCH (p:Patient)-[:HAS_SAMPLE]->(s)
           OPTIONAL MATCH (p)<-[:IS_ALIAS]-(pa:PatientAlias{namespace: 'cmoId'})
           WITH s, t, latestSm, p, pa AS cmoIdAlias
           OPTIONAL MATCH (p)<-[:IS_ALIAS]-(pa:PatientAlias{namespace: 'dmpId'})
           WITH s,t,latestSm,p,cmoIdAlias, pa as dmpIdAlias,
            CASE WHEN latestSm.investigatorSampleId =~ '^P-\\d{7}-.*-WES$'
            THEN true ELSE false END as recapture
           WITH latestSm.primaryId AS primaryId, latestSm.cmoSampleName AS cmoSampleName,
            t.accessLevel AS accessLevel, t.custodianInformation AS custodianInformation,
            latestSm.baitSet AS baitSet, latestSm.genePanel AS genePanel,
            latestSm.oncotreeCode AS oncotreeCode, latestSm.cmoPatientId AS cmoPatientId,
            dmpIdAlias.value AS dmpPatientId, recapture AS recapture,
            latestSm.investigatorSampleId AS dmpSampleId
           WITH ({ primaryId: primaryId, cmoSampleName: cmoSampleName, accessLevel: accessLevel,
            custodianInformation: custodianInformation, baitSet: baitSet, genePanel: genePanel,
            oncotreeCode: oncotreeCode, cmoPatientId: cmoPatientId, dmpPatientId: dmpPatientId,
            recapture: recapture, dmpSampleId: dmpSampleId }) AS result
           WHERE result.primaryId = $primaryId
           RETURN result
           """)
    Map<String, Object> findTempoSampleDataBySamplePrimaryId(@Param("primaryId") String primaryId);

    @Query("""
           MATCH (t: Tempo{smileTempoId: $smileTempoId})
           SET t.initialPipelineRunDate = $initialPipelineRunDate,
           t.embargoDate = $embargoDate,
           t.accessLevel = $accessLevel
           """)
    void updateTempoData(@Param("smileTempoId") UUID smileTempoId,
            @Param("initialPipelineRunDate") String initialPipelineRunDate,
            @Param("embargoDate") String embargoDate,
            @Param("accessLevel") String accessLevel);
}
