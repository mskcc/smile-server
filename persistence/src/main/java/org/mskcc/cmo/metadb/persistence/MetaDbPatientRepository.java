package org.mskcc.cmo.metadb.persistence;

import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.metadb.model.MetaDbPatient;
import org.mskcc.cmo.metadb.model.PatientAlias;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */
@Repository
public interface MetaDbPatientRepository extends Neo4jRepository<MetaDbPatient, Long> {
    @Query("MATCH (pm: Patient)<-[:IS_ALIAS]-(pa:PatientAlias "
            + "{value: $patientId}) RETURN pm")
    MetaDbPatient findPatientByPatientAlias(
            @Param("patientId") String patientId);

    @Query("MATCH (pa: PatientAlias)-[:IS_ALIAS]->(p: Patient {metaDbPatientId: $patient.metaDbPatientId}) "
            + "RETURN pa")
    List<PatientAlias> getPatientAliasesByPatient(@Param("patient") MetaDbPatient patient);

    @Query("MATCH (p: Patient)<-[:IS_ALIAS]-(pa: PatientAlias {value: $cmoPatientId, namespace: 'cmoId'}) "
            + " RETURN p")
    MetaDbPatient findPatientByCmoPatientId(
            @Param("cmoPatientId") String cmoPatientId);

    @Query("MATCH (sm: Sample {metaDbSampleId: $metaDbSampleId})"
            + "MATCH (sm)<-[:HAS_SAMPLE]-(p: Patient)"
            + "RETURN p;")
    MetaDbPatient findPatientBySampleId(@Param("metaDbSampleId") UUID metaDbSampleId);

    @Query("MATCH (s: Sample {metaDbSampleId: $metaDbSampleId}) "
            + "MATCH (s)<-[:HAS_SAMPLE]-(p: Patient) "
            + "RETURN p.metaDbPatientId")
    UUID findPatientIdBySample(@Param("metaDbSampleId") UUID metaDbSampleId);
}
