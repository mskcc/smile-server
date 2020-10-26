package org.mskcc.cmo.metadb.persistence;

import org.mskcc.cmo.messaging.model.SampleMetadataEntity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

/**
 *
 * @author ochoaa
 */
public interface SampleMetadataRepository extends Neo4jRepository<SampleMetadataEntity, UUID> {
    @Query("MATCH (s:SampleMetadata) RETURN DISTINCT(s.sampleName)")
    List<String> findAllSampleNames();

    @Query("MATCH (s:SampleMetadata) WHERE $investigatorSampleId = s.investigatorSampleId RETURN s")
    SampleMetadataEntity findSampleByInvestigatorId(@Param("investigatorSampleId") String investigatorSampleId);

    /**
     * TODO: Follow up with LIMS/IGO folks.
     *
     *  - Will IGO sample ID be the same for a given sample even if it is updated in LIMS?
     *  - In the event that we get an update for an existing sample in the metadb, will all of the properties for that sample be published or will only the updated properties be sent?
     * @param sample
     */
    @Query(
        "MERGE (sm:SampleMetadata {investigatorSampleId: $sample.investigatorSampleId}) " +
            "ON MATCH SET sm.sampleName = $sample.sampleName " +
            "ON CREATE SET " +
                "sm.metaDbUuid = apoc.create.uuid(), sm.igoId = $sample.igoId, sm.investigatorSampleId = $sample.investigatorSampleId, " +
                "sm.sampleName = $sample.sampleName, sm.sampleOrigin = $sample.sampleOrigin, sm.sex = $sample.sex, sm.species = $sample.species, " +
                "sm.specimenType = $sample.specimenType, sm.tissueLocation = $sample.tissueLocation, sm.tubeId = $sample.tubeId, sm.tumorOrNormal = $sample.tumorOrNormal " +
                "FOREACH (linkedSample IN $sample.linkedSampleList | " +
                    "MERGE (s:LinkedSample {linkedSampleName: linkedSample.linkedSampleName, linkedSystemName: linkedSample.linkedSystemName})" +
                    "SET s = linkedSample " +
                    "MERGE (s)-[:SP_TO_SP]->(sm)" +
                ") " +
            "MERGE (pm:PatientMetadata {investigatorPatientId: $sample.patient.investigatorPatientId}) " +
                "MERGE (pm)-[:PX_TO_SP]->(sm)" +
        "RETURN sm"
    )
    void saveSampleMetadata(@Param("sample") SampleMetadataEntity sample);
}
