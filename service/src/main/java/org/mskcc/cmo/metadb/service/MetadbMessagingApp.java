package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.messaging.model.LinkedPatient;
import org.mskcc.cmo.messaging.model.LinkedSample;
import org.mskcc.cmo.messaging.model.PatientMetadata;
import org.mskcc.cmo.messaging.model.SampleMetadataEntity;
import org.mskcc.cmo.metadb.persistence.GeneralGraphDbRepository;
import org.mskcc.cmo.metadb.persistence.PatientMetadataRepository;
import org.mskcc.cmo.metadb.persistence.SampleMetadataRepository;

import java.util.List;
import junit.framework.Assert;
import org.mskcc.cmo.messaging.Gateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@EnableNeo4jRepositories(basePackages="org.mskcc.cmo.metadb.persistence")
@SpringBootApplication(scanBasePackages="org.mskcc.cmo.messaging")
public class MetadbMessagingApp implements CommandLineRunner {
    @Autowired
    private SampleMetadataRepository sampleMetadataRepository;
    @Autowired
    private GeneralGraphDbRepository generalGraphDbRepository;
    @Autowired
    private PatientMetadataRepository patientMetadataRepository;

    public static void main(String[] args) {
        SpringApplication.run(MetadbMessagingApp.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n\nCleaning up graphdb...");
        generalGraphDbRepository.deleteAllFromGraphDb();

        PatientMetadata patient1 = mockPatientMetadata("patient1");
        SampleMetadataEntity sample1 = mockSampleMetadata("sample1");
        sample1.setPatient(patient1);
        System.out.println("\n\nSaving or merging sample #1 to graphdb");
        patientMetadataRepository.savePatientMetadata(patient1);
        sampleMetadataRepository.saveSampleMetadata(sample1);

        System.out.println("\n\nFetching samples from graphdb");
        List<String> samples = sampleMetadataRepository.findAllSampleNames();
        Assert.assertEquals(1, samples.size());

        System.out.println("Exiting application...");
        System.exit(0);
    }

    private SampleMetadataEntity mockSampleMetadata(String sampleId) {
        SampleMetadataEntity sMetadata = new SampleMetadataEntity();

        sMetadata.setInvestigatorSampleId(sampleId);
        sMetadata.setIgoId("IGO-" + sampleId);
        sMetadata.setSampleOrigin("");
        sMetadata.setSex("");
        sMetadata.setSpecies("");
        sMetadata.setSpecimenType("");
        sMetadata.setTissueLocation("");
        sMetadata.setTumorOrNormal("TUMOR");
        sMetadata.linkSample(new LinkedSample("DMP_SAMPLE_" + sampleId, "DMP"));
        sMetadata.linkSample(new LinkedSample("DARWIN_" + sampleId, "DARWIN"));
        return sMetadata;
    }

    private PatientMetadata mockPatientMetadata(String patientId) {
        PatientMetadata pMetadata = new PatientMetadata();
        pMetadata.setInvestigatorPatientId(patientId);
        pMetadata.linkPatient(new LinkedPatient("DMP_PATIENT_" + patientId, "DMP"));
        pMetadata.linkPatient(new LinkedPatient("DARWIN_" + patientId, "DARWIN"));
        return pMetadata;
    }

}
