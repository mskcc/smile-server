package org.mskcc.smile.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.tempo.BamComplete;
import org.mskcc.smile.model.tempo.Cohort;
import org.mskcc.smile.model.tempo.MafComplete;
import org.mskcc.smile.model.tempo.QcComplete;
import org.mskcc.smile.model.tempo.Tempo;
import org.mskcc.smile.model.tempo.json.CohortCompleteJson;
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
public class TempoServiceTest {
    private ObjectMapper mapper = new ObjectMapper();

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

    @Autowired
    private CohortCompleteService cohortCompleteService;

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
    public TempoServiceTest(SmileRequestRepository requestRepository,
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

        // mock request id: 22022_BZ
        MockJsonTestData request2Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest2bJsonDataMissing1N");
        SmileRequest request2 =
                RequestDataFactory.buildNewLimsRequestFromJson(request2Data.getJsonString());
        requestService.saveRequest(request2);

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

    @Test
    public void testBamCompleteEventSave() throws Exception {
        Tempo tempo1 = new Tempo();
        tempo1.addBamCompleteEvent(getBamCompleteEventData("mockBamCompleteSampleB1"));

        // get sample from db
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_1";
        SmileSample sample1 = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        tempo1.setSmileSample(sample1); // this should link the tempo node to the correct sample node
        tempoService.saveTempoData(tempo1);

        // confirm can be fetched in this direction (tempo node to sample)
        Tempo tempoAfterSave = tempoService.getTempoDataBySampleId(sample1);
        Assertions.assertNotNull(tempoAfterSave);

        // confirm can get to tempo data from the sample node as well
        SmileSample sample1Updated = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        Tempo sampleTempoUpdated = sample1Updated.getTempo();
        Assertions.assertNotNull(sampleTempoUpdated);
    }

    @Test
    public void testTempoMultipleBamCompleteEvents() throws Exception {
        Tempo tempo3 = new Tempo();
        // this bam complete has a FAIL status
        tempo3.addBamCompleteEvent(getBamCompleteEventData("mockBamCompleteSampleB3"));

        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_3";
        SmileSample sample3 = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        tempo3.setSmileSample(sample3); // this should link the tempo node to the correct sample node
        tempoService.saveTempoData(tempo3);

        // confirm can be fetched in this direction (tempo node to sample)
        Tempo tempoAfterSave = tempoService.getTempoDataBySampleId(sample3);
        Assertions.assertNotNull(tempoAfterSave);
        Assertions.assertEquals(1, tempoAfterSave.getBamCompleteEvents().size());

        // mock a new bam complete event for sample, this time with status PASS
        tempoAfterSave.addBamCompleteEvent(getBamCompleteEventData("mockBamCompleteSampleB3pass"));
        tempoService.saveTempoData(tempoAfterSave); // persist new bam complete event

        // fetch updated tempo data for sample - there should be two
        // bam complete events after the second update
        Tempo tempoAfterSaveAgain = tempoService.getTempoDataBySampleId(sample3);
        Assertions.assertEquals(2, tempoAfterSaveAgain.getBamCompleteEvents().size());
    }

    @Test
    public void testMafCompleteEventSave() throws Exception {
        String igoId = "MOCKREQUEST1_B_1";
        MafComplete mafCompleteB1 = getMafCompleteEventData("mockMafCompleteSampleB1");
        tempoService.mergeMafCompleteEventBySamplePrimaryId(igoId, mafCompleteB1);
        // confirming that the query does return the correct amount of specific events
        // and distinguishes them from other event types
        Tempo tempoAfterSave = tempoService.getTempoDataBySamplePrimaryId(igoId);
        Assertions.assertEquals(1, tempoAfterSave.getMafCompleteEvents().size());
        Assertions.assertTrue(!tempoAfterSave.getCustodianInformation().isBlank());
        Assertions.assertTrue(!tempoAfterSave.getAccessLevel().isBlank());
    }

    @Test
    public void testQcCompleteEventSave() throws Exception {
        String igoId = "MOCKREQUEST1_B_1";
        QcComplete qcComplete1 = getQcCompleteEventData("mockQcCompleteSampleB1");
        tempoService.mergeQcCompleteEventBySamplePrimaryId(igoId, qcComplete1);
        // confirming that the query does return the correct amount of specific events
        // and distinguishes them from other event types
        Tempo tempoAfterSave = tempoService.getTempoDataBySamplePrimaryId(igoId);
        Assertions.assertEquals(1, tempoAfterSave.getQcCompleteEvents().size());
        Assertions.assertTrue(!tempoAfterSave.getCustodianInformation().isBlank());
        Assertions.assertTrue(!tempoAfterSave.getAccessLevel().isBlank());
    }

    @Test
    public void testCohortCompleteEventSave() throws Exception {
        CohortCompleteJson ccJson = getCohortEventData("mockCohortCompleteCCSPPPQQQQ");
        cohortCompleteService.saveCohort(new Cohort(ccJson), ccJson.getTumorNormalPairsAsSet());
        // cohort should have 4 samples linked to it
        Cohort cohort = cohortCompleteService.getCohortByCohortId("CCS_PPPQQQQ");
        Assertions.assertEquals(4, cohort.getCohortSamples().size());

        // confirm we can get number of cohorts for a sample by primary id
        List<Cohort> cohortsBySample =
                cohortCompleteService.getCohortsBySamplePrimaryId("MOCKREQUEST1_B_1");
        Assertions.assertEquals(1, cohortsBySample.size());

        // save a new cohort with the same sample as above
        CohortCompleteJson ccJson2 = getCohortEventData("mockCohortCompleteCCSPPPQQQQ2");
        cohortCompleteService.saveCohort(new Cohort(ccJson2), ccJson2.getTumorNormalPairsAsSet());

        // sample should now have 2 cohorts linked to it
        List<Cohort> cohortsBySampleUpdated =
                cohortCompleteService.getCohortsBySamplePrimaryId("MOCKREQUEST1_B_1");
        Assertions.assertEquals(2, cohortsBySampleUpdated.size());
    }

    @Test
    public void testUpdateCohortCompleteData() throws Exception {
        CohortCompleteJson ccJson = getCohortEventData("mockCohortCompleteCCSPPPQQQQ");
        cohortCompleteService.saveCohort(new Cohort(ccJson), ccJson.getTumorNormalPairsAsSet());
        Cohort cohort = cohortCompleteService.getCohortByCohortId("CCS_PPPQQQQ");

        CohortCompleteJson ccJsonUpdate = getCohortEventData("mockCohortCompleteCCSPPPQQQQUpdated");

        Cohort updatedCohort = new Cohort(ccJsonUpdate);
        Boolean hasUpdates = cohortCompleteService.hasUpdates(cohort, updatedCohort);
        Assertions.assertTrue(hasUpdates);
    }

    @Test
    public void testPopulatingTempoDataNoInitialRunDate() throws Exception {
        // using a tumor sample to trigger population of tempo data
        String igoId = "MOCKREQUEST1_B_3";
        String requestId = "MOCKREQUEST1_B";
        SmileSample sample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);

        Tempo tempo = new Tempo();
        tempo.setSmileSample(sample);
        Tempo savedTempo = tempoService.saveTempoData(tempo);

        Assertions.assertEquals("MSK Embargo", savedTempo.getAccessLevel());
        Assertions.assertEquals("", savedTempo.getInitialPipelineRunDate());
        Assertions.assertEquals("", savedTempo.getEmbargoDate());
    }

