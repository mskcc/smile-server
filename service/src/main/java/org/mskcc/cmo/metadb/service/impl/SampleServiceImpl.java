package org.mskcc.cmo.metadb.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.common.MetadbJsonComparator;
import org.mskcc.cmo.metadb.model.MetaDbPatient;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.PatientAlias;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.persistence.MetaDbSampleRepository;
import org.mskcc.cmo.metadb.service.PatientService;
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
    private PatientService patientService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public MetaDbSample saveSampleMetadata(MetaDbSample
            metaDbSample) throws Exception {
        MetaDbSample updatedMetaDbSample = setUpMetaDbSample(metaDbSample);

        MetaDbSample existingMetaDbSample =
                sampleRepository.findMetaDbSampleByIgoId(updatedMetaDbSample.getSampleIgoId());
        if (existingMetaDbSample == null) {
            UUID newSampleId = sampleRepository.save(updatedMetaDbSample).getMetaDbSampleId();
            updatedMetaDbSample.setMetaDbSampleId(newSampleId);
            return updatedMetaDbSample;
        } else {
            existingMetaDbSample.addSampleMetadata(updatedMetaDbSample.getLatestSampleMetadata());
            sampleRepository.save(existingMetaDbSample);
            return existingMetaDbSample;
        }
    }

    @Override
    public MetaDbSample setUpMetaDbSample(MetaDbSample metaDbSample) throws Exception {
        SampleMetadata sampleMetadata = metaDbSample.getLatestSampleMetadata();
        metaDbSample.setSampleClass(sampleMetadata.getTumorOrNormal());
        metaDbSample.addSampleAlias(new SampleAlias(sampleMetadata.getIgoId(), "igoId"));
        metaDbSample.addSampleAlias(
                new SampleAlias(sampleMetadata.getInvestigatorSampleId(), "investigatorId"));

        // TODO: CONSIDER MOVING THIS TO PATIENT SERVICE?
        // fetch existing patient from database or persist new patient node
        MetaDbPatient patient = new MetaDbPatient();
        patient.addPatientAlias(new PatientAlias(sampleMetadata.getCmoPatientId(), "cmoId"));
        MetaDbPatient existingPatient = patientService.findPatientByCmoPatientId(
                sampleMetadata.getCmoPatientId());
        if (existingPatient == null) {
            UUID newPatientId = patientService.savePatientMetadata(patient);
            patient.setMetaDbPatientId(newPatientId);
            metaDbSample.setPatient(patient);
            metaDbSample.setPatientUuid(newPatientId);
        } else {
            metaDbSample.setPatient(existingPatient);
            metaDbSample.setPatientUuid(existingPatient.getMetaDbPatientId());
        }

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
            s.setMetaDbPatientId(patientService.findPatientIdBySample(metaDbSampleId));
            s.setMetaDbSampleId(metaDbSampleId);
        }
        metaDbSample.setSampleAliases(sampleRepository.findAllSampleAlias(metaDbSampleId));
        return metaDbSample;
    }

    @Override
    public MetaDbSample getMetaDbSampleByRequestAndAlias(String requestId, SampleAlias igoId)
            throws Exception {
        MetaDbSample metadbSample = sampleRepository.findMetaDbSampleByRequestAndIgoId(requestId,
                igoId.getSampleId());
        metadbSample.setSampleMetadataList(sampleRepository
                .findSampleMetadataListBySampleId(metadbSample.getMetaDbSampleId()));
        for (SampleMetadata s : metadbSample.getSampleMetadataList()) {
            s.setMetaDbPatientId(patientService.findPatientIdBySample(metadbSample.getMetaDbSampleId()));
            s.setMetaDbSampleId(metadbSample.getMetaDbSampleId());
        }
        metadbSample.setSampleAliases(sampleRepository.findAllSampleAlias(metadbSample.getMetaDbSampleId()));
        return metadbSample;
    }

    @Override
    public MetaDbSample getMetaDbSampleByRequestAndIgoId(String requestId, String igoId)
            throws Exception {
        MetaDbSample metadbSample = sampleRepository.findMetaDbSampleByRequestAndIgoId(requestId, igoId);
        metadbSample.setSampleMetadataList(sampleRepository
                .findSampleMetadataListBySampleId(metadbSample.getMetaDbSampleId()));

        String cmoPatientId = metadbSample.getLatestSampleMetadata().getCmoPatientId();

        MetaDbPatient pt = patientService.findPatientByCmoPatientId(cmoPatientId);
        metadbSample.setPatient(pt);

        for (SampleMetadata s : metadbSample.getSampleMetadataList()) {
            s.setMetaDbPatientId(pt.getMetaDbPatientId());
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
        return sampleRepository.getSampleMetadataHistoryByIgoId(igoId);
    }

    @Override
    public Boolean sampleHasMetadataUpdates(SampleMetadata existingSampleMetadata,
            SampleMetadata sampleMetadata) throws Exception {
        String existingMetadata = mapper.writeValueAsString(existingSampleMetadata);
        String currentMetadata = mapper.writeValueAsString(sampleMetadata);
        try {
            metadbJsonComparator.isConsistent(currentMetadata, existingMetadata);
        } catch (AssertionError e) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
}
