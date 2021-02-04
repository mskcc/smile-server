package org.mskcc.cmo.metadb.persistence;

import java.util.List;
import java.util.UUID;
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
    @Query("MATCH (s: SampleAlias {value: $igoId.sampleId, idSource: 'igoId'}) "
        + "MATCH (s)-[:IS_ALIAS]->(sm) "
        + "RETURN sm")
    MetaDbSample findSampleByIgoId(@Param("igoId") SampleAlias igoId);

    @Query("MATCH (sm: MetaDbSample{uuid: $uuid})"
            + "MATCH (sm)-[:IS_ALIAS]->(s)"
            + "WHERE s.idSource = 'igoId' RETURN s;")
    SampleAlias findSampleIgoId(@Param("uuid") UUID uuid);

    @Query("MATCH (sm: MetaDbSample{uuid: $uuid})"
            + "MATCH (sm)-[:IS_ALIAS]->(s)"
            + "WHERE s.idSource = 'investigatorId' RETURN s;")
    SampleAlias findInvestigatorId(@Param("uuid") UUID uuid);

    @Query("MATCH (s: MetaDbSample{uuid: $sampleManifestEntity.uuid})"
            + "MATCH (s)<-[:HAS_SAMPLE]-(p: MetaDbPatient)"
            + "MATCH (n)<-[:HAS_SAMPLE]-(p) "
            + "WHERE n.tumorOrNormal = 'Normal'"
            + "RETURN n")
    List<MetaDbSample> findMatchedNormals(
            @Param("sampleManifestEntity") MetaDbSample metaDbSample);

    @Query("MATCH (s: MetaDbSample{uuid: $sampleManifestEntity.uuid}) "
            + "MATCH (s)<-[:REQUEST_TO_SP]-(r: MetaDbRequest) "
            + "RETURN r.pooledNormals")
    List<String> findPooledNormals(@Param("sampleManifestEntity") MetaDbSample metaDbSample);
}
