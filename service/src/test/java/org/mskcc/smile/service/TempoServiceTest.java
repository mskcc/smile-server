package org.mskcc.smile.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.tempo.BamComplete;
import org.mskcc.smile.model.tempo.Cohort;
import org.mskcc.smile.model.tempo.CohortComplete;
import org.mskcc.smile.model.tempo.MafComplete;
import org.mskcc.smile.model.tempo.QcComplete;
import org.mskcc.smile.model.tempo.Tempo;
import org.mskcc.smile.model.tempo.json.CohortCompleteJson;
import org.mskcc.smile.persistence.neo4j.SmilePatientRepository;
import org.mskcc.smile.persistence.neo4j.SmileRequestRepository;
import org.mskcc.smile.persistence.neo4j.SmileSampleRepository;
import org.mskcc.smile.persistence.neo4j.TempoRepository;
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
public class TempoServiceTest {
    @Autowired
    private MockDataUtils mockDataUtils;

    @Autowired
    private SmileRequestService requestService;

    @Autowired
    private SmileSampleService sampleService;

    @Autowired
    private SmilePatientService patientService;

    @Autowired
    private CohortCompleteService cohortCompleteService;

    @Autowired
    private TempoService tempoService;

    private final ObjectMapper mapper = new ObjectMapper();

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
    private final TempoRepository tempoRepository;

    /**
     * Initializes the Neo4j repositories.
     * @param requestRepository
     * @param sampleRepository
     * @param patientRepository
     * @param tempoRepository
     */
    @Autowired
    public TempoServiceTest(SmileRequestRepository requestRepository,
            SmileSampleRepository sampleRepository, SmilePatientRepository patientRepository,
            TempoRepository tempoRepository) {
        this.requestRepository = requestRepository;
        this.sampleRepository = sampleRepository;
        this.patientRepository = patientRepository;
        this.tempoRepository = tempoRepository;
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
        SmileRequest request1 =
                RequestDataFactory.buildNewLimsRequestFromJson(request1Data.getJsonString());
        requestService.saveRequest(request1);

        // mock request id: 22022_BZ
        MockJsonTestData request2bData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest2bJsonDataMissing1N");
        SmileRequest request2b =
                RequestDataFactory.buildNewLimsRequestFromJson(request2bData.getJsonString());
        requestService.saveRequest(request2b);
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
        Assertions.assertThat(tempoAfterSave).isNotNull();

        // confirm can get to tempo data from the sample node as well
        SmileSample sample1Updated = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        Tempo sampleTempoUpdated = sample1Updated.getTempo();
        Assertions.assertThat(sampleTempoUpdated).isNotNull();
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
        Assertions.assertThat(tempoAfterSave).isNotNull();
        Assertions.assertThat(tempoAfterSave.getBamCompleteEvents().size()).isEqualTo(1);

        // mock a new bam complete event for sample, this time with status PASS
        tempoAfterSave.addBamCompleteEvent(getBamCompleteEventData("mockBamCompleteSampleB3pass"));
        tempoService.saveTempoData(tempo3); // persist new bam complete event

        // fetch updated tempo data for sample - there should be two
        // bam complete events after the second update
        Tempo tempoAfterSaveAgain = tempoService.getTempoDataBySampleId(sample3);
        Assertions.assertThat(tempoAfterSaveAgain.getBamCompleteEvents().size()).isEqualTo(2);
    }

    @Test
    public void testMafCompleteEventSave() throws Exception {
        String igoId = "MOCKREQUEST1_B_1";
        MafComplete mafCompleteB1 = getMafCompleteEventData("mockMafCompleteSampleB1");
        tempoService.mergeMafCompleteEventBySamplePrimaryId(igoId, mafCompleteB1);
        // confirming that the query does return the correct amount of specific events
        // and distinguishes them from other event types
        Tempo tempoAfterSave = tempoService.getTempoDataBySamplePrimaryId(igoId);
        Assertions.assertThat(tempoAfterSave.getMafCompleteEvents().size()).isEqualTo(1);
        Assertions.assertThat(tempoAfterSave.getCustodianInformation()).isNotBlank();
        Assertions.assertThat(tempoAfterSave.getAccessLevel()).isNotBlank();
    }

    @Test
    public void testQcCompleteEventSave() throws Exception {
        String igoId = "MOCKREQUEST1_B_1";
        QcComplete qcComplete1 = getQcCompleteEventData("mockQcCompleteSampleB1");
        tempoService.mergeQcCompleteEventBySamplePrimaryId(igoId, qcComplete1);
        // confirming that the query does return the correct amount of specific events
        // and distinguishes them from other event types
        Tempo tempoAfterSave = tempoService.getTempoDataBySamplePrimaryId(igoId);
        Assertions.assertThat(tempoAfterSave.getQcCompleteEvents().size()).isEqualTo(1);
        Assertions.assertThat(tempoAfterSave.getCustodianInformation()).isNotBlank();
        Assertions.assertThat(tempoAfterSave.getAccessLevel()).isNotBlank();
    }

