package org.mskcc.cmo.metadb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    // mocked data filepaths and resources
    private final String MOCKED_REQUEST_DATA_DETAILS_FILEPATH = "data/mocked_request_data_details.txt";
    private final String MOCKED_DMP_METADATA_DETAILS_FILEPATH
            = "data/dmp_clinical/mocked_dmp_data_details.txt";
    private final String MOCKED_DMP_PATIENT_MAPPING_FILEPATH
            = "data/dmp_clinical/mocked_dmp_patient_mappings.txt";
    private final String MOCKED_DMP_SAMPLE_MAPPING_FILEPATH
            = "data/dmp_clinical/mocked_dmp_sample_mappings.txt";
    private final String MOCKED_JSON_DATA_DIR = "data";
    private final ClassPathResource mockJsonTestDataResource = new ClassPathResource(MOCKED_JSON_DATA_DIR);
    // mocked data maps
    public Map<String, MockJsonTestData> mockedRequestJsonDataMap;
    public Map<String, MockJsonTestData> mockedDmpMetadataMap;
    public Map<String, String> mockedDmpPatientMapping;
    public Map<String, String> mockedDmpSampleMapping;

    /**
     * Inits the mocked dmp metadata map.
     * @throws IOException
     */
    @Autowired
    public void mockedDmpMetadataMap() throws IOException {
        this.mockedDmpMetadataMap = new HashMap<>();
        ClassPathResource jsonDataDetailsResource =
                new ClassPathResource(MOCKED_DMP_METADATA_DETAILS_FILEPATH);
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
            mockedDmpMetadataMap.put(identifier,
                    createMockJsonTestData(identifier, filepath, description));
        }
        reader.close();
    }

    /**
     * Inits the mocked dmp patient id mappings.
     * @throws IOException
     */
    @Autowired
    public void mockedDmpPatientMapping() throws IOException {
        this.mockedDmpPatientMapping = new HashMap<>();
        ClassPathResource jsonDataDetailsResource =
                new ClassPathResource(MOCKED_DMP_PATIENT_MAPPING_FILEPATH);
        BufferedReader reader = new BufferedReader(new FileReader(jsonDataDetailsResource.getFile()));
        List<String> columns = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] data = line.split("\t");
            if (columns.isEmpty()) {
                columns = Arrays.asList(data);
                continue;
            }
            String cmoPatientId = data[columns.indexOf("CMO_PATIENT_ID")];
            // not every cmo patient will have a matching dmp id
            String dmpPatientId = null;
            try {
                dmpPatientId = data[columns.indexOf("DMP_ID")];
            } catch (ArrayIndexOutOfBoundsException e) {
                // do nothing
            }
            mockedDmpPatientMapping.put(dmpPatientId, cmoPatientId);
        }
        reader.close();
    }

    /**
     * Inits the mocked dmp sample id mappings.
     * @throws IOException
     */
    @Autowired
    public void mockedDmpSampleMapping() throws IOException {
        this.mockedDmpSampleMapping = new HashMap<>();
        ClassPathResource jsonDataDetailsResource =
                new ClassPathResource(MOCKED_DMP_SAMPLE_MAPPING_FILEPATH);
        BufferedReader reader = new BufferedReader(new FileReader(jsonDataDetailsResource.getFile()));
        List<String> columns = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] data = line.split("\t");
            if (columns.isEmpty()) {
                columns = Arrays.asList(data);
                continue;
            }
            String igoSampleId = data[columns.indexOf("IGO_SAMPLE_ID")];
            // not every igo sample will have a matching dmp id
            String dmpSampleId = null;
            try {
                dmpSampleId = data[columns.indexOf("DMP_ID")];
            } catch (ArrayIndexOutOfBoundsException e) {
                // do nothing
            }
            mockedDmpSampleMapping.put(igoSampleId, dmpSampleId);
        }
        reader.close();
    }

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

}
