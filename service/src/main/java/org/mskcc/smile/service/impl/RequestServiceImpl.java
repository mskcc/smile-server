package org.mskcc.smile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.smile.commons.JsonComparator;
import org.mskcc.smile.model.RequestMetadata;
import org.mskcc.smile.model.SmileProject;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.web.PublishedSmileRequest;
import org.mskcc.smile.model.web.PublishedSmileSample;
import org.mskcc.smile.model.web.RequestSummary;
import org.mskcc.smile.persistence.neo4j.SmileRequestRepository;
import org.mskcc.smile.service.SmileRequestService;
import org.mskcc.smile.service.SmileSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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

    @Autowired @Lazy // prevents circular dependencies and initializes when component is first needed
    private SmileSampleService sampleService;

    private final DateFormat IMPORT_DATE_FORMATTER = initDateFormatter();
    private static final Log LOG = LogFactory.getLog(RequestServiceImpl.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Boolean saveRequest(SmileRequest request) throws Exception {
        SmileProject project = new SmileProject();
        project.setIgoProjectId(request.getIgoProjectId());
        project.setNamespace(request.getNamespace());
        request.setSmileProject(project);

        SmileRequest savedRequest = getSmileRequestById(request.getIgoRequestId());
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
                .findRequestMetadataHistoryByRequestId(request.getIgoRequestId());
        requestRepository.save(request);
        List<RequestMetadata> updatedMetadataList = requestRepository
                .findRequestMetadataHistoryByRequestId(request.getIgoRequestId());
        if (updatedMetadataList.size() != (currentMetadataList.size() + 1)) {
            StringBuilder builder = new StringBuilder();
            builder.append("Failed to update the Request-level metadata for request id: ")
                    .append(request.getIgoRequestId())
                    .append(" - Total versions of request metadata  - before save: ")
                    .append(currentMetadataList.size())
                    .append(" - after save: ")
                    .append(updatedMetadataList.size());
            LOG.error(builder.toString());
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    @Override
    public SmileRequest getSmileRequestById(String requestId) throws Exception {
        SmileRequest request = requestRepository.findRequestById(requestId);
        if (request == null) {
            return null;
        }
        List<SmileSample> smileSampleList = sampleService.getResearchSamplesByRequestId(requestId);
        request.setSmileSampleList(smileSampleList);
        return request;
    }

    @Override
    public PublishedSmileRequest getPublishedSmileRequestById(String requestId) throws Exception {
        SmileRequest request = getSmileRequestById(requestId);
        if (request == null) {
            return null;
        }

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
    @Transactional(rollbackFor = {Exception.class})
    public Boolean updateRequestMetadata(RequestMetadata requestMetadata, Boolean fromLims) throws Exception {
        SmileRequest existingRequest = getSmileRequestById(requestMetadata.getIgoRequestId());
        if (existingRequest == null) {
            LOG.error("Cannot persist updates to a request that does not already exist: "
                    + requestMetadata.getIgoRequestId());
            return Boolean.FALSE;
        }
        if (requestHasMetadataUpdates(existingRequest.getLatestRequestMetadata(),
                requestMetadata, fromLims)) {
            if (fromLims) {
                LOG.info("Persisting igo property updates for request: " + existingRequest.getIgoRequestId());
                existingRequest.applyIgoRequestMetadataUpdates(requestMetadata);
            } else {
                LOG.info("Persisting updates for request: " + existingRequest.getIgoRequestId());
                existingRequest.updateRequestMetadataByMetadata(requestMetadata);
            }
            saveRequestMetadata(existingRequest);
            return Boolean.TRUE;
        }
        LOG.warn("No updates to persist for request: " + existingRequest.getIgoRequestId());
        return Boolean.FALSE;
    }

    @Override
    public Boolean requestHasUpdates(SmileRequest existingRequest, SmileRequest request,
            Boolean fromLims) throws Exception {
        if (fromLims) {
            return !jsonComparator.isConsistentByIgoProperties(mapper.writeValueAsString(existingRequest),
                    mapper.writeValueAsString(request));
        }
        return !jsonComparator.isConsistent(mapper.writeValueAsString(existingRequest),
                mapper.writeValueAsString(request));
    }

    @Override
    public Boolean requestHasMetadataUpdates(RequestMetadata existingRequestMetadata,
            RequestMetadata requestMetadata, Boolean fromLims) throws Exception {
        // if request is  from LIMS, look for updates by igo properties
        if (fromLims) {
            return !jsonComparator.isConsistentByIgoProperties(
                    existingRequestMetadata.getRequestMetadataJson(),
                    requestMetadata.getRequestMetadataJson());
        }
        // if request is not from LIMS, look for updates by all properties
        return !jsonComparator.isConsistent(existingRequestMetadata.getRequestMetadataJson(),
                requestMetadata.getRequestMetadataJson());
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
            Boolean sampleHasUpdates =
                    sampleService.sampleHasMetadataUpdates(existingSample.getLatestSampleMetadata(),
                    sample.getLatestSampleMetadata(), Boolean.TRUE, Boolean.FALSE);
            if (sampleHasUpdates) {
                existingSample.updateSampleMetadata(sample.getLatestSampleMetadata());
                updatedSamples.add(existingSample);
            }
        }
        return updatedSamples;
    }

    @Override
    public List<RequestSummary> getRequestsByDate(String startDate, String endDate) throws Exception {
        if (StringUtils.isBlank(startDate)) {
            throw new RuntimeException("Start date " + startDate + " cannot be null or empty");
        }
        if (StringUtils.isBlank(endDate)) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDateTime now = LocalDateTime.now().plusDays(1L); // inclusive of whole day
            endDate = dtf.format(now);
        }
        // get formatted dates and validate inputs
        Date formattedStartDate = getFormattedDate(startDate);
        Date formattedEndDate = getFormattedDate(endDate);

        if (formattedStartDate.after(formattedEndDate)) {
            throw new RuntimeException("Start date " + startDate + " cannot occur after end date "
            + endDate);
        }
        List<Object> requests = requestRepository.findRequestWithinDateRange(
                formattedStartDate.toInstant().toEpochMilli(), formattedEndDate.toInstant().toEpochMilli());
        return Arrays.asList(mapper.convertValue(requests, RequestSummary[].class));
    }

    @Override
    public List<RequestMetadata> getRequestMetadataHistory(String reqId) {
        return requestRepository.findRequestMetadataHistoryByRequestId(reqId);
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

    private DateFormat initDateFormatter() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df.setLenient(Boolean.FALSE);
        return df;
    }
}
