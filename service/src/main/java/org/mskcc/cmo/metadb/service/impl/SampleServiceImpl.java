package org.mskcc.cmo.metadb.service.impl;

import org.mskcc.cmo.metadb.model.PatientMetadata;
import org.mskcc.cmo.metadb.model.SampleManifestEntity;
import org.mskcc.cmo.metadb.persistence.PatientMetadataRepository;
import org.mskcc.cmo.metadb.persistence.SampleManifestRepository;
import org.mskcc.cmo.metadb.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SampleServiceImpl implements SampleService {

    @Autowired
    private SampleManifestRepository sampleManifestRepository;
    
    @Autowired
    private PatientMetadataRepository patientMetadataRepository;

    @Override
    public SampleManifestEntity saveSampleManifest(SampleManifestEntity sample) {
        SampleManifestEntity foundSample = 
                sampleManifestRepository.findSampleByIgoId(sample.getSampleIgoId());
        if (foundSample == null) {
            PatientMetadata patient = patientMetadataRepository.findPatientByInvestigatorId(
                    sample.getPatient().getInvestigatorPatientId());
            if (patient != null) {
                sample.setPatientUuid(patient.getUuid());
            }
            sampleManifestRepository.save(sample);
        } else {
            sample.setUuid(foundSample.getUuid());
            sampleManifestRepository.updateSampleManifestJson(
                    sample.getSampleManifestJsonEntity(), foundSample.getUuid());
        }
        return sample;
    }
}
