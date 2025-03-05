package org.mskcc.smile.persistence.neo4j;

import java.util.UUID;
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
    @Query("MATCH (p: Patient {smilePatientId: $smilePatientId})<-[ia:IS_ALIAS]-(pa: PatientAlias) "
            + "RETURN DISTINCT p, ia, pa")
    SmilePatient findPatientByPatientSmileId(@Param("smilePatientId") UUID smileSampleId);

    @Query("OPTIONAL MATCH (s: Sample {smileSampleId: $smileSampleId})<-[hs:HAS_SAMPLE]-(p: Patient)"
            + "<-[ia:IS_ALIAS]-(pa: PatientAlias) "
            + "RETURN DISTINCT p, hs, ia, pa")
    SmilePatient findPatientBySampleSmileId(@Param("smileSampleId") UUID smileSampleId);

    @Query("MATCH (p: Patient)<-[:IS_ALIAS]-(pa: PatientAlias) "
            + "WHERE pa.namespace = 'cmoId' AND pa.value = $cmoPatientId "
            + "MATCH (p)<-[ipa:IS_ALIAS]-(pa2: PatientAlias) "
            + "RETURN DISTINCT p, ipa, pa2")
    SmilePatient findPatientByCmoPatientId(
            @Param("cmoPatientId") String cmoPatientId);

    @Query("MATCH (p: Patient)<-[:IS_ALIAS]-(pa: PatientAlias {value: $oldCmoId, namespace: 'cmoId'}) "
            + "SET pa.value = $newCmoId WITH p "
            + "MATCH (p)<-[ipa:IS_ALIAS]-(pa2: PatientAlias) "
            + "RETURN DISTINCT p, ipa, pa2")
    SmilePatient updateCmoPatientIdInPatientNode(@Param("oldCmoId") String oldCmoId,
            @Param("newCmoId") String newCmoId);

    @Query("MATCH (p: Patient {smilePatientId: $patient.smilePatientId})"
            + "<-[:IS_ALIAS]-(pa: PatientAlias) DETACH DELETE p, pa")
    void deletePatientAndAliases(@Param("patient") SmilePatient patient);
}
