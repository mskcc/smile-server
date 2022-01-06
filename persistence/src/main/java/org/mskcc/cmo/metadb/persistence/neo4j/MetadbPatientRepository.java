package org.mskcc.cmo.metadb.persistence.neo4j;

import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.metadb.model.MetadbPatient;
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
public interface MetadbPatientRepository extends Neo4jRepository<MetadbPatient, Long> {
    @Query("MATCH (pa: PatientAlias)-[:IS_ALIAS]->(p: Patient {metaDbPatientId: $patient.metaDbPatientId}) "
            + "RETURN pa")
    List<PatientAlias> findPatientAliasesByPatient(@Param("patient") MetadbPatient patient);

    @Query("MATCH (p: Patient)<-[:IS_ALIAS]-(pa: PatientAlias {value: $cmoPatientId, namespace: 'cmoId'}) "
            + " RETURN p")
    MetadbPatient findPatientByCmoPatientId(
            @Param("cmoPatientId") String cmoPatientId);

    @Query("MATCH (s: Sample {metaDbSampleId: $metaDbSampleId}) "
            + "MATCH (s)<-[:HAS_SAMPLE]-(p: Patient) "
            + "RETURN p.metaDbPatientId")
    UUID findPatientIdBySample(@Param("metaDbSampleId") UUID metaDbSampleId);

    @Query("MATCH (s: Sample)<-[:IS_ALIAS]-(sa: SampleAlias{value: $value, namespace: $namespace}) "
            + "MATCH (s)<-[:HAS_SAMPLE]-(p: Patient) "
            + "RETURN p")
    MetadbPatient findPatientByNamespaceValue(
            @Param("namespace") String namespace, @Param("value") String value);
}
