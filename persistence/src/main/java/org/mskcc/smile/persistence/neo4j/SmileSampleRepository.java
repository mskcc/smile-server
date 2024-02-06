package org.mskcc.smile.persistence.neo4j;

import java.util.List;
import java.util.UUID;
import org.mskcc.smile.model.SampleAlias;
import org.mskcc.smile.model.SampleMetadata;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.Status;
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
    @Query("MATCH (s: Sample {smileSampleId: $smileSampleId}) "
            + "RETURN s")
    SmileSample findSampleById(@Param("smileSampleId") UUID smileSampleId);

    @Query("MATCH (sa: SampleAlias {value: $igoId, namespace: 'igoId'})"
        + "<-[:IS_ALIAS]-(s: Sample) "
        + "RETURN s")
    SmileSample findResearchSampleByIgoId(@Param("igoId") String igoId);

    @Query("MATCH (sa: SampleAlias {value: $alias.value, namespace: $alias.namespace})"
        + "<-[:IS_ALIAS]-(s: Sample) "
        + "RETURN s")
    SmileSample findResearchSampleBySampleAlias(@Param("alias") SampleAlias alias);

    @Query("MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId})"
            + "RETURN s")
    SmileSample findSampleByPrimaryId(@Param("primaryId") String primaryId);

    @Query("MATCH (s: Sample {smileSampleId: $smileSampleId})"
            + "MATCH (s)<-[:IS_ALIAS]-(sa: SampleAlias)"
            + "RETURN sa;")
    List<SampleAlias> findAllSampleAliases(@Param("smileSampleId") UUID smileSampleId);

    @Query("MATCH (s: Sample {smileSampleId: $smileSampleId})"
            + "MATCH (s)-[:HAS_METADATA]->(sm: SampleMetadata)"
            + "RETURN sm;")
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
            + "RETURN s;")
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
            + "-[:HAS_METADATA]->(sm: SampleMetadata)"
            + "RETURN sm")
    List<SampleMetadata> findSampleMetadataHistoryByNamespaceValue(
            @Param("namespace") String namespace, @Param("value") String value);

    @Query("MATCH (s: Sample {smileSampleId: $smileSampleId}) "
            + "MATCH (p: Patient {smilePatientId: $smilePatientId}) "
            + "MERGE (s)<-[:HAS_SAMPLE]-(p)")
    void updateSamplePatientRelationship(@Param("smileSampleId") UUID smileSampleId,
            @Param("smilePatientId") UUID smilePatientId);

    @Query("MATCH (s: Sample {sampleCategory: 'research'})-[:HAS_METADATA]->(sm: SampleMetadata) "
            + "WHERE sm.importDate >= $inputDate RETURN DISTINCT s.smileSampleId")
    List<UUID> findSamplesByLatestImportDate(@Param("inputDate") String inputDate);

    @Query("MATCH (s: Sample {smileSampleId: $smileSampleId})-[:HAS_METADATA]->(sm: SampleMetadata) "
            + "RETURN sm ORDER BY sm.importDate DESC LIMIT 1")
    SampleMetadata findLatestSampleMetadataBySmileId(@Param("smileSampleId") UUID smileSampleId);

    @Query("MATCH (sm:SampleMetadata)<-[:HAS_METADATA]-(s:Sample)<-[:IS_ALIAS]-(sa:SampleAlias) "
            + "WHERE sm.primaryId = $inputId "
            + "OR sa.value = $inputId "
            + "OR s.smileSampleId = $inputId "
            + "OR sm.cmoSampleName = $inputId "
            + "RETURN s;")
    SmileSample findSampleByInputId(@Param("inputId") String inputId);

    @Query("MATCH (s: Sample {smileSampleId: $smileSampleId}) "
            + "MATCH (s)<-[r:HAS_SAMPLE]-(p: Patient {smilePatientId: $smilePatientId}) "
            + "DELETE r")
    void removeSamplePatientRelationship(@Param("smileSampleId") UUID smileSampleId,
            @Param("smilePatientId") UUID smilePatientId);

    @Query("MATCH (r:Request {smileRequestId: $smileRequestId}) "
            + "MATCH (s:Sample {smileSampleId: $smileSampleId}) "
            + "MERGE (r)-[:HAS_SAMPLE]->(s)")
    void createSampleRequestRelationship(@Param("smileSampleId") UUID smileSampleId,
            @Param("smileRequestId") UUID smileRequestId);

    @Query("MATCH (sm: SampleMetadata)-[:HAS_STATUS]->(st: Status) "
        + "WHERE ID(sm) = $smId "
        + "RETURN st")
    Status findStatusForSampleMetadataById(@Param("smId") Long smId);

    @Query("MATCH (s: Sample {smileSampleId: $smileSampleId}) "
            + "SET s.revisable = $revisable "
            + "RETURN s")
    SmileSample updateRevisableBySampleId(@Param("smileSampleId") UUID smileSampleId,
            @Param("revisable") Boolean revisable);

    @Query("MATCH (s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata {primaryId: $primaryId}) RETURN s")
    SmileSample sampleExistsByPrimaryId(@Param("primaryId") String primaryId);
}
