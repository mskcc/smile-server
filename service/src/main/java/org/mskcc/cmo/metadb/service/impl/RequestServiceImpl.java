package org.mskcc.cmo.metadb.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import org.mskcc.cmo.metadb.model.MetadbProject;
import org.mskcc.cmo.metadb.model.MetadbRequest;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.RequestMetadata;
import org.mskcc.cmo.metadb.model.web.PublishedMetadbRequest;
import org.mskcc.cmo.metadb.model.web.PublishedMetadbSample;
import org.mskcc.cmo.metadb.model.web.RequestSummary;
import org.mskcc.cmo.metadb.persistence.neo4j.MetadbRequestRepository;
import org.mskcc.cmo.metadb.service.MetadbRequestService;
import org.mskcc.cmo.metadb.service.MetadbSampleService;
import org.mskcc.cmo.metadb.service.util.RequestStatusLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author ochoaa
 */
@Component
public class RequestServiceImpl implements MetadbRequestService {
    @Autowired
    private MetadbJsonComparator metadbJsonComparator;

    @Autowired
    private MetadbRequestRepository requestRepository;

    @Autowired
    private MetadbSampleService sampleService;

    @Autowired
    private RequestStatusLogger requestStatusLogger;

    // 24 hours in milliseconds
    private final Integer TIME_ADJ_24HOURS_MS = 24 * 60 * 60 * 1000;
    private Map<String, Date> loggedExistingRequests = new HashMap<>();
    private static final Log LOG = LogFactory.getLog(RequestServiceImpl.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Boolean saveRequest(MetadbRequest request) throws Exception {
        MetadbProject project = new MetadbProject();
        project.setIgoProjectId(request.getIgoProjectId());
        project.setNamespace(request.getNamespace());
        request.setMetaDbProject(project);

        MetadbRequest savedRequest = requestRepository.findRequestById(request.getIgoRequestId());
        if (savedRequest == null) {
            if (request.getMetaDbSampleList() != null) {
                List<MetadbSample> updatedSamples = new ArrayList<>();
                for (MetadbSample s: request.getMetaDbSampleList()) {
                    // considering adding the patientService.savePatient() stuff here
                    // and remove from the sample service.
                    updatedSamples.add(sampleService.saveMetadbSample(s));
                }
                request.setMetaDbSampleList(updatedSamples);
            }
            requestRepository.save(request);
            return Boolean.TRUE;
        }
        logDuplicateRequest(request);
        return Boolean.FALSE;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Boolean saveRequestMetadata(MetadbRequest request) {
        // input is already an existing metadb request so there's no need
        // to check for an existing request again - maybe something we can do
        // is compare the size of the current request metadata history and
        // compare the size after the update?
        List<RequestMetadata> currentMetadataList = requestRepository
                .findRequestMetadataHistoryById(request.getIgoRequestId());
        requestRepository.save(request);
        List<RequestMetadata> updatedMetadataList = requestRepository
                .findRequestMetadataHistoryById(request.getIgoRequestId());
        if (updatedMetadataList.size() != (currentMetadataList.size() + 1)) {
            StringBuilder builder = new StringBuilder();
            builder.append("Failed to update the Request-level metadata for request id: ")
                    .append(request.getIgoRequestId())
                    .append("\n\tTotal versions of request metadata -->\n\t\t - before save: ")
                    .append(currentMetadataList.size())
                    .append("\n\t\t - after save: ")
                    .append(updatedMetadataList.size());
            LOG.error(builder.toString());
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
     * Logs duplicate requests.
     * @param request
     * @throws IOException
     */
    private void logDuplicateRequest(MetadbRequest request) throws IOException {
        // if request has not been logged before then save request to request logger file
        // otherwise check if new timestamp occurs within 24 hours since the last time
        // the same request was seen. If it does not then save request to logger file
        Date newTimestamp = new Date();
        Boolean logRequest = Boolean.FALSE;
        if (!loggedExistingRequests.containsKey(request.getIgoRequestId())) {
            loggedExistingRequests.put(request.getIgoRequestId(), newTimestamp);
            logRequest = Boolean.TRUE;
        } else {
            // check if new timestamp occurs within 24 hours of the reference timestamp
            // if check does not pass then log the request to the logger file
            Date referenceTimestamp = loggedExistingRequests.get(request.getIgoRequestId());
            if (!timestampWithin24Hours(referenceTimestamp, newTimestamp)) {
                logRequest = Boolean.TRUE;
                loggedExistingRequests.put(request.getIgoRequestId(), newTimestamp);
            }
        }

        if (logRequest) {
            requestStatusLogger.logRequestStatus(request.getRequestJson(),
                    RequestStatusLogger.StatusType.DUPLICATE_REQUEST);
        }
    }

    @Override
    public MetadbRequest getMetadbRequestById(String requestId) throws Exception {
        MetadbRequest request = requestRepository.findRequestById(requestId);
        if (request == null) {
            return null;
        }
        // get request metadata and sample metadata for request if exists
        List<RequestMetadata> requestMetadataList =
                requestRepository.findRequestMetadataHistoryById(requestId);
        request.setRequestMetadataList(requestMetadataList);
        List<MetadbSample> metadbSampleList = sampleService.getResearchSamplesByRequestId(requestId);
        request.setMetaDbSampleList(metadbSampleList);
        return request;
    }

    @Override
    public PublishedMetadbRequest getPublishedMetadbRequestById(String requestId) throws Exception {
        MetadbRequest request = getMetadbRequestById(requestId);

        // for each metadb sample get the latest version of its sample metadata
        List<PublishedMetadbSample> samples = new ArrayList<>();
        for (MetadbSample sample : request.getMetaDbSampleList()) {
            PublishedMetadbSample publishedSample = sampleService
                    .getPublishedMetadbSample(sample.getMetaDbSampleId());
            samples.add(publishedSample);
        }
        return new PublishedMetadbRequest(request, samples);
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

    @Override
    public Boolean requestHasUpdates(MetadbRequest existingRequest, MetadbRequest request) throws Exception {
        try {
            metadbJsonComparator.isConsistent(existingRequest.getRequestJson(),
                request.getRequestJson());
        } catch (AssertionError e) {
            LOG.warn("Found discrepancies between JSONs:\n" + e.getLocalizedMessage());
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean requestHasMetadataUpdates(RequestMetadata existingRequestMetadata,
            RequestMetadata requestMetadata) throws Exception {
        String existingMetadata = mapper.writeValueAsString(existingRequestMetadata);
        String currentMetadata = mapper.writeValueAsString(requestMetadata);
        try {
            metadbJsonComparator.isConsistent(currentMetadata, existingMetadata);
        } catch (AssertionError e) {
            LOG.warn("Found discrepancies between JSONs:\n" + e.getLocalizedMessage());
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public List<MetadbSample> getRequestSamplesWithUpdates(MetadbRequest request) throws Exception {
        List<MetadbSample> updatedSamples = new ArrayList<>();
        for (MetadbSample sample: request.getMetaDbSampleList()) {
            MetadbSample existingSample = sampleService.getResearchSampleByRequestAndIgoId(
                    request.getIgoRequestId(), sample.getLatestSampleMetadata().getPrimaryId());
            // skip samples that do not already exist since they do not have a sample metadata
            // history to publish to the CMO_SAMPLE_METADATA_UPDATE topic
            if (existingSample == null) {
                continue;
            }
            // compare sample metadata from current request and the saved request
            String latestMetadata = mapper.writeValueAsString(existingSample.getLatestSampleMetadata());
            String currentMetadata = mapper.writeValueAsString(sample.getLatestSampleMetadata());

            try {
                metadbJsonComparator.isConsistent(latestMetadata, currentMetadata);
            } catch (AssertionError e) {
                LOG.warn("Found discrepancies between JSONs:\n" + e.getLocalizedMessage());
                existingSample.updateSampleMetadata(sample.getLatestSampleMetadata());
                updatedSamples.add(existingSample);
            }
        }
        return updatedSamples;
    }

    @Override
    public List<RequestSummary> getRequestsByDate(String startDate, String endDate) throws Exception {
        if (Strings.isNullOrEmpty(startDate)) {
            throw new RuntimeException("Start date " + startDate + " cannot be null or empty");
        }
        if (Strings.isNullOrEmpty(endDate)) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDateTime now = LocalDateTime.now();
            endDate = dtf.format(now);
        }
        // get formatted dates and validate inputs
        Date formattedStartDate = getFormattedDate(startDate);
        Date formattedEndDate = getFormattedDate(endDate);

        if (formattedStartDate.after(formattedEndDate)) {
            throw new RuntimeException("Start date " + startDate + " cannot occur after end date "
            + endDate);
        }

        return transformRequestSummaryResults(
                requestRepository.findRequestWithinDateRange(startDate, endDate));
    }

    @Override
    public List<RequestMetadata> getRequestMetadataHistory(String reqId) {
        return requestRepository.findRequestMetadataHistoryById(reqId);
    }

    @Override
    public MetadbRequest getRequestBySample(MetadbSample sample) throws Exception {
        return requestRepository.findRequestByResearchSample(sample);
    }

    private Date getFormattedDate(String dateString) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException("Could not parse date: " + dateString, e);
        }
    }

    private List<RequestSummary> transformRequestSummaryResults(List<List<String>> results) {
        List<RequestSummary> requestSummaryList = new ArrayList<>();
        for (List<String> result : results) {
            requestSummaryList.add(new RequestSummary(result));
        }
        return requestSummaryList;
    }
}
