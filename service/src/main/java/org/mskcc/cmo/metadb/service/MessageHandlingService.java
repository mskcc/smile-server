package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.metadb.model.MetaDbRequest;

public interface MessageHandlingService {

    void initialize(Gateway gateway) throws Exception;

    void newRequestHandler(MetaDbRequest request) throws Exception;

    void shutdown() throws Exception;
}
