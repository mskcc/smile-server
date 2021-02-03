package org.mskcc.cmo.metadb.persistence;

import org.mskcc.cmo.metadb.model.PatientMetadata;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

/**
 *
 * @author ochoaa
 */
public interface PatientMetadataRepository extends Neo4jRepository<PatientMetadata, Long> {
    @Query("MATCH (pm: PatientMetadata) "
        + "WHERE $investigatorPatientId = pm.investigatorPatientId RETURN pm")
    PatientMetadata findPatientByInvestigatorId(
            @Param("investigatorPatientId") String investigatorPatientId);

}
