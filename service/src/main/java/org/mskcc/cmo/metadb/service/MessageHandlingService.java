package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.metadb.model.CmoRequestEntity;

public interface MessageHandlingService {

    void initialize(Gateway gateway) throws Exception;

    void newRequestHandler(CmoRequestEntity request) throws Exception;

    void shutdown() throws Exception;
}
