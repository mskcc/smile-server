package org.mskcc.cmo.metadb.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

public class DataTransformer {
    Map<String, String> setUpResearchRequestMapping() {
        Map<String, String> researchRequestMapping = new HashMap<>();
        researchRequestMapping.put("projectId", "igoProjectId");
        researchRequestMapping.put("requestId", "igoRequestId");
        researchRequestMapping.put("recipe", "genePanel");
        return researchRequestMapping;
    }

    Map<String, String> setUpClinicalSampleMapping() {
        Map<String, String> clinicalSampleMapping = new HashMap<>();
        clinicalSampleMapping.put("primary_site", "tissueLocation");
        clinicalSampleMapping.put("tumor_type_code", "oncotreeCode");
        clinicalSampleMapping.put("gene_panel", "baitSet");
        clinicalSampleMapping.put("recipe", "genePanel");
        clinicalSampleMapping.put("dmp_sample_id", "primaryId");
        clinicalSampleMapping.put("gender", "sex");
        clinicalSampleMapping.put("is_metastasis", "sampleType");
        return clinicalSampleMapping;
    }

    Map<String, String> setUpResearchSampleMapping() {
        Map<String, String> researchSampleMapping = new HashMap<>();
        researchSampleMapping.put("cmoSampleClass", "sampleType");
        researchSampleMapping.put("specimenType", "sampleClass");
        researchSampleMapping.put("oncoTreeCode", "oncotreeCode");
        researchSampleMapping.put("requestId", "igoRequestId");
        researchSampleMapping.put("recipe", "genePanel");
        researchSampleMapping.put("igoId", "primaryId");
        return researchSampleMapping;
    }
    
    Map<String, String> setUpMetastasisValueMapping() {
        Map<String, String> metastasisValueMapping = new HashMap<>();
        metastasisValueMapping.put("0", "Primary");
        metastasisValueMapping.put("1", "Metastasis");
        metastasisValueMapping.put("2", "Local Recurrence");
        metastasisValueMapping.put("127", "Unknown");
        return metastasisValueMapping;
    }

    private final ObjectMapper mapper = new ObjectMapper();

    public String transformRequestMetadata(String requestJson) throws JsonProcessingException {
        return updateMapKeyValueBasedOnMapping(requestJson, setUpResearchRequestMapping());
    }

    public String transformResearchSampleMetadata(String sampleJson) throws JsonProcessingException {
        return updateMapKeyValueBasedOnMapping(sampleJson, setUpResearchSampleMapping());
    }

    /**
     * transformClinicalSampleMetadata
     * @param sampleJson
     * @return
     * @throws JsonProcessingException
     */
    public String transformClinicalSampleMetadata(String sampleJson)
            throws JsonProcessingException {
        String transformedSampleJson = updateMapKeyValueBasedOnMapping(
                sampleJson, setUpClinicalSampleMapping());
        
        Map<String, Object> transformedSampleMetadataMap = mapper.readValue(transformedSampleJson, Map.class);
        String primaryId = mapper.writeValueAsString(transformedSampleMetadataMap.get("primaryId"));

        String sampleType = setUpMetastasisValueMapping().get(transformedSampleMetadataMap.get("sampleType"));
        transformedSampleMetadataMap.replace("sampleType", sampleType);

        String sampleClass = primaryId.matches("[ACCESS REGEX PATTERN]") ? "cfDNA" : "Tumor";
        transformedSampleMetadataMap.replace("sampleClass", sampleClass);

        String sex = transformedSampleMetadataMap.get("gender").toString().equals("0") ? "Male" : "Female";
        transformedSampleMetadataMap.replace("sex", sex);

        String tumorOrNormal = primaryId.matches("DMP NORMAL SAMPLE SUFFIX PATTERN") ? "Normal" : "Tumor";
        transformedSampleMetadataMap.replace("tumorOrNormal", tumorOrNormal);

        return transformedSampleJson;
    }

    /**
     * updateMapKeyValueBasedOnMapping
     * @param unParsedJson
     * @param universalMapping
     * @return
     * @throws JsonProcessingException
     */
    public String updateMapKeyValueBasedOnMapping(String unParsedJson,
            Map<String, String> universalMapping) throws JsonProcessingException {
        Map<String, Object> transformedSampleMetadataMap = new HashMap<>();
        Map<String, Object> rawMapValues = mapper.readValue(unParsedJson, Map.class);
        for (Map.Entry<String, Object> entry : rawMapValues.entrySet()) {
            if (universalMapping.containsKey(entry.getKey())) {
                transformedSampleMetadataMap.put(universalMapping.get(entry.getKey()), entry.getValue());
            } else {
                transformedSampleMetadataMap.put(entry.getKey(), entry.getValue());
            }
        }
        return mapper.writeValueAsString(transformedSampleMetadataMap);
    }



}
