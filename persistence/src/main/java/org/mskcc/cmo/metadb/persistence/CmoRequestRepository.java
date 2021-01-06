package org.mskcc.cmo.metadb.persistence;

import org.mskcc.cmo.shared.neo4j.CmoRequestEntity;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */
@Repository
public interface CmoRequestRepository extends Neo4jRepository<CmoRequestEntity, Long> {

}
