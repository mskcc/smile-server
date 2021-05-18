package org.mskcc.cmo.metadb.service.impl;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.common.FileUtil;
import org.mskcc.cmo.metadb.model.MetaDbProject;
import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.model.web.PublishedMetaDbRequest;
import org.mskcc.cmo.metadb.persistence.MetaDbRequestRepository;
import org.mskcc.cmo.metadb.service.MetaDbRequestService;
import org.mskcc.cmo.metadb.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author ochoaa
 */
@Component
public class MetaDbRequestServiceImpl implements MetaDbRequestService {

    @Value("${metadb.request_handling_failures_filepath}")
    private String metadbRequestFailuresFilepath;

    @Autowired
    private MetaDbRequestRepository metaDbRequestRepository;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private FileUtil fileUtil;

    // 24 hours in milliseconds
    private final Integer TIME_ADJ_24HOURS_MS = 24 * 60 * 60 * 1000;
    private Map<String, Date> loggedExistingRequests = new HashMap<>();
    private static final Log LOG = LogFactory.getLog(MetaDbRequestServiceImpl.class);
    private static final String REQ_FAILURES_FILE_HEADER = "DATE\tREASON\tMESSAGE\n";

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean saveRequest(MetaDbRequest request) throws Exception {
        MetaDbProject project = new MetaDbProject();
        project.setProjectId(request.getProjectId());
        project.setNamespace(request.getNamespace());
        request.setMetaDbProject(project);

        MetaDbRequest savedRequest = metaDbRequestRepository.findMetaDbRequestById(request.getRequestId());
        if (savedRequest == null) {
            if (request.getMetaDbSampleList() != null) {
                List<MetaDbSample> updatedSamples = new ArrayList<>();
                for (MetaDbSample s: request.getMetaDbSampleList()) {
                    updatedSamples.add(sampleService.saveSampleMetadata(s));
                }
                request.setMetaDbSampleList(updatedSamples);
            }
            metaDbRequestRepository.save(request);
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
                File requestFailureFile = fileUtil.getOrCreateFileWithHeader(
                        metadbRequestFailuresFilepath, REQ_FAILURES_FILE_HEADER);
                fileUtil.writeToFile(requestFailureFile,
                        generateRequestFailureRecord("Request already exists", request.getRequestJson()));
            }
            return false;
        }
    }

    /**
     * Generates record to write to publishing failure file.
     * @param reason
     * @param message
     * @return String
     */
    private String generateRequestFailureRecord(String reason, String message) {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        StringBuilder builder = new StringBuilder();
        builder.append(currentDate)
                .append("\t")
                .append(reason)
                .append("\t")
                .append(message)
                .append("\n");
        return builder.toString();
    }

    @Override
    public PublishedMetaDbRequest getMetaDbRequest(String requestId) throws Exception {
        MetaDbRequest metaDbRequest = metaDbRequestRepository.findMetaDbRequestById(requestId);
        if (metaDbRequest == null) {
            LOG.error("Couldn't find a request with requestId " + requestId);
            return null;
        }
        List<SampleMetadata> samples = new ArrayList<>();
        for (MetaDbSample metaDbSample: metaDbRequestRepository.findAllMetaDbSamplesByRequest(requestId)) {
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
}
