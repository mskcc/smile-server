package org.mskcc.cmo.metadb.persistence;

import org.mskcc.cmo.shared.neo4j.PatientMetadata;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

/**
 *
 * @author ochoaa
 */
public interface PatientMetadataRepository extends Neo4jRepository<PatientMetadata, Long> {
    @Query("MATCH (pm:cmo_metadb_patient_metadata) WHERE $investigatorPatientId = pm.investigatorPatientId RETURN pm")
    PatientMetadata findPatientByInvestigatorId(@Param("investigatorPatientId") String investigatorPatientId);

    @Query(
            "MERGE (pm:cmo_metadb_patient_metadata {investigatorPatientId: $patient.investigatorPatientId}) " +
                    "ON CREATE SET " +
                        "pm.timestamp = timestamp(), pm.uuid = apoc.create.uuid(), pm.investigatorPatientId = $patient.investigatorPatientId " +
                        "FOREACH (n_patient IN $patient.patientList | " +
                            "MERGE (p {patientId: n_patient.patientId, idSource: n_patient.idSource}) " +
                            "SET p = n_patient " +
                            "MERGE (p)-[:PX_TO_PX]->(pm) " +
                        ") " +
            "RETURN pm"
    )
    void savePatientMetadata(@Param("patient") PatientMetadata patientMetadata);
}
