package org.mskcc.cmo.metadb.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.common.MetadbJsonComparator;
import org.mskcc.cmo.metadb.model.MetadbPatient;
import org.mskcc.cmo.metadb.model.MetadbRequest;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.PatientAlias;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.service.MetadbRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.mskcc.cmo.metadb.service.MetadbPatientService;
import org.mskcc.cmo.metadb.service.MetadbSampleService;
import org.mskcc.cmo.metadb.persistence.MetadbSampleRepository;

@Component
public class SampleServiceImpl implements MetadbSampleService {
    @Autowired
    private MetadbJsonComparator metadbJsonComparator;

    @Autowired
    private MetadbRequestService requestService;

    @Autowired
    private MetadbSampleRepository sampleRepository;

    @Autowired
    private MetadbPatientService patientService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public MetadbSample saveSampleMetadata(MetadbSample
            metaDbSample) throws Exception {
        MetadbSample updatedMetaDbSample = setUpMetaDbSample(metaDbSample);

        MetadbSample existingMetaDbSample =
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
    public MetadbSample setUpMetaDbSample(MetadbSample metaDbSample) throws Exception {
        SampleMetadata sampleMetadata = metaDbSample.getLatestSampleMetadata();
        metaDbSample.setSampleClass(sampleMetadata.getTumorOrNormal());
        metaDbSample.addSampleAlias(new SampleAlias(sampleMetadata.getIgoId(), "igoId"));
        metaDbSample.addSampleAlias(
                new SampleAlias(sampleMetadata.getInvestigatorSampleId(), "investigatorId"));

        // TODO: CONSIDER MOVING THIS TO PATIENT SERVICE?
        // fetch existing patient from database or persist new patient node
        MetadbPatient patient = new MetadbPatient();
        patient.addPatientAlias(new PatientAlias(sampleMetadata.getCmoPatientId(), "cmoId"));
        MetadbPatient existingPatient = patientService.getPatientByCmoPatientId(
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
    public List<MetadbSample> getMatchedNormalsBySample(
            MetadbSample metaDbSample) throws Exception {
        return sampleRepository.findMatchedNormalsBySample(metaDbSample);
    }

    @Override
    public List<String> getPooledNormalsBySample(MetadbSample metaDbSample) throws Exception {
        MetadbRequest request = requestService.getRequestBySample(metaDbSample);
        return request.getPooledNormals();
    }

    @Override
    public MetadbSample getMetadbSample(UUID metaDbSampleId) throws Exception {
        MetadbSample metaDbSample = sampleRepository.findMetaDbSampleById(metaDbSampleId);
        metaDbSample.setSampleMetadataList(sampleRepository.findSampleMetadataListBySampleId(metaDbSampleId));
        for (SampleMetadata s: metaDbSample.getSampleMetadataList()) {
            s.setMetaDbPatientId(patientService.getPatientIdBySample(metaDbSampleId));
            s.setMetaDbSampleId(metaDbSampleId);
        }
        metaDbSample.setSampleAliases(sampleRepository.findAllSampleAlias(metaDbSampleId));
        return metaDbSample;
    }

    @Override
    public MetadbSample getMetadbSampleByRequestAndAlias(String requestId, SampleAlias igoId)
            throws Exception {
        MetadbSample metadbSample = sampleRepository.findMetaDbSampleByRequestAndIgoId(requestId,
                igoId.getSampleId());
        metadbSample.setSampleMetadataList(sampleRepository
                .findSampleMetadataListBySampleId(metadbSample.getMetaDbSampleId()));
        for (SampleMetadata s : metadbSample.getSampleMetadataList()) {
            s.setMetaDbPatientId(patientService.getPatientIdBySample(metadbSample.getMetaDbSampleId()));
            s.setMetaDbSampleId(metadbSample.getMetaDbSampleId());
        }
        metadbSample.setSampleAliases(sampleRepository.findAllSampleAlias(metadbSample.getMetaDbSampleId()));
        return metadbSample;
    }

    @Override
    public MetadbSample getMetadbSampleByRequestAndIgoId(String requestId, String igoId)
            throws Exception {
        MetadbSample metadbSample = sampleRepository.findMetaDbSampleByRequestAndIgoId(requestId, igoId);
        metadbSample.setSampleMetadataList(sampleRepository
                .findSampleMetadataListBySampleId(metadbSample.getMetaDbSampleId()));

        String cmoPatientId = metadbSample.getLatestSampleMetadata().getCmoPatientId();

        MetadbPatient pt = patientService.getPatientByCmoPatientId(cmoPatientId);
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
    public List<MetadbSample> getAllMetadbSamplesByRequestId(String requestId) throws Exception {
        List<MetadbSample> requestSamples = new ArrayList<>();
        for (MetadbSample s : sampleRepository.findAllMetaDbSamplesByRequest(requestId)) {
            requestSamples.add(getMetadbSample(s.getMetaDbSampleId()));
        }
        return requestSamples;
    }

    @Override
    public List<SampleMetadata> getSampleMetadataHistoryByIgoId(String igoId) throws Exception {
        return sampleRepository.findSampleMetadataHistoryByIgoId(igoId);
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