    @Test
    public void testNormalSampleTumorSampleTempoImportDefaults() throws Exception {
        String requestId = "MOCKREQUEST1_B";

        // sample MOCKREQUEST1_B_2 is "Normal" which should not have default values set to
        // tempo.custodianInformation or tempo.accessLevel
        String igoId2 = "MOCKREQUEST1_B_2";
        Tempo tempo2 = new Tempo();
        tempo2.addBamCompleteEvent(getBamCompleteEventData("mockBamCompleteSampleB2"));
        SmileSample sample2 = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId2);
        tempo2.setSmileSample(sample2); // this should link the tempo node to the correct sample node
        tempoService.saveTempoData(tempo2);

        // assert data persisted correctly
        Tempo tempoAfterSave2 = tempoService.getTempoDataBySamplePrimaryId(igoId2);
        Assertions.assertTrue(StringUtils.isBlank(tempoAfterSave2.getCustodianInformation()));
        Assertions.assertTrue(StringUtils.isBlank(tempoAfterSave2.getAccessLevel()));

        // sample MOCKREQUEST1_B_3 is "Tumor" which should have default values set to
        // tempo.custodianInformation and tempo.accessLevel
        String igoId3 = "MOCKREQUEST1_B_3";
        Tempo tempo3 = new Tempo();
        tempo3.addBamCompleteEvent(getBamCompleteEventData("mockBamCompleteSampleB3"));
        SmileSample sample3 = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId3);
        tempo3.setSmileSample(sample3); // this should link the tempo node to the correct sample node
        tempoService.saveTempoData(tempo3);

        // assert data persisted correctly
        Tempo tempoAfterSave3 = tempoService.getTempoDataBySamplePrimaryId(igoId3);
        Assertions.assertTrue(!tempoAfterSave3.getCustodianInformation().isBlank());
        Assertions.assertTrue(!tempoAfterSave3.getAccessLevel().isBlank());
    }

    @Test
    public void testCohortSampleListUpdate() throws Exception {
        CohortCompleteJson ccJson = getCohortEventData("mockCohortCompleteCCSPPPQQQQ");
        cohortCompleteService.saveCohort(new Cohort(ccJson), ccJson.getTumorNormalPairsAsSet());
        Cohort cohort = cohortCompleteService.getCohortByCohortId("CCS_PPPQQQQ");
        Assertions.assertEquals(4, cohort.getCohortSamplePrimaryIds().size());

        CohortCompleteJson ccJsonUpdate =
                getCohortEventData("mockCohortCompleteCCSPPPQQQQUpdatedSamplesOnly");
        Assertions.assertEquals(6, ccJsonUpdate.getTumorNormalPairsAsSet().size());

        Cohort updatedCohort = new Cohort(ccJsonUpdate);
        Boolean hasUpdates = cohortCompleteService.hasUpdates(cohort, updatedCohort);
        Assertions.assertTrue(hasUpdates);

        // verify there are 2 new samples getting added to the cohort
        Set<String> newSamples = ccJsonUpdate.getTumorNormalPairsAsSet();
        newSamples.removeAll(cohort.getCohortSamplePrimaryIds());
        Assertions.assertEquals(2, newSamples.size());

        // save cohort and verify that it now has 6 samples instead of 4
        cohortCompleteService.saveCohort(cohort, newSamples);
        Cohort cohortAfterSave = cohortCompleteService.getCohortByCohortId("CCS_PPPQQQQ");
        Assertions.assertEquals(6, cohortAfterSave.getCohortSamplePrimaryIds().size());
    }

    private CohortCompleteJson getCohortEventData(String dataIdentifier) throws JsonProcessingException {
        MockJsonTestData mockData = mockDataUtils.mockedTempoDataMap.get(dataIdentifier);
        CohortCompleteJson cohortCompleteData = mapper.readValue(mockData.getJsonString(),
                CohortCompleteJson.class);
        return cohortCompleteData;
    }

    private QcComplete getQcCompleteEventData(String dataIdentifier) throws JsonProcessingException {
        MockJsonTestData mockData = mockDataUtils.mockedTempoDataMap.get(dataIdentifier);
        Map<String, String> qcCompleteMap = mapper.readValue(mockData.getJsonString(), Map.class);
        QcComplete qcComplete = new QcComplete(qcCompleteMap.get("date"),
                qcCompleteMap.get("result"), qcCompleteMap.get("reason"),
                qcCompleteMap.get("status"));
        return qcComplete;
    }

    private MafComplete getMafCompleteEventData(String dataIdentifier) throws JsonProcessingException {
        MockJsonTestData mockData = mockDataUtils.mockedTempoDataMap.get(dataIdentifier);
        Map<String, String> mafCompleteMap = mapper.readValue(mockData.getJsonString(), Map.class);
        MafComplete mafComplete = new MafComplete(mafCompleteMap.get("date"),
                mafCompleteMap.get("normalPrimaryId"),
                mafCompleteMap.get("status"));
        return mafComplete;
    }

    private BamComplete getBamCompleteEventData(String dataIdentifier) throws JsonProcessingException {
        MockJsonTestData mockData = mockDataUtils.mockedTempoDataMap.get(dataIdentifier);
        Map<String, String> bamCompleteMap = mapper.readValue(mockData.getJsonString(), Map.class);
        return new BamComplete(bamCompleteMap.get("date"), bamCompleteMap.get("status"));
    }
}
