package org.mskcc.smile.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.mskcc.smile.model.RequestMetadata;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.Status;
import org.mskcc.smile.model.igo.IgoRequest;
import org.mskcc.smile.model.igo.IgoSampleManifest;

/**
 *
 * @author ochoaa
 */
public class RequestDataFactory {
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Method factory returns an instance of SmileRequest built from
     * request JSON string.
     * @param requestJson
     * @return SmileRequest
     * @throws JsonProcessingException
     */
    public static SmileRequest buildNewLimsRequestFromJson(String requestJson)
            throws JsonProcessingException {
        IgoRequest igoRequest = mapper.readValue(requestJson, IgoRequest.class);
        SmileRequest request = new SmileRequest(igoRequest);
        request.setSmileSampleList(extractSmileSamplesFromIgoResponse(requestJson));
        // creates and inits request metadata
        request.addRequestMetadata(extractRequestMetadataFromJson(requestJson));
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

    private static List<SmileSample> extractSmileSamplesFromIgoResponse(Object message)
            throws JsonProcessingException {
        Map<String, Object> map = mapper.readValue(message.toString(), Map.class);
        List<Object> sampleManifests =
                Arrays.asList(mapper.convertValue(map.get("samples"),
                        Object[].class));
        String requestId = (String) map.get("requestId");
        Boolean isCmoRequest = (Boolean) map.get("isCmoRequest");

        List<SmileSample> requestSamplesList = new ArrayList<>();
        for (Object s : sampleManifests) {
            Map<String, Object> sampleMap = mapper.convertValue(s, Map.class);
            Map<String, Object> sampleStatusMap = mapper.convertValue(
                                    sampleMap.get("status"), Map.class);
            Status sampleStatus = new Status(Boolean.valueOf(
                                    sampleStatusMap.get("validationStatus").toString()),
                                    sampleStatusMap.get("validationReport").toString());

            IgoSampleManifest sampleManifest = mapper.convertValue(s,
                                    IgoSampleManifest.class);
            SmileSample sample = SampleDataFactory
                    .buildNewResearchSampleFromMetadata(requestId,
                            sampleManifest, isCmoRequest, sampleStatus);

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
        Object requestId = requestMetadataMap.containsKey("requestId")
                ? requestMetadataMap.get("requestId") : requestMetadataMap.get("igoRequestId");

        RequestMetadata requestMetadata = new RequestMetadata(
                requestId.toString(),
                mapper.writeValueAsString(requestMetadataMap),
                Instant.now().toEpochMilli());
        requestMetadata.setStatus(extractStatusFromJson(requestMetadataJson));
        return requestMetadata;
    }

    private static Status extractStatusFromJson(String inputJson)
            throws JsonMappingException, JsonProcessingException {
        Status status = new Status();
        Map<String, Object> jsonMap = mapper.readValue(inputJson, Map.class);
        if (jsonMap.containsKey("status")) {
            Map<String,Object> statusJsonMap = mapper.convertValue(jsonMap.get("status"), Map.class);
            status = new Status(statusJsonMap);
        }
        return status;
    }
}
