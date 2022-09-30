package org.mskcc.smile.service;

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mskcc.smile.model.RequestMetadata;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.web.RequestSummary;
import org.mskcc.smile.persistence.neo4j.SmilePatientRepository;
import org.mskcc.smile.persistence.neo4j.SmileRequestRepository;
import org.mskcc.smile.persistence.neo4j.SmileSampleRepository;
import org.mskcc.smile.service.util.RequestDataFactory;
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
    private SmileRequestService requestService;

    @Autowired
    private SmileSampleService sampleService;

    @Autowired
    private SmilePatientService patientService;

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

    private final SmileRequestRepository requestRepository;
    private final SmileSampleRepository sampleRepository;
    private final SmilePatientRepository patientRepository;

    /**
     * Initializes the Neo4j repositories.
     * @param requestRepository
     * @param sampleRepository
     * @param patientRepository
     * @param requestService
     * @param sampleService
     */
    @Autowired
    public RequestServiceTest(SmileRequestRepository requestRepository,
            SmileSampleRepository sampleRepository, SmilePatientRepository patientRepository,
            SmileRequestService requestService, SmileSampleService sampleService,
            SmilePatientService patientService) {
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
        SmileRequest request1 = RequestDataFactory.buildNewLimsRequestFromJson(request1Data.getJsonString());
        requestService.saveRequest(request1);

        // mock request id: 33344_Z
        MockJsonTestData request3Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest3JsonDataPooledNormals");
        SmileRequest request3 = RequestDataFactory.buildNewLimsRequestFromJson(request3Data.getJsonString());
        requestService.saveRequest(request3);

        // mock request id: 145145_IM
        MockJsonTestData request5Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest5JsonPtMultiSamples");
        SmileRequest request5 = RequestDataFactory.buildNewLimsRequestFromJson(request5Data.getJsonString());
        requestService.saveRequest(request5);
    }

    /**
     * Tests integrity of mock database by fetching request.
     * @throws Exception
     */
    @Test
    public void testGetValidRequest() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        SmileRequest existingRequest = requestService.getSmileRequestById(requestId);
        Assertions.assertThat(existingRequest).isNotNull();
        Assertions.assertThat(requestHasExpectedFieldsPopulated(existingRequest)).isTrue();
    }

    /**
     * Tests integrity of mock database by checking if it returns
     * null when requestId is invalid.
     * @throws Exception
     */
    @Test
    public void testGetInvalidRequestAsNull() throws Exception {
        String requestId = "";
        SmileRequest existingRequest = requestService.getSmileRequestById(requestId);
        Assertions.assertThat(existingRequest).isNull();
    }

    /**
     * Tests case where request does not have any updates.
     * @throws Exception
     */
    @Test
    public void testRequestWithNoUpdates() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        SmileRequest existingRequest = requestService.getSmileRequestById(requestId);
        Assertions.assertThat(requestHasExpectedFieldsPopulated(existingRequest)).isTrue();
        // get the matching request from the mock data utils
        MockJsonTestData request1Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1JsonDataWith2T2N");
        SmileRequest requestFromMockData = RequestDataFactory.buildNewLimsRequestFromJson(
                request1Data.getJsonString());
        Assertions.assertThat(requestHasExpectedFieldsPopulated(requestFromMockData)).isTrue();

        Boolean isUpdated = requestService.requestHasUpdates(existingRequest,
                requestFromMockData, Boolean.FALSE);
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
        SmileRequest origRequest = requestService.getSmileRequestById(requestId);
        Assertions.assertThat(requestHasExpectedFieldsPopulated(origRequest)).isTrue();
        // this updated request as a different investigator email than its original
        SmileRequest updatedRequest = RequestDataFactory.buildNewLimsRequestFromJson(
                updatedRequestData.getJsonString());
        Assertions.assertThat(requestHasExpectedFieldsPopulated(updatedRequest)).isTrue();

        Boolean hasUpdates = requestService.requestHasUpdates(
                origRequest, updatedRequest, Boolean.FALSE);
        Assertions.assertThat(hasUpdates).isEqualTo(Boolean.TRUE);


        RequestMetadata updatedMetadata = RequestDataFactory.buildNewRequestMetadataFromMetadata(
                updatedRequestData.getJsonString());
        Boolean hasMetadataUpdates = requestService.requestHasMetadataUpdates(
                origRequest.getLatestRequestMetadata(), updatedMetadata, Boolean.FALSE);
        Assertions.assertThat(hasMetadataUpdates).isEqualTo(Boolean.TRUE);
    }

    /**
     * Tests instance of RequestMetadata that's been updated against the latest request
     * metadata persisted in the database for the matching Request ID.
     * @throws Exception
     */
    @Test
    public void testRequestHasUpdatesWithUniversalSchema() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        SmileRequest origRequest = requestService.getSmileRequestById(requestId);
        Assertions.assertThat(requestHasExpectedFieldsPopulated(origRequest)).isTrue();

        MockJsonTestData updatedRequestMetadataData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockUpdatedPublishedRequest1Metadata");
        RequestMetadata updatedRequestMetadata = RequestDataFactory
                .buildNewRequestMetadataFromMetadata(updatedRequestMetadataData.getJsonString());

        Boolean hasUpdates = requestService.requestHasMetadataUpdates(
                origRequest.getLatestRequestMetadata(), updatedRequestMetadata, Boolean.FALSE);
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
        SmileRequest requestFromMockData = RequestDataFactory.buildNewLimsRequestFromJson(
                request3Data.getJsonString());
        Assertions.assertThat(requestHasExpectedFieldsPopulated(requestFromMockData)).isTrue();

        List<SmileSample> samplesWithUpdates =
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
        SmileRequest updatedRequest = RequestDataFactory.buildNewLimsRequestFromJson(
                updatedRequestData.getJsonString());
        Assertions.assertThat(requestHasExpectedFieldsPopulated(updatedRequest)).isTrue();

        List<SmileSample> sampleList = requestService.getRequestSamplesWithUpdates(
                updatedRequest);
        Assertions.assertThat(sampleList.size()).isEqualTo(2);

    }
    
    /**
     * Tests case where incoming request contains samples with
     * invalid metadata updates and should not be persisted.
     * @throws Exception
     */
    @Test
    public void testInvaildIgoRequestUpdates() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        SmileRequest origRequest = requestService.getSmileRequestById(requestId);
        
        MockJsonTestData updatedRequestData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1UpdatedJsonDataWith2T2N");
        SmileRequest updatedRequest = RequestDataFactory.buildNewLimsRequestFromJson(
                updatedRequestData.getJsonString());
        
        Boolean hasUpdates = requestService.requestHasMetadataUpdates(origRequest.getLatestRequestMetadata(),
                updatedRequest.getLatestRequestMetadata(), Boolean.TRUE);
        Assertions.assertThat(hasUpdates).isTrue();
        
        requestService.saveRequest(updatedRequest);
        SmileRequest existingRequest = requestService.getSmileRequestById(requestId);
        Assertions.assertThat(existingRequest.getIsCmoRequest()).isTrue();
        Assertions.assertThat(existingRequest.getQcAccessEmails()).isNotEqualTo("invalid-igo-update"); 
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
        List<RequestSummary> requestDataList = requestService.getRequestsByDate(startDate, null);
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

    private Boolean requestHasExpectedFieldsPopulated(SmileRequest request) {
        return ((request.getRequestMetadataList() != null && !request.getRequestMetadataList().isEmpty())
                && (request.getSmileSampleList() != null && !request.getSmileSampleList().isEmpty())
                && (request.getNamespace() != null && !request.getNamespace().isEmpty())
                && (request.getRequestJson() != null && !request.getRequestJson().isEmpty()));
    }

    /**
     * Tests if requestMetadata with updates is being persisted correctly
     * @throws Exception
     */
    @Test
    public void testUpdateRequestMetadata() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        SmileRequest origRequest = requestService.getSmileRequestById(requestId);
        Assertions.assertThat(requestHasExpectedFieldsPopulated(origRequest)).isTrue();

        MockJsonTestData updatedRequestMetadataData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockUpdatedPublishedRequest1Metadata");
        RequestMetadata updatedRequestMetadata = RequestDataFactory
                .buildNewRequestMetadataFromMetadata(updatedRequestMetadataData.getJsonString());
        requestService.updateRequestMetadata(updatedRequestMetadata, Boolean.FALSE);

        Assertions.assertThat(requestService.getRequestMetadataHistory(requestId).size()).isEqualTo(2);
    }

    /**
     * Tests if requestMetadata with invalid updates is being handled correctly
     * @throws Exception
     */
    @Test
    public void testIgoUpdateRequestMetadata() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        SmileRequest origRequest = requestService.getSmileRequestById(requestId);
        Assertions.assertThat(requestHasExpectedFieldsPopulated(origRequest)).isTrue();

        MockJsonTestData updatedRequestMetadataData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockUpdatedPublishedRequest1Metadata");
        RequestMetadata updatedRequestMetadata = RequestDataFactory
                .buildNewRequestMetadataFromMetadata(updatedRequestMetadataData.getJsonString());
        // The update shouldn't be persisted because no changes are recognized
        requestService.updateRequestMetadata(updatedRequestMetadata, Boolean.TRUE);

        Assertions.assertThat(requestService.getRequestMetadataHistory(requestId).size()).isEqualTo(1);
    }
}
