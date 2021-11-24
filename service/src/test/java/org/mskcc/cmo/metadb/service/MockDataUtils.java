package org.mskcc.cmo.metadb.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
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
import org.mskcc.cmo.metadb.model.MetadbRequest;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.RequestMetadata;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 *
 * @author ochoaa
 */
@Component
public final class MockDataUtils {
    private final ObjectMapper mapper = new ObjectMapper();
    private final String MOCKED_REQUEST_DATA_DETAILS_FILEPATH = "data/mocked_request_data_details.txt";
    private final String MOCKED_JSON_DATA_DIR = "data";
    private final ClassPathResource mockJsonTestDataResource = new ClassPathResource(MOCKED_JSON_DATA_DIR);
    public Map<String, MockJsonTestData> mockedRequestJsonDataMap;

    /**
     * Inits the mocked request json data map.
     * @throws IOException
     */
    @Autowired
    public void mockedRequestJsonDataMap() throws IOException {
        this.mockedRequestJsonDataMap = new HashMap<>();
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
    }

    private MockJsonTestData createMockJsonTestData(String identifier, String filepath,
            String description) throws IOException {
        String jsonString = loadMockRequestJsonTestData(filepath);
        return new MockJsonTestData(identifier, filepath, description, jsonString);
    }

    private String loadMockRequestJsonTestData(String filepath) throws IOException {
        ClassPathResource res = new ClassPathResource(mockJsonTestDataResource.getPath()
                + File.separator + filepath);
        Map<String, Object> filedata = mapper.readValue(res.getFile(), Map.class);
        return mapper.writeValueAsString(filedata);
    }

    /**
     * Returns an instance of MetadbRequest from a request json string.
     * @param requestJson
     * @return MetadbRequest
     * @throws Exception
     */
    public MetadbRequest extractRequestFromJsonData(String requestJson) throws Exception {
        MetadbRequest request = mapper.readValue(requestJson,
                MetadbRequest.class);
        request.setRequestJson(requestJson);
        request.setMetaDbSampleList(extractMetadbSamplesFromIgoResponse(requestJson));
        request.setNamespace("igo");
        request.addRequestMetadata(extractRequestMetadata(requestJson));
        return request;
    }

    /**
     * Extracts a List of MetadbSample's given a request json string
     * @param message
     * @return List
     * @throws JsonProcessingException
     * @throws IOException
     */
    public List<MetadbSample> extractMetadbSamplesFromIgoResponse(Object message)
            throws JsonProcessingException, IOException {
        Map<String, Object> map = mapper.readValue(message.toString(), Map.class);
        SampleMetadata[] samples = mapper.convertValue(map.get("samples"),
                SampleMetadata[].class);

        List<MetadbSample> requestSamplesList = new ArrayList<>();
        for (SampleMetadata s: samples) {
            // update import date here since we are parsing from json
            s.setImportDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            s.setRequestId((String) map.get("requestId"));
            MetadbSample sample = new MetadbSample();
            sample.addSampleMetadata(s);
            sample.setSampleCategory("research");
            sample.setSampleClass(s.getTumorOrNormal());
            sample.addSampleAlias(new SampleAlias(s.getPrimaryId(), "igoId"));
            sample.addSampleAlias(new SampleAlias(s.getInvestigatorSampleId(), "investigatorId"));
            requestSamplesList.add(sample);
        }
        return requestSamplesList;
    }

    private RequestMetadata extractRequestMetadata(String requestMetadataJson)
            throws JsonMappingException, JsonProcessingException {
        Map<String, Object> requestMetadataMap = mapper.readValue(requestMetadataJson, Map.class);
        // remove samples if present for request metadata
        if (requestMetadataMap.containsKey("samples")) {
            requestMetadataMap.remove("samples");
        }
        RequestMetadata requestMetadata = new RequestMetadata(
                requestMetadataMap.get("requestId").toString(),
                mapper.writeValueAsString(requestMetadataMap),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        return requestMetadata;
    }
}
