package org.mskcc.smile.service;

import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.smile.model.SmileSample;

public interface ClinicalMessageHandlingService {
    void initialize(Gateway gateway) throws Exception;
    void newClinicalSampleHandler(SmileSample smileSample) throws Exception;
    void clinicalSampleUpdateHandler(SmileSample smileSample) throws Exception;
    void shutdown() throws Exception;
}
