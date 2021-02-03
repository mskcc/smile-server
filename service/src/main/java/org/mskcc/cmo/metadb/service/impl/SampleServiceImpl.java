package org.mskcc.cmo.metadb.service.impl;

import java.sql.Timestamp;
import java.util.List;
import org.mskcc.cmo.metadb.model.MetaDbPatient;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.MetaDbSample;
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
    public MetaDbSample saveSampleManifest(MetaDbSample
            metaDbSample) throws Exception {
        MetaDbSample updatedSampleManifestEntity = setUpSampleManifest(metaDbSample);
        MetaDbSample foundSample =
                sampleManifestRepository.findSampleByIgoId(updatedSampleManifestEntity.getSampleIgoId());
        if (foundSample == null) {
            MetaDbPatient patient = patientMetadataRepository.findPatientByInvestigatorId(
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
    public MetaDbSample setUpSampleManifest(MetaDbSample
            metaDbSample) throws Exception {
        MetaDbPatient patient = new MetaDbPatient();
        patient.setInvestigatorPatientId(metaDbSample.getCmoPatientId());
        metaDbSample.setPatient(patient);

        SampleAlias igoId = new SampleAlias();
        igoId.setIdSource("igoId");
        igoId.setSampleId(metaDbSample.getIgoId());
        metaDbSample.addSample(igoId);

        SampleAlias investigatorId = new SampleAlias();
        investigatorId.setIdSource("investigatorId");
        investigatorId.setSampleId(metaDbSample.getInvestigatorSampleId());
        metaDbSample.addSample(investigatorId);

        SampleManifestEntity sampleJson = new SampleManifestEntity();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        sampleJson.setCreationDate(String.valueOf(timestamp.getTime()));
        sampleJson.setSampleManifestJson(metaDbSample.toString());
        metaDbSample.setSampleManifestJsonEntity(sampleJson);

        return metaDbSample;
    }

    @Override
    public List<MetaDbSample> findMatchedNormalSample(
            MetaDbSample metaDbSample) throws Exception {
        return sampleManifestRepository.findSamplesWithSamePatient(metaDbSample);
    }

    @Override
    public List<String> findPooledNormalSample(MetaDbSample metaDbSample) throws Exception {
        return sampleManifestRepository.findPooledNormals(metaDbSample);
    }
}
