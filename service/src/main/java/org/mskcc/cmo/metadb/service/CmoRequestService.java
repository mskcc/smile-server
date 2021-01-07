package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.metadb.model.CmoRequestEntity;

/**
 *
 * @author ochoaa
 */
public interface CmoRequestService {
    CmoRequestEntity saveRequest(CmoRequestEntity request);
}
