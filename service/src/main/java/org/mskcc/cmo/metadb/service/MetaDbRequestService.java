package org.mskcc.cmo.metadb.service;

import java.util.Map;
import org.mskcc.cmo.metadb.model.MetaDbRequest;

/**
 *
 * @author ochoaa
 */
public interface MetaDbRequestService {

    boolean saveRequest(MetaDbRequest request) throws Exception;
    
    boolean updateRequest(MetaDbRequest request, MetaDbRequest savedRequest) throws Exception;

    Map<String, Object> getMetaDbRequestMap(String requestId) throws Exception;
    
    MetaDbRequest getMetaDbRequest(String requestId) throws Exception;

}
