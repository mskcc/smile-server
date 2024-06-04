package org.mskcc.smile.service.msg;

import org.mskcc.cmo.messaging.Gateway;

public interface RequestReplyHandlingService {
    void initialize(Gateway gateway) throws Exception;
    void patientSamplesHandler(String patientId, String replyTo) throws Exception;
    void crdbMappingHandler(String inputId, String replyTo) throws Exception;
    void shutdown() throws Exception;
}
