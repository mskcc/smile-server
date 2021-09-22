package org.mskcc.cmo.metadb.service;

import java.util.List;
import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.RequestMetadata;
import org.mskcc.cmo.metadb.model.web.PublishedMetaDbRequest;

/**
 *
 * @author ochoaa
 */
public interface MetadbRequestService {
    Boolean saveRequest(MetaDbRequest request) throws Exception;
    MetaDbRequest getMetadbRequestById(String requestId) throws Exception;
    PublishedMetaDbRequest getPublishedMetadbRequestById(String requestId) throws Exception;
    Boolean requestHasUpdates(MetaDbRequest existingRequest, MetaDbRequest request) throws Exception;
    Boolean requestHasMetadataUpdates(RequestMetadata existingRequestMetadata,
            RequestMetadata requestMetadata) throws Exception;
    List<MetaDbSample> getRequestSamplesWithUpdates(MetaDbRequest request) throws Exception;
    List<RequestMetadata> getRequestMetadataHistoryByRequestId(String reqId);
}
