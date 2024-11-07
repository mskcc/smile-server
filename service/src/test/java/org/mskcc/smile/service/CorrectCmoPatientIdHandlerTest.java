package org.mskcc.smile.service;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mskcc.smile.model.SampleMetadata;
import org.mskcc.smile.model.SmilePatient;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
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
        properties = {"spring.neo4j.authentication.username:neo4j"}
)
@Testcontainers
@Import(MockDataUtils.class)
public class CorrectCmoPatientIdHandlerTest {
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
    public CorrectCmoPatientIdHandlerTest(SmileRequestRepository requestRepository,
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
     * Tests sample fetch before patient swap and after the patient id swap in the
     * event that the patient already exists by the new id.
     */
    @Test
    public void testPatientIdSwapWithExistingPatient() throws Exception {
        String oldCmoPatientId = "C-MP789JR";
        String newCmoPatientId = "C-1MP6YY";
        final Integer samplesByNewCmoPatient = sampleService.getSamplesByCmoPatientId(newCmoPatientId).size();

        String request1 = "MOCKREQUEST1_B";
        String sampleId1 = "MOCKREQUEST1_B_1";
        SmileSample sample1 = sampleService.getResearchSampleByRequestAndIgoId(request1, sampleId1);
        Assertions.assertEquals(oldCmoPatientId, sample1.getLatestSampleMetadata().getCmoPatientId());

        SmilePatient newPatient = patientService.getPatientByCmoPatientId(newCmoPatientId);
        SampleMetadata newSample1Metadata = sample1.getLatestSampleMetadata();
        newSample1Metadata.setCmoPatientId(newCmoPatientId);
        sample1.updateSampleMetadata(newSample1Metadata);
        sample1.setPatient(newPatient);
        sampleService.saveSmileSample(sample1);
        sampleService.updateSamplePatientRelationship(sample1.getSmileSampleId(),
                newPatient.getSmilePatientId());

        Integer expectedSampleCount = samplesByNewCmoPatient + 1;
        List<SmileSample> samplesByNewCmoPatientAfterSwap =
                sampleService.getSamplesByCmoPatientId(newCmoPatientId);
        Assertions.assertEquals(expectedSampleCount, samplesByNewCmoPatientAfterSwap.size());
    }
}
