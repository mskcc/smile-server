package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.metadb.model.neo4j.MetaDbRequest;

/**
 *
 * @author ochoaa
 */
public interface CmoRequestService {

    void saveRequest(MetaDbRequest request) throws Exception;

    MetaDbRequest getMetaDbRequest(String requestId);

}
