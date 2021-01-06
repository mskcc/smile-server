package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.shared.neo4j.CmoRequestEntity;

/**
 *
 * @author ochoaa
 */
public interface CmoRequestService {
    CmoRequestEntity saveRequest(CmoRequestEntity request);
}
