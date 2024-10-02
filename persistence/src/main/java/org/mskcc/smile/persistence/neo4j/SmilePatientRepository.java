package org.mskcc.smile.persistence.neo4j;

//import org.springframework.data.neo4j.repository.query.Query;
import java.util.List;
import java.util.UUID;
import org.mskcc.smile.model.PatientAlias;
import org.mskcc.smile.model.SmilePatient;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */
@Repository
public interface SmilePatientRepository extends Neo4jRepository<SmilePatient, Long> {
    @Query("MATCH (pa: PatientAlias)-[:IS_ALIAS]->(p: Patient {smilePatientId: $patient.smilePatientId}) "
            + "RETURN pa")
    List<PatientAlias> findPatientAliasesByPatient(@Param("patient") SmilePatient patient);

    @Query("MATCH (p: Patient)<-[:IS_ALIAS]-(pa: PatientAlias {value: $cmoPatientId, namespace: 'cmoId'}) "
            + " RETURN p")
    SmilePatient findPatientByCmoPatientId(
            @Param("cmoPatientId") String cmoPatientId);

    @Query("MATCH (s: Sample {smileSampleId: $smileSampleId}) "
            + "MATCH (s)<-[:HAS_SAMPLE]-(p: Patient) "
            + "RETURN p.smilePatientId")
    UUID findPatientIdBySample(@Param("smileSampleId") UUID smileSampleId);

    @Query("MATCH (p: Patient)<-[:IS_ALIAS]-(pa: PatientAlias {value: $oldCmoId, namespace: 'cmoId'}) "
            + "SET pa.value = $newCmoId "
            + "RETURN p")
    SmilePatient updateCmoPatientIdInPatientNode(@Param("oldCmoId") String oldCmoId,
            @Param("newCmoId") String newCmoId);

    @Query("MATCH (s: Sample)<-[:IS_ALIAS]-(sa: SampleAlias {value: $value, namespace: $namespace}) "
            + "MATCH (s)<-[:HAS_SAMPLE]-(p: Patient) "
            + "RETURN p")
    SmilePatient findPatientByNamespaceValue(
            @Param("namespace") String namespace, @Param("value") String value);

    @Query("MATCH (p: Patient {smilePatientId: $patient.smilePatientId})"
            + "<-[:IS_ALIAS]-(pa: PatientAlias) DETACH DELETE p, pa")
    void deletePatientAndAliases(@Param("patient") SmilePatient patient);
}
