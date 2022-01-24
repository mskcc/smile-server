package org.mskcc.cmo.metadb.service;

import java.util.Map;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.metadb.model.MetadbRequest;
import org.mskcc.cmo.metadb.model.RequestMetadata;
import org.mskcc.cmo.metadb.model.SampleMetadata;

public interface MessageHandlingService {
    void initialize(Gateway gateway) throws Exception;
    void newRequestHandler(MetadbRequest request) throws Exception;
    void requestUpdateHandler(RequestMetadata requestMetadata) throws Exception;
    void sampleUpdateHandler(SampleMetadata sampleMetadata) throws Exception;
    void correctCmoPatientIdHandler(Map<String, String> idCorrectionMap) throws Exception;
    void shutdown() throws Exception;
}
