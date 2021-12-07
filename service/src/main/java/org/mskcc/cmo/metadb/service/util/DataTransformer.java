package org.mskcc.cmo.metadb.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

public class DataTransformer {
    Map<String, String> setUpResearchRequestMapping() {
        final Map<String, String> researchRequestMapping = new HashMap<>();
        return researchRequestMapping;
    }
    
    Map<String, String> setUpClinicalSampleMapping() {
        final Map<String, String> clinicalSampleMapping = new HashMap<>();
        return clinicalSampleMapping;
    }
    
    Map<String, String> setUpResearchSampleMapping() {
        final Map<String, String> researchSampleMapping = new HashMap<>();
        researchSampleMapping.put("cmoSampleClass", "sampleType");
        researchSampleMapping.put("specimenType", "sampleClass");
        researchSampleMapping.put("oncoTreeCode", "oncotreeCode");
        researchSampleMapping.put("requestId", "igoRequestId");
        researchSampleMapping.put("recipe", "genePanel");
        researchSampleMapping.put("igoId", "primaryId");
        return researchSampleMapping;
    }
    
    private final ObjectMapper mapper = new ObjectMapper();
    
    public String transformRequestMetadata(String requestJson) throws JsonProcessingException {
        return updateMapKeyValueBasedOnMapping(requestJson, setUpResearchRequestMapping());
    }
    
    public String transformClinicalSampleMetadata(String sampleJson) throws JsonProcessingException {
        return updateMapKeyValueBasedOnMapping(sampleJson, setUpClinicalSampleMapping());
    }
    
    public String transformResearchSampleMetadata(String sampleJson) throws JsonProcessingException {
        return updateMapKeyValueBasedOnMapping(sampleJson, setUpResearchSampleMapping());
    }
    
    public String updateMapKeyValueBasedOnMapping(String unParsedJson,
            Map<String, String> universalMapping) throws JsonProcessingException {
        Map<String, String> rawMapValues = mapper.convertValue(unParsedJson, Map.class);
        for (Map.Entry<String, String> entry : rawMapValues.entrySet()) {
            if(universalMapping.containsKey(entry.getKey())) {
                rawMapValues.replace(entry.getKey(), universalMapping.get(entry.getKey()));
            }
        }
        return mapper.writeValueAsString(rawMapValues);
    }
}
