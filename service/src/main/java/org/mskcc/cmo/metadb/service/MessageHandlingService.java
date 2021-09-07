package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.RequestMetadata;
import org.mskcc.cmo.metadb.model.SampleMetadata;

public interface MessageHandlingService {
    void initialize(Gateway gateway) throws Exception;
    void newRequestHandler(MetaDbRequest request) throws Exception;
    void requestUpdateHandler(RequestMetadata requestMetadata) throws Exception;
    void sampleUpdateHandler(SampleMetadata sampleMetadata) throws Exception;
    void shutdown() throws Exception;
}
