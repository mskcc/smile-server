package org.mskcc.smile.service;

import java.util.List;
import org.mskcc.smile.model.RequestMetadata;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.web.PublishedSmileRequest;
import org.mskcc.smile.model.web.RequestSummary;

/**
 *
 * @author ochoaa
 */
public interface SmileRequestService {
    Boolean saveRequest(SmileRequest request) throws Exception;
    Boolean saveRequestMetadata(SmileRequest request);
    SmileRequest getSmileRequestById(String requestId) throws Exception;
    PublishedSmileRequest getPublishedSmileRequestById(String requestId) throws Exception;
    Boolean requestHasUpdates(SmileRequest existingRequest, SmileRequest request) throws Exception;
    Boolean requestHasMetadataUpdates(RequestMetadata existingRequestMetadata,
            RequestMetadata requestMetadata) throws Exception;
    List<SmileSample> getRequestSamplesWithUpdates(SmileRequest request) throws Exception;
    List<RequestSummary> getRequestsByDate(String startDate, String endDate) throws Exception;
    List<RequestMetadata> getRequestMetadataHistory(String reqId);
    SmileRequest getRequestBySample(SmileSample sample) throws Exception;
}
