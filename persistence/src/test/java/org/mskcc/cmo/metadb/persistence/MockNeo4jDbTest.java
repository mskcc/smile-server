package org.mskcc.cmo.metadb.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Might also be renaming this class.. maybe this will be where the data
 * initialization occurs and other test classes can reference it?
 * @author ochoaa
 */
@Testcontainers
@DataNeo4jTest
public class MockNeo4jDbTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Container
    private static final Neo4jContainer databaseServer = new Neo4jContainer<>()
            .withEnv("NEO4J_dbms_security_procedures_unrestricted", "apoc.*,algo.*");

    @TestConfiguration // <2>
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

    @Autowired
    public MockNeo4jDbTest(MetaDbRequestRepository requestRepository,
            MetaDbSampleRepository sampleRepository) {
        this.requestRepository = requestRepository;
        this.sampleRepository = sampleRepository;
    }

    @Test
    public void testSaveMethod() throws Exception {
        // this file might actually already be saved as a mock request json data file
        // under a different name but this was just added temporarily to facilitate
        // troubleshooting issues with initializing the mock database
        File file = new File("/Users/laptop/metadb-projects/cmo-metadb/"
                + "persistence/src/test/resources/data/incoming_requests"
                + "/mocked_request_mockdb.json");
        Map<String, Object> filedata = mapper.readValue(file, Map.class);
        String requestJson = mapper.writeValueAsString(filedata);

        MetaDbRequest metaDbRequest = mapper.readValue(requestJson,
                MetaDbRequest.class);
        metaDbRequest.setRequestJson(requestJson);
        metaDbRequest.setMetaDbSampleList(extractMetaDbSamplesFromIgoResponse(requestJson));
        metaDbRequest.setNamespace("igo");

        requestRepository.save(metaDbRequest);

        List<MetaDbSample> sampleList = sampleRepository
                .findAllMetaDbSamplesByRequest(metaDbRequest.getRequestId());
        Assertions.assertTrue(sampleList.size() == 4);
    }

    private List<MetaDbSample> extractMetaDbSamplesFromIgoResponse(Object message)
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
}
