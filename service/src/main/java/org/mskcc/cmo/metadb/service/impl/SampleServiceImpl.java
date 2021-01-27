package org.mskcc.cmo.metadb.service.impl;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.metadb.model.NormalSampleManifestEntity;
import org.mskcc.cmo.metadb.model.PatientMetadata;
import org.mskcc.cmo.metadb.model.Sample;
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
    public List<NormalSampleManifestEntity> findMatchedNormalSample(
            SampleManifestEntity sampleManifestEntity) throws Exception {
        return sampleManifestRepository.findMatchedNormals(sampleManifestEntity);
    }

    @Override
    public List<String> findPooledNormalSample(SampleManifestEntity sampleManifestEntity) throws Exception {
        return sampleManifestRepository.findPooledNormals(sampleManifestEntity);
    }

    /**
     * @param sampleManifestEntity
     */
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

        Sample igoId = new Sample();
        igoId.setIdSource("igoId");
        igoId.setSampleId(sampleManifestEntity.getIgoId());
        sampleManifestEntity.addSample(igoId);

        Sample investigatorId = new Sample();
        investigatorId.setIdSource("igoId");
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
    public SampleManifestEntity findSampleManifest(UUID uuid) {
        SampleManifestEntity sampleManifest = sampleManifestRepository.findSampleByUuid(uuid);
        sampleManifest.addSample(sampleManifestRepository.findInvestigatorId(uuid));
        sampleManifest.addSample(sampleManifestRepository.findSampleIgoId(uuid));
        sampleManifest.setPatient(sampleManifestRepository.findPatientMetadata(uuid));
        sampleManifest.setSampleManifestJsonEntity(sampleManifestRepository.findSampleManifestJson(uuid));
        return sampleManifest;
    }
}
