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
    private final String MOCKED_JSON_DATA_DIR = "data";
    private final ClassPathResource mockJsonTestDataResource = new ClassPathResource(MOCKED_JSON_DATA_DIR);

    // mocked data maps
    public Map<String, MockJsonTestData> mockedRequestJsonDataMap;
    public Map<String, MockJsonTestData> mockedDmpMetadataMap;
    public Map<String, String> mockedDmpPatientMapping;
    public Map<String, String> mockedDmpSampleMapping;

    // expected patient-sample counts (research and clinical)
    public final Map<String, Integer> EXPECTED_PATIENT_SAMPLES_COUNT = initExpectedPatientSamplesCount();

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
     * Returns the CMO patient ID for a given DMP patient ID.
     * @param dmpPatientId
     * @return String
     */
    public String getCmoPatientIdForDmpPatient(String dmpPatientId) {
        return mockedDmpPatientMapping.get(dmpPatientId);
    }

    /**
     * Returns the DMP patient ID for a given CMO patient ID.
     * @param cmoPatientId
     * @return String
     */
    public String getDmpPatientIdForCmoPatient(String cmoPatientId) {
        for (Map.Entry<String, String> entry : mockedDmpPatientMapping.entrySet()) {
            if (entry.getValue().equals(cmoPatientId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Inits the mocked request json data map.
     * @throws IOException
     */
    @Autowired
    public void mockedRequestJsonDataMap() {
        this.mockedRequestJsonDataMap = new HashMap<>();
        ClassPathResource jsonDataDetailsResource =
                new ClassPathResource(MOCKED_REQUEST_DATA_DETAILS_FILEPATH);
        try {
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
        } catch (IOException e) {
            throw new RuntimeException("Error loading data from test file source", e);
        }
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
     * Inits map of expected sample counts for each cmo patient id.
     * @return Map
     */
    private Map<String, Integer> initExpectedPatientSamplesCount() {
        Map<String, Integer> map = new HashMap<>();
        map.put("C-KXXL3J", 2);
        map.put("C-X09281", 2);
        map.put("C-999XX", 2);
        map.put("C-1MP6YY", 6);
        map.put("C-9XX8808", 2);
        map.put("C-PXXXD9", 2);
        map.put("C-XXA40X", 2);
        map.put("C-MXX99F", 1);
        map.put("C-MP789JR", 4);
        map.put("C-8DH24X", 4);
        map.put("C-DPCXX1", 2);
        map.put("C-XXC4XX", 1);
        map.put("C-FFX222", 2);
        map.put("C-HXXX3X", 1);
        map.put("C-TX6DNG", 1);
        map.put("C-XXX711", 2);
        map.put("C-PPPXX2", 2);
        map.put("C-YXX89J", 2);
        return map;
    }
}
