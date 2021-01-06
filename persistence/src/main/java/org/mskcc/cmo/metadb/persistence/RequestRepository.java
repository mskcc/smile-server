package org.mskcc.cmo.metadb.persistence;

import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.shared.neo4j.CmoRequestEntity;
import org.mskcc.cmo.shared.neo4j.SampleManifestEntity;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestRepository extends Neo4jRepository<CmoRequestEntity,String> {
    
    @Query("MATCH (c: cmo_metadb_request {requestId: $reqId})"
            + "RETURN c"
            )
    CmoRequestEntity findByRequestId(@Param("reqId") String requestId);
    //List<SampleManifestEntity> findAllSampleManifestList(String reqId);
    
    @Query(
            "MATCH (s: cmo_metadb_sample_metadata {uuid: $sampleUuid}"
            + "MATCH (c: cmo_metadb_request {requestId: $reqId}"
            + "MERGE (c)-[:REQUEST_TO_SP]->(s)"
            )
    void addSampleManifest(@Param("sampleUuid") UUID sampleUuid, @Param("reqId") String reqId);
    
}
