package org.mskcc.smile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import java.io.IOException;
import java.text.DateFormat;
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
import org.mskcc.smile.commons.JsonComparator;
import org.mskcc.smile.model.RequestMetadata;
import org.mskcc.smile.model.SampleMetadata;
import org.mskcc.smile.model.SmileProject;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.web.PublishedSmileRequest;
import org.mskcc.smile.model.web.PublishedSmileSample;
import org.mskcc.smile.model.web.RequestSummary;
import org.mskcc.smile.persistence.neo4j.SmileRequestRepository;
import org.mskcc.smile.service.SmileRequestService;
import org.mskcc.smile.service.SmileSampleService;
import org.mskcc.smile.service.util.RequestStatusLogger;
import org.mskcc.smile.service.util.SampleDataFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author ochoaa
 */
@Component
public class RequestServiceImpl implements SmileRequestService {
    @Autowired
    private JsonComparator jsonComparator;

    @Autowired
    private SmileRequestRepository requestRepository;

    @Autowired
    private SmileSampleService sampleService;

    @Autowired
    private RequestStatusLogger requestStatusLogger;

    private final DateFormat IMPORT_DATE_FORMATTER = initDateFormatter();
    // 24 hours in milliseconds
    private final Integer TIME_ADJ_24HOURS_MS = 24 * 60 * 60 * 1000;
    private Map<String, Date> loggedExistingRequests = new HashMap<>();
    private static final Log LOG = LogFactory.getLog(RequestServiceImpl.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Boolean saveRequest(SmileRequest request) throws Exception {
        SmileProject project = new SmileProject();
        project.setIgoProjectId(request.getIgoProjectId());
        project.setNamespace(request.getNamespace());
        request.setSmileProject(project);

        SmileRequest savedRequest = requestRepository.findRequestById(request.getIgoRequestId());
        if (savedRequest == null) {
            if (request.getSmileSampleList() != null) {
                List<SmileSample> updatedSamples = new ArrayList<>();
                for (SmileSample s: request.getSmileSampleList()) {
                    // considering adding the patientService.savePatient() stuff here
                    // and remove from the sample service.
                    updatedSamples.add(sampleService.saveSmileSample(s));
                }
                request.setSmileSampleList(updatedSamples);
            }
            requestRepository.save(request);
            return Boolean.TRUE;
        }
        logDuplicateRequest(request);
        return Boolean.FALSE;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Boolean saveRequestMetadata(SmileRequest request) {
        // input is already an existing smile request so there's no need
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
    private void logDuplicateRequest(SmileRequest request) throws IOException {
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
    public SmileRequest getSmileRequestById(String requestId) throws Exception {
        SmileRequest request = requestRepository.findRequestById(requestId);
        if (request == null) {
            return null;
        }
        // get request metadata and sample metadata for request if exists
        List<RequestMetadata> requestMetadataList =
                requestRepository.findRequestMetadataHistoryById(requestId);
        request.setRequestMetadataList(requestMetadataList);
        List<SmileSample> smileSampleList = sampleService.getResearchSamplesByRequestId(requestId);
        request.setSmileSampleList(smileSampleList);
        return request;
    }

    @Override
    public PublishedSmileRequest getPublishedSmileRequestById(String requestId) throws Exception {
        SmileRequest request = getSmileRequestById(requestId);

        // for each smile sample get the latest version of its sample metadata
        List<PublishedSmileSample> samples = new ArrayList<>();
        for (SmileSample sample : request.getSmileSampleList()) {
            PublishedSmileSample publishedSample = sampleService
                    .getPublishedSmileSample(sample.getSmileSampleId());
            // add request id and cmo indicator to sample's additional properties
            publishedSample.addAdditionalProperty("igoRequestId", requestId);
            publishedSample.addAdditionalProperty("isCmoSample", String.valueOf(request.getIsCmoRequest()));
            samples.add(publishedSample);
        }
        return new PublishedSmileRequest(request, samples);
    }

    @Override
    public Boolean updateRequestMetadata(RequestMetadata newRequest) throws Exception {
        SmileRequest existingRequest = getSmileRequestById(newRequest.getIgoRequestId());
        // Request doesn't exist in db
        if (existingRequest == null) {
            LOG.warn("Request does not already exist in the database: "
                    + newRequest.getIgoRequestId()
                    + " - will not be persisting updates.");
            return Boolean.FALSE;
        // Request Metadata has updates
        } else if (requestHasMetadataUpdates(existingRequest.getLatestRequestMetadata(), newRequest)) {
            LOG.info("Found updates in request metadata: " + newRequest.getIgoRequestId()
                + " - persisting to database");
            existingRequest.updateRequestMetadataByMetadata(newRequest);
            saveRequestMetadata(existingRequest);
            return Boolean.TRUE;
        // Request Metadata has no updates
        } else {
            LOG.warn("Request already exists in database and no updates were detected - "
                    + "it will not be saved: " + newRequest.getIgoRequestId());
            return Boolean.FALSE;
        }
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
    public Boolean requestHasUpdates(SmileRequest existingRequest, SmileRequest request) throws Exception {
        try {
            jsonComparator.isConsistent(existingRequest.getRequestJson(),
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
            jsonComparator.isConsistent(currentMetadata, existingMetadata);
        } catch (AssertionError e) {
            LOG.warn("Found discrepancies between JSONs:\n" + e.getLocalizedMessage());
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public List<SmileSample> getRequestSamplesWithUpdates(SmileRequest request) throws Exception {
        List<SmileSample> updatedSamples = new ArrayList<>();
        for (SmileSample sample: request.getSmileSampleList()) {
            SmileSample existingSample = sampleService.getResearchSampleByRequestAndIgoId(
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
                jsonComparator.isConsistent(latestMetadata, currentMetadata);
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
    public SmileRequest getRequestBySample(SmileSample sample) throws Exception {
        return requestRepository.findRequestByResearchSample(sample);
    }

    private Date getFormattedDate(String dateString) {
        try {
            return IMPORT_DATE_FORMATTER.parse(dateString);
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

    private DateFormat initDateFormatter() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df.setLenient(Boolean.FALSE);
        return df;
    }
}
