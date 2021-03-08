package org.mskcc.cmo.metadb.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.metadb.model.MetaDbPatient;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleManifestEntity;
import org.mskcc.cmo.metadb.persistence.MetaDbPatientRepository;
import org.mskcc.cmo.metadb.persistence.MetaDbSampleRepository;
import org.mskcc.cmo.metadb.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SampleServiceImpl implements SampleService {

    @Autowired
    private MetaDbSampleRepository metaDbSampleRepository;

    @Autowired
    private MetaDbPatientRepository metaDbPatientRepository;

    @Override
    public MetaDbSample saveSampleManifest(MetaDbSample
            metaDbSample) throws Exception {
        MetaDbSample updatedMetaDbSample = setUpMetaDbSample(metaDbSample);
        MetaDbSample foundSample =
                metaDbSampleRepository.findSampleByIgoId(updatedMetaDbSample.getSampleIgoId());
        if (foundSample == null) {
            MetaDbPatient patient = metaDbPatientRepository.findPatientByInvestigatorId(
                    updatedMetaDbSample.getPatient().getInvestigatorPatientId());
            if (patient != null) {
                updatedMetaDbSample.setPatientUuid(patient.getMetaDbPatientId());
            }
            metaDbSampleRepository.save(updatedMetaDbSample);
        } else {
            foundSample.addSampleManifest(updatedMetaDbSample.getSampleManifestList().get(0));
            metaDbSampleRepository.save(foundSample);
        }
        return updatedMetaDbSample;
    }

    @Override
    public MetaDbSample setUpMetaDbSample(MetaDbSample
            metaDbSample) throws Exception {
        metaDbSample = setUpSampleManifestEntity(metaDbSample);
        SampleManifestEntity sampleManifestEntity = metaDbSample.getSampleManifestList().get(0);
        metaDbSample.setSampleClass(sampleManifestEntity.getTumorOrNormal());

        MetaDbPatient patient = new MetaDbPatient();
        patient.setInvestigatorPatientId(sampleManifestEntity.getCmoPatientId());
        metaDbSample.setPatient(patient);

        SampleAlias igoId = new SampleAlias();
        igoId.setNamespace("igoId");
        igoId.setSampleId(sampleManifestEntity.getIgoId());
        metaDbSample.addSample(igoId);

        SampleAlias investigatorId = new SampleAlias();
        investigatorId.setNamespace("investigatorId");
        investigatorId.setSampleId(sampleManifestEntity.getInvestigatorSampleId());
        metaDbSample.addSample(investigatorId);
        return metaDbSample;
    }

    @Override
    public MetaDbSample setUpSampleManifestEntity(MetaDbSample metaDbSample) throws Exception {
        SampleManifestEntity s = metaDbSample.getSampleManifestList().get(0);
        if (s != null) {
            s.setImportDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        return metaDbSample;
    }

    @Override
    public List<MetaDbSample> findMatchedNormalSample(
            MetaDbSample metaDbSample) throws Exception {
        return metaDbSampleRepository.findMatchedNormals(metaDbSample);
    }

    @Override
    public List<String> findPooledNormalSample(MetaDbSample metaDbSample) throws Exception {
        return metaDbSampleRepository.findPooledNormals(metaDbSample);
    }

    @Override
    public MetaDbSample getMetaDbSample(UUID metaDbSampleId) throws Exception {
        MetaDbSample metaDbSample = metaDbSampleRepository.findSampleByUUID(metaDbSampleId);
        metaDbSample.setSampleManifestList(metaDbSampleRepository.findSampleManifestList(metaDbSampleId));
        for (SampleManifestEntity s: metaDbSample.getSampleManifestList()) {
            s.setPatientUuid(metaDbSampleRepository.findPatientUuid(metaDbSampleId));
            s.setSampleUuid(metaDbSampleId);
        }
        return metaDbSample;
    }
}
