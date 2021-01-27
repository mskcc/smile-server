package org.mskcc.cmo.metadb.service.impl;

import java.sql.Timestamp;
import java.util.UUID;
import org.mskcc.cmo.metadb.model.NormalSampleManifestEntity;
import org.mskcc.cmo.metadb.model.PatientMetadata;
import org.mskcc.cmo.metadb.model.Sample;
import org.mskcc.cmo.metadb.model.SampleManifestJsonEntity;
import org.mskcc.cmo.metadb.persistence.NormalSampleRepository;
import org.mskcc.cmo.metadb.persistence.PatientMetadataRepository;
import org.mskcc.cmo.metadb.service.NormalSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NormalSampleServiceImpl implements NormalSampleService {

    @Autowired
    private NormalSampleRepository normalSampleRepository;

    @Autowired
    private PatientMetadataRepository patientMetadataRepository;

    @Override
    public NormalSampleManifestEntity saveNormalSampleManifest(NormalSampleManifestEntity normalSampleEntity)
            throws Exception {
        NormalSampleManifestEntity updatedNormalSampleManifestEntity =
                setUpNormalSampleManifest(normalSampleEntity);

        NormalSampleManifestEntity foundSample =
                normalSampleRepository.findSampleByIgoId(updatedNormalSampleManifestEntity.getSampleIgoId());
        if (foundSample == null) {
            PatientMetadata patient = patientMetadataRepository.findPatientByInvestigatorId(
                    updatedNormalSampleManifestEntity.getPatient().getInvestigatorPatientId());
            if (patient != null) {
                updatedNormalSampleManifestEntity.setPatientUuid(patient.getUuid());
            }
            normalSampleRepository.save(updatedNormalSampleManifestEntity);
        } else {
            updatedNormalSampleManifestEntity.setUuid(foundSample.getUuid());
            normalSampleRepository.updateSampleManifestJson(
                    updatedNormalSampleManifestEntity.getSampleManifestJsonEntity(), foundSample.getUuid());
        }
        return updatedNormalSampleManifestEntity;
    }

    @Override
    public NormalSampleManifestEntity setUpNormalSampleManifest(NormalSampleManifestEntity normalSampleEntity)
            throws Exception {
        PatientMetadata patient = new PatientMetadata();
        patient.setInvestigatorPatientId(normalSampleEntity.getCmoPatientId());
        normalSampleEntity.setPatient(patient);

        Sample igoId = new Sample();
        igoId.setIdSource("igoId");
        igoId.setSampleId(normalSampleEntity.getIgoId());
        normalSampleEntity.addSample(igoId);

        Sample investigatorId = new Sample();
        investigatorId.setIdSource("igoId");
        investigatorId.setSampleId(normalSampleEntity.getInvestigatorSampleId());
        normalSampleEntity.addSample(investigatorId);

        SampleManifestJsonEntity sampleJson = new SampleManifestJsonEntity();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        sampleJson.setCreationDate(String.valueOf(timestamp.getTime()));
        sampleJson.setSampleManifestJson(normalSampleEntity.toString());
        normalSampleEntity.setSampleManifestJsonEntity(sampleJson);

        return normalSampleEntity;
    }

    @Override
    public NormalSampleManifestEntity findNormalSampleManifest(UUID uuid) {
        NormalSampleManifestEntity normalSampleEntity = normalSampleRepository.findSampleByUuid(uuid);
        normalSampleEntity.addSample(normalSampleRepository.findInvestigatorId(uuid));
        normalSampleEntity.addSample(normalSampleRepository.findSampleIgoId(uuid));
        normalSampleEntity.setPatient(normalSampleRepository.findPatientMetadata(uuid));
        normalSampleEntity.setSampleManifestJsonEntity(normalSampleRepository.findSampleManifestJson(uuid));
        return normalSampleEntity;
    }

}
