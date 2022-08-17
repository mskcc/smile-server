package org.mskcc.smile.service.util;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.apache.commons.lang.StringUtils;
import org.mskcc.smile.commons.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Class for logging request statuses to the provided
 * Request Handling filepath.
 * @author ochoaa
 */
@Component
public class RequestStatusLogger {
    @Value("${smile.request_handling_failures_filepath}")
    private String smileRequestFailuresFilepath;

    @Autowired
    private FileUtil fileUtil;

    private File requestStatusLoggerFile;

    private static final String[] REQUEST_LOGGER_FILE_HEADER = new String[]{"DATE", "STATUS", "MESSAGE"};

    /**
     * Request StatusType descriptions:
     * - DUPLICATE_REQUEST: a request which already exists in the graph db
     * - REQUEST_WITH_MISSING_SAMPLES: a request that came in with no sample metadata
     * - CMO_REQUEST_MISSING_REQ_FIELDS: a CMO request with sample metadata
     *        that is missing required fields (cmoPatientId, baitSet)
     * - REQUEST_PARSING_ERROR: json parsing exception thrown
     * - CMO_REQUEST_FILTER_SKIPPED_REQUEST: applies if smile server is running
     *        with the cmoRequestFilter enabled and a non-cmo request is encountered
     */
    public enum StatusType {
        DUPLICATE_REQUEST,
        REQUEST_WITH_MISSING_SAMPLES,
        CMO_REQUEST_MISSING_REQ_FIELDS,
        REQUEST_PARSING_ERROR,
        CMO_REQUEST_FILTER_SKIPPED_REQUEST
    }

    /**
     * Writes request contents and status to the request status logger file.
     * @param message
     * @param status
     * @throws IOException
     */
    public void logRequestStatus(String message, StatusType status) throws IOException {
        if (requestStatusLoggerFile ==  null) {
            this.requestStatusLoggerFile = fileUtil.getOrCreateFileWithHeader(smileRequestFailuresFilepath,
                    StringUtils.join(REQUEST_LOGGER_FILE_HEADER, "\t") + "\n");
        }
        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        StringBuilder builder = new StringBuilder();
        builder.append(currentDate)
                .append("\t")
                .append(status.toString())
                .append("\t")
                .append(message)
                .append("\n");
        fileUtil.writeToFile(requestStatusLoggerFile, builder.toString());
    }
}
