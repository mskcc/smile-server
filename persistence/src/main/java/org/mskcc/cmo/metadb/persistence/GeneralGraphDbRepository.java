package org.mskcc.cmo.metadb.persistence;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;

/**
 *
 * @author ochoaa
 */
public interface GeneralGraphDbRepository extends Neo4jRepository<Object, Long> {
    @Query("MATCH (n) DETACH DELETE n")
    void deleteAllFromGraphDb();
}
