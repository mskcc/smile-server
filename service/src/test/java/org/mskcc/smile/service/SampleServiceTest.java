package org.mskcc.smile.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mskcc.smile.model.SampleMetadata;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.dmp.DmpSampleMetadata;
import org.mskcc.smile.persistence.neo4j.SmilePatientRepository;
import org.mskcc.smile.persistence.neo4j.SmileRequestRepository;
import org.mskcc.smile.persistence.neo4j.SmileSampleRepository;
import org.mskcc.smile.service.util.RequestDataFactory;
import org.mskcc.smile.service.util.SampleDataFactory;
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
public class SampleServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

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
     * Initializes the Neo4j repositories.
     * @param requestRepository
     * @param sampleRepository
     * @param patientRepository
     * @param requestService
     * @param sampleService
     */
    @Autowired
    public SampleServiceTest(SmileRequestRepository requestRepository,
            SmileSampleRepository sampleRepository, SmilePatientRepository patientRepository,
            SmileRequestService requestService, SmileSampleService sampleService,
            SmilePatientService patientService) {
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

        //persist all mocked clinical data
        for (MockJsonTestData mockJsonTestData : mockDataUtils.mockedDmpMetadataMap.values()) {
            DmpSampleMetadata dmpSample = mapper.readValue(mockJsonTestData.getJsonString(),
                DmpSampleMetadata.class);
            String cmoPatientId = mockDataUtils.getCmoPatientIdForDmpPatient(dmpSample.getDmpPatientId());
            SmileSample clinicalSample =
                    SampleDataFactory.buildNewClinicalSampleFromMetadata(cmoPatientId, dmpSample);
            sampleService.saveSmileSample(clinicalSample);
        }
    }

    /**
     * Tests if the graphDb is set up accurately
     * @throws Exception
     */
    @Test
    public void testRequestRepositoryAccess() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        SmileRequest savedRequest = requestService.getSmileRequestById(requestId);
        Assertions.assertThat(savedRequest.getSmileSampleList().size()).isEqualTo(4);
    }

    /**
     * Tests whether findMatchedNormalSample retrieves an accurate list SmileSample.
     * Note: after adding and persisting the mocked clinical data, there is now
     * an additional matched normal for the sample below.
     * @throws Exception
     */
    @Test
    public void testFindMatchedNormalSample() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_1";
        SmileSample sample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        List<SmileSample> matchedNormalList = sampleService.getMatchedNormalsBySample(sample);
        Assertions.assertThat(matchedNormalList.size()).isEqualTo(2);
    }

    /**
     * Tests whether findPooledNormalSample retrieves an accurate list pooled normals
     * @throws Exception
     */
    @Test
    public void testFindPooledNormalSample() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_3";
        SmileSample sample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        List<String> pooledNormalList = sampleService.getPooledNormalsBySample(sample);
        Assertions.assertThat(pooledNormalList.size()).isEqualTo(10);
    }

    /**
     * Tests if the number of sampleMetadata, from a list retrieved
     * using getSampleMetadataListByCmoPatientId,
     * matches the expected number
     * @throws Exception
     */
    @Test
    public void testGetSampleMetadataListByCmoPatientId() throws Exception {
        String cmoPatientId = "C-PXXXD9";
        List<SmileSample> savedSampleList =
                sampleService.getSamplesByCmoPatientId(cmoPatientId);
        Assertions.assertThat(savedSampleList.size()).isEqualTo(2);
    }

    /**
     * Tests if the number of sampleMetadata, from a list retrieved
     * using getSampleMetadataListByCmoPatientId,
     * matches the expected number
     * @throws Exception
     */
    @Test
    public void testGetAllSmileSamplesByRequestId() throws Exception {
        String requestId = "33344_Z";
        List<SmileSample> requestSamplesList = sampleService.getResearchSamplesByRequestId(requestId);
        Assertions.assertThat(requestSamplesList.size()).isEqualTo(4);
    }

    /**
     * Tests if the number of sampleMetadata history nodes,
     * from a list retrieved using getSampleMetadataHistoryByIgoId,
     * matches the expected number
     * @throws Exception
     */
    @Test
    public void testGetSampleMetadataHistoryByIgoId() throws Exception {
        String igoId = "MOCKREQUEST1_B_1";
        List<SampleMetadata> sampleMetadataHistory = sampleService
                .getResearchSampleMetadataHistoryByIgoId(igoId);
        Assertions.assertThat(sampleMetadataHistory.size()).isEqualTo(1);
    }

    /**
     * Tests if sampleHasMetadataUpdates accurately recognizes changes in sampleMetadata
     * @throws Exception
     */
    @Test
    public void testSampleHasMetadataUpdates() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_1";
        SmileSample sample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);

        MockJsonTestData updatedRequestData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1UpdatedJsonDataWith2T2N");
        SmileRequest updatedRequest = RequestDataFactory.buildNewLimsRequestFromJson(
                updatedRequestData.getJsonString());
        SmileSample updatedSample = updatedRequest.getSmileSampleList().get(0);

        Boolean hasUpdates = sampleService.sampleHasMetadataUpdates(sample.getLatestSampleMetadata(),
                updatedSample.getLatestSampleMetadata());
        Assertions.assertThat(hasUpdates).isEqualTo(Boolean.TRUE);

    }

    /**
     * Tests if the number of sampleMetadata history nodes
     * matches the expected number after updating sampleMetadata
     * @throws Exception
     */
    @Test
    public void testSampleHistoryAfterUpdate() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_2";

        MockJsonTestData updatedRequestData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1UpdatedJsonDataWith2T2N");
        SmileRequest updatedRequest = RequestDataFactory.buildNewLimsRequestFromJson(
                updatedRequestData.getJsonString());
        SmileSample updatedSample = updatedRequest.getSmileSampleList().get(1);
        sampleService.saveSmileSample(updatedSample);

        List<SampleMetadata> sampleMetadataHistory = sampleService
                .getResearchSampleMetadataHistoryByIgoId(igoId);
        Assertions.assertThat(sampleMetadataHistory.size()).isEqualTo(2);
    }

    /**
     * Tests if the returned list of sampleMetadata history is sorted based on importDate
     * @throws Exception
     */
    @Test
    public void testSampleHistoryListIsAscendingByImportDate() throws Exception {
        String igoId = "MOCKREQUEST1_B_4";
        List<SampleMetadata> sampleMetadataHistory = sampleService
                .getResearchSampleMetadataHistoryByIgoId(igoId);
        Assertions.assertThat(sampleMetadataHistory).isSorted();
    }

    /**
     * Test if the persisted clinical sample is accurately mapped.
     * DMP patient 'P-0000001' is expected to only have 2 clinical samples
     * and no research samples.
     * @throws Exception
     */
    @Test
    public void testPersistClinicalSample() throws Exception {
        String dmpPatientId = "P-0000001";
        String cmoPatientId = mockDataUtils.getCmoPatientIdForDmpPatient(dmpPatientId);
        List<SmileSample> sampleList = sampleService
                .getSamplesByCmoPatientId(cmoPatientId);
        Assertions.assertThat(sampleList.size()).isEqualTo(2);
    }

    /**
     * Tests that the number of samples (research and clinical) persisted for
     * each patient matches the expected  number of samples.
     * @throws Exception
     */
    @Test
    public void testPatientSamplesCountMatchExpectedValues() throws Exception {
        for (Map.Entry<String, Integer> entry : mockDataUtils.EXPECTED_PATIENT_SAMPLES_COUNT.entrySet()) {
            String cmoPatientId = entry.getKey();
            Integer expectedSampleCount = entry.getValue();
            List<SmileSample> sampleList = sampleService.getSamplesByCmoPatientId(cmoPatientId);
            if (expectedSampleCount != sampleList.size()) {
                StringBuilder builder = new StringBuilder();
                builder.append("CMO patient id: ").append(cmoPatientId)
                        .append("\n\tExpected count ").append(expectedSampleCount)
                        .append(", Actual count: ").append(sampleList.size());
                Assertions.fail(builder.toString());
            }
        }
    }

    /**
     * Tests that samples can be found by an import date and cmo label matches expected output.
     * @throws Exception
     */
    @Test
    public void testFindSamplesByDate() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_2";

        SmileSample sample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        sample.getLatestSampleMetadata().setCmoSampleName("C-LATESTLABEL-T02-d002");

        SampleMetadata updatedMetadata = new SampleMetadata();
        updatedMetadata.setImportDate("2000-06-10");
        updatedMetadata.setPrimaryId(igoId);
        updatedMetadata.setCmoSampleName("C-OLDSAMPLELABEL-T11");
        sample.updateSampleMetadata(updatedMetadata);
        sampleService.saveSmileSample(sample);

        // confirm that new sample metadata was persisted
        List<SampleMetadata> sampleMetadataHistory = sampleService
                .getResearchSampleMetadataHistoryByIgoId(igoId);
        Assertions.assertThat(sampleMetadataHistory.size()).isEqualTo(3);

        // confirms that both methods return the same latest metadata and
        // same cmo sample label corresponding to it
        SmileSample updatedSample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        Assertions.assertThat(updatedSample.getLatestSampleMetadata().getCmoSampleName())
                .isEqualTo("C-LATESTLABEL-T02-d002");
        SampleMetadata latestMetadata =
                sampleRepository.findLatestSampleMetadataBySmileId(updatedSample.getSmileSampleId());
        Assertions.assertThat(latestMetadata.getCmoSampleName()).isEqualTo("C-LATESTLABEL-T02-d002");
    }
}
