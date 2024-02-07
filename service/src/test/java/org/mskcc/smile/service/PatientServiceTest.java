package org.mskcc.smile.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mskcc.smile.model.PatientAlias;
import org.mskcc.smile.model.SmilePatient;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.persistence.neo4j.SmilePatientRepository;
import org.mskcc.smile.persistence.neo4j.SmileRequestRepository;
import org.mskcc.smile.persistence.neo4j.SmileSampleRepository;
import org.mskcc.smile.service.util.RequestDataFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
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
public class PatientServiceTest {
    @Autowired
    private MockDataUtils mockDataUtils;

    @Autowired
    private SmileRequestService requestService;

    @Autowired
    private SmileSampleService sampleService;

    @Autowired
    private SmilePatientService patientService;

    @Container
    private static final Neo4jContainer<?> databaseServer = new Neo4jContainer<>()
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
     */
    @Autowired
    public PatientServiceTest(SmileRequestRepository requestRepository,
            SmileSampleRepository sampleRepository, SmilePatientRepository patientRepository) {
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
     * Tests if patient service retrieves SmilePatient by cmoPatientId.
     * @throws Exception
     */
    @Test
    public void testFindPatientByPatientAlias() throws Exception {
        String cmoPatientId = "C-1MP6YY";
        Assertions.assertThat(
                patientService.getPatientByCmoPatientId(cmoPatientId)).isNotNull();
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
        patientRepository.save(patient);

        Assertions.assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> {
                patientService.getPatientByCmoPatientId(cmoPatientId);
            });
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

        Assertions.assertThat(numOfSampleBeforeUpdate)
            .isEqualTo(numOfSampleAfterUpdate)
            .isNotEqualTo(0);
    }
}
