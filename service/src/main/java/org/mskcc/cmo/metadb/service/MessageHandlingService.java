package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.metadb.model.MetadbRequest;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.RequestMetadata;
import org.mskcc.cmo.metadb.model.SampleMetadata;

public interface MessageHandlingService {
    void initialize(Gateway gateway) throws Exception;
    void newRequestHandler(MetadbRequest request) throws Exception;
    void requestUpdateHandler(RequestMetadata requestMetadata) throws Exception;
    void researchSampleUpdateHandler(SampleMetadata sampleMetadata) throws Exception;
    void newSampleHandler(MetadbSample metadbSample) throws Exception;
    void shutdown() throws Exception;
}
