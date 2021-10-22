package org.mskcc.cmo.metadb.service;

import java.util.List;
import org.mskcc.cmo.metadb.model.MetadbRequest;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.RequestMetadata;
import org.mskcc.cmo.metadb.model.web.PublishedMetadbRequest;

/**
 *
 * @author ochoaa
 */
public interface MetadbRequestService {
    Boolean saveRequest(MetadbRequest request) throws Exception;
    Boolean saveRequestMetadata(MetadbRequest request);
    MetadbRequest getMetadbRequestById(String requestId) throws Exception;
    PublishedMetadbRequest getPublishedMetadbRequestById(String requestId) throws Exception;
    Boolean requestHasUpdates(MetadbRequest existingRequest, MetadbRequest request) throws Exception;
    Boolean requestHasMetadataUpdates(RequestMetadata existingRequestMetadata,
            RequestMetadata requestMetadata) throws Exception;
    List<MetadbSample> getRequestSamplesWithUpdates(MetadbRequest request) throws Exception;
    List<List<String>> getRequestsByDate(String startDate, String endDate) throws Exception;
    List<RequestMetadata> getRequestMetadataHistory(String reqId);
    MetadbRequest getRequestBySample(MetadbSample sample) throws Exception;
}
