package org.mskcc.cmo.metadb.persistence.neo4j;

import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */

@Repository
public interface MetadbSampleRepository extends Neo4jRepository<MetadbSample, UUID> {
    @Query("MATCH (sm: Sample {metaDbSampleId: $metaDbSampleId}) "
            + "RETURN sm")
    MetadbSample findAllSamplesById(@Param("metaDbSampleId") UUID metaDbSampleId);

    @Query("MATCH (s:Sample)-[:HAS_METADATA]->(s: SampleMetadata {primaryId: $primaryId})"
            + "RETURN s")
    MetadbSample findSampleByPrimaryId(@Param("primaryId") String primaryId);

    @Query("MATCH (sm: Sample {metaDbSampleId: $metaDbSampleId})"
            + "MATCH (sm)<-[:IS_ALIAS]-(s: SampleAlias)"
            + "RETURN s;")
    List<SampleAlias> findAllSampleAliases(@Param("metaDbSampleId") UUID metaDbSampleId);

    @Query("MATCH (sm: Sample {metaDbSampleId: $metaDbSampleId})"
            + "MATCH (sm)-[:HAS_METADATA]->(s: SampleMetadata)"
            + "RETURN s;")
    List<SampleMetadata> findAllSampleMetadataListBySampleId(@Param("metaDbSampleId") UUID metaDbSampleId);

    @Query("MATCH (s: Sample {metaDbSampleId: $metaDbSample.metaDbSampleId})"
            + "MATCH (s)<-[:HAS_SAMPLE]-(p: Patient)"
            + "MATCH (n: Sample)<-[:HAS_SAMPLE]-(p) "
            + "WHERE toLower(n.sampleClass) = 'normal'"
            + "RETURN n")
    List<MetadbSample> findMatchedNormalsByResearchSample(
            @Param("metaDbSample") MetadbSample metaDbSample);

    @Query("Match (r: Request {igoRequestId: $reqId})-[:HAS_SAMPLE]->"
            + "(s: Sample) "
            + "RETURN s;")
    List<MetadbSample> findResearchSamplesByRequest(@Param("reqId") String reqId);

    @Query("MATCH (r: Request {igoRequestId: $reqId}) "
        + "MATCH(r)-[:HAS_SAMPLE]->(sm: Sample) "
        + "MATCH (sm)<-[:IS_ALIAS]-(s: SampleAlias {namespace: 'igoId', value: $igoId}) "
        + "RETURN sm")
    MetadbSample findResearchSampleByRequestAndIgoId(@Param("reqId") String reqId,
            @Param("igoId") String igoId);

    @Query("MATCH (pa: PatientAlias {namespace: 'cmoId', value: $cmoPatientId})-[:IS_ALIAS]->"
            + "(p: Patient)-[:HAS_SAMPLE]->(s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata) "
            + "RETURN sm")
    List<SampleMetadata> findAllSampleMetadataByCmoPatientId(
            @Param("cmoPatientId") String cmoPatientId);

    @Query("MATCH (sa :SampleAlias {value:$value, namespace: $namespace})"
            + "-[:IS_ALIAS]->(s: Sample)"
            + "-[:HAS_METADATA]->(sm: SampleMetadata)"
            + "RETURN sm")
    List<SampleMetadata> findSampleMetadataHistoryByNamespaceValue(
            @Param("namespace") String namespace, @Param("value") String value);
}
