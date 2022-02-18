package org.mskcc.cmo.metadb.service;

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mskcc.cmo.metadb.model.MetadbPatient;
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
public class CorrectCmoPatientIdHandlerTest {
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
     * Persists the Mock Request data to the test database.
     * @throws Exception
     */
    @Autowired
    public CorrectCmoPatientIdHandlerTest(MetadbRequestRepository requestRepository,
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
        // mock request id: 145145_IM
        MockJsonTestData request5Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest5JsonPtMultiSamples");
        MetadbRequest request5 = RequestDataFactory.buildNewLimsRequestFromJson(request5Data.getJsonString());
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


        List<MetadbSample> samplesByNewCmoPatient = sampleService.getSamplesByCmoPatientId(newCmoPatientId);
        System.out.println("Samples for new cmo patient id: " + samplesByNewCmoPatient.size());

        String request1 = "MOCKREQUEST1_B";
        String sampleId1 = "MOCKREQUEST1_B_1";
        MetadbSample sample1 = sampleService.getResearchSampleByRequestAndIgoId(request1, sampleId1);

        MetadbPatient newPatient = patientService.getPatientByCmoPatientId(newCmoPatientId);
        SampleMetadata newSample1Metadata = sample1.getLatestSampleMetadata();
        newSample1Metadata.setCmoPatientId(newCmoPatientId);
        sample1.updateSampleMetadata(newSample1Metadata);
        sample1.setPatient(newPatient);
        sampleService.saveMetadbSample(sample1);
        sampleService.updateSamplePatientRelationship(sample1.getMetaDbSampleId(),
                newPatient.getMetaDbPatientId());

        Integer expectedSampleCount = samplesByNewCmoPatient.size() + 1;
        List<MetadbSample> samplesByNewCmoPatientAfterSwap =
                sampleService.getSamplesByCmoPatientId(newCmoPatientId);
        Assertions.assertThat(samplesByNewCmoPatientAfterSwap.size())
                .isEqualTo(expectedSampleCount);
    }
}
