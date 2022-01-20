package org.mskcc.cmo.metadb.service;

import java.util.Map;
import org.mskcc.cmo.messaging.Gateway;

/**
 *
 * @author ochoaa
 */
public interface AdminMessageHandlingService {
    void initialize(Gateway gateway) throws Exception;
    void correctCmoPatientIdHandler(Map<String, String> idCorrectionMap) throws Exception;
    void shutdown() throws Exception;
}
