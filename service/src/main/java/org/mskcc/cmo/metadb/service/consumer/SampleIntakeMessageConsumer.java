package org.mskcc.cmo.metadb.service.consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.messaging.MessageConsumer;
import org.springframework.stereotype.Component;

/**
 *
 * @author ochoaa
 */
@Component
public class SampleIntakeMessageConsumer implements MessageConsumer {
    private Gateway messagingGateway;
    private final Log LOG = LogFactory.getLog(SampleIntakeMessageConsumer.class);

    public SampleIntakeMessageConsumer(Gateway messagingGateway) {
        this.messagingGateway = messagingGateway;
    }

    @Override
    public void onMessage(Object message) {
        LOG.info("*** Message received on igo.new-sample-intake topic ***\n");
        LOG.info(message);
        try {
            LOG.info("Publishing on topic: cmo.new-sample-submission\n");
            messagingGateway.publish("cmo.new-sample-submission",
                    "\"message from igo.new-sample-intake handler to cmo.new-sample-submission\"");
        } catch (Exception e) {
            LOG.error("Error encountered during publishing on topic: cmo.new-sample-submission", e);
        }
    }

}
