package org.mskcc.smile.service;

import java.util.Map;
import org.mskcc.cmo.messaging.Gateway;

public interface CorrectCmoPatientHandlingService {
    void initialize(Gateway gateway) throws Exception;
    void correctCmoPatientIdHandler(Map<String, String> idCorrectionMap) throws Exception;
    void shutdown() throws Exception;
}
