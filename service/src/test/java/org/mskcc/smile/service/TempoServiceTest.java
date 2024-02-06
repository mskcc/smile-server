package org.mskcc.smile.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.tempo.BamComplete;
import org.mskcc.smile.model.tempo.Tempo;
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
    private TempoService tempoService;

    private final ObjectMapper mapper = new ObjectMapper();

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
        SmileRequest request1 = RequestDataFactory.buildNewLimsRequestFromJson(request1Data.getJsonString());
        requestService.saveRequest(request1);
    }

    @Test
    public void testTempoDataSave() throws Exception {
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

    private BamComplete getBamCompleteEventData(String dataIdentifier) throws JsonProcessingException {
        MockJsonTestData mockData = mockDataUtils.mockedTempoDataMap.get(dataIdentifier);
        Map<String, String> bamCompleteMap = mapper.readValue(mockData.getJsonString(), Map.class);
        return new BamComplete(bamCompleteMap.get("timestamp"), bamCompleteMap.get("status"));
    }
}
