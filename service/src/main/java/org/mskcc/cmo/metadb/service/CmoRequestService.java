package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.metadb.model.MetaDbRequest;

/**
 *
 * @author ochoaa
 */
public interface CmoRequestService {

    void saveRequest(MetaDbRequest request) throws Exception;

    MetaDbRequest getCmoRequest(String requestId);

}
