package org.mskcc.cmo.metadb.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.metadb.model.MetaDbProject;
import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.RequestMetadata;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.model.web.PublishedMetaDbRequest;
import org.mskcc.cmo.metadb.persistence.MetaDbRequestRepository;
import org.mskcc.cmo.metadb.persistence.MetaDbSampleRepository;
import org.mskcc.cmo.metadb.service.MetaDbRequestService;
import org.mskcc.cmo.metadb.service.SampleService;
import org.mskcc.cmo.metadb.service.util.RequestStatusLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author ochoaa
 */
@Component
public class MetaDbRequestServiceImpl implements MetaDbRequestService {

    @Autowired
    private MetaDbRequestRepository requestRepository;

    @Autowired
    private MetaDbSampleRepository sampleRepository;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private RequestStatusLogger requestStatusLogger;

    // 24 hours in milliseconds
    private final Integer TIME_ADJ_24HOURS_MS = 24 * 60 * 60 * 1000;
    private Map<String, Date> loggedExistingRequests = new HashMap<>();
    private static final Log LOG = LogFactory.getLog(MetaDbRequestServiceImpl.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean saveRequest(MetaDbRequest request) throws Exception {
        MetaDbProject project = new MetaDbProject();
        project.setProjectId(request.getProjectId());
        project.setNamespace(request.getNamespace());
        request.setMetaDbProject(project);

        request.addRequestMetadata(extractRequestMetadata(request.getRequestJson()));

        MetaDbRequest savedRequest = requestRepository.findMetaDbRequestById(request.getRequestId());
        if (savedRequest == null) {
            if (request.getMetaDbSampleList() != null) {
                List<MetaDbSample> updatedSamples = new ArrayList<>();
                for (MetaDbSample s: request.getMetaDbSampleList()) {
                    updatedSamples.add(sampleService.saveSampleMetadata(s));
                }
                request.setMetaDbSampleList(updatedSamples);
            }
            requestRepository.save(request);
            return true;
        } else {
            // if request has not been logged before then save request to request logger file
            // otherwise check if new timestamp occurs within 24 hours since the last time
            // the same request was seen. If it does not then save request to logger file
            Date newTimestamp = new Date();
            Boolean logRequest = Boolean.FALSE;
            if (!loggedExistingRequests.containsKey(savedRequest.getRequestId())) {
                loggedExistingRequests.put(savedRequest.getRequestId(), newTimestamp);
                logRequest = Boolean.TRUE;
            } else {
                // check if new timestamp occurs within 24 hours of the reference timestamp
                // if check does not pass then log the request to the logger file
                Date referenceTimestamp = loggedExistingRequests.get(savedRequest.getRequestId());
                if (!timestampWithin24Hours(referenceTimestamp, newTimestamp)) {
                    logRequest = Boolean.TRUE;
                    loggedExistingRequests.put(savedRequest.getRequestId(), newTimestamp);
                }
            }

            if (logRequest) {
                requestStatusLogger.logRequestStatus(request.getRequestJson(),
                        RequestStatusLogger.StatusType.DUPLICATE_REQUEST);
            }
            return false;
        }
    }

    @Override
    public PublishedMetaDbRequest getMetaDbRequest(String requestId) throws Exception {
        MetaDbRequest metaDbRequest = requestRepository.findMetaDbRequestById(requestId);
        if (metaDbRequest == null) {
            LOG.error("Couldn't find a request with requestId " + requestId);
            return null;
        }
        List<SampleMetadata> samples = new ArrayList<>();
        for (MetaDbSample metaDbSample: sampleRepository.findAllMetaDbSamplesByRequest(requestId)) {
            samples.addAll(sampleService.getMetaDbSample(metaDbSample.getMetaDbSampleId())
                    .getSampleMetadataList());
        }
        return new PublishedMetaDbRequest(metaDbRequest, samples);
    }

    /**
     * Returns true if new timestamp occurs within 24 hours of the reference timestamp.
     * @param referenceTimestamp
     * @param newTimestamp
     * @return Boolean
     */
    private Boolean timestampWithin24Hours(Date referenceTimestamp, Date newTimestamp) {
        // create reference timestamp shifted 24 hours in milliseconds
        Calendar adjustedReferenceTimestamp = Calendar.getInstance();
        adjustedReferenceTimestamp.setTime(referenceTimestamp);
        adjustedReferenceTimestamp.add(Calendar.MILLISECOND, TIME_ADJ_24HOURS_MS);
        return newTimestamp.before(adjustedReferenceTimestamp.getTime());
    }
    
    private RequestMetadata extractRequestMetadata(String requestMetadataJson)
            throws JsonMappingException, JsonProcessingException {
        Map<String, String> requestMetadataMap = mapper.readValue(requestMetadataJson.toString(), Map.class);
        requestMetadataMap.remove("samples");
        RequestMetadata requestMetadata = new RequestMetadata(
                mapper.convertValue(requestMetadataMap, String.class),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        return requestMetadata;
    }
    
    private String updateRequestJson(String requestMetadataJson, String currentRequestJson)
            throws JsonMappingException, JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        Map<String, String> currentRequestMap = mapper.readValue(currentRequestJson.toString(), Map.class);
        Map<String, String> updatedRequestMap = mapper.readValue(requestMetadataJson.toString(), Map.class);
         
        for (Map.Entry<String,String> entry : updatedRequestMap.entrySet()) {
            currentRequestMap.replace(entry.getKey(), entry.getValue());
        }
        
        return mapper.writeValueAsString(currentRequestJson);
    }
    
    private void updateRequestMetadata(String requestMetadataJson)
            throws JsonMappingException, JsonProcessingException {    
        //UPDATE THE CURRENT METADATA
        RequestMetadata requestMetadata = new RequestMetadata(requestMetadataJson,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        
        //CREATE A NEW NODE WITH JSON
        Map<String, String> map = mapper.readValue(requestMetadataJson, Map.class);
        MetaDbRequest savedRequest = requestRepository.findMetaDbRequestById(map.get("requestId").toString());
        savedRequest.updateRequestMetadata(map);
        savedRequest.setRequestJson(updateRequestJson(requestMetadataJson, savedRequest.getRequestJson()));
        
        //PERSIST NEW NODE TO NEO4J, UPDATE THE LINK BETWEEN REQUESTMETADATA
    }
}
