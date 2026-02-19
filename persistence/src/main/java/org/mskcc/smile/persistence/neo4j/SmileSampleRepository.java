package org.mskcc.smile.persistence.neo4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.mskcc.smile.model.SampleMetadata;
import org.mskcc.smile.model.SmileSample;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */

@Repository
public interface SmileSampleRepository extends Neo4jRepository<SmileSample, UUID> {
    @Query("""
           MATCH (s: Sample {smileSampleId: $smileSampleId})-[hsm:HAS_METADATA]->(sm: SampleMetadata)
           MATCH (s)<-[isa:IS_ALIAS]-(sa: SampleAlias)
           OPTIONAL MATCH (sm)-[hss:HAS_STATUS]->(ss: Status)
           OPTIONAL MATCH (s)-[ht:HAS_TEMPO]->(t: Tempo)
           OPTIONAL MATCH (t)-[hbe:HAS_EVENT]->(bc: BamComplete)
           OPTIONAL MATCH (t)-[hqe:HAS_EVENT]->(qc: QcComplete)
           OPTIONAL MATCH (t)-[hme:HAS_EVENT]->(mc: MafComplete)
           RETURN DISTINCT s, hsm, sm, hss, ss, isa, sa, ht, t, hbe, bc, hqe, qc, hme, mc
           """)
    SmileSample findSampleBySampleSmileId(@Param("smileSampleId") UUID smileSampleId);

    @Query("""
           MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId})
           RETURN DISTINCT s
           """)
    SmileSample findSampleByPrimaryId(@Param("primaryId") String primaryId);

    @Query("""
           MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {cmoSampleName: $cmoSampleName})
           RETURN DISTINCT s
           """)
    List<SmileSample> findSamplesByCmoSampleName(@Param("cmoSampleName") String cmoSampleName);

    @Query("""
           MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata)
           WHERE sm.additionalProperties CONTAINS $altId
           RETURN DISTINCT s
           """)
    List<SmileSample> findSamplesByAltId(@Param("altId") String altId);

    @Query("""
           MATCH (s: Sample {smileSampleId: $smileSampleId})-[:HAS_METADATA]->(sm: SampleMetadata)
           OPTIONAL MATCH (sm)-[hs:HAS_STATUS]->(ss: Status)
           RETURN sm, hs, ss
           """)
    List<SampleMetadata> findAllSampleMetadataListBySampleId(@Param("smileSampleId") UUID smileSampleId);

    @Query("""
           MATCH (s: Sample {smileSampleId: $smileSample.smileSampleId})
           MATCH (s)<-[:HAS_SAMPLE]-(p: Patient)
           MATCH (n: Sample)<-[:HAS_SAMPLE]-(p)
           WHERE toLower(n.sampleClass) = 'normal'
           RETURN DISTINCT n
           """)
    List<SmileSample> findMatchedNormalsByResearchSample(
            @Param("smileSample") SmileSample smileSample);

    @Query("""
           MATCH (r: Request {igoRequestId: $reqId})-[:HAS_SAMPLE]->(s: Sample)
           RETURN DISTINCT s
           """)
    List<SmileSample> findResearchSamplesByRequest(@Param("reqId") String reqId);

    @Query("""
           MATCH (r: Request {igoRequestId: $reqId})
           MATCH (r)-[:HAS_SAMPLE]->(s: Sample)
           MATCH (s)<-[:IS_ALIAS]-(sa: SampleAlias {namespace: 'igoId', value: $igoId})
           RETURN DISTINCT s
           """)
    SmileSample findResearchSampleByRequestAndIgoId(@Param("reqId") String reqId,
            @Param("igoId") String igoId);

    @Query("""
           MATCH (pa: PatientAlias {namespace: 'cmoId', value: $cmoPatientId})-[:IS_ALIAS]->
           (p: Patient)-[:HAS_SAMPLE]->(s: Sample)
           RETURN DISTINCT s
           """)
    List<SmileSample> findAllSamplesByCmoPatientId(
            @Param("cmoPatientId") String cmoPatientId);

