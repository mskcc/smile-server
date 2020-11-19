package org.mskcc.cmo.metadb.service.impl;

import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.messaging.MessageConsumer;
import org.mskcc.cmo.shared.neo4j.SampleMetadataEntity;
import org.mskcc.cmo.metadb.service.SampleIntakeMessageService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author ochoaa
 */
@Service
public class SampleIntakeMessageServiceImpl implements SampleIntakeMessageService, MessageConsumer {
    private final String NEW_SAMPLE_INTAKE = "NEW_SAMPLE_INTAKE";
    private final String NEW_SAMPLE_SUBMISSION = "NEW_SAMPLE_SUBMISSION";

    private final Log LOG = LogFactory.getLog(SampleIntakeMessageServiceImpl.class);

    @Autowired
    private Gateway messagingGateway;

    @Override
    public void onSampleIntakeMessage() throws Exception {
        messagingGateway.subscribe(NEW_SAMPLE_INTAKE, SampleMetadataEntity.class, this);
        SampleMetadataEntity sampleMetadata = (SampleMetadataEntity) messagingGateway.request(NEW_SAMPLE_INTAKE, SampleMetadataEntity.class);
        messagingGateway.publish(NEW_SAMPLE_SUBMISSION, sampleMetadata);
    }

    @Override
    public void onMessage(Object message) {
        System.out.println("\n***\nRecevied sample metadata:\n" + message.toString() + "\n***");
    }

}
