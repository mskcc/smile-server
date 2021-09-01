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
    private static final Log LOG = LogFactory.getLog(MetaDbRequestServiceImpl.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean saveRequest(MetaDbRequest request) throws Exception {
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
        } else {
            // determine whether there are changes in the current request that are not
            // in the existing request
            if (metadbJsonComparator.isConsistent(savedRequest.getRequestJson(), request.getRequestJson())) {
                // consistent jsons indicate there are no new updates to persist to the graph db
                logDuplicateRequest(request);
                return Boolean.FALSE;
            } else {
                // update the saved request with changes in the current request
                savedRequest.updateRequestMetadata(request);
                savedRequest.addRequestMetadata(requestMetadata);

                // check consistency for each sample in request samples list
                // TODO: how to keep track of what samples are updated for the message handler to
                // publish to CMO_SAMPLE_METADATA_UPDATE && publish request metadata history
                // to CMO_REQUEST_METADATA_UPDATE
                //   --> some ideas: in message handler check if request
                //       exists already before persiting any udpates
                List<MetaDbSample> updatedSamples = new ArrayList<>();
                for (MetaDbSample s: request.getMetaDbSampleList()) {
                    MetaDbSample savedSample = sampleService.getMetaDbSampleByRequestAndIgoId(
                            savedRequest.getRequestId(), s.getSampleIgoId().getSampleId());
                    // compare sample metadata from current request and the saved request
                    String latestMetadata = mapper.writeValueAsString(savedSample.getLatestSampleMetadata());
                    String currentMetadata = mapper.writeValueAsString(s.getLatestSampleMetadata());
                    if (!metadbJsonComparator.isConsistent(latestMetadata, currentMetadata)) {
                        // differences detected indicates we need to save these updates
                        savedSample.updateSampleMetadata(s);
                        sampleService.saveSampleMetadata(savedSample); // persist updates
                    }
                    updatedSamples.add(savedSample);
                }
                savedRequest.setMetaDbSampleList(updatedSamples);
                requestRepository.save(savedRequest);
                return Boolean.TRUE;
            }
        }
    }

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
    public PublishedMetaDbRequest getMetaDbRequest(String requestId) throws Exception {
        MetaDbRequest metaDbRequest = requestRepository.findMetaDbRequestById(requestId);
        if (metaDbRequest == null) {
            LOG.error("Couldn't find a request with requestId " + requestId);
            return null;
        }
        List<SampleMetadata> samples = new ArrayList<>();
        for (MetaDbSample metaDbSample: sampleService.getAllMetadbSamplesByRequestId(requestId)) {
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

}
