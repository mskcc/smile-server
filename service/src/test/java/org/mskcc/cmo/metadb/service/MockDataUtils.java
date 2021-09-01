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
import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.MetaDbSample;
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
     * Returns an instance of MetaDbRequest from a request json string.
     * @param requestJson
     * @return MetaDbRequest
     * @throws Exception
     */
    public MetaDbRequest extractRequestFromJsonData(String requestJson) throws Exception {
        MetaDbRequest request = mapper.readValue(requestJson,
                MetaDbRequest.class);
        request.setRequestJson(requestJson);
        request.setMetaDbSampleList(extractMetaDbSamplesFromIgoResponse(requestJson));
        request.setNamespace("igo");
        return request;
    }

    /**
     * Extracts a List of MetaDbSample's given a request json string
     * @param message
     * @return List
     * @throws JsonProcessingException
     * @throws IOException
     */
    public List<MetaDbSample> extractMetaDbSamplesFromIgoResponse(Object message)
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
