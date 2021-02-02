package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.metadb.model.CmoRequestEntity;

/**
 *
 * @author ochoaa
 */
public interface CmoRequestService {

    void saveRequest(CmoRequestEntity request) throws Exception;

    CmoRequestEntity getCmoRequest(String requestId);

}
