package org.mskcc.cmo.metadb.service;

import java.util.Map;
import org.mskcc.cmo.messaging.Gateway;

public interface PatientCorrectionHandlingService {
    void initialize(Gateway gateway) throws Exception;
    void correctCmoPatientIdHandler(Map<String, String> idCorrectionMap) throws Exception;
    void shutdown() throws Exception;
}
