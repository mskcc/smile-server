package org.mskcc.cmo.metadb.persistence;

import java.util.UUID;
import org.mskcc.cmo.shared.neo4j.SampleMetadataEntity;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */

@Repository
public interface SampleMetadataRepository extends Neo4jRepository<SampleMetadataEntity, UUID> {
    @Query(
        "MATCH (s:cmo_metadb_sample_metadata) WHERE $igoId = s.igoId RETURN s"
    )
    SampleMetadataEntity findSampleByIgoId(@Param("igoId") String igoId);

    @Query(
            "CREATE (sm:cmo_metadb_sample_metadata {time: timestamp(), uuid:apoc.create.uuid(),"
                    + "igoId:$sample.igoId, investigatorSampleId:$sample.investigatorSampleId,"
                    + "sampleName:$sample.sampleName, sampleOrigin:$sample.sampleOrigin, sex:$sample.sex,"
                    + "species:$sample.species, specimenType:$sample.specimenType, tissueLocation:$sample.tissueLocation,"
                    + "tubeId:$sample.tubeId, tumorOrNormal:$sample.tumorOrNormal"
                    + "FOREACH (n_sample IN $sample.sampleList | "
                        + "MERGE (s:sample {sampleId:n_sample.sampleId, idSource:n_sample.idSource}) "
                        + "MERGE (s)-[:SP_TO_SP]->(sm)"
                    + "})"
            + "CREATE (s_id:cmo_metadb_sample_metadata_id {igoId:$sample.igoId})"
            + "MERGE (pm:cmo_metadb_patient_metadata "
                + "{investigatorPatientId: $sample.patient.investigatorPatientId}) "
            + "MERGE (pm)-[:PX_TO_SP]->(s_id)"
            + "MERGE (s_id)-[:SAMPLE_METADATA]->(sm)"
            + "RETURN sm"
    )
    SampleMetadataEntity insertSampleMetadata(@Param("sample") SampleMetadataEntity sample);
    
    @Query(
            "MATCH (s_id:cmo_metadb_sample_metadata_id {igoId:$sample.igoId})"
            + "MATCH (sm_old_node:cmo_metadb_sample_metadata)<-[:SAMPLE_METADATA]-(sm)"
            + "WITH s_id, sm_old_node"
            + "MATCH (s_id)-[r:SAMPLE_METADATA]->(sm_old_node)"
            + "DELETE r"
            + "CREATE (sm:cmo_metadb_sample_metadata {time: timestamp(), uuid:apoc.create.uuid(),"
                    + "igoId:$sample.igoId, investigatorSampleId:$sample.investigatorSampleId,"
                    + "sampleName:$sample.sampleName, sampleOrigin:$sample.sampleOrigin, sex:$sample.sex,"
                    + "species:$sample.species, specimenType:$sample.specimenType, tissueLocation:$sample.tissueLocation,"
                    + "tubeId:$sample.tubeId, tumorOrNormal:$sample.tumorOrNormal })"
            + "MERGE(s_id)-[:SAMPLE_METADATA]->(sm)"
            + "MERGE(sm)-[:SAMPLE_METADATA]->(sm_old_node)"
            + "RETURN sm"
    )
    SampleMetadataEntity updateSampleMetadata(@Param("sample") SampleMetadataEntity sample);
}
