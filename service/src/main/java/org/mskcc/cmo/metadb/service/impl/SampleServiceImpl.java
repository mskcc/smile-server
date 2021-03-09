package org.mskcc.cmo.metadb.service.impl;

import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.metadb.model.MetaDbPatient;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleMetadata;
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
    public MetaDbSample saveSampleMetadata(MetaDbSample
            metaDbSample) throws Exception {
        MetaDbSample updatedMetaDbSample = setUpMetaDbSample(metaDbSample);
        MetaDbSample foundSample =
                metaDbSampleRepository.findMetaDbSampleByIgoId(updatedMetaDbSample.getSampleIgoId());
        if (foundSample == null) {
            MetaDbPatient patient = metaDbPatientRepository.findPatientByInvestigatorId(
                    updatedMetaDbSample.getPatient().getInvestigatorPatientId());
            if (patient != null) {
                updatedMetaDbSample.setPatientUuid(patient.getMetaDbPatientId());
            }
            metaDbSampleRepository.save(updatedMetaDbSample);
        } else {
            foundSample.addSampleMetadata(updatedMetaDbSample.getSampleMetadataList().get(0));
            metaDbSampleRepository.save(foundSample);
        }
        return updatedMetaDbSample;
    }

    @Override
    public MetaDbSample setUpMetaDbSample(MetaDbSample metaDbSample) throws Exception {
        SampleMetadata sampleMetadata = metaDbSample.getLatestSampleMetadata();
        metaDbSample.setSampleClass(sampleMetadata.getTumorOrNormal());
        metaDbSample.setPatient(new MetaDbPatient(sampleMetadata.getCmoPatientId()));
        metaDbSample.addSample(new SampleAlias(sampleMetadata.getIgoId(), "igoId"));
        metaDbSample.addSample(new SampleAlias(sampleMetadata.getInvestigatorSampleId(), "investigatorId"));
        return metaDbSample;
    }

    @Override
    public List<MetaDbSample> findMatchedNormalSample(
            MetaDbSample metaDbSample) throws Exception {
        return metaDbSampleRepository.findMatchedNormalsBySample(metaDbSample);
    }

    @Override
    public List<String> findPooledNormalSample(MetaDbSample metaDbSample) throws Exception {
        return metaDbSampleRepository.findPooledNormalsBySample(metaDbSample);
    }

    @Override
    public MetaDbSample getMetaDbSample(UUID metaDbSampleId) throws Exception {
        MetaDbSample metaDbSample = metaDbSampleRepository.findMetaDbSampleById(metaDbSampleId);
        metaDbSample.setSampleMetadataList(
                metaDbSampleRepository.findSampleMetadataListBySampleId(metaDbSampleId));
        for (SampleMetadata s: metaDbSample.getSampleMetadataList()) {
            s.setPatientUuid(metaDbSampleRepository.findPatientIdBySample(metaDbSampleId));
            s.setSampleUuid(metaDbSampleId);
        }
        return metaDbSample;
    }
}
