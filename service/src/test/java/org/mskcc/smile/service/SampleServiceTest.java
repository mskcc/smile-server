package org.mskcc.smile.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mskcc.smile.model.SampleAlias;
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
                updatedSample.getLatestSampleMetadata(), Boolean.TRUE);
        Assertions.assertThat(hasUpdates).isEqualTo(Boolean.TRUE);

    }

    /**
     * Tests if the number of sampleMetadata history nodes
     * matches the expected number after updating sampleMetadata
     * @throws Exception
     */
    @Test
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
     * Test that the latest metadata returns the expected cmo label,
     * even when metadata is updated to a sample that a fake date that precedes the "latest" date
     * @throws Exception
     */
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

        sampleService.updateSampleMetadata(predatedSampleMetadata);
        SampleMetadata updatedSampleMetadataAfterUpdate = sampleService.getResearchSampleByRequestAndIgoId(
                requestId, igoId).getLatestSampleMetadata();
        Assertions.assertThat(updatedSampleMetadataAfterUpdate.getImportDate()).isNotEqualTo(importDate);
        Assertions.assertThat(updatedSampleMetadataAfterUpdate.getImportDate()).isEqualTo(
                sampleMetadata.getImportDate());
    }

    /**
     * Test that the latest metadata returns the expected cmo label,
     * even when metadata is saved to a sample that a fake date that precedes the "latest" date
     * @throws Exception
     */
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

        sampleService.saveSmileSample(predatedSample);
        SampleMetadata updatedSampleMetadataAfterSave = sampleService.getResearchSampleByRequestAndIgoId(
                requestId, igoId).getLatestSampleMetadata();
        Assertions.assertThat(updatedSampleMetadataAfterSave.getImportDate()).isNotEqualTo(importDate);
        Assertions.assertThat(updatedSampleMetadataAfterSave.getImportDate()).isEqualTo(
                sampleMetadata.getImportDate());
    }

    /**
     * Tests if samples found by a valid uuid and igoId are the same (not null)
     * @throws Exception
     */
    @Test
    public void testFindSampleByUuid() throws Exception {
        String igoId = "MOCKREQUEST1_B_2";
        String investigatorId = "01-0012345a";

        SmileSample sample = sampleService.getSampleByInputId(igoId);
        SmileSample sampleByUuid = sampleService.getSampleByInputId(investigatorId);

        Assertions.assertThat(sample).isNotNull();
        Assertions.assertThat(sample).isEqualToComparingFieldByField(sampleByUuid);
    }

    /**
     * Tests that sample can not be found by a invalid inputId
     * @throws Exception
     */
    @Test
    public void testFindSampleByInvalidInputId() throws Exception {
        String inputId = "invalidInput";

        SmileSample sample = sampleService.getSampleByInputId(inputId);
        Assertions.assertThat(sample).isNull();
    }

    /**
     * Tests if sampleMetadata with updates is being persisted correctly
     * @throws Exception
     */
    @Test
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
        Assertions.assertThat(updatedSample).isNotNull();
        SampleMetadata updatedMetadata = updatedSample.getLatestSampleMetadata();
        updatedMetadata.setImportDate("2000-10-15");
        updatedMetadata.setBaitSet("NEW BAIT SET");
        updatedMetadata.setGenePanel("NEW GENE PANEL");
        updatedSample.addSampleMetadata(updatedMetadata);
        sampleService.saveSmileSample(updatedSample);

        // confirm that the sample metadata history size increases
        List<SampleMetadata> sampleMetadataHistory = sampleService
                .getResearchSampleMetadataHistoryByIgoId(igoId);
        Assertions.assertThat(sampleMetadataHistory.size()).isEqualTo(2);
    }


    /**
     * Tests if sampleMetadata with updates that includes a patient swap is being persisted correctly
     * @throws Exception
     */
    @Test
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
        Assertions.assertThat(updatedSample).isNotNull();
        SampleMetadata updatedMetadata = updatedSample.getLatestSampleMetadata();

        // do a quick string replacement for the current cmo sample label and persist update
        String currentCmoPtId = updatedMetadata.getCmoPatientId();
        String swappedCmoPtId = "C-123456H";

        // first confirm that there arent any samples by the swapped cmo pt id
        List<SmileSample> samplesBeforeUpdateForCurrentPt =
                sampleService.getSamplesByCmoPatientId(currentCmoPtId);
        Assertions.assertThat(samplesBeforeUpdateForCurrentPt.size()).isEqualTo(4);
        List<SmileSample> samplesBeforeUpdate =
                sampleService.getSamplesByCmoPatientId(swappedCmoPtId);
        Assertions.assertThat(samplesBeforeUpdate).isEmpty();

        // perform update on the metadata and save to db
        String updatedLabel = updatedMetadata.getCmoSampleName().replace(currentCmoPtId, swappedCmoPtId);
        updatedMetadata.setCmoPatientId(swappedCmoPtId);
        updatedMetadata.setCmoSampleName(updatedLabel);
        updatedSample.updateSampleMetadata(updatedMetadata);
        sampleService.saveSmileSample(updatedSample);

        // confirm that the sample metadata history size increases
        List<SampleMetadata> sampleMetadataHistory = sampleService
                .getResearchSampleMetadataHistoryByIgoId(igoId);
        Assertions.assertThat(sampleMetadataHistory.size()).isEqualTo(2);

        // confirm that the patient linked to the sample after the update matches the swapped id
        // first confirm that there arent any samples by the swapped cmo pt id
        List<SmileSample> samplesAfterUpdate =
                sampleService.getSamplesByCmoPatientId(swappedCmoPtId);
        Assertions.assertThat(samplesAfterUpdate.size()).isEqualTo(1);
        List<SmileSample> samplesStillLinkedToOldPt =
                sampleService.getSamplesByCmoPatientId(currentCmoPtId);
        Assertions.assertThat(samplesStillLinkedToOldPt.size())
                .isEqualTo(samplesBeforeUpdateForCurrentPt.size() - 1);

    }

    /**
     * Tests updateSampleMetadata when incoming sampleMetadata update
     * is a new sample with a existing request
     * @throws Exception
     */
    @Test
    public void testNewSampleMetadataUpdateWithExistingRequest() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_2";

        SmileSample sample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        SampleMetadata sampleMetadata = sample.getLatestSampleMetadata();

        SampleMetadata newSampleMetadata = new SampleMetadata();
        newSampleMetadata.setIgoRequestId(requestId);
        newSampleMetadata.setPrimaryId("NEW-IGO-ID-A");
        newSampleMetadata.setCmoPatientId(sampleMetadata.getCmoPatientId());

        sampleService.updateSampleMetadata(newSampleMetadata);

        Assertions.assertThat(sampleService.getResearchSamplesByRequestId(requestId).size()).isEqualTo(5);
    }

    /**
     * Tests updateSampleMetadata when incoming sampleMetadata
     * update is a new sample when it's request does not exist
     * @throws Exception
     */
    @Test
    public void testNewSampleMetadataUpdateWithNewRequest() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_2";

        SmileSample sample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        SampleMetadata sampleMetadata = sample.getLatestSampleMetadata();

        SampleMetadata newSampleMetadata = new SampleMetadata();
        newSampleMetadata.setIgoRequestId("NEW-REQUEST-ID");
        newSampleMetadata.setPrimaryId("NEW-IGO-ID-B");
        newSampleMetadata.setCmoPatientId(sampleMetadata.getCmoPatientId());

        Boolean isUpdated = sampleService.updateSampleMetadata(newSampleMetadata);

        Assertions.assertThat(isUpdated).isEqualTo(Boolean.FALSE);
    }

    /**
     * Tests if sampleAlias list is updated when sampleMetadata
     * has an investigatorId update
     * @throws Exception
     */
    @Test
    public void testInvestigatorIdUpdate() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_4";

        SmileSample oldSample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        SampleMetadata oldSampleMetadata = oldSample.getLatestSampleMetadata();

        SampleMetadata newSampleMetadata = new SampleMetadata();
        newSampleMetadata.setIgoRequestId(requestId);
        newSampleMetadata.setPrimaryId(igoId);
        newSampleMetadata.setCmoPatientId(oldSampleMetadata.getCmoPatientId());
        newSampleMetadata.setInvestigatorSampleId("NEW-INVEST-ID");
        Boolean isUpdated = sampleService.updateSampleMetadata(newSampleMetadata);

        Assertions.assertThat(isUpdated).isEqualTo(Boolean.TRUE);

        SmileSample newSample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        SampleMetadata sampleMetadata = newSample.getLatestSampleMetadata();
        List<SampleAlias> sampleAliasList =
                sampleRepository.findAllSampleAliases(oldSample.getSmileSampleId());

        for (SampleAlias sa: sampleAliasList) {
            if (sa.getNamespace().equals("investigatorId")) {
                Assertions.assertThat(sa.getValue()).isEqualTo(sampleMetadata.getInvestigatorSampleId());
            }
        }
    }

    /**
     * Tests if sampleClass from SmileSample level is updated
     * when there is tumorOrNormal update in the SampleMetadata level
     * @throws Exception
     */
    @Test
    public void testTumorOrNormalUpdate() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_3";

        SmileSample oldSample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        SampleMetadata oldSampleMetadata = oldSample.getLatestSampleMetadata();

        SampleMetadata sampleMetadata = new SampleMetadata();
        sampleMetadata.setIgoRequestId(requestId);
        sampleMetadata.setPrimaryId(igoId);
        sampleMetadata.setCmoPatientId(oldSampleMetadata.getCmoPatientId());
        sampleMetadata.setTumorOrNormal("tumor");
        Boolean isUpdated = sampleService.updateSampleMetadata(sampleMetadata);

        Assertions.assertThat(isUpdated).isEqualTo(Boolean.TRUE);

        SmileSample newSample = sampleService.getResearchSampleByRequestAndIgoId(requestId, igoId);
        SampleMetadata newSampleMetadata = newSample.getLatestSampleMetadata();

        Assertions.assertThat(newSample.getSampleClass()).isEqualTo(newSampleMetadata.getTumorOrNormal());
    }
}
