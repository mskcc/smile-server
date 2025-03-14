package org.mskcc.smile.service;

import org.mskcc.cmo.messaging.Gateway;

/**
 *
 * @author qu8n
 */
public interface CBioPortalMessageSchedulingService {
    void initialize(Gateway gateway) throws Exception;
    void shutdown() throws Exception;
    void scheduleTask() throws Exception;
}
