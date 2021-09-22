package org.mskcc.cmo.metadb.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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
import org.mskcc.cmo.common.MetadbJsonComparator;
import org.mskcc.cmo.metadb.model.MetaDbProject;
import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.RequestMetadata;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.model.web.PublishedMetaDbRequest;
import org.mskcc.cmo.metadb.persistence.MetaDbRequestRepository;
import org.mskcc.cmo.metadb.service.MetadbRequestService;
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
public class MetadbRequestServiceImpl implements MetadbRequestService {
    @Autowired
    private MetadbJsonComparator metadbJsonComparator;

    @Autowired
    private MetaDbRequestRepository requestRepository;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private RequestStatusLogger requestStatusLogger;

    // 24 hours in milliseconds
    private final Integer TIME_ADJ_24HOURS_MS = 24 * 60 * 60 * 1000;
    private Map<String, Date> loggedExistingRequests = new HashMap<>();
    private static final Log LOG = LogFactory.getLog(MetadbRequestServiceImpl.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Boolean saveRequest(MetaDbRequest request) throws Exception {
        MetaDbProject project = new MetaDbProject();
        project.setProjectId(request.getProjectId());
        project.setNamespace(request.getNamespace());
        RequestMetadata requestMetadata = extractRequestMetadata(request.getRequestJson());
        request.setMetaDbProject(project);
        request.addRequestMetadata(requestMetadata);

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
            return Boolean.TRUE;
        }
        logDuplicateRequest(request);
        return Boolean.FALSE;
    }

    /**
     * Logs duplicate requests.
     * @param request
     * @throws IOException
     */
    private void logDuplicateRequest(MetaDbRequest request) throws IOException {
        // if request has not been logged before then save request to request logger file
        // otherwise check if new timestamp occurs within 24 hours since the last time
        // the same request was seen. If it does not then save request to logger file
        Date newTimestamp = new Date();
        Boolean logRequest = Boolean.FALSE;
        if (!loggedExistingRequests.containsKey(request.getRequestId())) {
            loggedExistingRequests.put(request.getRequestId(), newTimestamp);
            logRequest = Boolean.TRUE;
        } else {
            // check if new timestamp occurs within 24 hours of the reference timestamp
            // if check does not pass then log the request to the logger file
            Date referenceTimestamp = loggedExistingRequests.get(request.getRequestId());
            if (!timestampWithin24Hours(referenceTimestamp, newTimestamp)) {
                logRequest = Boolean.TRUE;
                loggedExistingRequests.put(request.getRequestId(), newTimestamp);
            }
        }

        if (logRequest) {
            requestStatusLogger.logRequestStatus(request.getRequestJson(),
                    RequestStatusLogger.StatusType.DUPLICATE_REQUEST);
        }
    }

    @Override
    public MetaDbRequest getMetadbRequestById(String requestId) throws Exception {
        MetaDbRequest request = requestRepository.findMetaDbRequestById(requestId);
        if (request == null) {
            LOG.error("Couldn't find a request with requestId " + requestId);
            return null;
        }
        List<MetaDbSample> metadbSampleList = sampleService.getAllMetadbSamplesByRequestId(requestId);
        request.setMetaDbSampleList(metadbSampleList);
        return request;
    }

    @Override
    public PublishedMetaDbRequest getPublishedMetadbRequestById(String requestId) throws Exception {
        MetaDbRequest request = getMetadbRequestById(requestId);

        // for each metadb sample get the latest version of its sample metadata
        List<SampleMetadata> samples = new ArrayList<>();
        for (MetaDbSample sample : request.getMetaDbSampleList()) {
            samples.add(sample.getLatestSampleMetadata());
        }
        return new PublishedMetaDbRequest(request, samples);
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
        Map<String, String> requestMetadataMap = mapper.readValue(requestMetadataJson, Map.class);
        // remove samples if present for request metadata
        if (requestMetadataMap.containsKey("samples")) {
            requestMetadataMap.remove("samples");
        }
        RequestMetadata requestMetadata = new RequestMetadata(
                requestMetadataMap.get("requestId"),
                mapper.writeValueAsString(requestMetadataMap),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        return requestMetadata;
    }

    @Override
    public Boolean requestHasUpdates(MetaDbRequest existingRequest, MetaDbRequest request) throws Exception {
        return !metadbJsonComparator.isConsistent(existingRequest.getRequestJson(),
                request.getRequestJson());
    }

    @Override
    public Boolean requestHasMetadataUpdates(RequestMetadata existingRequestMetadata,
            RequestMetadata requestMetadata) throws Exception {
        String existingMetadata = mapper.writeValueAsString(existingRequestMetadata);
        String currentMetadata = mapper.writeValueAsString(requestMetadata);
        return (!metadbJsonComparator.isConsistent(currentMetadata, existingMetadata));
    }

    @Override
    public List<MetaDbSample> getRequestSamplesWithUpdates(MetaDbRequest request) throws Exception {
        List<MetaDbSample> updatedSamples = new ArrayList<>();
        for (MetaDbSample sample: request.getMetaDbSampleList()) {
            MetaDbSample existingSample = sampleService.getMetaDbSampleByRequestAndAlias(
                    request.getRequestId(), sample.getSampleIgoId());
            // skip samples that do not already exist since they do not have a sample metadata
            // history to publish to the CMO_SAMPLE_METADATA_UPDATE topic
            if (existingSample == null) {
                continue;
            }
            // compare sample metadata from current request and the saved request
            String latestMetadata = mapper.writeValueAsString(existingSample.getLatestSampleMetadata());
            String currentMetadata = mapper.writeValueAsString(sample.getLatestSampleMetadata());
            if (!metadbJsonComparator.isConsistent(latestMetadata, currentMetadata)) {
                // differences detected indicates we need to save these updates
                existingSample.updateSampleMetadata(sample.getLatestSampleMetadata());
                updatedSamples.add(existingSample);
            }
        }
        return updatedSamples;
    }

    @Override
    public List<RequestMetadata> getRequestMetadataHistoryByRequestId(String reqId) {
        return requestRepository.getRequestMetadataHistoryByRequestId(reqId);
    }

}
