package org.mskcc.cmo.metadb.service.impl;

import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.metadb.model.MetaDbPatient;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.PatientAlias;
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
    private MetaDbSampleRepository sampleRepository;

    @Autowired
    private MetaDbPatientRepository patientRepository;

    @Override
    public MetaDbSample saveSampleMetadata(MetaDbSample
            metaDbSample) throws Exception {
        MetaDbSample updatedMetaDbSample = setUpMetaDbSample(metaDbSample);
        MetaDbSample foundSample =
                sampleRepository.findMetaDbSampleByIgoId(updatedMetaDbSample.getSampleIgoId());
        if (foundSample == null) {
            MetaDbPatient patient = patientRepository.findPatientByPatientAlias(
                    updatedMetaDbSample.getPatient().getCmoPatientId().getPatientId());
            if (patient != null) {
                updatedMetaDbSample.setPatient(patient);
            }
            sampleRepository.save(updatedMetaDbSample);
        } else {
            foundSample.addSampleMetadata(updatedMetaDbSample.getSampleMetadataList().get(0));
            sampleRepository.save(foundSample);
        }
        return updatedMetaDbSample;
    }

    @Override
    public MetaDbSample setUpMetaDbSample(MetaDbSample metaDbSample) throws Exception {
        SampleMetadata sampleMetadata = metaDbSample.getLatestSampleMetadata();
        metaDbSample.setSampleClass(sampleMetadata.getTumorOrNormal());
        metaDbSample.addSample(new SampleAlias(sampleMetadata.getIgoId(), "igoId"));
        metaDbSample.addSample(new SampleAlias(sampleMetadata.getInvestigatorSampleId(), "investigatorId"));

        MetaDbPatient patient = new MetaDbPatient();
        patient.addPatientAlias(new PatientAlias(sampleMetadata.getCmoPatientId(), "cmoId"));
        metaDbSample.setPatient(patient);

        return metaDbSample;
    }

    @Override
    public List<MetaDbSample> findMatchedNormalSample(
            MetaDbSample metaDbSample) throws Exception {
        return sampleRepository.findMatchedNormalsBySample(metaDbSample);
    }

    @Override
    public List<String> findPooledNormalSample(MetaDbSample metaDbSample) throws Exception {
        return sampleRepository.findPooledNormalsBySample(metaDbSample);
    }

    @Override
    public MetaDbSample getMetaDbSample(UUID metaDbSampleId) throws Exception {
        MetaDbSample metaDbSample = sampleRepository.findMetaDbSampleById(metaDbSampleId);
        metaDbSample.setSampleMetadataList(sampleRepository.findSampleMetadataListBySampleId(metaDbSampleId));
        for (SampleMetadata s: metaDbSample.getSampleMetadataList()) {
            s.setMetaDbPatientId(patientRepository.findPatientIdBySample(metaDbSampleId));
            s.setMetaDbSampleId(metaDbSampleId);
        }
        return metaDbSample;
    }

    @Override
    public List<SampleMetadata> getSampleMetadataListByCmoPatientId(String cmoPatientId) throws Exception {
        return sampleRepository.findSampleMetadataListByCmoPatientId(cmoPatientId);
    }
}
