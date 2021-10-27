package org.mskcc.cmo.metadb.service;

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mskcc.cmo.metadb.model.MetadbRequest;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.RequestMetadata;
import org.mskcc.cmo.metadb.persistence.MetadbPatientRepository;
import org.mskcc.cmo.metadb.persistence.MetadbRequestRepository;
import org.mskcc.cmo.metadb.persistence.MetadbSampleRepository;
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
    public RequestServiceTest(MetadbRequestRepository requestRepository,
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
        MetadbRequest request1 = mockDataUtils.extractRequestFromJsonData(request1Data.getJsonString());
        requestService.saveRequest(request1);

        // mock request id: 33344_Z
        MockJsonTestData request3Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest3JsonDataPooledNormals");
        MetadbRequest request3 = mockDataUtils.extractRequestFromJsonData(request3Data.getJsonString());
        requestService.saveRequest(request3);

        // mock request id: 145145_IM
        MockJsonTestData request5Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest5JsonPtMultiSamples");
        MetadbRequest request5 = mockDataUtils.extractRequestFromJsonData(request5Data.getJsonString());
        requestService.saveRequest(request5);
    }


    /**
     * Tests integrity of mock database by fetching request.
     * @throws Exception
     */
    @Test
    public void testGetValidRequest() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        MetadbRequest existingRequest = requestService.getMetadbRequestById(requestId);
        Assertions.assertThat(existingRequest).isNotNull();
    }

    /**
     * Tests integrity of mock database by checking if it returns
     * null when requestId is invalid.
     * @throws Exception
     */
    @Test
    public void testGetInvalidRequestAsNull() throws Exception {
        String requestId = "";
        MetadbRequest existingRequest = requestService.getMetadbRequestById(requestId);
        Assertions.assertThat(existingRequest).isNull();
    }

    /**
     * Tests case where request does not have any updates.
     * @throws Exception
     */
    @Test
    public void testRequestWithNoUpdates() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        MetadbRequest existingRequest = requestService.getMetadbRequestById(requestId);
        // get the matching request from the mock data utils
        MockJsonTestData request1Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1JsonDataWith2T2N");
        MetadbRequest requestFromMockData = mockDataUtils.extractRequestFromJsonData(
                request1Data.getJsonString());

        Boolean isUpdated = requestService.requestHasUpdates(existingRequest, requestFromMockData);
        Assertions.assertThat(isUpdated).isEqualTo(Boolean.FALSE);
    }

    /**
     * Tests case where request does contain updates.
     * @throws Exception
     */
    @Test
    public void testRequestHasMetadataUpdates() throws Exception {
        MockJsonTestData updatedRequestData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1UpdatedJsonDataWith2T2N");
        String requestId = "MOCKREQUEST1_B";
        MetadbRequest origRequest = requestService.getMetadbRequestById(requestId);
        // this updated request as a different investigator email than its original
        MetadbRequest updatedRequest = mockDataUtils.extractRequestFromJsonData(
                updatedRequestData.getJsonString());

        Boolean hasUpdates = requestService.requestHasUpdates(
                origRequest, updatedRequest);
        Assertions.assertThat(hasUpdates).isEqualTo(Boolean.TRUE);
    }

    /**
     * Tests case where samples in a request do not contain updates.
     * By checking if the number of returned list of sampleMetadata is zeros
     * @throws Exception
     */
    @Test
    public void testRequestSamplesWithNoUpdates() throws Exception {
        // get request from the mock data utils to compare against request in db
        MockJsonTestData request3Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest3JsonDataPooledNormals");
        MetadbRequest requestFromMockData = mockDataUtils.extractRequestFromJsonData(
                request3Data.getJsonString());

        List<MetadbSample> samplesWithUpdates =
                requestService.getRequestSamplesWithUpdates(requestFromMockData);
        Assertions.assertThat(samplesWithUpdates).isEmpty();
    }

    /**
     * Tests case where incoming request contains samples with metadata updates.
     * @throws Exception
     */
    @Test
    public void testRequestSamplesWithUpdates() throws Exception {
        MockJsonTestData updatedRequestData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1UpdatedJsonDataWith2T2N");
        MetadbRequest updatedRequest = mockDataUtils.extractRequestFromJsonData(
                updatedRequestData.getJsonString());

        List<MetadbSample> sampleList = requestService.getRequestSamplesWithUpdates(
                updatedRequest);
        Assertions.assertThat(sampleList.size()).isEqualTo(2);

    }

    /**
     * Tests case where a given request has a single version of metadata persisted in db.
     * @throws Exception
     */
    @Test
    public void testRequestMetadataHistoryByRequestId() throws Exception {
        String requestId = "145145_IM";
        List<RequestMetadata> existingRequestHistoryList = requestService
                .getRequestMetadataHistory(requestId);

        Assertions.assertThat(existingRequestHistoryList.size()).isEqualTo(1);
    }

    /**
     * Test for getRequestsByDate
     * Case 1: Tests when end date is null
     * @throws Exception
     */

    @Test
    public void testGetRequestsByNullEndDate() throws Exception {
        String startDate = "2021-10-25";
        List<List<String>> requestDataList = requestService.getRequestsByDate(startDate, null);
        Assertions.assertThat(requestDataList.size()).isEqualTo(3);
    }

    /**
     * Test for getRequestsByDate
     * Case 2: Test when both start and end date are null,
     * Expected to throw an exception
     * @throws Exception
     */
    @Test
    public void testGetRequestsByNullDates() throws Exception {
        Assertions.assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> {
                requestService.getRequestsByDate(null, null);
            });
    }

    /**
     * Test for getRequestsByDate
     * Case 3: Test when start date is null and end date isn't,
     * Expected to throw an exception
     * @throws Exception
     */
    @Test
    public void testGetRequestsByNullStartDate() throws Exception {
        String endDate = "2021-10-25";
        Assertions.assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> {
                requestService.getRequestsByDate(null, endDate);
            });
    }

    /**
     * Test for getRequestsByDate
     * Case 4: Test when end date is less than start date,
     * Expected to throw an exception
     * @throws Exception
     */
    @Test
    public void testGetRequestsByInvalidDateRange() throws Exception {
        String endDate = "2021-10-24";
        String startDate = "2021-10-25";
        Assertions.assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> {
                requestService.getRequestsByDate(startDate, endDate);
            });
    }

    /**
     * Test for getRequestsByDate
     * Case 5: Test when dates are invalid(example: 2021-13-34),
     * Expected to throw an exception
     * @throws Exception
     */
    @Test
    public void testGetRequestsByInvalidDate() throws Exception {
        String startDate = "2021-13-25";
        Assertions.assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> {
                requestService.getRequestsByDate(startDate, null);
            });
    }

    /**
     * Test for getRequestsByDate
     * Case 6: Test when dates are in invalid format(example: 10/10/2021),
     * Expected to throw an exception
     * @throws Exception
     */
    @Test
    public void testGetRequestsByInvalidDateFormat() throws Exception {
        String startDate = "25/10/2021";
        Assertions.assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> {
                requestService.getRequestsByDate(startDate, null);
            });
    }
}
