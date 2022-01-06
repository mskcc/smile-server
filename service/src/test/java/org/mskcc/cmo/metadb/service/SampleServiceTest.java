package org.mskcc.cmo.metadb.service;

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mskcc.cmo.metadb.model.MetadbRequest;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.persistence.neo4j.MetadbPatientRepository;
import org.mskcc.cmo.metadb.persistence.neo4j.MetadbRequestRepository;
import org.mskcc.cmo.metadb.persistence.neo4j.MetadbSampleRepository;
import org.mskcc.cmo.metadb.service.util.RequestDataFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 *
 * @author ochoaa
 */
@Testcontainers
@DataNeo4jTest
@Import(MockDataUtils.class)
public class SampleServiceTest {
    @Autowired
    private MockDataUtils mockDataUtils;

    @Autowired
    private MetadbRequestService requestService;

    @Autowired
    private MetadbSampleService sampleService;

    @Autowired
    private MetadbPatientService patientService;

    @Container
    private static final Neo4jContainer databaseServer = new Neo4jContainer<>()
            .withEnv("NEO4J_dbms_security_procedures_unrestricted", "apoc.*,algo.*");

    @TestConfiguration
    static class Config {
        @Bean
        public org.neo4j.ogm.config.Configuration configuration() {
            return new org.neo4j.ogm.config.Configuration.Builder()
                    .uri(databaseServer.getBoltUrl())
                    .credentials("neo4j", databaseServer.getAdminPassword())
                    .build();
        }
    }

    private final MetadbRequestRepository requestRepository;
    private final MetadbSampleRepository sampleRepository;
    private final MetadbPatientRepository patientRepository;

    /**
     * Initializes the Neo4j repositories.
     * @param requestRepository
     * @param sampleRepository
     * @param patientRepository
     * @param requestService
     * @param sampleService
     */
    @Autowired
    public SampleServiceTest(MetadbRequestRepository requestRepository,
            MetadbSampleRepository sampleRepository, MetadbPatientRepository patientRepository,
            MetadbRequestService requestService, MetadbSampleService sampleService,
            MetadbPatientService patientService) {
        this.requestRepository = requestRepository;
        this.sampleRepository = sampleRepository;
        this.patientRepository = patientRepository;
    }

    /**
     * Persists the Mock Request data to the test database.
     * @throws Exception
     */
    @Autowired
    public void initializeMockDatabase() throws Exception {
        // mock request id: MOCKREQUEST1_B
        MockJsonTestData request1Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1JsonDataWith2T2N");
        MetadbRequest request1 = RequestDataFactory.buildNewLimsRequestFromJson(request1Data.getJsonString());
        requestService.saveRequest(request1);

        // mock request id: 33344_Z
        MockJsonTestData request3Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest3JsonDataPooledNormals");
        MetadbRequest request3 = RequestDataFactory.buildNewLimsRequestFromJson(request3Data.getJsonString());
        requestService.saveRequest(request3);

        // mock request id: 145145_IM
        MockJsonTestData request5Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest5JsonPtMultiSamples");
        MetadbRequest request5 = RequestDataFactory.buildNewLimsRequestFromJson(request5Data.getJsonString());
        requestService.saveRequest(request5);
    }


