package org.mskcc.cmo.metadb.persistence;

import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.metadb.model.MetaDbPatient;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleManifestEntity;
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
    @Query("MATCH (sm: MetaDbSample {uuid: $uuid}) "
            + "RETURN sm")
    MetaDbSample findSampleByUUID(@Param("uuid") UUID uuid);

    @Query("MATCH (s: SampleAlias {value: $igoId.sampleId, idSource: 'igoId'}) "
        + "MATCH (s)<-[:IS_ALIAS]-(sm: MetaDbSample) "
        + "RETURN sm")
    MetaDbSample findSampleByIgoId(@Param("igoId") SampleAlias igoId);

    @Query("MATCH (sm: MetaDbSample {uuid: $uuid})"
            + "MATCH (sm)<-[:IS_ALIAS]-(s: SampleAlias)"
            + "WHERE toLower(s.idSource) = 'igoid' RETURN s;")
    SampleAlias findSampleIgoId(@Param("uuid") UUID uuid);

    @Query("MATCH (sm: MetaDbSample {uuid: $uuid})"
            + "MATCH (sm)<-[:IS_ALIAS]-(s: SampleAlias)"
            + "WHERE s.idSource = 'investigatorId' RETURN s;")
    SampleAlias findInvestigatorId(@Param("uuid") UUID uuid);

    @Query("MATCH (sm: MetaDbSample {uuid: $uuid})"
            + "MATCH (sm)<-[:HAS_SAMPLE]-(p: MetaDbPatient)"
            + "RETURN p;")
    MetaDbPatient findPatientbyUUID(@Param("uuid") UUID uuid);

    @Query("MATCH (sm: MetaDbSample {uuid: $uuid})"
            + "MATCH (sm)-[:HAS_METADATA]->(s: SampleManifestEntity)"
            + "RETURN s;")
    List<SampleManifestEntity> findSampleManifestList(@Param("uuid") UUID uuid);

    @Query("MATCH (s: MetaDbSample {uuid: $sampleManifestEntity.uuid})"
            + "MATCH (s)<-[:HAS_SAMPLE]-(p: MetaDbPatient)"
            + "MATCH (n: MetaDbSample)<-[:HAS_SAMPLE]-(p) "
            + "WHERE toLower(n.sampleClass) = 'normal'"
            + "RETURN n")
    List<MetaDbSample> findMatchedNormals(
            @Param("sampleManifestEntity") MetaDbSample metaDbSample);

    @Query("MATCH (s: MetaDbSample {uuid: $sampleManifestEntity.uuid}) "
            + "MATCH (s)<-[:HAS_SAMPLE]-(r: MetaDbRequest) "
            + "RETURN r.pooledNormals")
    List<String> findPooledNormals(@Param("sampleManifestEntity") MetaDbSample metaDbSample);
}
