package org.mskcc.cmo.metadb.service.impl;

import java.util.List;
import org.mskcc.cmo.metadb.model.NormalSampleManifestEntity;
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
    public SampleManifestEntity saveSampleManifest(SampleManifestEntity sampleManifestEntity) {
        SampleManifestEntity foundSample =
                sampleManifestRepository.findSampleByIgoId(sampleManifestEntity.getSampleIgoId());
        if (foundSample == null) {
            PatientMetadata patient = patientMetadataRepository.findPatientByInvestigatorId(
                    sampleManifestEntity.getPatient().getInvestigatorPatientId());
            if (patient != null) {
                sampleManifestEntity.setPatientUuid(patient.getUuid());
            }
            sampleManifestRepository.save(sampleManifestEntity);
        } else {
            sampleManifestEntity.setUuid(foundSample.getUuid());
            sampleManifestRepository.updateSampleManifestJson(
                    sampleManifestEntity.getSampleManifestJsonEntity(), foundSample.getUuid());
        }
        return sampleManifestEntity;
    }

    @Override
    public List<NormalSampleManifestEntity> findMatchedNormalSample(
            SampleManifestEntity sampleManifestEntity) throws Exception {
        return sampleManifestRepository.findMatchedNormals(sampleManifestEntity);
    }

    @Override
    public List<String> findPooledNormalSample(SampleManifestEntity sampleManifestEntity) throws Exception {
        return sampleManifestRepository.findPooledNormals(sampleManifestEntity);
    }
}
