package org.mskcc.cmo.metadb.service.consumer;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.messaging.MessageConsumer;
import org.mskcc.cmo.metadb.persistence.SampleMetadataRepository;
import org.mskcc.cmo.shared.neo4j.Patient;
import org.mskcc.cmo.shared.neo4j.PatientMetadata;
import org.mskcc.cmo.shared.neo4j.SampleManifestEntity;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

/**
 *
 * @author ochoaa
 */
@Service
@ComponentScan(basePackages = "org.mskcc.cmo.metadb.persistence")
public class IGOSampleIntakeMessageConsumer implements MessageConsumer {
    private Gateway messagingGateway;
    private final Log LOG = LogFactory.getLog(IGOSampleIntakeMessageConsumer.class);
    private SampleMetadataRepository sampleMetadataRepository;

    public IGOSampleIntakeMessageConsumer(Gateway messagingGateway,
            SampleMetadataRepository sampleMetadataRepository) {
        this.messagingGateway = messagingGateway;
        this.sampleMetadataRepository = sampleMetadataRepository;
    }

    @Override
    public void onMessage(Object message) {

        LOG.info("*** Message received on igo.new-sample-intake topic ***\n");
        LOG.info(message);
        Gson gson = new Gson();
        SampleManifestEntity sampleMetadata = gson.fromJson(gson.toJson(message),
                SampleManifestEntity.class);
        if (addSampleMetadata(sampleMetadata)) {
            try {
                LOG.info("Publishing on topic: cmo.new-sample-submission\n");
                messagingGateway.publish("cmo.new-sample-submission",
                        "\"message from igo.new-sample-intake handler to cmo.new-sample-submission\"");
            } catch (Exception e) {
                LOG.error("Error encountered during publishing on topic: cmo.new-sample-submission", e);
            }
        }
    }

    private PatientMetadata mockPatientMetadata(String patientId) {
        PatientMetadata pMetadata = new PatientMetadata();
        pMetadata.setInvestigatorPatientId(patientId);
        pMetadata.addPatient(new Patient("P-0002978", "DMP"));
        pMetadata.addPatient(new Patient("215727", "DARWIN"));
        return pMetadata;
    }

    private boolean addSampleMetadata(SampleManifestEntity sampleMetadata) {
        try {
            LOG.info("*** Persisting to NEO4j ***\n");
            sampleMetadata.setPatient(mockPatientMetadata("12345"));
            //sampleMetadataRepository.saveSampleMetadata(sampleMetadata);
            return true;
        } catch (Exception e) {
            LOG.error("Error encountered during persistence of sampleMetadata to graphDB", e);
        }
        return false;
    }

}
