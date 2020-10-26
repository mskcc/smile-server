package org.mskcc.cmo.metadb.persistence;

import org.mskcc.cmo.messaging.model.PatientMetadata;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

/**
 *
 * @author ochoaa
 */
public interface PatientMetadataRepository extends Neo4jRepository<PatientMetadata, Long> {
    @Query("MATCH (pm:PatientMetadata) WHERE $investigatorPatientId = pm.investigatorPatientId RETURN pm")
    PatientMetadata findPatientByInvestigatorId(@Param("investigatorPatientId") String investigatorPatientId);

    @Query(
            "MERGE (pm:PatientMetadata {investigatorPatientId: $patient.investigatorPatientId}) " +
                    "ON CREATE SET " +
                        "pm.metaDbUuid = apoc.create.uuid(), pm.investigatorPatientId = $patient.investigatorPatientId " +
                        "FOREACH (linkedPatient IN $patient.linkedPatientList | " +
                            "MERGE (p:LinkedPatient {linkedPatientName: linkedPatient.linkedPatientName, linkedSystemName: linkedPatient.linkedSystemName}) " +
                            "SET p = linkedPatient " +
                            "MERGE (p)-[:PX_TO_PX]->(pm) " +
                        ") " +
            "RETURN pm"
    )
    void savePatientMetadata(@Param("patient") PatientMetadata patientMetadata);
}
