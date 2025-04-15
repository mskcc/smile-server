package org.mskcc.smile.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mskcc.smile.model.SampleAlias;
import org.mskcc.smile.model.SampleMetadata;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.dmp.DmpSampleMetadata;
import org.mskcc.smile.model.web.PublishedSmileRequest;
import org.mskcc.smile.model.web.PublishedSmileSample;
import org.mskcc.smile.persistence.neo4j.CohortCompleteRepository;
import org.mskcc.smile.persistence.neo4j.SmilePatientRepository;
import org.mskcc.smile.persistence.neo4j.SmileRequestRepository;
import org.mskcc.smile.persistence.neo4j.SmileSampleRepository;
import org.mskcc.smile.persistence.neo4j.TempoRepository;
import org.mskcc.smile.service.util.RequestDataFactory;
import org.mskcc.smile.service.util.SampleDataFactory;
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
@TestMethodOrder(OrderAnnotation.class)
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

    @Autowired
    private TempoService tempoService;

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
    public SampleServiceTest(SmileRequestRepository requestRepository,
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

        // mock request id: MOCKREQUEST8_D
        MockJsonTestData request8Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest8DupCmoLabels");
        SmileRequest request8 = RequestDataFactory.buildNewLimsRequestFromJson(request8Data.getJsonString());
        requestService.saveRequest(request8);

        // mock request id: MOCKREQUEST9_D
        MockJsonTestData request9Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest9DupAltIds");
        SmileRequest request9 = RequestDataFactory.buildNewLimsRequestFromJson(request9Data.getJsonString());
        requestService.saveRequest(request9);

        //persist all mocked clinical data
        for (MockJsonTestData mockJsonTestData : mockDataUtils.mockedDmpMetadataMap.values()) {
            DmpSampleMetadata dmpSample = mapper.readValue(mockJsonTestData.getJsonString(),
                    DmpSampleMetadata.class);
            String cmoPatientId =
                    mockDataUtils.getCmoPatientIdForDmpPatient(dmpSample.getDmpPatientId());
            SmileSample clinicalSample =
                    SampleDataFactory.buildNewClinicalSampleFromMetadata(cmoPatientId, dmpSample);
            if (!sampleService.sampleExistsByInputId(clinicalSample.getPrimarySampleAlias())) {
                sampleService.saveSmileSample(clinicalSample);
            }
        }

        // save mock non-cmo request: 45102
        MockJsonTestData nonCmoRequestData
                = mockDataUtils.mockedRequestJsonDataMap.get("mockNonCmoRequest45102");
        SmileRequest nonCmoRequest
                = RequestDataFactory.buildNewLimsRequestFromJson(nonCmoRequestData.getJsonString());
        requestService.saveRequest(nonCmoRequest);
    }

    /**
     * Tests if the graphDb is set up accurately
     * @throws Exception
     */
    @Test
    @Order(1)
    public void testRequestRepositoryAccess() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        SmileRequest savedRequest = requestService.getSmileRequestById(requestId);
        Assertions.assertEquals(4, savedRequest.getSmileSampleList().size());
    }

    /**
     * Tests whether findMatchedNormalSample retrieves an accurate list SmileSample.
     * Note: after adding and persisting the mocked clinical data, there is now
     * an additional matched normal for the sample below.
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testFindMatchedNormalSample() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_1";
        SmileSample sample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        List<SmileSample> matchedNormalList = sampleService.getMatchedNormalsBySample(sample);
        Assertions.assertEquals(2, matchedNormalList.size());
    }

    /**
     * Tests whether findPooledNormalSample retrieves an accurate list pooled normals
     * @throws Exception
     */
    @Test
    @Order(3)
    public void testFindPooledNormalSample() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_3";
        SmileSample sample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        List<String> pooledNormalList = sampleService.getPooledNormalsBySample(sample);
        Assertions.assertEquals(10, pooledNormalList.size());
    }

    /**
     * Tests if the number of sampleMetadata, from a list retrieved
     * using getSampleMetadataListByCmoPatientId,
     * matches the expected number
     * @throws Exception
     */
    @Test
    @Order(4)
    public void testGetSampleMetadataListByCmoPatientId() throws Exception {
        String cmoPatientId = "C-PXXXD9";
        List<SmileSample> savedSampleList =
                sampleService.getSamplesByCmoPatientId(cmoPatientId);
        Assertions.assertEquals(2, savedSampleList.size());
    }

    /**
     * Tests if the number of sampleMetadata, from a list retrieved
     * using getSampleMetadataListByCmoPatientId,
     * matches the expected number
     * @throws Exception
     */
    @Test
    @Order(5)
    public void testGetAllSmileSamplesByRequestId() throws Exception {
        String requestId = "33344_Z";
        List<SmileSample> requestSamplesList =
                sampleService.getResearchSamplesByRequestId(requestId);
        Assertions.assertEquals(4, requestSamplesList.size());
    }

    /**
     * Tests if the number of sampleMetadata history nodes,
     * from a list retrieved using getSampleMetadataHistoryByIgoId,
     * matches the expected number
     * @throws Exception
     */
    @Test
    @Order(6)
    public void testGetSampleMetadataHistoryByIgoId() throws Exception {
        String igoId = "MOCKREQUEST1_B_1";
        List<SampleMetadata> sampleMetadataHistory = sampleService
                .getResearchSampleMetadataHistoryByIgoId(igoId);
        Assertions.assertEquals(1, sampleMetadataHistory.size());
    }

    /**
     * Tests if sampleHasMetadataUpdates accurately recognizes non-IGO property changes in sampleMetadata
     * @throws Exception
     */
    @Test
    @Order(7)
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
                updatedSample.getLatestSampleMetadata(), Boolean.TRUE, Boolean.FALSE);
        Assertions.assertTrue(hasUpdates);
    }

    /**
     * Tests if the number of sampleMetadata history nodes
     * matches the expected number after updating sampleMetadata
     * @throws Exception
     */
    @Test
    @Order(8)
    public void testSampleHistoryAfterUpdate() throws Exception {
        String igoId = "MOCKREQUEST1_B_2";

        MockJsonTestData updatedRequestData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1UpdatedJsonDataWith2T2N");
        SmileRequest updatedRequest = RequestDataFactory.buildNewLimsRequestFromJson(
                updatedRequestData.getJsonString());
        for (SmileSample updatedSample : updatedRequest.getSmileSampleList()) {
            if (updatedSample.getLatestSampleMetadata().getPrimaryId().equals(igoId)) {
                sampleService.saveSmileSample(updatedSample);
            }
        }
        List<SampleMetadata> sampleMetadataHistory = sampleService
                .getResearchSampleMetadataHistoryByIgoId(igoId);
        Assertions.assertEquals(2, sampleMetadataHistory.size());
    }

    /**
     * Test if the persisted clinical sample is accurately mapped.
     * DMP patient 'P-0000001' is expected to only have 2 clinical samples
     * and no research samples.
     * @throws Exception
     */
    @Test
    @Order(9)
    public void testPersistClinicalSample() throws Exception {
        String dmpPatientId = "P-0000001";
        String cmoPatientId = mockDataUtils.getCmoPatientIdForDmpPatient(dmpPatientId);
        List<SmileSample> sampleList = sampleService
                .getSamplesByCmoPatientId(cmoPatientId);
        Assertions.assertEquals(2, sampleList.size());
    }

    /**
     * Tests that the number of samples (research and clinical) persisted for
     * each patient matches the expected  number of samples.
     * @throws Exception
     */
    @Test
    @Order(10)
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
     * Test that the latest metadata returns the expected cmo label,
     * even when metadata is updated to a sample that a fake date that precedes the "latest" date
     * @throws Exception
     */
    @Test
    @Order(11)
    public void testFindLatestSampleMetadataAfterUpdatingNewPredatedSample() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_2";
        String importDate = "2000-06-10";

        SampleMetadata sampleMetadata = sampleService.getResearchSampleByRequestAndIgoId(
                requestId, igoId).getLatestSampleMetadata();
        SampleMetadata predatedSampleMetadata = new SampleMetadata();
        predatedSampleMetadata.setPrimaryId(igoId);
        predatedSampleMetadata.setIgoRequestId(requestId);
        predatedSampleMetadata.setCmoPatientId(sampleMetadata.getCmoPatientId());
        predatedSampleMetadata.setImportDate(importDate);
        predatedSampleMetadata.setCmoSampleName("C-OLDSAMPLELABEL-T11");

        sampleService.updateSampleMetadata(predatedSampleMetadata, Boolean.FALSE);
        SampleMetadata updatedSampleMetadataAfterUpdate = sampleService.getResearchSampleByRequestAndIgoId(
                requestId, igoId).getLatestSampleMetadata();
        Assertions.assertNotEquals(importDate, updatedSampleMetadataAfterUpdate.getImportDate());
        Assertions.assertEquals(sampleMetadata.getImportDate(),
                updatedSampleMetadataAfterUpdate.getImportDate());
    }

    /**
     * Test that the latest metadata returns the expected cmo label,
     * even when metadata is saved to a sample that a fake date that precedes the "latest" date
     * @throws Exception
     */
    @Test
    @Order(12)
    public void testFindLatestSampleMetadataAfterSavingNewPredatedSample() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_3";
        String importDate = "2000-06-10";

        SampleMetadata sampleMetadata = sampleService.getResearchSampleByRequestAndIgoId(
                requestId, igoId).getLatestSampleMetadata();
        SampleMetadata predatedSampleMetadata = new SampleMetadata();
        predatedSampleMetadata.setPrimaryId(igoId);
        predatedSampleMetadata.setIgoRequestId(requestId);
        predatedSampleMetadata.setCmoPatientId(sampleMetadata.getCmoPatientId());
        predatedSampleMetadata.setImportDate(importDate);
        predatedSampleMetadata.setCmoSampleName("C-OLDSAMPLELABEL-T11");
        SmileSample predatedSample = new SmileSample();
        predatedSample.addSampleMetadata(predatedSampleMetadata);
        predatedSample.setPatient(patientService.getPatientByCmoPatientId(sampleMetadata.getCmoPatientId()));

        sampleService.saveSmileSample(predatedSample);
        SampleMetadata updatedSampleMetadataAfterSave = sampleService.getResearchSampleByRequestAndIgoId(
                requestId, igoId).getLatestSampleMetadata();
        Assertions.assertNotEquals(importDate, updatedSampleMetadataAfterSave.getImportDate());
        Assertions.assertEquals(sampleMetadata.getImportDate(),
                updatedSampleMetadataAfterSave.getImportDate());
    }

    /**
     * Tests if samples found by a valid uuid and igoId are the same (not null)
     * @throws Exception
     */
    @Test
    @Order(13)
    public void testFindSampleByInvestigatorId() throws Exception {
        String igoId = "MOCKREQUEST1_B_2";
        String investigatorId = "01-0012345a";

        SmileSample sample = sampleService.getDetailedSampleByInputId(igoId);
        SmileSample sampleByInvestigatorId = sampleService.getDetailedSampleByInputId(investigatorId);

        Assertions.assertNotNull(sample);
        Assertions.assertEquals(sample.getSmileSampleId(),
                sampleByInvestigatorId.getSmileSampleId());
    }

    /**
     * Tests that sample can not be found by a invalid inputId
     * @throws Exception
     */
    @Test
    @Order(14)
    public void testFindSampleByInvalidInputId() throws Exception {
        String inputId = "invalidInput";

        SmileSample sample = sampleService.getDetailedSampleByInputId(inputId);
        Assertions.assertNull(sample);
    }

    /**
     * Tests if sampleMetadata with updates is being persisted correctly
     * @throws Exception
     */
    @Test
    @Order(15)
    public void testUpdateSampleMetadata() throws Exception {
        MockJsonTestData updatedRequestData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1UpdatedJsonDataWith2T2N");
        SmileRequest updatedRequest = RequestDataFactory.buildNewLimsRequestFromJson(
                updatedRequestData.getJsonString());
        // get the updated sample data from the mocked updated request
        String igoId = "MOCKREQUEST1_B_2";
        SmileSample updatedSample = null;
        for (SmileSample s : updatedRequest.getSmileSampleList()) {
            if (s.getLatestSampleMetadata().getPrimaryId().equals(igoId)) {
                updatedSample = s;
                break;
            }
        }
        Assertions.assertNotNull(updatedSample);
        SampleMetadata updatedMetadata = updatedSample.getLatestSampleMetadata();
        updatedMetadata.setBaitSet("NEW BAIT SET");
        updatedMetadata.setGenePanel("NEW GENE PANEL");
        sampleService.updateSampleMetadata(updatedMetadata, Boolean.TRUE);

        // confirm that the sample metadata history size increases
        List<SampleMetadata> sampleMetadataHistory = sampleService
                .getResearchSampleMetadataHistoryByIgoId(igoId);
        Assertions.assertEquals(2, sampleMetadataHistory.size());

        // confirm that the latest metadata has NEW BAIT SET
        SmileSample latestSample = sampleService.getDetailedSampleByInputId(igoId);
        Boolean testPassed = Boolean.FALSE;
        // both of the import dates are from "today" so just confirm that at least one of the
        // sample metadata objects in the list have the NEW BAIT SET value
        for (SampleMetadata sm : latestSample.getSampleMetadataList()) {
            if (sm.getBaitSet().equals("NEW BAIT SET")) {
                testPassed = Boolean.TRUE;
                break;
            }
        }
        Assertions.assertTrue(testPassed);
    }

    /**
     * Tests if sampleMetadata with invalid updates are not persisted to database
     * @throws Exception
     */
    @Test
    @Order(16)
    public void testInvalidIgoUpdateSampleMetadata() throws Exception {
        MockJsonTestData updatedRequestData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1UpdatedJsonDataWith2T2N");
        SmileRequest updatedRequest = RequestDataFactory.buildNewLimsRequestFromJson(
                updatedRequestData.getJsonString());
        // get the updated sample data from the mocked updated request
        String igoId = "MOCKREQUEST1_B_2";
        SmileSample updatedSample = null;
        for (SmileSample s : updatedRequest.getSmileSampleList()) {
            if (s.getLatestSampleMetadata().getPrimaryId().equals(igoId)) {
                updatedSample = s;
                break;
            }
        }
        Assertions.assertNotNull(updatedSample);

        String invalidCollectionYear = "INVALID IGO UPDATE";
        SampleMetadata updatedMetadata = updatedSample.getLatestSampleMetadata();
        updatedMetadata.setImportDate("2000-10-15");
        updatedMetadata.setBaitSet("NEW BAIT SET");
        updatedMetadata.setGenePanel("NEW GENE PANEL");
        updatedMetadata.setCollectionYear(invalidCollectionYear);
        sampleService.updateSampleMetadata(updatedMetadata, Boolean.TRUE);

        // confirm that the sample metadata only has updated with accepted igo property updates
        String requestId = "MOCKREQUEST1_B";
        SmileSample savedSample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        SampleMetadata latestSampleMetadata = savedSample.getLatestSampleMetadata();
        Assertions.assertNotEquals(invalidCollectionYear,
                latestSampleMetadata.getCollectionYear());
    }

    /**
     * Tests if sampleMetadata with updates that includes a patient swap is being persisted correctly
     * @throws Exception
     */
    @Test
    @Order(17)
    public void testUpdateSampleMetadataWithPatientSwap() throws Exception {
        MockJsonTestData updatedRequestData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1UpdatedJsonDataWith2T2N");
        SmileRequest updatedRequest = RequestDataFactory.buildNewLimsRequestFromJson(
                updatedRequestData.getJsonString());
        // get the updated sample data from the mocked updated request
        String igoId = "MOCKREQUEST1_B_2";
        SmileSample updatedSample = null;
        for (SmileSample s : updatedRequest.getSmileSampleList()) {
            if (s.getLatestSampleMetadata().getPrimaryId().equals(igoId)) {
                updatedSample = s;
                break;
            }
        }
        Assertions.assertNotNull(updatedSample);
        SampleMetadata updatedMetadata = updatedSample.getLatestSampleMetadata();

        // do a quick string replacement for the current cmo sample label and persist update
        String currentCmoPtId = updatedMetadata.getCmoPatientId();
        String swappedCmoPtId = "C-123456H";

        // first confirm that there arent any samples by the swapped cmo pt id
        List<SmileSample> samplesBeforeUpdateForCurrentPt =
                sampleService.getSamplesByCmoPatientId(currentCmoPtId);
        Assertions.assertEquals(4, samplesBeforeUpdateForCurrentPt.size());
        List<SmileSample> samplesBeforeUpdate =
                sampleService.getSamplesByCmoPatientId(swappedCmoPtId);
        Assertions.assertTrue(samplesBeforeUpdate.isEmpty());

        // perform update on the metadata and save to db
        String updatedLabel = updatedMetadata.getCmoSampleName().replace(currentCmoPtId, swappedCmoPtId);
        updatedMetadata.setCmoPatientId(swappedCmoPtId);
        updatedMetadata.setCmoSampleName(updatedLabel);
        updatedSample.addSampleMetadata(updatedMetadata);
        sampleService.saveSmileSample(updatedSample);

        // confirm that the sample metadata history size increases
        List<SampleMetadata> sampleMetadataHistory = sampleService
                .getResearchSampleMetadataHistoryByIgoId(igoId);
        Assertions.assertEquals(2, sampleMetadataHistory.size());

        // confirm that the patient linked to the sample after the update matches the swapped id
        // first confirm that there arent any samples by the swapped cmo pt id
        List<SmileSample> samplesAfterUpdate =
                sampleService.getSamplesByCmoPatientId(swappedCmoPtId);
        Assertions.assertEquals(1, samplesAfterUpdate.size());
        List<SmileSample> samplesStillLinkedToOldPt =
                sampleService.getSamplesByCmoPatientId(currentCmoPtId);
        Assertions.assertEquals(samplesBeforeUpdateForCurrentPt.size() - 1,
                samplesStillLinkedToOldPt.size());
    }

    /**
     * Tests updateSampleMetadata when incoming sampleMetadata update
     * is a new sample with a existing request
     * @throws Exception
     */
    @Test
    @Order(18)
    public void testNewSampleMetadataUpdateWithExistingRequest() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_2";

        SmileSample sample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        SampleMetadata sampleMetadata = sample.getLatestSampleMetadata();

        SampleMetadata newSampleMetadata = new SampleMetadata();
        newSampleMetadata.setIgoRequestId(requestId);
        newSampleMetadata.setPrimaryId("NEW-IGO-ID-A");
        newSampleMetadata.setCmoPatientId(sampleMetadata.getCmoPatientId());

        sampleService.updateSampleMetadata(newSampleMetadata, Boolean.TRUE);

        Assertions.assertEquals(5, sampleService.getResearchSamplesByRequestId(requestId).size());
    }

    /**
     * Tests updateSampleMetadata when incoming sampleMetadata
     * update is a new sample when it's request does not exist
     * @throws Exception
     */
    @Test
    @Order(19)
    public void testNewSampleMetadataUpdateWithNewRequest() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_2";

        SmileSample sample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        SampleMetadata sampleMetadata = sample.getLatestSampleMetadata();

        SampleMetadata newSampleMetadata = new SampleMetadata();
        newSampleMetadata.setIgoRequestId("NEW-REQUEST-ID");
        newSampleMetadata.setPrimaryId("NEW-IGO-ID-B");
        newSampleMetadata.setCmoPatientId(sampleMetadata.getCmoPatientId());

        Boolean isUpdated = sampleService.updateSampleMetadata(newSampleMetadata, Boolean.FALSE);
        Assertions.assertFalse(isUpdated);
    }

    /**
     * Tests if sampleAlias list is updated when sampleMetadata
     * has an investigatorId update
     * @throws Exception
     */
    @Test
    @Order(20)
    public void testInvestigatorIdUpdate() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_4";

        SmileSample oldSample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        SampleMetadata origSampleMetadata = oldSample.getLatestSampleMetadata();

        SampleMetadata updatedSampleMetadata = new SampleMetadata();
        updatedSampleMetadata.setIgoRequestId(requestId);
        updatedSampleMetadata.setPrimaryId(igoId);
        updatedSampleMetadata.setCmoPatientId(origSampleMetadata.getCmoPatientId());
        updatedSampleMetadata.setInvestigatorSampleId("NEW-INVEST-ID");
        Boolean isUpdated = sampleService.updateSampleMetadata(updatedSampleMetadata, Boolean.FALSE);

        Assertions.assertTrue(isUpdated);

        // this initial query doesn't populate all of the sample details, just the main sample node
        SmileSample sampleAfterUpdates = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        sampleAfterUpdates = sampleService.getSmileSample(sampleAfterUpdates.getSmileSampleId());
        List<SampleAlias> updatedSampleAliasList = sampleAfterUpdates.getSampleAliases();

        for (SampleAlias sa: updatedSampleAliasList) {
            if (sa.getNamespace().equals("investigatorId")) {
                Assertions.assertEquals("NEW-INVEST-ID",
                        sa.getValue());
            }
        }
    }

    /**
     * Tests if sampleClass from SmileSample level is updated
     * when there is tumorOrNormal update in the SampleMetadata level
     * @throws Exception
     */
    @Test
    @Order(21)
    public void testTumorOrNormalUpdate() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_3";

        SmileSample oldSample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        SampleMetadata oldSampleMetadata = oldSample.getLatestSampleMetadata();

        SampleMetadata sampleMetadata = new SampleMetadata();
        sampleMetadata.setIgoRequestId(requestId);
        sampleMetadata.setPrimaryId(igoId);
        sampleMetadata.setCmoPatientId(oldSampleMetadata.getCmoPatientId());
        sampleMetadata.setTumorOrNormal("Tumor");
        Boolean isUpdated = sampleService.updateSampleMetadata(sampleMetadata, Boolean.FALSE);

        Assertions.assertTrue(isUpdated);

        SmileSample newSample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        SampleMetadata newSampleMetadata = newSample.getLatestSampleMetadata();

        Assertions.assertEquals(newSampleMetadata.getTumorOrNormal(), newSample.getSampleClass());
    }

    /**
     * Tests that changes in nested sample metadata properties are detected.
     * @throws Exception
     */
    @Test
    @Order(22)
    public void testSampleUpdatesAtLibraryLevel() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_1";

        SmileSample existingSample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        MockJsonTestData updatedRequestData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1JsonDataWithLibraryUpdate");
        SmileRequest updatedRequest = RequestDataFactory.buildNewLimsRequestFromJson(
                updatedRequestData.getJsonString());
        for (SmileSample updatedSample : updatedRequest.getSmileSampleList()) {
            SampleMetadata updatedMetadata = updatedSample.getLatestSampleMetadata();
            if (updatedMetadata.getPrimaryId().equals(igoId)) {
                Boolean hasUpdates = sampleService.sampleHasMetadataUpdates(
                        existingSample.getLatestSampleMetadata(), updatedMetadata,
                        Boolean.TRUE, Boolean.FALSE);
                Assertions.assertTrue(hasUpdates);
            }
        }
    }

    /**
     * Test just confirms that we can fetch all samples given a CMO label.
     * - these samples are from mocked request MOCKREQUEST8_D
     * @throws Exception
     */
    @Test
    @Order(23)
    public void testDuplicateCmoSampleLabels() throws Exception {
        String cmoLabel = "C-MP636AP-N001-d";
        List<SmileSample> samplesMatchingLabels = sampleService.getSamplesByCmoSampleName(cmoLabel);
        Assertions.assertEquals(2, samplesMatchingLabels.size());
    }

    @Test
    @Order(24)
    public void testDuplicateAltIds() throws Exception {
        String altId = "AB9-ABC";
        List<SmileSample> samplesMatchingAltIds = sampleService.getSamplesByAltId(altId);
        Assertions.assertEquals(2, samplesMatchingAltIds.size());
    }

    @Test
    @Order(25)
    public void testNonCmoRequest() throws Exception {
        String nonCmoRequestId = "45102";
        SmileRequest request = requestService.getSmileRequestById(nonCmoRequestId);
        List<SmileSample> samples = request.getSmileSampleList();
        Assertions.assertEquals(3, samples.size());
        for (SmileSample s : samples) {
            Assertions.assertNull(s.getPatient());
        }

        PublishedSmileRequest r = requestService.getPublishedSmileRequestById(nonCmoRequestId);
        for (PublishedSmileSample s : r.getSamples()) {
            Assertions.assertNull(s.getSmilePatientId());
        }
    }
}
