package org.mskcc.cmo.metadb.persistence;

import java.util.List;
import org.mskcc.cmo.metadb.model.MetaDbProject;
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
public interface MetaDbRequestRepository extends Neo4jRepository<MetaDbRequest, Long> {
    @Query("MATCH (r: Request {requestId: $reqId}) RETURN r;")
    MetaDbRequest findMetaDbRequestById(@Param("reqId") String reqId);

    @Query("Match (r: Request {requestId: $reqId})-[:HAS_SAMPLE]->"
            + "(s: Sample) "
            + "RETURN s;")
    List<MetaDbSample> findAllMetaDbSamplesByRequest(@Param("reqId") String reqId);

    @Query("MATCH (r: Request {requestId: $reqId}) "
            + "MATCH(r)-[:HAS_SAMPLE]->(sm: Sample) "
            + "MATCH (sm)<-[:IS_ALIAS]-(s: SampleAlias {toLower(namespace): 'igoid', value: $igoId}) "
            + "RETURN sm")
    MetaDbSample findMetaDbSampleByRequestAndIgoId(@Param("reqId") String reqId,
            @Param("igoId") String igoId);

    @Query("MATCH (r: Request {requestId: $reqId}) "
            + "MATCH (r)<-[:HAS_REQUEST]-(p: Project) "
            + "RETURN p")
    MetaDbProject findMetaDbProjectByRequest(@Param("reqId") String reqId);
}
