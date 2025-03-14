package org.mskcc.smile.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.smile.service.CBioPortalMessageSchedulingService;
import org.springframework.stereotype.Component;

/**
 *
 * @author ochoaa
 */
@Component
public class CBioPortalMessageSchedulingServiceImpl implements CBioPortalMessageSchedulingService {
    private static Gateway messagingGateway;
    private static boolean initialized = false;
    private static final Log LOG = LogFactory.getLog(TempoMessageHandlingServiceImpl.class);

    @Override
    public void initialize(Gateway gateway) throws Exception {
        if (!initialized) {
            messagingGateway = gateway;
            initialized = true;
        } else {
            LOG.error("Messaging Scheduler Service has already been initialized, ignoring request.\n");
        }
    }
}
