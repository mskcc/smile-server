package org.mskcc.smile.service.msg;

import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.smile.model.RequestMetadata;
import org.mskcc.smile.model.SampleMetadata;
import org.mskcc.smile.model.SmileRequest;

public interface ResearchMessageHandlingService {
    void initialize(Gateway gateway) throws Exception;
    void newRequestHandler(SmileRequest request) throws Exception;
    void promotedRequestHandler(SmileRequest request) throws Exception;
    void requestUpdateHandler(RequestMetadata requestMetadata) throws Exception;
    void researchSampleUpdateHandler(SampleMetadata sampleMetadata) throws Exception;
    void shutdown() throws Exception;
}
