package org.mskcc.cmo.metadb.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mskcc.cmo.common.MetadbJsonComparator;
import org.mskcc.cmo.common.impl.MetadbJsonComparatorImpl;
import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.persistence.MetaDbPatientRepository;
import org.mskcc.cmo.metadb.persistence.MetaDbRequestRepository;
import org.mskcc.cmo.metadb.persistence.MetaDbSampleRepository;
import org.mskcc.cmo.metadb.service.impl.MetadbRequestServiceImpl;
import org.mskcc.cmo.metadb.service.impl.SampleServiceImpl;
import org.mskcc.cmo.metadb.service.util.RequestStatusLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 *
 * @author ochoaa
 */
@Testcontainers
@DataNeo4jTest
public class SampleServiceTest {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String MOCKED_REQUEST_DATA_DETAILS_FILEPATH = "data/mocked_request_data_details.txt";
    private static final String MOCKED_JSON_DATA_DIR = "data";
    private static ClassPathResource mockJsonTestDataResource;
    private static Map<String, MockJsonTestData> mockedRequestJsonDataMap;

    @Container
    private static final Neo4jContainer databaseServer = new Neo4jContainer<>()
            .withEnv("NEO4J_dbms_security_procedures_unrestricted", "apoc.*,algo.*");
    @Autowired
    private MetadbRequestService requestService;

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
        public MetadbRequestService requestService() {
            return new MetadbRequestServiceImpl();
        }

        @Bean
        public SampleService sampleService() {
            return new SampleServiceImpl();
        }

        @Bean
        public MetadbJsonComparator metadbJsonComparator() {
            return new MetadbJsonComparatorImpl();
        }

        @MockBean
        public RequestStatusLogger requestStatusLogger;
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
    public SampleServiceTest(MetaDbRequestRepository requestRepository,
            MetaDbSampleRepository sampleRepository, MetaDbPatientRepository patientRepository,
            MetadbRequestService requestService, SampleService sampleService) {
        this.requestRepository = requestRepository;
        this.sampleRepository = sampleRepository;
        this.patientRepository = patientRepository;
    }

    /**
     * Sets up the Mock Request JSON data resources.
     * @throws Exception
     */
    @BeforeAll
    public static void loadMockRequestDataResources() throws Exception {
        mockJsonTestDataResource = new ClassPathResource(MOCKED_JSON_DATA_DIR);
        mockedRequestJsonDataMap = mockedRequestJsonDataMap();
    }

    /**
     * Persists the Mock Request data to the test database.
     * @throws Exception
     */
    @Autowired
    public void persistMockRequestDataToTestDb() throws Exception {
        MockJsonTestData request1Data = mockedRequestJsonDataMap.get("mockIncomingRequest1JsonDataWith2T2N");
        MetaDbRequest request = extractRequestFromJsonData(request1Data.getJsonString());
        requestService.saveRequest(request);
    }

    @Test
    public void testRequestRepositoryAccess() throws Exception {

        String requestId = "MOCKREQUEST1_B";
        MetaDbRequest savedRequest = requestService.getMetadbRequestById(requestId);
        Assertions.assertThat(savedRequest.getMetaDbSampleList().size() == 4);
    }

    private static MetaDbRequest extractRequestFromJsonData(String requestJson) throws Exception {
        MetaDbRequest request = mapper.readValue(requestJson,
                MetaDbRequest.class);
        request.setRequestJson(requestJson);
        request.setMetaDbSampleList(extractMetaDbSamplesFromIgoResponse(requestJson));
        request.setNamespace("igo");
        return request;
    }

    private static List<MetaDbSample> extractMetaDbSamplesFromIgoResponse(Object message)
            throws JsonProcessingException, IOException {
        Map<String, Object> map = mapper.readValue(message.toString(), Map.class);
        SampleMetadata[] sampleList = mapper.convertValue(map.get("samples"),
                SampleMetadata[].class);

        List<MetaDbSample> metaDbSampleList = new ArrayList<>();
        for (SampleMetadata sample: sampleList) {
            // update import date here since we are parsing from json
            sample.setImportDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            sample.setRequestId((String) map.get("requestId"));
            MetaDbSample metaDbSample = new MetaDbSample();
            metaDbSample.addSampleMetadata(sample);
            metaDbSampleList.add(metaDbSample);
        }
        return metaDbSampleList;
    }

    private static Map<String, MockJsonTestData> mockedRequestJsonDataMap() throws IOException {
        SampleServiceTest.mockedRequestJsonDataMap = new HashMap<>();
        ClassPathResource jsonDataDetailsResource =
                new ClassPathResource(MOCKED_REQUEST_DATA_DETAILS_FILEPATH);
        BufferedReader reader = new BufferedReader(new FileReader(jsonDataDetailsResource.getFile()));
        List<String> columns = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] data = line.split("\t");
            if (columns.isEmpty()) {
                columns = Arrays.asList(data);
                continue;
            }
            String identifier = data[columns.indexOf("identifier")];
            String filepath = data[columns.indexOf("filepath")];
            String description = data[columns.indexOf("description")];
            mockedRequestJsonDataMap.put(identifier,
                    createMockJsonTestData(identifier, filepath, description));
        }
        reader.close();
        return mockedRequestJsonDataMap;
    }

    private static MockJsonTestData createMockJsonTestData(String identifier, String filepath,
            String description) throws IOException {
        String jsonString = loadMockRequestJsonTestData(filepath);
        return new MockJsonTestData(identifier, filepath, description, jsonString);
    }

    private static String loadMockRequestJsonTestData(String filepath) throws IOException {
        ClassPathResource res = new ClassPathResource(mockJsonTestDataResource.getPath()
                + File.separator + filepath);
        Map<String, Object> filedata = mapper.readValue(res.getFile(), Map.class);
        return mapper.writeValueAsString(filedata);
    }
}
