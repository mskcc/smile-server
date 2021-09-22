package org.mskcc.cmo.metadb.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.common.MetadbJsonComparator;
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
    private MetadbJsonComparator metadbJsonComparator;

    @Autowired
    private MetaDbSampleRepository sampleRepository;

    @Autowired
    private MetaDbPatientRepository patientRepository;

    private final ObjectMapper mapper = new ObjectMapper();

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
            patientRepository.save(updatedMetaDbSample.getPatient());
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
        metaDbSample.addSampleAlias(new SampleAlias(sampleMetadata.getIgoId(), "igoId"));
        metaDbSample.addSampleAlias(
                new SampleAlias(sampleMetadata.getInvestigatorSampleId(), "investigatorId"));

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
    public MetaDbSample getMetaDbSampleByRequestAndAlias(String requestId, SampleAlias igoId)
            throws Exception {
        MetaDbSample metadbSample = sampleRepository.findMetaDbSampleByRequestAndIgoId(requestId, igoId);
        metadbSample.setSampleMetadataList(sampleRepository
                .findSampleMetadataListBySampleId(metadbSample.getMetaDbSampleId()));
        for (SampleMetadata s : metadbSample.getSampleMetadataList()) {
            s.setMetaDbPatientId(patientRepository.findPatientIdBySample(metadbSample.getMetaDbSampleId()));
            s.setMetaDbSampleId(metadbSample.getMetaDbSampleId());
        }
        return metadbSample;
    }

    @Override
    public MetaDbSample getMetaDbSampleByRequestAndIgoId(String requestId, String igoId)
            throws Exception {
        MetaDbSample metadbSample = sampleRepository.findMetaDbSampleByRequestAndIgoId(requestId, igoId);
        metadbSample.setSampleMetadataList(sampleRepository
                .findSampleMetadataListBySampleId(metadbSample.getMetaDbSampleId()));
        for (SampleMetadata s : metadbSample.getSampleMetadataList()) {
            s.setMetaDbPatientId(patientRepository.findPatientIdBySample(metadbSample.getMetaDbSampleId()));
            s.setMetaDbSampleId(metadbSample.getMetaDbSampleId());
        }
        return metadbSample;
    }

    @Override
    public List<SampleMetadata> getSampleMetadataListByCmoPatientId(String cmoPatientId) throws Exception {
        return sampleRepository.findSampleMetadataListByCmoPatientId(cmoPatientId);
    }

    @Override
    public List<MetaDbSample> getAllMetadbSamplesByRequestId(String requestId) throws Exception {
        List<MetaDbSample> requestSamples = new ArrayList<>();
        for (MetaDbSample s : sampleRepository.findAllMetaDbSamplesByRequest(requestId)) {
            requestSamples.add(getMetaDbSample(s.getMetaDbSampleId()));
        }
        return requestSamples;
    }

    @Override
    public List<SampleMetadata> getSampleMetadataHistoryByIgoId(String igoId) throws Exception {
        List<SampleMetadata> requestSamples = sampleRepository.getSampleMetadataHistoryByIgoId(igoId);
        //This sorts a given list of SampleMetadata in ascending order based on importDate
        Collections.sort(requestSamples);
        return requestSamples;
    }

    @Override
    public Boolean sampleHasMetadataUpdates(SampleMetadata existingSampleMetadata,
            SampleMetadata sampleMetadata) throws Exception {
        String existingMetadata = mapper.writeValueAsString(existingSampleMetadata);
        String currentMetadata = mapper.writeValueAsString(sampleMetadata);
        return (!metadbJsonComparator.isConsistent(currentMetadata, existingMetadata));
    }
}
