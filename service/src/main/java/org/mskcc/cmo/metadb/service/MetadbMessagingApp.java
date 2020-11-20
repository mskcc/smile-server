package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.shared.neo4j.Patient;
import org.mskcc.cmo.shared.neo4j.Sample;
import org.mskcc.cmo.shared.neo4j.PatientMetadata;
import org.mskcc.cmo.shared.neo4j.SampleMetadataEntity;
import org.mskcc.cmo.metadb.persistence.GeneralGraphDbRepository;
import org.mskcc.cmo.metadb.persistence.PatientMetadataRepository;
import org.mskcc.cmo.metadb.persistence.SampleMetadataRepository;

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
//        System.out.println("\n\nCleaning up graphdb...");
//        generalGraphDbRepository.deleteAllFromGraphDb();

        PatientMetadata patient1 = mockPatientMetadata("patient1");
        SampleMetadataEntity sample1 = mockSampleMetadata("sample1");
        sample1.setPatient(patient1);
        System.out.println("\n\nSaving or merging sample #1 to graphdb");

        patientMetadataRepository.savePatientMetadata(patient1);
        sampleMetadataRepository.saveSampleMetadata(sample1);

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
        sMetadata.addSample(new Sample("P-0002978-IM5-T02", "DMP"));
        sMetadata.addSample(new Sample("DrilA_NTRK_X_0001_JV_P1", "DARWIN"));
        sMetadata.addSample(new Sample("s_C_000520_X001_d", "CMO"));
        return sMetadata;
    }

    private PatientMetadata mockPatientMetadata(String patientId) {
        PatientMetadata pMetadata = new PatientMetadata();
        pMetadata.setInvestigatorPatientId(patientId);
        pMetadata.addPatient(new Patient("P-0002978", "DMP"));
        pMetadata.addPatient(new Patient("215727", "DARWIN"));
        return pMetadata;
    }

}
