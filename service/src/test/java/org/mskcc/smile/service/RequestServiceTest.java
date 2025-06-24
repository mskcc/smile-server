package org.mskcc.smile.service;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mskcc.smile.model.RequestMetadata;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.web.RequestSummary;
import org.mskcc.smile.persistence.neo4j.CohortCompleteRepository;
import org.mskcc.smile.persistence.neo4j.SmilePatientRepository;
import org.mskcc.smile.persistence.neo4j.SmileRequestRepository;
import org.mskcc.smile.persistence.neo4j.SmileSampleRepository;
import org.mskcc.smile.persistence.neo4j.TempoRepository;
import org.mskcc.smile.service.util.RequestDataFactory;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 *
 * @author ochoaa
 */
@SpringBootTest(
        classes = SmileTestApp.class,
        properties = {"spring.neo4j.authentication.username:neo4j", "databricks.url:"}
)
@Testcontainers
@Import(MockDataUtils.class)
@TestMethodOrder(OrderAnnotation.class)
public class RequestServiceTest {
    @Autowired
    private MockDataUtils mockDataUtils;

    @Autowired
    private SmileRequestService requestService;

    @Autowired
    private SmileSampleService sampleService;

    @Autowired
    private SmilePatientService patientService;

    @Autowired
    private TempoService tempoService;

    // required for all test classes
    @Container
    private static final Neo4jContainer<?> databaseServer = new Neo4jContainer<>(
            DockerImageName.parse("neo4j:5.19.0"))
            .withEnv("NEO4J_dbms_security_procedures_unrestricted", "apoc.*,algo.*");

    @DynamicPropertySource
    static void neo4jProperties(DynamicPropertyRegistry registry) {
        databaseServer.start();
        registry.add("spring.neo4j.authentication.password", databaseServer::getAdminPassword);
        registry.add("spring.neo4j.uri", databaseServer::getBoltUrl);
    }

    @TestConfiguration
    static class Config {
        @Bean
        public org.neo4j.ogm.config.Configuration configuration() {
            return new org.neo4j.ogm.config.Configuration.Builder()
                    .uri(databaseServer.getBoltUrl())
                    .credentials("neo4j", databaseServer.getAdminPassword())
                    .build();
        }

        @Bean
        public SessionFactory sessionFactory() {
            // with domain entity base package(s)
            return new SessionFactory(configuration(), "org.mskcc.smile.persistence");
        }
    }

    @Autowired
    private SessionFactory sessionFactory;

    private final SmileRequestRepository requestRepository;
    private final SmileSampleRepository sampleRepository;
    private final SmilePatientRepository patientRepository;
    private final TempoRepository tempoRepository;
    private final CohortCompleteRepository cohortCompleteRepository;

    /**
     * Initializes the Neo4j repositories.
     * @param requestRepository
     * @param sampleRepository
     * @param patientRepository
     * @param tempoRepository
     * @param cohortCompleteRepository
     */
    @Autowired
    public RequestServiceTest(SmileRequestRepository requestRepository,
            SmileSampleRepository sampleRepository, SmilePatientRepository patientRepository,
            TempoRepository tempoRepository, CohortCompleteRepository cohortCompleteRepository) {
        this.requestRepository = requestRepository;
        this.sampleRepository = sampleRepository;
        this.patientRepository = patientRepository;
        this.tempoRepository = tempoRepository;
        this.cohortCompleteRepository = cohortCompleteRepository;
    }

    /**
     * Persists the Mock Request data to the test database.
     * @throws Exception
     */
    @BeforeEach
    public void initializeMockDatabase() throws Exception {
        Session session = sessionFactory.openSession();
        session.purgeDatabase();

        // mock request id: MOCKREQUEST1_B
        MockJsonTestData request1Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1JsonDataWith2T2N");
        SmileRequest request1 =
                RequestDataFactory.buildNewLimsRequestFromJson(request1Data.getJsonString());
        requestService.saveRequest(request1);

        // mock request id: 33344_Z
        MockJsonTestData request3Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest3JsonDataPooledNormals");
        SmileRequest request3 =
                RequestDataFactory.buildNewLimsRequestFromJson(request3Data.getJsonString());
        requestService.saveRequest(request3);

        // mock request id: 145145_IM
        MockJsonTestData request5Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest5JsonPtMultiSamples");
        SmileRequest request5 =
                RequestDataFactory.buildNewLimsRequestFromJson(request5Data.getJsonString());
        requestService.saveRequest(request5);
    }

