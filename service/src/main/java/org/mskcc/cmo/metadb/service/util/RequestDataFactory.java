package org.mskcc.cmo.metadb.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.mskcc.cmo.metadb.model.MetadbRequest;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.RequestMetadata;
import org.mskcc.cmo.metadb.model.SampleMetadata;

/**
 *
 * @author ochoaa
 */
public class RequestDataFactory {
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Method factory returns an instance of MetadbRequest built from
     * request JSON string.
     * @param requestJson
     * @return MetadbRequest
     * @throws JsonProcessingException
     */
    public static MetadbRequest buildNewLimsRequestFromJson(String requestJson)
            throws JsonProcessingException {
        MetadbRequest request = mapper.readValue(requestJson,
                MetadbRequest.class);
        request.setRequestJson(requestJson);
        request.setMetaDbSampleList(extractMetadbSamplesFromIgoResponse(requestJson));
        request.setNamespace("igo");
        // creates and inits request metadata
        request.addRequestMetadata(extractRequestMetadataFromJson(requestJson));
        return request;
    }

    /**
     * Method factory returns an instance of MetadbRequest built from
     * an instance of RequestMetadata;
     * @param requestMetadata
     * @return MetadbRequest
     * @throws JsonProcessingException
     */
    public static MetadbRequest buildNewRequestFromMetadata(RequestMetadata requestMetadata)
            throws JsonProcessingException {
        MetadbRequest request = new MetadbRequest();
        request.updateRequestMetadataByMetadata(requestMetadata);
        return request;
    }

    /**
     * Method factory returns an instance of RequestMetadata built from
     * request metadata JSON string.
     * @param requestMetadataJson
     * @return RequestMetadata
     * @throws JsonProcessingException
     */
    public static RequestMetadata buildNewRequestMetadataFromMetadata(String requestMetadataJson)
            throws JsonProcessingException {
        return extractRequestMetadataFromJson(requestMetadataJson);
    }

    private static List<MetadbSample> extractMetadbSamplesFromIgoResponse(Object message)
            throws JsonProcessingException {
        Map<String, String> map = mapper.readValue(message.toString(), Map.class);
        String[] samples = mapper.convertValue(map.get("samples"),
                String[].class);
        String requestId = (String) map.get("requestId");

        List<MetadbSample> requestSamplesList = new ArrayList<>();
        for (String s: samples) {
            //String jsonString = new JSONObject(s).toString();
            //String sampleMetadataJson = new ObjectMapper().writeValueAsString(s);
            
            DataTransformer dataTransformer = new DataTransformer();
            String processedSampleMetadataJson = dataTransformer.transformResearchSampleMetadata(s);
            
            SampleMetadata sampleMetadata = mapper.convertValue(processedSampleMetadataJson, SampleMetadata.class);
            MetadbSample sample = SampleDataFactory.buildNewResearchSampleFromMetadata(requestId, sampleMetadata);
            requestSamplesList.add(sample);
        }
        return requestSamplesList;
    }

    private static RequestMetadata extractRequestMetadataFromJson(String requestMetadataJson)
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