    /**
     * Tests if the graphDb is set up accurately
     * @throws Exception
     */
    @Test
    public void testRequestRepositoryAccess() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        MetadbRequest savedRequest = requestService.getMetadbRequestById(requestId);
        Assertions.assertThat(savedRequest.getMetaDbSampleList().size()).isEqualTo(4);
    }

    /**
     * Tests whether findMatchedNormalSample retrieves an accurate list MetadbSample
     * @throws Exception
     */
    @Test
    public void testFindMatchedNormalSample() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_1";
        MetadbSample sample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        List<MetadbSample> matchedNormalList = sampleService.getMatchedNormalsBySample(sample);
        Assertions.assertThat(matchedNormalList.size()).isEqualTo(1);
    }

    /**
     * Tests whether findPooledNormalSample retrieves an accurate list pooled normals
     * @throws Exception
     */
    @Test
    public void testFindPooledNormalSample() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_3";
        MetadbSample sample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        List<String> pooledNormalList = sampleService.getPooledNormalsBySample(sample);
        Assertions.assertThat(pooledNormalList.size()).isEqualTo(10);
    }

    /**
     * Tests if the number of sampleMetadata, from a list retrieved
     * using getSampleMetadataListByCmoPatientId,
     * matches the expected number
     * @throws Exception
     */
    @Test
    public void testGetSampleMetadataListByCmoPatientId() throws Exception {
        String cmoPatientId = "C-PXXXD9";
        List<SampleMetadata> savedSampleMetadataList = sampleService
                .getSampleMetadataListByCmoPatientId(cmoPatientId);
        Assertions.assertThat(savedSampleMetadataList.size()).isEqualTo(2);
    }

    /**
     * Tests if the number of sampleMetadata, from a list retrieved
     * using getSampleMetadataListByCmoPatientId,
     * matches the expected number
     * @throws Exception
     */
    @Test
    public void testGetAllMetadbSamplesByRequestId() throws Exception {
        String requestId = "33344_Z";
        List<MetadbSample> requestSamplesList = sampleService.getResearchSamplesByRequestId(requestId);
        Assertions.assertThat(requestSamplesList.size()).isEqualTo(4);
    }

    /**
     * Tests if the number of sampleMetadata history nodes,
     * from a list retrieved using getSampleMetadataHistoryByIgoId,
     * matches the expected number
     * @throws Exception
     */
    @Test
    public void testGetSampleMetadataHistoryByIgoId() throws Exception {
        String igoId = "MOCKREQUEST1_B_1";
        List<SampleMetadata> sampleMetadataHistory = sampleService
                .getResearchSampleMetadataHistoryByIgoId(igoId);
        Assertions.assertThat(sampleMetadataHistory.size()).isEqualTo(1);
    }

    /**
     * Tests if sampleHasMetadataUpdates accurately recognizes changes in sampleMetadata
     * @throws Exception
     */
    @Test
    public void testSampleHasMetadataUpdates() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_1";
        MetadbSample sample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);

        MockJsonTestData updatedRequestData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1UpdatedJsonDataWith2T2N");
        MetadbRequest updatedRequest = RequestDataFactory.buildNewLimsRequestFromJson(
                updatedRequestData.getJsonString());
        MetadbSample updatedSample = updatedRequest.getMetaDbSampleList().get(0);

        Boolean hasUpdates = sampleService.sampleHasMetadataUpdates(sample.getLatestSampleMetadata(),
                updatedSample.getLatestSampleMetadata());
        Assertions.assertThat(hasUpdates).isEqualTo(Boolean.TRUE);

    }

    /**
     * Tests if the number of sampleMetadata history nodes
     * matches the expected number after updating sampleMetadata
     * @throws Exception
     */
    @Test
    public void testSampleHistoryAfterUpdate() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_2";

        MockJsonTestData updatedRequestData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1UpdatedJsonDataWith2T2N");
        MetadbRequest updatedRequest = RequestDataFactory.buildNewLimsRequestFromJson(
                updatedRequestData.getJsonString());
        MetadbSample updatedSample = updatedRequest.getMetaDbSampleList().get(1);
        sampleService.saveSampleMetadata(updatedSample);

        List<SampleMetadata> sampleMetadataHistory = sampleService
                .getResearchSampleMetadataHistoryByIgoId(igoId);
        Assertions.assertThat(sampleMetadataHistory.size()).isEqualTo(2);

    }

    /**
     * Tests if the returned list of sampleMetadata history is sorted based on importDate
     * @throws Exception
     */
    @Test
    public void testSampleHistoryListIsAscendingByImportDate() throws Exception {
        String igoId = "MOCKREQUEST1_B_4";
        List<SampleMetadata> sampleMetadataHistory = sampleService
                .getResearchSampleMetadataHistoryByIgoId(igoId);
        Assertions.assertThat(sampleMetadataHistory).isSorted();
    }

}
