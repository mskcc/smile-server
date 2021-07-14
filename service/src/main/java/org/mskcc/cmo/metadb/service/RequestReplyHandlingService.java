package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.messaging.Gateway;

public interface RequestReplyHandlingService {
    
    void initialize(Gateway gateway) throws Exception;

    void newPatientSamplesHandler(String patientId, String replyTo) throws Exception;

    void shutdown() throws Exception;
}
