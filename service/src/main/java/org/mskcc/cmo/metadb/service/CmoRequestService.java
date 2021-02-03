package org.mskcc.cmo.metadb.service;

import java.util.Map;
import org.mskcc.cmo.metadb.model.neo4j.MetaDbRequest;

/**
 *
 * @author ochoaa
 */
public interface CmoRequestService {

    void saveRequest(MetaDbRequest request) throws Exception;

    Map<String, Object> getMetaDbRequest(String requestId) throws Exception;

}
