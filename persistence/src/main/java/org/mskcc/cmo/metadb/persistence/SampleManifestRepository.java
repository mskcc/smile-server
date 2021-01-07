package org.mskcc.cmo.metadb.persistence;

import java.util.UUID;
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
public interface SampleManifestRepository extends Neo4jRepository<SampleManifestEntity, UUID> {
    @Query(
        "MATCH (s:cmo_metadb_sample_metadata) WHERE $igoId = s.igoId RETURN s"
    )
    SampleManifestEntity findSampleByIgoId(@Param("igoId") String igoId);

    @Query(
        "MERGE (sm:cmo_metadb_sample_metadata {igoId: $sample.igoId}) "
        + "ON MATCH SET sm.sampleName = $sample.sampleName "
        + "ON CREATE SET "
            + "sm.timestamp = timestamp(), sm.uuid = apoc.create.uuid(), "
            + "sm.igoId = $sample.igoId, sm.investigatorSampleId = $sample.investigatorSampleId, "
            + "sm.sampleName = $sample.sampleName, sm.sampleOrigin = $sample.sampleOrigin, "
            + "sm.sex = $sample.sex, sm.species = $sample.species, "
            + "sm.specimenType = $sample.specimenType, sm.tissueLocation = $sample.tissueLocation, "
            + "sm.tubeId = $sample.tubeId, sm.tumorOrNormal = $sample.tumorOrNormal "
            + "FOREACH (n_sample IN $sample.sampleList | "
                + "MERGE (s:sample {sampleId:n_sample.sampleId, idSource:n_sample.idSource}) "
                + "MERGE (s)-[:SP_TO_SP]->(sm) "
            + ") "
        + "MERGE (pm:cmo_metadb_patient_metadata "
            + "{investigatorPatientId: $sample.patient.investigatorPatientId}) "
        + "MERGE (pm)-[:PX_TO_SP]->(sm)"
        + "RETURN sm"
    )
    SampleManifestEntity saveSampleManifest(@Param("sample") SampleManifestEntity sample);
}
