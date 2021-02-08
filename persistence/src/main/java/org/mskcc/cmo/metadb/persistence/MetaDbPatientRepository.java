package org.mskcc.cmo.metadb.persistence;

import org.mskcc.cmo.metadb.model.neo4j.MetaDbPatient;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

/**
 *
 * @author ochoaa
 */
public interface MetaDbPatientRepository extends Neo4jRepository<MetaDbPatient, Long> {
    @Query("MATCH (pm: MetaDbPatient) "
        + "WHERE $investigatorPatientId = pm.investigatorPatientId RETURN pm")
    MetaDbPatient findPatientByInvestigatorId(
            @Param("investigatorPatientId") String investigatorPatientId);

}
