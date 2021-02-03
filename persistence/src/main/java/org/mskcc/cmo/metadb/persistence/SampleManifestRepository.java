package org.mskcc.cmo.metadb.persistence;

import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleManifestEntity;
import org.mskcc.cmo.metadb.model.SampleManifestJsonEntity;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */

@Repository
public interface SampleManifestRepository extends Neo4jRepository<SampleManifestEntity, UUID> {
    @Query("MATCH (s: SampleAlias {value: $igoId.sampleId, idSource: 'igoId'}) "
        + "MATCH (s)-[:IS_ALIAS]->(sm) "
        + "RETURN sm")
    SampleManifestEntity findSampleByIgoId(@Param("igoId") SampleAlias igoId);

    @Query("MATCH (sm: SampleManifestEntity{uuid: $uuid})"
            + "MATCH (sm)-[:IS_ALIAS]->(s)"
            + "WHERE s.idSource = 'igoId' RETURN s;")
    SampleAlias findSampleIgoId(@Param("uuid") UUID uuid);

    @Query("MATCH (sm: SampleManifestEntity{uuid: $uuid})"
            + "MATCH (sm)-[:IS_ALIAS]->(s)"
            + "WHERE s.idSource = 'investigatorId' RETURN s;")
    SampleAlias findInvestigatorId(@Param("uuid") UUID uuid);

    @Query("MATCH (sm: SampleManifestEntity{uuid: $uuid}) "
            + "MATCH (json)<-[HAS_METADATA]-(sm) "
            + "DELETE r "
            + "CREATE (new_json: SampleManifestJsonEntity "
            + "{sampleManifestJson: $sampleManifestJson.sampleManifestJson}) "
            + "MERGE(sm)-[:HAS_METADATA]->(new_json) "
            + "MERGE(new_json)-[:HAS_METADATA]->(json)")
    SampleManifestEntity updateSampleManifestJson(
            @Param("sampleManifestJson")SampleManifestJsonEntity sampleManifestJson,
            @Param("uuid") UUID uuid);

    @Query("MATCH (s: SampleManifestEntity{uuid: $sampleManifestEntity.uuid})"
            + "MATCH (s)<-[:HAS_SAMPLE]-(p: PatientMetadata)"
            + "MATCH (n)<-[:HAS_SAMPLE]-(p) "
            + "WHERE n.tumorOrNormal = 'Normal'"
            + "RETURN n")
    List<SampleManifestEntity> findSamplesWithSamePatient(
            @Param("sampleManifestEntity") SampleManifestEntity sampleManifestEntity);

    @Query("MATCH (s: SampleManifestEntity{uuid: $sampleManifestEntity.uuid}) "
            + "MATCH (s)<-[:REQUEST_TO_SP]-(r: CmoRequestEntity) "
            + "RETURN r.pooledNormals")
    List<String> findPooledNormals(@Param("sampleManifestEntity") SampleManifestEntity sampleManifestEntity);
}
