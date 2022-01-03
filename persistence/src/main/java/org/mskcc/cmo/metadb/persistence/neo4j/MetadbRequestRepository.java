package org.mskcc.cmo.metadb.persistence.neo4j;

import java.util.List;
import org.mskcc.cmo.metadb.model.MetadbRequest;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.RequestMetadata;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */
@Repository
public interface MetadbRequestRepository extends Neo4jRepository<MetadbRequest, Long> {
    @Query("MATCH (r: Request {igoRequestId: $reqId}) RETURN r;")
    MetadbRequest findRequestById(@Param("reqId") String reqId);

    @Query("MATCH (s: Sample {metaDbSampleId: $metaDbSample.metaDbSampleId}) "
            + "MATCH (s)<-[:HAS_SAMPLE]-(r: Request) "
            + "RETURN r")
    MetadbRequest findRequestByResearchSample(@Param("metaDbSample") MetadbSample metaDbSample);

    @Query("MATCH (r: Request {igoRequestId: $reqId}) "
            + "MATCH (r)-[:HAS_METADATA]->(rm: RequestMetadata) "
            + "RETURN rm")
    List<RequestMetadata> findRequestMetadataHistoryById(@Param("reqId") String reqId);

    @Query("MATCH (r: Request)-[:HAS_METADATA]->(rm: RequestMetadata) "
            + "WHERE $dateRangeStart <= [rm][0].importDate <= $dateRangeEnd "
            + "RETURN [r.metaDbRequestId, r.igoProjectId, r.igoRequestId, [rm][0].importDate]")
    List<List<String>> findRequestWithinDateRange(@Param("dateRangeStart") String startDate,
            @Param("dateRangeEnd") String endDate);
}