    @Test
    public void testCohortCompleteEventSave() throws Exception {
        CohortCompleteJson ccJson = getCohortEventData("mockCohortCompleteCCSPPPQQQQ");
        cohortCompleteService.saveCohort(new Cohort(ccJson), ccJson.getTumorNormalPairsAsSet());
        // cohort should have 4 samples linked to it
        Cohort cohort = cohortCompleteService.getCohortByCohortId("CCS_PPPQQQQ");
        Assertions.assertThat(cohort.getCohortSamples().size()).isEqualTo(4);

        // confirm we can get number of cohorts for a sample by primary id
        List<Cohort> cohortsBySample = cohortCompleteService.getCohortsBySamplePrimaryId("MOCKREQUEST1_B_1");
        Assertions.assertThat(cohortsBySample.size()).isEqualTo(1);

        // save a new cohort with the same sample as above
        CohortCompleteJson ccJson2 = getCohortEventData("mockCohortCompleteCCSPPPQQQQ2");
        cohortCompleteService.saveCohort(new Cohort(ccJson2), ccJson2.getTumorNormalPairsAsSet());

        // sample should now have 2 cohorts linked to it
        List<Cohort> cohortsBySampleUpdated =
                cohortCompleteService.getCohortsBySamplePrimaryId("MOCKREQUEST1_B_1");
        Assertions.assertThat(cohortsBySampleUpdated.size()).isEqualTo(2);
    }

    @Test
    public void testUpdateCohortCompleteData() throws Exception {
        CohortCompleteJson ccJson = getCohortEventData("mockCohortCompleteCCSPPPQQQQ");
        cohortCompleteService.saveCohort(new Cohort(ccJson), ccJson.getTumorNormalPairsAsSet());
        Cohort cohort = cohortCompleteService.getCohortByCohortId("CCS_PPPQQQQ");

        CohortCompleteJson ccJsonUpdate = getCohortEventData("mockCohortCompleteCCSPPPQQQQUpdated");

        Cohort updatedCohort = new Cohort(ccJsonUpdate);
        Boolean hasUpdates = cohortCompleteService.hasUpdates(cohort, updatedCohort);
        Assertions.assertThat(hasUpdates).isTrue();
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
        Assertions.assertThat(tempoAfterSave2.getCustodianInformation()).isNullOrEmpty();
        Assertions.assertThat(tempoAfterSave2.getAccessLevel()).isNullOrEmpty();

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
        Assertions.assertThat(tempoAfterSave3.getCustodianInformation()).isNotBlank();
        Assertions.assertThat(tempoAfterSave3.getAccessLevel()).isNotBlank();
    }

    @Test
    public void testCohortSampleListUpdate() throws Exception {
        CohortCompleteJson ccJson = getCohortEventData("mockCohortCompleteCCSPPPQQQQ");
        cohortCompleteService.saveCohort(new Cohort(ccJson), ccJson.getTumorNormalPairsAsSet());
        Cohort cohort = cohortCompleteService.getCohortByCohortId("CCS_PPPQQQQ");
        Assertions.assertThat(cohort.getCohortSamplePrimaryIds().size()).isEqualTo(4);

        CohortCompleteJson ccJsonUpdate =
                getCohortEventData("mockCohortCompleteCCSPPPQQQQUpdatedSamplesOnly");
        Assertions.assertThat(ccJsonUpdate.getTumorNormalPairsAsSet().size()).isEqualTo(6);

        Cohort updatedCohort = new Cohort(ccJsonUpdate);
        Boolean hasUpdates = cohortCompleteService.hasUpdates(cohort, updatedCohort);
        Assertions.assertThat(hasUpdates).isTrue();

        // verify there are 2 new samples getting added to the cohort
        Set<String> newSamples = ccJsonUpdate.getTumorNormalPairsAsSet();
        newSamples.removeAll(cohort.getCohortSamplePrimaryIds());
        Assertions.assertThat(newSamples.size()).isEqualTo(2);

        // save cohort and verify that it now has 6 samples instead of 4
        cohortCompleteService.saveCohort(cohort, newSamples);
        Cohort cohortAfterSave = cohortCompleteService.getCohortByCohortId("CCS_PPPQQQQ");
        Assertions.assertThat(cohortAfterSave.getCohortSamplePrimaryIds().size()).isEqualTo(6);
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
