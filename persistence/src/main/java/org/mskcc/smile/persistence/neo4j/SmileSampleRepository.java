package org.mskcc.smile.persistence.neo4j;

import java.util.List;
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
    @Query("MATCH (s: Sample {smileSampleId: $smileSampleId})-[hsm:HAS_METADATA]->(sm: SampleMetadata) "
            + "MATCH (s)<-[isa:IS_ALIAS]-(sa: SampleAlias) "
            + "OPTIONAL MATCH (sm)-[hss:HAS_STATUS]->(ss: Status) "
            + "OPTIONAL MATCH (s)-[ht:HAS_TEMPO]->(t: Tempo) "
            + "OPTIONAL MATCH (t)-[hbe:HAS_EVENT]->(bc: BamComplete) "
            + "OPTIONAL MATCH (t)-[hqe:HAS_EVENT]->(qc: QcComplete) "
            + "OPTIONAL MATCH (t)-[hme:HAS_EVENT]->(mc: MafComplete) "
            + "RETURN s, hsm, sm, hss, ss, isa, sa, ht, t, hbe, bc, hqe, qc, hme, mc")
    SmileSample findSampleBySampleSmileId(@Param("smileSampleId") UUID smileSampleId);

    @Query("MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId})"
            + "RETURN s")
    SmileSample findSampleByPrimaryId(@Param("primaryId") String primaryId);

    @Query("MATCH (s: Sample {smileSampleId: $smileSampleId})-[:HAS_METADATA]->(sm: SampleMetadata) "
            + "OPTIONAL MATCH (sm)-[hs:HAS_STATUS]->(ss: Status) "
            + "RETURN sm, hs, ss")
    List<SampleMetadata> findAllSampleMetadataListBySampleId(@Param("smileSampleId") UUID smileSampleId);

    @Query("MATCH (s: Sample {smileSampleId: $smileSample.smileSampleId})"
            + "MATCH (s)<-[:HAS_SAMPLE]-(p: Patient)"
            + "MATCH (n: Sample)<-[:HAS_SAMPLE]-(p) "
            + "WHERE toLower(n.sampleClass) = 'normal'"
            + "RETURN n")
    List<SmileSample> findMatchedNormalsByResearchSample(
            @Param("smileSample") SmileSample smileSample);

    @Query("MATCH (r: Request {igoRequestId: $reqId})-[:HAS_SAMPLE]->"
            + "(s: Sample) "
            + "RETURN s")
    List<SmileSample> findResearchSamplesByRequest(@Param("reqId") String reqId);

    @Query("MATCH (r: Request {igoRequestId: $reqId}) "
        + "MATCH(r)-[:HAS_SAMPLE]->(s: Sample) "
        + "MATCH (s)<-[:IS_ALIAS]-(sa: SampleAlias {namespace: 'igoId', value: $igoId}) "
        + "RETURN s")
    SmileSample findResearchSampleByRequestAndIgoId(@Param("reqId") String reqId,
            @Param("igoId") String igoId);

    @Query("MATCH (pa: PatientAlias {namespace: 'cmoId', value: $cmoPatientId})-[:IS_ALIAS]->"
            + "(p: Patient)-[:HAS_SAMPLE]->(s: Sample) "
            + "RETURN s")
    List<SmileSample> findAllSamplesByCmoPatientId(
            @Param("cmoPatientId") String cmoPatientId);

    @Query("MATCH (pa: PatientAlias {namespace: 'cmoId', value: $cmoPatientId})-[:IS_ALIAS]->"
            + "(p: Patient)-[:HAS_SAMPLE]->(s: Sample {sampleCategory: $sampleCategory}) "
            + "RETURN s")
    List<SmileSample> findAllSamplesByCategoryAndCmoPatientId(
            @Param("cmoPatientId") String cmoPatientId, @Param("sampleCategory") String sampleCategory);

    @Query("MATCH (sa :SampleAlias {value:$value, namespace: $namespace})"
            + "-[:IS_ALIAS]->(s: Sample)"
            + "-[:HAS_METADATA]->(sm: SampleMetadata) "
            + "OPTIONAL MATCH (sm)-[hs:HAS_STATUS]->(ss: Status) "
            + "RETURN sm, hs, ss")
    List<SampleMetadata> findSampleMetadataHistoryByNamespaceValue(
            @Param("namespace") String namespace, @Param("value") String value);

    @Query("MATCH (s: Sample {smileSampleId: $smileSampleId}) "
            + "MATCH (p: Patient {smilePatientId: $smilePatientId}) "
            + "MERGE (s)<-[hs:HAS_SAMPLE]-(p)")
    void updateSamplePatientRelationship(@Param("smileSampleId") UUID smileSampleId,
            @Param("smilePatientId") UUID smilePatientId);

    @Query("MATCH (s: Sample {sampleCategory: 'research'})-[:HAS_METADATA]->(sm: SampleMetadata) "
            + "WHERE sm.importDate >= $inputDate RETURN DISTINCT s.smileSampleId")
    List<UUID> findSamplesByLatestImportDate(@Param("inputDate") String inputDate);

    @Query("MATCH (s: Sample {smileSampleId: $smileSampleId})-[:HAS_METADATA]->(sm: SampleMetadata) "
            + "OPTIONAL MATCH (sm)-[hs:HAS_STATUS]->(ss: Status) "
            + "RETURN sm, hs, ss ORDER BY sm.importDate DESC LIMIT 1")
    SampleMetadata findLatestSampleMetadataBySmileId(@Param("smileSampleId") UUID smileSampleId);

    @Query("MATCH (sm:SampleMetadata)<-[:HAS_METADATA]-(s:Sample)<-[:IS_ALIAS]-(sa:SampleAlias) "
            + "WHERE sm.primaryId = $inputId "
            + "OR sa.value = $inputId "
            + "OR s.smileSampleId = $inputId "
            + "OR sm.cmoSampleName = $inputId "
            + "OR sm.investigatorSampleId = $inputId "
            + "RETURN s")
    SmileSample findSampleByInputId(@Param("inputId") String inputId);

    @Query("MATCH (s: Sample {smileSampleId: $smileSampleId}) "
            + "MATCH (s)<-[r:HAS_SAMPLE]-(p: Patient {smilePatientId: $smilePatientId}) "
            + "DELETE r")
    void removeSamplePatientRelationship(@Param("smileSampleId") UUID smileSampleId,
            @Param("smilePatientId") UUID smilePatientId);

    @Query("MATCH (r:Request {smileRequestId: $smileRequestId}) "
            + "MATCH (s:Sample {smileSampleId: $smileSampleId}) "
            + "MERGE (r)-[hs:HAS_SAMPLE]->(s)")
    void createSampleRequestRelationship(@Param("smileSampleId") UUID smileSampleId,
            @Param("smileRequestId") UUID smileRequestId);

    @Query("MATCH (s: Sample {smileSampleId: $smileSampleId}) "
            + "SET s.revisable = $revisable "
            + "RETURN s")
    SmileSample updateRevisableBySampleId(@Param("smileSampleId") UUID smileSampleId,
            @Param("revisable") Boolean revisable);

    @Query("MATCH (c: Cohort {cohortId: $cohortId})-[:HAS_COHORT_SAMPLE]->"
            + "(s: Sample) "
            + "RETURN s")
    List<SmileSample> findSamplesByCohortId(@Param("cohortId") String cohortId);
}
