package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.web.PublishedMetaDbRequest;

/**
 *
 * @author ochoaa
 */
public interface MetaDbRequestService {

    boolean saveRequest(MetaDbRequest request) throws Exception;

    PublishedMetaDbRequest getMetaDbRequest(String requestId) throws Exception;

}
