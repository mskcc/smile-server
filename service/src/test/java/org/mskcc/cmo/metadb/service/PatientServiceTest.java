package org.mskcc.cmo.metadb.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mskcc.cmo.metadb.model.MetadbPatient;
import org.mskcc.cmo.metadb.model.MetadbRequest;
import org.mskcc.cmo.metadb.model.PatientAlias;
import org.mskcc.cmo.metadb.persistence.neo4j.MetadbPatientRepository;
import org.mskcc.cmo.metadb.persistence.neo4j.MetadbRequestRepository;
import org.mskcc.cmo.metadb.persistence.neo4j.MetadbSampleRepository;
import org.mskcc.cmo.metadb.service.util.RequestDataFactory;
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
     */
    @Autowired
    public PatientServiceTest(MetadbRequestRepository requestRepository,
            MetadbSampleRepository sampleRepository, MetadbPatientRepository patientRepository) {
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

    @Test
    public void testFindPatientByPatientAlias() throws Exception {
        String cmoPatientId = "C-1MP6YY";
        Assertions.assertThat(
                patientService.getPatientByCmoPatientId(cmoPatientId)).isNotNull();
    }

    @Test
    public void testFindPatientByPatientAliasWithExpectedFailure() {
        String cmoPatientId = "C-1MP6YY";
        MetadbPatient patient = new MetadbPatient();
        patient.addPatientAlias(new PatientAlias(cmoPatientId, "cmoId"));
        // this should create a duplicate patient node that will throw the exception
        // below when queried
        patientRepository.save(patient);

        Assertions.assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> {
                patientService.getPatientByCmoPatientId(cmoPatientId);
            });
    }
}
