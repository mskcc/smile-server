package org.mskcc.cmo.metadb.persistence;

import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.metadb.model.MetaDbSample;
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
public interface MetaDbSampleRepository extends Neo4jRepository<MetaDbSample, UUID> {
    @Query("MATCH (sm: Sample {metaDbSampleId: $metaDbSampleId}) "
            + "RETURN sm")
    MetaDbSample findMetaDbSampleById(@Param("metaDbSampleId") UUID metaDbSampleId);

    @Query("MATCH (s: SampleAlias {value: $igoId}) RETURN s")
    SampleAlias findSampleAliasByIgoId(@Param("igoId") String igoId);

    @Query("MATCH (s: SampleAlias {value: $igoId.sampleId, namespace: 'igoId'}) "
        + "MATCH (s)<-[:IS_ALIAS]-(sm: Sample) "
        + "RETURN sm")
    MetaDbSample findMetaDbSampleByIgoId(@Param("igoId") SampleAlias igoId);

    @Query("MATCH (sm: Sample {metaDbSampleId: $metaDbSampleId})"
            + "MATCH (sm)<-[:IS_ALIAS]-(s: SampleAlias)"
            + "WHERE toLower(s.namespace) = 'igoid' RETURN s;")
    SampleAlias findSampleIgoId(@Param("metaDbSampleId") UUID metaDbSampleId);

    @Query("MATCH (sm: Sample {metaDbSampleId: $metaDbSampleId})"
            + "MATCH (sm)<-[:IS_ALIAS]-(s: SampleAlias)"
            + "WHERE s.namespace = 'investigatorId' RETURN s;")
    SampleAlias findSampleInvestigatorId(@Param("metaDbSampleId") UUID metaDbSampleId);

    @Query("MATCH (sm: Sample {metaDbSampleId: $metaDbSampleId})"
            + "MATCH (sm)-[:HAS_METADATA]->(s: SampleMetadata)"
            + "RETURN s;")
    List<SampleMetadata> findSampleMetadataListBySampleId(@Param("metaDbSampleId") UUID metaDbSampleId);

    @Query("MATCH (s: Sample {metaDbSampleId: $metaDbSample.metaDbSampleId})"
            + "MATCH (s)<-[:HAS_SAMPLE]-(p: Patient)"
            + "MATCH (n: Sample)<-[:HAS_SAMPLE]-(p) "
            + "WHERE toLower(n.sampleClass) = 'normal'"
            + "RETURN n")
    List<MetaDbSample> findMatchedNormalsBySample(
            @Param("metaDbSample") MetaDbSample metaDbSample);

    @Query("MATCH (s: Sample {metaDbSampleId: $metaDbSample.metaDbSampleId}) "
            + "MATCH (s)<-[:HAS_SAMPLE]-(r: Request)"
            + "RETURN r.pooledNormals")
    List<String> findPooledNormalsBySample(@Param("metaDbSample") MetaDbSample metaDbSample);

    @Query("Match (r: Request {requestId: $reqId})-[:HAS_SAMPLE]->"
            + "(s: Sample) "
            + "RETURN s;")
    List<MetaDbSample> findAllMetaDbSamplesByRequest(@Param("reqId") String reqId);

    @Query("MATCH (r: Request {requestId: $reqId}) "
            + "MATCH(r)-[:HAS_SAMPLE]->(sm: Sample) "
            + "MATCH (sm)<-[:IS_ALIAS]-(s: SampleAlias {namespace: 'igoId', value: $igoId.sampleId}) "
            + "RETURN sm")
    MetaDbSample findMetaDbSampleByRequestAndIgoId(@Param("reqId") String reqId,
            @Param("igoId") SampleAlias igoId);

    @Query("MATCH (r: Request {requestId: $reqId}) "
        + "MATCH(r)-[:HAS_SAMPLE]->(sm: Sample) "
        + "MATCH (sm)<-[:IS_ALIAS]-(s: SampleAlias {namespace: 'igoId', value: $igoId}) "
        + "RETURN sm")
    MetaDbSample findMetaDbSampleByRequestAndIgoId(@Param("reqId") String reqId,
            @Param("igoId") String igoId);

    @Query("MATCH (pa: PatientAlias {namespace: 'cmoId', value: $cmoPatientId})-[:IS_ALIAS]->"
            + "(p: Patient)-[:HAS_SAMPLE]->(s: Sample)-[:HAS_METADATA]->(sm: SampleMetadata) "
            + "MATCH (r: Request)-[:HAS_SAMPLE]->(s) SET sm.requestId = r.requestId "
            + "RETURN sm"
    )
    List<SampleMetadata> findSampleMetadataListByCmoPatientId(@Param("cmoPatientId") String cmoPatientId);

    @Query("MATCH (sm: SampleMetadata {igoId: $igoId})"
            + "RETURN sm")
    List<SampleMetadata> getSampleMetadataHistoryByIgoId(@Param("igoId") String igoId);
}