    /**
     * Tests integrity of mock database by fetching request.
     * @throws Exception
     */
    @Test
    @Order(1)
    public void testGetValidRequest() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        SmileRequest existingRequest = requestService.getSmileRequestById(requestId);
        Assertions.assertNotNull(existingRequest);
        Assertions.assertTrue(requestHasExpectedFieldsPopulated(existingRequest));
    }

    /**
     * Tests integrity of mock database by checking if it returns
     * null when requestId is invalid.
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testGetInvalidRequestAsNull() throws Exception {
        String requestId = "";
        SmileRequest existingRequest = requestService.getSmileRequestById(requestId);
        Assertions.assertNull(existingRequest);
    }

    /**
     * Tests case where request does not have any updates.
     * @throws Exception
     */
    @Test
    @Order(3)
    public void testRequestWithNoUpdates() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        SmileRequest existingRequest = requestService.getSmileRequestById(requestId);
        Assertions.assertTrue(requestHasExpectedFieldsPopulated(existingRequest));
        // get the matching request from the mock data utils
        MockJsonTestData request1Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1JsonDataWith2T2N");
        SmileRequest requestFromMockData = RequestDataFactory.buildNewLimsRequestFromJson(
                request1Data.getJsonString());
        Assertions.assertTrue(requestHasExpectedFieldsPopulated(requestFromMockData));

        Boolean isUpdated = requestService.requestHasUpdates(existingRequest,
                requestFromMockData, Boolean.FALSE);
        Assertions.assertFalse(isUpdated);
    }

    /**
     * Tests case where request does contain updates.
     * @throws Exception
     */
    @Test
    @Order(4)
    public void testRequestHasMetadataUpdates() throws Exception {
        MockJsonTestData updatedRequestData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1UpdatedJsonDataWith2T2N");
        String requestId = "MOCKREQUEST1_B";
        SmileRequest origRequest = requestService.getSmileRequestById(requestId);
        Assertions.assertTrue(requestHasExpectedFieldsPopulated(origRequest));
        // this updated request as a different investigator email than its original
        SmileRequest updatedRequest = RequestDataFactory.buildNewLimsRequestFromJson(
                updatedRequestData.getJsonString());
        Assertions.assertTrue(requestHasExpectedFieldsPopulated(updatedRequest));

        // run comparator and assert true, then persist update and assert new
        // metadata  node added to request node
        Boolean hasUpdates = requestService.requestHasUpdates(
                origRequest, updatedRequest, Boolean.FALSE);
        Assertions.assertTrue(hasUpdates);

        // requestAfterInitialUpdates will have this new version of metadata and will be
        // referenced again below
        requestService.updateRequestMetadata(updatedRequest.getLatestRequestMetadata(), Boolean.FALSE);
        Assertions.assertEquals(2, requestService.getRequestMetadataHistory(requestId).size());

        // now load instance of RequestMetadata ONLY from the mocked data object
        // and assert that it is different ("updated") compared to the original request mdata
        RequestMetadata updatedMetadata = RequestDataFactory.buildNewRequestMetadataFromMetadata(
                updatedRequestData.getJsonString());
        Boolean hasMetadataUpdates = requestService.requestHasMetadataUpdates(
                origRequest.getLatestRequestMetadata(), updatedMetadata, Boolean.FALSE);
        Assertions.assertTrue(hasMetadataUpdates);

        // now that it's confirmed that there are in fact additional updates for this request
        // check for more updates following the initial update from above
        // this time the updated data is getting pulled directly from the database instead
        // of being extracted from the mock data json object
        SmileRequest requestAfterInitialUpdates = requestService.getSmileRequestById(requestId);
        Assertions.assertTrue(requestHasExpectedFieldsPopulated(requestAfterInitialUpdates));
        MockJsonTestData updatedRequestMetadataData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockUpdatedPublishedRequest1Metadata");
        RequestMetadata updatedRequestMetadata = RequestDataFactory
                .buildNewRequestMetadataFromMetadata(updatedRequestMetadataData.getJsonString());
        Boolean hasMoreUpdates = requestService.requestHasMetadataUpdates(
                requestAfterInitialUpdates.getLatestRequestMetadata(), updatedRequestMetadata, Boolean.FALSE);
        Assertions.assertTrue(hasMoreUpdates);

        // now that more updates are confirmed, persist these and verify that an additional
        // metadata node has been added to the request node (bringing the total to 3)
        requestService.updateRequestMetadata(updatedRequestMetadata, Boolean.FALSE);
        Assertions.assertEquals(3, requestService.getRequestMetadataHistory(requestId).size());
    }

    /**
     * Tests case where samples in a request do not contain updates.
     * By checking if the number of returned list of sampleMetadata is zeros
     * @throws Exception
     */
    @Test
    @Order(5)
    public void testRequestSamplesWithNoUpdates() throws Exception {
        // get request from the mock data utils to compare against request in db
        MockJsonTestData request3Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest3JsonDataPooledNormals");
        SmileRequest requestFromMockData = RequestDataFactory.buildNewLimsRequestFromJson(
                request3Data.getJsonString());
        Assertions.assertTrue(requestHasExpectedFieldsPopulated(requestFromMockData));

        List<SmileSample> samplesWithUpdates =
                requestService.getRequestSamplesWithUpdates(requestFromMockData);
        Assertions.assertTrue(samplesWithUpdates.isEmpty());
    }

    /**
     * Tests case where incoming request contains samples with metadata updates.
     * @throws Exception
     */
    @Test
    @Order(6)
    public void testRequestSamplesWithUpdates() throws Exception {
        MockJsonTestData updatedRequestData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1UpdatedJsonDataWith2T2N");
        SmileRequest updatedRequest = RequestDataFactory.buildNewLimsRequestFromJson(
                updatedRequestData.getJsonString());
        Assertions.assertTrue(requestHasExpectedFieldsPopulated(updatedRequest));

        List<SmileSample> sampleList = requestService.getRequestSamplesWithUpdates(
                updatedRequest);
        Assertions.assertEquals(2, sampleList.size());
    }

    /**
     * Tests case where incoming request contains samples with
     * invalid metadata updates and should not be persisted.
     * @throws Exception
     */
    @Test
    @Order(7)
    public void testInvaildIgoRequestUpdates() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        SmileRequest origRequest = requestService.getSmileRequestById(requestId);

        MockJsonTestData updatedRequestData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1UpdatedJsonDataWith2T2N");
        SmileRequest updatedRequest = RequestDataFactory.buildNewLimsRequestFromJson(
                updatedRequestData.getJsonString());

        Boolean hasUpdates =
                requestService.requestHasMetadataUpdates(origRequest.getLatestRequestMetadata(),
                        updatedRequest.getLatestRequestMetadata(), Boolean.FALSE);
        Assertions.assertTrue(hasUpdates);

        requestService.saveRequest(updatedRequest);
        SmileRequest existingRequest = requestService.getSmileRequestById(requestId);
        Assertions.assertTrue(existingRequest.getIsCmoRequest());
        Assertions.assertNotEquals("invalid-igo-update", existingRequest.getQcAccessEmails());
    }

    /**
     * Tests case where a given request has a single version of metadata persisted in db.
     * @throws Exception
     */
    @Test
    @Order(8)
    public void testRequestMetadataHistoryByRequestId() throws Exception {
        String requestId = "145145_IM";
        List<RequestMetadata> existingRequestHistoryList = requestService
                .getRequestMetadataHistory(requestId);

        Assertions.assertEquals(1, existingRequestHistoryList.size());
    }

    /**
     * Test for getRequestsByDate
     * Case 1: Tests when end date is null
     * @throws Exception
     */
    @Test
    @Order(9)
    public void testGetRequestsByNullEndDate() throws Exception {
        String startDate = "2021-10-25";
        List<RequestSummary> requestDataList = requestService.getRequestsByDate(startDate, null);
        Assertions.assertEquals(3, requestDataList.size());
    }

    /**
     * Test for getRequestsByDate
     * Case 2: Test when both start and end date are null,
     * Expected to throw an exception
     * @throws Exception
     */
    @Test
    @Order(10)
    public void testGetRequestsByNullDates() throws Exception {
        Assertions.assertThrows(RuntimeException.class,  () -> {
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
    @Order(11)
    public void testGetRequestsByNullStartDate() throws Exception {
        String endDate = "2021-10-25";
        Assertions.assertThrows(RuntimeException.class,  () -> {
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
    @Order(12)
    public void testGetRequestsByInvalidDateRange() throws Exception {
        String endDate = "2021-10-24";
        String startDate = "2021-10-25";
        Assertions.assertThrows(RuntimeException.class,  () -> {
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
    @Order(13)
    public void testGetRequestsByInvalidDate() throws Exception {
        String startDate = "2021-13-25";
        Assertions.assertThrows(RuntimeException.class,  () -> {
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
    @Order(14)
    public void testGetRequestsByInvalidDateFormat() throws Exception {
        String startDate = "25/10/2021";
        Assertions.assertThrows(RuntimeException.class,  () -> {
            requestService.getRequestsByDate(startDate, null);
        });
    }

    private Boolean requestHasExpectedFieldsPopulated(SmileRequest request) {
        return ((request.getRequestMetadataList() != null && !request.getRequestMetadataList().isEmpty())
                && (request.getSmileSampleList() != null && !request.getSmileSampleList().isEmpty())
                && (request.getNamespace() != null && !request.getNamespace().isEmpty())
                && (request.getRequestJson() != null && !request.getRequestJson().isEmpty()));
    }
}
