package org.mskcc.smile.persistence.neo4j;

import java.util.List;
import org.mskcc.smile.model.RequestMetadata;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */
@Repository
public interface SmileRequestRepository extends Neo4jRepository<SmileRequest, Long> {
    @Query("""
           MATCH (r: Request {igoRequestId: $reqId})
           RETURN r
           """)
    SmileRequest findRequestById(@Param("reqId") String reqId);

    @Query("""
           MATCH (s: Sample {smileSampleId: $smileSample.smileSampleId})
           MATCH (s)<-[:HAS_SAMPLE]-(r: Request)
           RETURN DISTINCT r
           """)
    SmileRequest findRequestByResearchSample(@Param("smileSample") SmileSample smileSample);

    @Query("""
           MATCH (r: Request {igoRequestId: $reqId})
           MATCH (r)-[:HAS_METADATA]->(rm: RequestMetadata)-[hs:HAS_STATUS]->(rs: Status)
           RETURN rm, hs, rs
           """)
    List<RequestMetadata> findRequestMetadataHistoryByRequestId(@Param("reqId") String reqId);

    @Query("""
           MATCH (r: Request)-[:HAS_METADATA]->(rm: RequestMetadata)
           WHERE $dateRangeStart <= [rm][0].importDate <= $dateRangeEnd
           RETURN DISTINCT [r.smileRequestId, r.igoProjectId, r.igoRequestId]
           """)
    List<List<String>> findRequestWithinDateRange(@Param("dateRangeStart") String startDate,
            @Param("dateRangeEnd") String endDate);
}
