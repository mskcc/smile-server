package org.mskcc.smile.persistence.neo4j;

import java.util.List;
import org.mskcc.smile.model.RequestMetadata;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.Status;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.annotation.Query;
//import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */
@Repository
public interface SmileRequestRepository extends Neo4jRepository<SmileRequest, Long> {
    @Query("MATCH (r: Request {igoRequestId: $reqId}) RETURN r")
    SmileRequest findRequestById(@Param("reqId") String reqId);

    @Query("MATCH (s: Sample {smileSampleId: $smileSample.smileSampleId}) "
            + "MATCH (s)<-[:HAS_SAMPLE]-(r: Request) "
            + "RETURN r")
    SmileRequest findRequestByResearchSample(@Param("smileSample") SmileSample smileSample);

    @Query("MATCH (r: Request {igoRequestId: $reqId}) "
            + "MATCH (r)-[:HAS_METADATA]->(rm: RequestMetadata) "
            + "RETURN rm")
    List<RequestMetadata> findRequestMetadataHistoryById(@Param("reqId") String reqId);

    @Query("MATCH (rm: RequestMetadata)-[:HAS_STATUS]->(st: Status) "
            + "WHERE ID(rm) = $rmId "
            + "RETURN st")
    Status findStatusForRequestMetadataById(@Param("rmId") Long rmId);

    @Query("MATCH (r: Request)-[:HAS_METADATA]->(rm: RequestMetadata) "
            + "WHERE $dateRangeStart <= [rm][0].importDate <= $dateRangeEnd "
            + "RETURN DISTINCT [r.smileRequestId, r.igoProjectId, r.igoRequestId]")
    List<List<String>> findRequestWithinDateRange(@Param("dateRangeStart") String startDate,
            @Param("dateRangeEnd") String endDate);
}