    @Query("""
           MATCH (pa: PatientAlias {namespace: 'cmoId', value: $cmoPatientId})-[:IS_ALIAS]->
           (p: Patient)-[:HAS_SAMPLE]->(s: Sample {sampleCategory: $sampleCategory})
           RETURN DISTINCT s
           """)
    List<SmileSample> findAllSamplesByCategoryAndCmoPatientId(
            @Param("cmoPatientId") String cmoPatientId, @Param("sampleCategory") String sampleCategory);

    @Query("""
           MATCH (sa :SampleAlias {value:$value, namespace: $namespace})-[:IS_ALIAS]->
           (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata)
           OPTIONAL MATCH (sm)-[hs:HAS_STATUS]->(ss: Status)
           RETURN sm, hs, ss
           """)
    List<SampleMetadata> findSampleMetadataHistoryByNamespaceValue(
            @Param("namespace") String namespace, @Param("value") String value);

    @Query("""
           MATCH (s: Sample {smileSampleId: $smileSampleId})
           MATCH (p: Patient {smilePatientId: $smilePatientId})
           MERGE (s)<-[hs:HAS_SAMPLE]-(p)
           """)
    void updateSamplePatientRelationship(@Param("smileSampleId") UUID smileSampleId,
            @Param("smilePatientId") UUID smilePatientId);

    @Query("""
           MATCH (s: Sample {sampleCategory: 'research'})-[:HAS_METADATA]->(sm: SampleMetadata)
           WHERE sm.importDate >= $inputDate
           RETURN DISTINCT ({smileSampleId: s.smileSampleId,
            importDate: apoc.date.format(sm.importDate, "ms", "yyyy-MM-dd"),
            primaryId: sm.primaryId, cmoSampleName: sm.cmoSampleName})
           """)
    List<Object> findSamplesByLatestImportDate(@Param("inputDate") Long inputDate);

    @Query("""
           MATCH (s: Sample {smileSampleId: $smileSampleId})-[:HAS_METADATA]->(sm: SampleMetadata)
           OPTIONAL MATCH (sm)-[hs:HAS_STATUS]->(ss: Status)
           RETURN sm, hs, ss ORDER BY sm.importDate DESC LIMIT 1
           """)
    SampleMetadata findLatestSampleMetadataBySmileId(@Param("smileSampleId") UUID smileSampleId);


    @Query("""
           MATCH (s: Sample)<-[:IS_ALIAS]-(sa: SampleAlias)
           WITH s, sa, COLLECT {
            MATCH (s)-[:HAS_METADATA]->(sm: SampleMetadata)
            RETURN sm ORDER BY sm.importDate DESC LIMIT 1
           } as latestSm
           WITH s, sa, latestSm[0] as latestSm
           WHERE latestSm.primaryId = $inputId
           OR latestSm.cmoSampleName = $inputId
           OR latestSm.investigatorSampleId = $inputId
           OR s.smileSampleId = $inputId
           OR sa.value = $inputId
           RETURN DISTINCT s
           """)
    SmileSample findSampleByInputId(@Param("inputId") String inputId);

    @Query("""
           MATCH (s: Sample {smileSampleId: $smileSampleId})
           MATCH (s)<-[r:HAS_SAMPLE]-(p: Patient {smilePatientId: $smilePatientId})
           DELETE r
           """)
    void removeSamplePatientRelationship(@Param("smileSampleId") UUID smileSampleId,
            @Param("smilePatientId") UUID smilePatientId);

    @Query("""
           MATCH (r:Request {smileRequestId: $smileRequestId})
           MATCH (s:Sample {smileSampleId: $smileSampleId})
           MERGE (r)-[hs:HAS_SAMPLE]->(s)
           """)
    void createSampleRequestRelationship(@Param("smileSampleId") UUID smileSampleId,
            @Param("smileRequestId") UUID smileRequestId);

