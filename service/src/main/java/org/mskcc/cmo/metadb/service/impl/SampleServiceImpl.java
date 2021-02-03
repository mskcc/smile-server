package org.mskcc.cmo.metadb.service.impl;

import java.sql.Timestamp;
import java.util.List;
import org.mskcc.cmo.metadb.model.PatientMetadata;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleManifestEntity;
import org.mskcc.cmo.metadb.model.SampleManifestJsonEntity;
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
    public SampleManifestEntity saveSampleManifest(SampleManifestEntity
            sampleManifestEntity) throws Exception {
        SampleManifestEntity updatedSampleManifestEntity = setUpSampleManifest(sampleManifestEntity);
        SampleManifestEntity foundSample =
                sampleManifestRepository.findSampleByIgoId(updatedSampleManifestEntity.getSampleIgoId());
        if (foundSample == null) {
            PatientMetadata patient = patientMetadataRepository.findPatientByInvestigatorId(
                    updatedSampleManifestEntity.getPatient().getInvestigatorPatientId());
            if (patient != null) {
                updatedSampleManifestEntity.setPatientUuid(patient.getUuid());
            }
            sampleManifestRepository.save(updatedSampleManifestEntity);
        } else {
            updatedSampleManifestEntity.setUuid(foundSample.getUuid());
            sampleManifestRepository.updateSampleManifestJson(
                    updatedSampleManifestEntity.getSampleManifestJsonEntity(), foundSample.getUuid());
        }

        return updatedSampleManifestEntity;
    }

    @Override
    public SampleManifestEntity setUpSampleManifest(SampleManifestEntity
            sampleManifestEntity) throws Exception {
        PatientMetadata patient = new PatientMetadata();
        patient.setInvestigatorPatientId(sampleManifestEntity.getCmoPatientId());
        sampleManifestEntity.setPatient(patient);

        SampleAlias igoId = new SampleAlias();
        igoId.setIdSource("igoId");
        igoId.setSampleId(sampleManifestEntity.getIgoId());
        sampleManifestEntity.addSample(igoId);

        SampleAlias investigatorId = new SampleAlias();
        investigatorId.setIdSource("investigatorId");
        investigatorId.setSampleId(sampleManifestEntity.getInvestigatorSampleId());
        sampleManifestEntity.addSample(investigatorId);

        SampleManifestJsonEntity sampleJson = new SampleManifestJsonEntity();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        sampleJson.setCreationDate(String.valueOf(timestamp.getTime()));
        sampleJson.setSampleManifestJson(sampleManifestEntity.toString());
        sampleManifestEntity.setSampleManifestJsonEntity(sampleJson);

        return sampleManifestEntity;
    }

    @Override
    public List<SampleManifestEntity> findMatchedNormalSample(
            SampleManifestEntity sampleManifestEntity) throws Exception {
        return sampleManifestRepository.findSamplesWithSamePatient(sampleManifestEntity);
    }

    @Override
    public List<String> findPooledNormalSample(SampleManifestEntity sampleManifestEntity) throws Exception {
        return sampleManifestRepository.findPooledNormals(sampleManifestEntity);
    }
}
