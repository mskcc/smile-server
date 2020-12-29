package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.messaging.Gateway;

public interface LimsRestService {

    void initialize(Gateway gateway) throws Exception;

    void shutdown() throws Exception;
}
