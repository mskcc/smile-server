package org.mskcc.smile.service;

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mskcc.smile.model.SampleMetadata;
import org.mskcc.smile.model.SmilePatient;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
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

/**
 *
 * @author ochoaa
 */
@Testcontainers
@DataNeo4jTest
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
     * Persists the Mock Request data to the test database.
     * @throws Exception
     */
    @Autowired
    public CorrectCmoPatientIdHandlerTest(SmileRequestRepository requestRepository,
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
        // mock request id: 145145_IM
        MockJsonTestData request5Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest5JsonPtMultiSamples");
        SmileRequest request5 = RequestDataFactory.buildNewLimsRequestFromJson(request5Data.getJsonString());
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


        List<SmileSample> samplesByNewCmoPatient = sampleService.getSamplesByCmoPatientId(newCmoPatientId);
        System.out.println("Samples for new cmo patient id: " + samplesByNewCmoPatient.size());

        String request1 = "MOCKREQUEST1_B";
        String sampleId1 = "MOCKREQUEST1_B_1";
        SmileSample sample1 = sampleService.getResearchSampleByRequestAndIgoId(request1, sampleId1);

        SmilePatient newPatient = patientService.getPatientByCmoPatientId(newCmoPatientId);
        SampleMetadata newSample1Metadata = sample1.getLatestSampleMetadata();
        newSample1Metadata.setCmoPatientId(newCmoPatientId);
        sample1.updateSampleMetadata(newSample1Metadata);
        sample1.setPatient(newPatient);
        sampleService.saveSmileSample(sample1);
        sampleService.updateSamplePatientRelationship(sample1.getSmileSampleId(),
                newPatient.getSmilePatientId());

        Integer expectedSampleCount = samplesByNewCmoPatient.size() + 1;
        List<SmileSample> samplesByNewCmoPatientAfterSwap =
                sampleService.getSamplesByCmoPatientId(newCmoPatientId);
        Assertions.assertThat(samplesByNewCmoPatientAfterSwap.size())
                .isEqualTo(expectedSampleCount);
    }
}
