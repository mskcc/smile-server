package org.mskcc.cmo.metadb.service;


import static org.junit.Assert.assertThrows;

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mskcc.cmo.metadb.model.MetaDbPatient;
import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.PatientAlias;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.persistence.MetaDbPatientRepository;
import org.mskcc.cmo.metadb.persistence.MetaDbRequestRepository;
import org.mskcc.cmo.metadb.persistence.MetaDbSampleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author ochoaa
 */
@Testcontainers
@DataNeo4jTest
@Import(MockDataUtils.class)
public class MetadbServiceTest {
    @Autowired
    private MockDataUtils mockDataUtils;

    @Autowired
    private MetadbRequestService requestService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private PatientService patientService;

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

    private final MetaDbRequestRepository requestRepository;
    private final MetaDbSampleRepository sampleRepository;
    private final MetaDbPatientRepository patientRepository;

    /**
     * Initializes the Neo4j repositories.
     * @param requestRepository
     * @param sampleRepository
     * @param patientRepository
     * @param requestService
     * @param sampleService
     */
    @Autowired
    public MetadbServiceTest(MetaDbRequestRepository requestRepository,
            MetaDbSampleRepository sampleRepository, MetaDbPatientRepository patientRepository,
            MetadbRequestService requestService, SampleService sampleService,
            PatientService patientService) {
        this.requestRepository = requestRepository;
        this.sampleRepository = sampleRepository;
        this.patientRepository = patientRepository;
    }

    /**
     * Persists the Mock Request data to the test database.
     * @throws Exception
     */
    @Autowired
    public void persistMockRequestDataToTestDb() throws Exception {
        MockJsonTestData request1Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1JsonDataWith2T2N");
        MetaDbRequest request1 = mockDataUtils.extractRequestFromJsonData(request1Data.getJsonString());
        requestService.saveRequest(request1);

        MockJsonTestData request3Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest3JsonDataPooledNormals");
        MetaDbRequest request3 = mockDataUtils.extractRequestFromJsonData(request3Data.getJsonString());
        requestService.saveRequest(request3);

        MockJsonTestData request5Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest5JsonPtMultiSamples");
        MetaDbRequest request5 = mockDataUtils.extractRequestFromJsonData(request5Data.getJsonString());
        requestService.saveRequest(request5);
    }


    /**
     * Tests if the graphDb is set up accurately
     * @throws Exception
     */
    @Test
    public void testRequestRepositoryAccess() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        MetaDbRequest savedRequest = requestService.getMetadbRequestById(requestId);
        Assertions.assertThat(savedRequest.getMetaDbSampleList().size()).isEqualTo(4);
    }

    /**
     * Tests whether findMatchedNormalSample retrieves an accurate list MetaDbSample
     * @throws Exception
     */
    @Test
    public void testFindMatchedNormalSample() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_1";
        MetaDbSample metaDbSample = sampleService.getMetaDbSampleByRequestAndIgoId(requestId, igoId);
        List<MetaDbSample> matchedNormalList = sampleService.findMatchedNormalSample(metaDbSample);
        Assertions.assertThat(matchedNormalList.size()).isEqualTo(1);
    }

    /**
     * Tests whether findPooledNormalSample retrieves an accurate list pooled normals
     * @throws Exception
     */
    @Test
    public void testFindPooledNormalSample() throws Exception {
        String requestId = "MOCKREQUEST1_B";
        String igoId = "MOCKREQUEST1_B_3";
        MetaDbSample metaDbSample = sampleService.getMetaDbSampleByRequestAndIgoId(requestId, igoId);
        List<String> pooledNormalList = sampleService.findPooledNormalSample(metaDbSample);
        
        final ObjectMapper mapper = new ObjectMapper();
        //System.out.println("\n\n\n\n\n " + pooledNormalListupdate.size());
        //The List of pooled normals is one long string of all normals
        //Need to figure out why this is happening
        //Assertions.assertThat(pooledNormalList.size()).isEqualTo(10);
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
        List<SampleMetadata> savedSampleMetadataList = sampleService
                .getSampleMetadataListByCmoPatientId(cmoPatientId);
        System.out.println("\n\n\n\n\n sample list size: " + savedSampleMetadataList.size());

        Assertions.assertThat(savedSampleMetadataList.size()).isEqualTo(2);
    }

    /**
     * Tests if the number of sampleMetadata, from a list retrieved
     * using getSampleMetadataListByCmoPatientId,
     * matches the expected number
     * @throws Exception
     */
    @Test
    public void testGetAllMetadbSamplesByRequestId() throws Exception {
        String requestId = "33344_Z";
        List<MetaDbSample> savedMetaDbSampleList = sampleService.getAllMetadbSamplesByRequestId(requestId);
        Assertions.assertThat(savedMetaDbSampleList.size()).isEqualTo(4);
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
        List<SampleMetadata> sampleMetadataHistory = sampleService.getSampleMetadataHistoryByIgoId(igoId);
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
        MetaDbSample metaDbSample = sampleService.getMetaDbSampleByRequestAndIgoId(requestId, igoId);
        
        MockJsonTestData updatedRequestData = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1UpdatedJsonDataWith2T2N");
        MetaDbRequest updatedRequest = mockDataUtils.extractRequestFromJsonData(
                updatedRequestData.getJsonString());
        MetaDbSample updatedMetaDbSample = updatedRequest.getMetaDbSampleList().get(0);
        
        Boolean hasUpdates = sampleService.sampleHasMetadataUpdates(
                metaDbSample.getLatestSampleMetadata(),
                updatedMetaDbSample.getLatestSampleMetadata());
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
        MetaDbRequest updatedRequest = mockDataUtils.extractRequestFromJsonData(
                updatedRequestData.getJsonString());
        MetaDbSample updatedMetaDbSample = updatedRequest.getMetaDbSampleList().get(1);
        sampleService.saveSampleMetadata(updatedMetaDbSample);
        
        List<SampleMetadata> sampleMetadataHistory = sampleService.getSampleMetadataHistoryByIgoId(igoId);
        Assertions.assertThat(sampleMetadataHistory.size()).isEqualTo(2);

    }

    /**
     * Tests if the returned list of sampleMetadata history is sorted based on importDate
     * @throws Exception
     */
    @Test
    public void testSampleHistoryListIsAscendingByImportDate() throws Exception {
        String igoId = "MOCKREQUEST1_B_4";
        List<SampleMetadata> sampleMetadataHistory = sampleService.getSampleMetadataHistoryByIgoId(igoId);
        Assertions.assertThat(sampleMetadataHistory).isSorted();
    }

    @Test
    public void testFindPatientByPatientAlias() throws Exception {
        String cmoPatientId = "C-1MP6YY";
        Assertions.assertThat(
                patientRepository.findPatientByCmoPatientId(cmoPatientId)).isNotNull();
    }

    @Test
    public void testFindPatientByPatientAliasWithExpectedFailure() {
        String cmoPatientId = "C-1MP6YY";
        MetaDbPatient patient = new MetaDbPatient();
        patient.addPatientAlias(new PatientAlias(cmoPatientId, "cmoId"));
        // this should create a duplicate patient node that will throw the exception
        // below when queried
        patientRepository.save(patient);

        Assertions.assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> {
                patientRepository.findPatientByCmoPatientId(cmoPatientId);
            });
    }

}
