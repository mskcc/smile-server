package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.metadb.model.MetadbSample;

public interface ClinicalMessageHandlingService {
    void initialize(Gateway gateway) throws Exception;
    void newClinicalSampleHandler(MetadbSample metadbSample) throws Exception;
    void clinicalSampleUpdateHandler(MetadbSample metadbSample) throws Exception;
    void shutdown() throws Exception;
}