    @Query("""
           MATCH (s: Sample {smileSampleId: $smileSampleId})
           SET s.revisable = $revisable
           RETURN DISTINCT s
           """)
    SmileSample updateRevisableBySampleId(@Param("smileSampleId") UUID smileSampleId,
            @Param("revisable") Boolean revisable);

    @Query("""
           MATCH (c: Cohort {cohortId: $cohortId})-[:HAS_COHORT_SAMPLE]->(s: Sample)
           MATCH (sa: SampleAlias)-[ria:IS_ALIAS]->(s)-[rhm:HAS_METADATA]->(sm: SampleMetadata)
           RETURN DISTINCT s, sa, sm, ria, rhm
           """)
    List<SmileSample> findSamplesByCohortId(@Param("cohortId") String cohortId);

    @Query("""
           MATCH (sm:SampleMetadata {primaryId: $primaryId})
           RETURN sm ORDER BY sm.importDate DESC LIMIT 1
           """)
    SampleMetadata findLatestSampleMetadataByPrimaryId(@Param("primaryId") String primaryId);

    @Query("""
           MATCH (t:Tempo)<-[:HAS_TEMPO]-(s:Sample)-[:HAS_METADATA]->(sm:SampleMetadata)
           WHERE t.smileTempoId IN $smileTempoIds
           RETURN DISTINCT sm.primaryId
           """)
    List<String> findSamplePrimaryIdsBySmileTempoIds(@Param("smileTempoIds") List<UUID> smileTempoIds);

    @Query("""
           MATCH (sm:SampleMetadata)<-[:HAS_METADATA]-(s:Sample)<-[:IS_ALIAS]-(sa:SampleAlias)
           WHERE sm.primaryId = $inputId
           OR sa.value = $inputId
           OR s.smileSampleId = $inputId
           OR sm.cmoSampleName = $inputId
           OR sm.investigatorSampleId = $inputId
           RETURN DISTINCT sm.primaryId LIMIT 1
           """)
    String findSamplePrimaryIdByInputId(@Param("inputId") String inputId);

    @Query("""
           MATCH (s: Sample)<-[:IS_ALIAS]-(sa: SampleAlias)
           WITH s, sa, COLLECT {
           MATCH (s)-[:HAS_METADATA]->(sm: SampleMetadata)
           RETURN sm ORDER BY sm.importDate DESC LIMIT 1
           } as latestSm
           WITH s, sa, latestSm[0] AS latestSm, $inputIds AS inputIds
           WHERE latestSm.primaryId IN $inputIds
           OR latestSm.cmoSampleName IN $inputIds
           OR latestSm.investigatorSampleId IN $inputIds
           OR s.smileSampleId IN $inputIds
           OR sa.value IN $inputIds
           WITH s, latestSm, inputIds
           WITH ({
            smileSampleId: s.smileSampleId,
            primaryId: latestSm.primaryId,
            cmoSampleName: latestSm.cmoSampleName,
            investigatorSampleId: latestSm.investigatorSampleId
           }) as matchedSample, inputIds
           UNWIND inputIds as inputId
           WITH inputId, matchedSample, inputIds
           WHERE ANY(prop IN keys(matchedSample) WHERE toString(matchedSample[prop]) = inputId)
           WITH
            COLLECT(DISTINCT inputId) AS matchedIds,
            COLLECT(DISTINCT matchedSample.primaryId) as matchedPrimaryIds,
            inputIds
           WITH matchedIds, matchedPrimaryIds,
            [item IN inputIds WHERE NOT item IN matchedIds] as unmatchedIds
           WITH ({
            matchedIds: matchedIds,
            matchedPrimaryIds: matchedPrimaryIds,
            unmatchedIds: unmatchedIds
           }) AS result
           RETURN result
           """)
    Map<String, Object> findMatchedAndUnmatchedInputSampleIds(List<String> inputIds);
}
