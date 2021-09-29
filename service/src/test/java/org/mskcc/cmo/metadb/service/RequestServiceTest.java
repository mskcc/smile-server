package org.mskcc.cmo.metadb.service;

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.RequestMetadata;
import org.mskcc.cmo.metadb.persistence.MetaDbPatientRepository;
import org.mskcc.cmo.metadb.persistence.MetaDbRequestRepository;
import org.mskcc.cmo.metadb.persistence.MetaDbSampleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataNeo4jTest
@Import(MockDataUtils.class)
public class RequestServiceTest {
    @Autowired
    private MockDataUtils mockDataUtils;

    @Autowired
    private MetadbRequestService requestService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private PatientService patientService;

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

    private final MetaDbRequestRepository requestRepository;
    private final MetaDbSampleRepository sampleRepository;
    private final MetaDbPatientRepository patientRepository;

    /**
     * Initializes the Neo4j repositories.
     * @param requestRepository
     * @param sampleRepository
     * @param patientRepository
     * @param requestService
     * @param sampleService
     */
    @Autowired
    public RequestServiceTest(MetaDbRequestRepository requestRepository,
            MetaDbSampleRepository sampleRepository, MetaDbPatientRepository patientRepository,
            MetadbRequestService requestService, SampleService sampleService,
            PatientService patientService) {
        this.requestRepository = requestRepository;
        this.sampleRepository = sampleRepository;
        this.patientRepository = patientRepository;
    }

    /**
     * Persists the Mock Request data to the test database.
     * @throws Exception
     */
    @Autowired
    public void persistMockRequestDataToTestDb() throws Exception {
        MockJsonTestData request1Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1JsonDataWith2T2N");
        MetaDbRequest request1 = mockDataUtils.extractRequestFromJsonData(request1Data.getJsonString());
        requestService.saveRequest(request1);

        MockJsonTestData request3Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest3JsonDataPooledNormals");
        MetaDbRequest request3 = mockDataUtils.extractRequestFromJsonData(request3Data.getJsonString());
        requestService.saveRequest(request3);

        MockJsonTestData request5Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest5JsonPtMultiSamples");
        MetaDbRequest request5 = mockDataUtils.extractRequestFromJsonData(request5Data.getJsonString());
        requestService.saveRequest(request5);
    }


    /**
     * Tests getMetadbRequestById
     * By checking if it retrieves not null or empty MetaDbRequest
     * @throws Exception
     */
    @Test
    public void getMetadbRequestByIdTest() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        MetaDbRequest existingRequest = requestService.getMetadbRequestById(requestId);
        Assertions.assertThat(existingRequest).isNotNull();
    }

    /**
     * Tests getMetadbRequestById
     * By checking if it returns null when requestId is invalid
     * @throws Exception
     */
    @Test
    public void getNullMetadbRequestByIdTest() throws Exception {
        String requestId = "";
        MetaDbRequest existingRequest = requestService.getMetadbRequestById(requestId);
        Assertions.assertThat(existingRequest).isNull();
    }

    /**
     * Tests requestHasUpdates
     * By checking if it returns false with the exact same requestJsons
     * @throws Exception
     */
    @Test
    public void requestWithNoUpdatesTest() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        MetaDbRequest existingRequest = requestService.getMetadbRequestById(requestId);

        Boolean isUpdated = requestService.requestHasUpdates(existingRequest, existingRequest);
        Assertions.assertThat(isUpdated).isEqualTo(false);
    }

    /**
     * Tests requestHasUpdates
     * Using updated requestJsons, should return true
     * @throws Exception
     */
    @Test
    public void requestHasMetadataUpdatesTest() throws Exception {
        MockJsonTestData updatedRequestData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1UpdatedJsonDataWith2T2N");
        String requestId = "MOCKREQUEST1_B";
        MetaDbRequest origRequest = requestService.getMetadbRequestById(requestId);
        // this updated request as a different investigator email than its original
        MetaDbRequest updatedRequest = mockDataUtils.extractRequestFromJsonData(
                updatedRequestData.getJsonString());

        Boolean hasUpdates = requestService.requestHasUpdates(
                origRequest, updatedRequest);
        Assertions.assertThat(hasUpdates).isEqualTo(true);
    }

    /**
     * Tests getRequestSamplesWithUpdates
     * By checking if the number of returned list of sampleMetadata is zeros
     * @throws Exception
     */
    @Test
    public void getRequestSamplesWithNoUpdatesTest() throws Exception {
        String requestId = "33344_Z";
        MetaDbRequest existingRequest = requestService.getMetadbRequestById(requestId);

        Assertions.assertThat(requestService.getRequestSamplesWithUpdates(
                existingRequest)).isEmpty();
    }

    /**
     * @throws Exception
     *
     */
    @Test
    public void getRequestSamplesWithUpdatesTest() throws Exception {
        MockJsonTestData updatedRequestData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1UpdatedJsonDataWith2T2N");
        MetaDbRequest updatedRequest = mockDataUtils.extractRequestFromJsonData(
                updatedRequestData.getJsonString());

        List<MetaDbSample> sampleList = requestService.getRequestSamplesWithUpdates(
                updatedRequest);
        Assertions.assertThat(sampleList.size()).isEqualTo(2);

    }

    /**
     * Tests getRequestMetadataHistoryByRequestId
     * By checking the size of returned list of RequestMetadata
     * @throws Exception
     */
    @Test
    public void getRequestMetadataHistoryByRequestIdTest() throws Exception {
        String requestId = "145145_IM";
        List<RequestMetadata> existingRequestHistoryList = requestService
                .getRequestMetadataHistoryByRequestId(requestId);

        Assertions.assertThat(existingRequestHistoryList.size()).isEqualTo(1);
    }

}
