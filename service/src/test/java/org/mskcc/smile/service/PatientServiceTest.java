package org.mskcc.smile.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mskcc.smile.model.PatientAlias;
import org.mskcc.smile.model.SmilePatient;
import org.mskcc.smile.model.SmileRequest;
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
import org.springframework.dao.IncorrectResultSizeDataAccessException;
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
        properties = {"spring.neo4j.authentication.username:neo4j"}
)
@Testcontainers
@Import(MockDataUtils.class)
public class PatientServiceTest {
    @Autowired
    private MockDataUtils mockDataUtils;

    @Autowired
    private SmileRequestService requestService;

    @Autowired
    private SmileSampleService sampleService;

    @Autowired
    private SmilePatientService patientService;

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
    public PatientServiceTest(SmileRequestRepository requestRepository,
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
     * Tests if patient service retrieves SmilePatient by cmoPatientId.
     * @throws Exception
     */
    @Test
    public void testFindPatientByPatientAlias() throws Exception {
        String cmoPatientId = "C-1MP6YY";
        Assertions.assertNotNull(patientService.getPatientByCmoPatientId(cmoPatientId));
    }

    /**
     * Tests if patientRepo throws an exception when duplicates
     * are attempted to be saved.
     */
    @Test
    public void testFindPatientByPatientAliasWithExpectedFailure() {
        String cmoPatientId = "C-1MP6YY";
        SmilePatient patient = new SmilePatient();
        patient.addPatientAlias(new PatientAlias(cmoPatientId, "cmoId"));
        // this should create a duplicate patient node that will throw the exception
        // below when queried
        SmilePatient p = patientRepository.save(patient);
        Assertions.assertThrows(IncorrectResultSizeDataAccessException.class, () -> {
            patientService.getPatientByCmoPatientId(cmoPatientId);
        });
        // cleanup the duplicate patient node added
        patientRepository.delete(p);
    }

    /**
     * Tests if Patient Alias node is properly updated to the new cmoPatientId.
     * @throws Exception
     */
    @Test
    public void testUpdateCmoPatientId() throws Exception {
        String oldCmoPatientId = "C-1MP6YY";
        String newCmoPatientId = "NewCmoPatientId";

        int numOfSampleBeforeUpdate = sampleService.getSamplesByCmoPatientId(
                oldCmoPatientId).size();
        patientService.updateCmoPatientId(oldCmoPatientId, newCmoPatientId);
        int numOfSampleAfterUpdate = sampleService.getSamplesByCmoPatientId(
                newCmoPatientId).size();

        Assertions.assertEquals(numOfSampleBeforeUpdate, numOfSampleAfterUpdate);
        Assertions.assertNotEquals(0, numOfSampleAfterUpdate);
    }
}
