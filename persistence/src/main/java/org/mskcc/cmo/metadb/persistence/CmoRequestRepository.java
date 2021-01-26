package org.mskcc.cmo.metadb.persistence;

import java.util.List;

import org.mskcc.cmo.metadb.model.CmoProjectEntity;
import org.mskcc.cmo.metadb.model.CmoRequestEntity;
import org.mskcc.cmo.metadb.model.SampleManifestEntity;
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
    @Query("MATCH (r: cmo_metadb_request {requestId: $reqId}) RETURN r;")
    CmoRequestEntity findByRequestId(@Param("reqId") String reqId);
    
    @Query("Match (r: cmo_metadb_request{requestId: $reqId})<-[:PR_TO_REQUEST]-"
            + "(p: cmo_metadb_project) "
            + "RETURN p ;")
    CmoProjectEntity findProjectEntity(@Param("reqId") String reqId);
    
    @Query("Match (r: cmo_metadb_request{requestId: $reqId})-[:REQUEST_TO_SP]->"
            + "(c: cmo_metadb_sample_metadata) "
            + "RETURN c ;")
    List<SampleManifestEntity> findAllSampleManifests(@Param("reqId") String reqId);
    
    @Query("MATCH(r:cmo_metadb_request {requestId: $reqId}) "
            + "MATCH(r)-[:REQUEST_TO_SP]->(sm) "
            + "MATCH (sm)<-[:SP_TO_SP]-(s: sample{idSource: 'igoId', value: $igoId}) "
            + "RETURN sm")
    SampleManifestEntity findSampleManifest(@Param("reqId") String reqId, @Param("igoId") String igoId);
}
