package org.mskcc.cmo.metadb.service;

import java.util.Map;
import org.mskcc.cmo.metadb.model.MetaDbRequest;

/**
 *
 * @author ochoaa
 */
public interface MetaDbRequestService {

    void saveRequest(MetaDbRequest request) throws Exception;

    Map<String, Object> getMetaDbRequest(String requestId) throws Exception;

}
