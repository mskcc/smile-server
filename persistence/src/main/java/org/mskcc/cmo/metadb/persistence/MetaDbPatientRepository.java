package org.mskcc.cmo.metadb.persistence;

import org.mskcc.cmo.metadb.model.MetaDbPatient;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

/**
 *
 * @author ochoaa
 */
public interface MetaDbPatientRepository extends Neo4jRepository<MetaDbPatient, Long> {
    @Query("MATCH (pm: Patient)<-[:IS_ALIAS]-(pa:PatientAlias "
            + "{value: $patientId}) RETURN pm")
    MetaDbPatient findPatientByPatientAlias(
            @Param("patientId") String patientId);

}
