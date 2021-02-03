package org.mskcc.cmo.metadb.persistence;

import java.util.List;
import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */
@Repository
public interface CmoRequestRepository extends Neo4jRepository<MetaDbRequest, Long> {
    @Query("MATCH (r: MetaDbRequest{requestId: $reqId}) RETURN r;")
    MetaDbRequest findByRequestId(@Param("reqId") String reqId);
    
    @Query("Match (r: MetaDbRequest{requestId: $reqId})-[:REQUEST_TO_SP]->"
            + "(c: MetaDbSample) "
            + "RETURN c ;")
    List<MetaDbSample> findAllSampleManifests(@Param("reqId") String reqId);
    
    @Query("MATCH(r: MetaDbRequest{requestId: $reqId}) "
            + "MATCH(r)-[:REQUEST_TO_SP]->(sm) "
            + "MATCH (sm)<-[:IS_ALIAS]-(s: SampleAlias{idSource: 'igoId', value: $igoId}) "
            + "RETURN sm")
    MetaDbSample findSampleManifest(@Param("reqId") String reqId, @Param("igoId") String igoId);
}
