package org.mskcc.smile.service;

import java.util.Map;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.smile.model.tempo.BamComplete;

/**
 *
 * @author ochoaa
 */
public interface TempoMessageHandlingService {
    void intialize(Gateway gateway) throws Exception;
    void bamCompleteHandler(Map.Entry<String, BamComplete> bcEvent) throws Exception;
    void shutdown() throws Exception;
}
