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
import org.mskcc.cmo.metadb.model.web.PublishedMetadbSample;
import org.mskcc.cmo.metadb.persistence.neo4j.MetadbSampleRepository;
import org.mskcc.cmo.metadb.service.MetadbPatientService;
import org.mskcc.cmo.metadb.service.MetadbRequestService;
import org.mskcc.cmo.metadb.service.MetadbSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


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
            sample) throws Exception {
        fetchAndLoadSampleDetails(sample);

        MetadbSample existingSample =
                sampleRepository.findSampleByIgoId(sample.getSampleIgoId());
        if (existingSample == null) {
            UUID newSampleId = sampleRepository.save(sample).getMetaDbSampleId();
            sample.setMetaDbSampleId(newSampleId);
            return sample;
        } else {
            existingSample.addSampleMetadata(sample.getLatestSampleMetadata());
            sampleRepository.save(existingSample);
            return existingSample;
        }
    }

    @Override
    public MetadbSample fetchAndLoadSampleDetails(MetadbSample sample) throws Exception {
        SampleMetadata sampleMetadata = sample.getLatestSampleMetadata();
        sample.setSampleClass(sampleMetadata.getTumorOrNormal());
        sample.addSampleAlias(new SampleAlias(sampleMetadata.getPrimaryId(), "igoId"));
        sample.addSampleAlias(
                new SampleAlias(sampleMetadata.getInvestigatorSampleId(), "investigatorId"));

        // find or save new patient for sample
        MetadbPatient patient = new MetadbPatient();
        patient.addPatientAlias(new PatientAlias(sampleMetadata.getCmoPatientId(), "cmoId"));
        MetadbPatient existingPatient = patientService.getPatientByCmoPatientId(
                sampleMetadata.getCmoPatientId());
        if (existingPatient == null) {
            UUID newPatientId = patientService.savePatientMetadata(patient);
            patient.setMetaDbPatientId(newPatientId);
            sample.setPatient(patient);
            sample.setPatientUuid(newPatientId);
        } else {
            sample.setPatient(existingPatient);
            sample.setPatientUuid(existingPatient.getMetaDbPatientId());
        }

        return sample;
    }

    @Override
    public List<MetadbSample> getMatchedNormalsBySample(
            MetadbSample sample) throws Exception {
        return sampleRepository.findMatchedNormalsBySample(sample);
    }

    @Override
    public List<String> getPooledNormalsBySample(MetadbSample sample) throws Exception {
        MetadbRequest request = requestService.getRequestBySample(sample);
        return request.getPooledNormals();
    }

    @Override
    public MetadbSample getMetadbSample(UUID metadbSampleId) throws Exception {
        MetadbSample sample = sampleRepository.findSampleById(metadbSampleId);
        if (sample == null) {
            return null;
        }
        sample.setSampleMetadataList(sampleRepository.findSampleMetadataListBySampleId(metadbSampleId));
        String cmoPatientId = sample.getLatestSampleMetadata().getCmoPatientId();
        MetadbPatient patient = patientService.getPatientByCmoPatientId(cmoPatientId);
        sample.setPatient(patient);
        sample.setSampleAliases(sampleRepository.findAllSampleAliases(metadbSampleId));
        return sample;
    }

    @Override
    public MetadbSample getMetadbSampleByRequestAndAlias(String requestId, SampleAlias igoId)
            throws Exception {
        MetadbSample sample = sampleRepository.findSampleByRequestAndIgoId(requestId,
                igoId.getValue());
        if (sample == null) {
            return null;
        }
        sample.setSampleMetadataList(sampleRepository
                .findSampleMetadataListBySampleId(sample.getMetaDbSampleId()));
        String cmoPatientId = sample.getLatestSampleMetadata().getCmoPatientId();
        MetadbPatient patient = patientService.getPatientByCmoPatientId(cmoPatientId);
        sample.setPatient(patient);
        sample.setSampleAliases(sampleRepository.findAllSampleAliases(sample.getMetaDbSampleId()));
        return sample;
    }

    @Override
    public MetadbSample getMetadbSampleByRequestAndIgoId(String requestId, String igoId)
            throws Exception {
        MetadbSample sample = sampleRepository.findSampleByRequestAndIgoId(requestId, igoId);
        if (sample == null) {
            return null;
        }
        sample.setSampleMetadataList(sampleRepository
                .findSampleMetadataListBySampleId(sample.getMetaDbSampleId()));
        String cmoPatientId = sample.getLatestSampleMetadata().getCmoPatientId();
        MetadbPatient patient = patientService.getPatientByCmoPatientId(cmoPatientId);
        sample.setPatient(patient);
        sample.setSampleAliases(sampleRepository.findAllSampleAliases(sample.getMetaDbSampleId()));
        return sample;
    }

    @Override
    public List<SampleMetadata> getSampleMetadataListByCmoPatientId(String cmoPatientId) throws Exception {
        return sampleRepository.findSampleMetadataListByCmoPatientId(cmoPatientId);
    }

    @Override
    public List<MetadbSample> getAllSamplesByRequestId(String requestId) throws Exception {
        List<MetadbSample> requestSamples = new ArrayList<>();
        for (MetadbSample s : sampleRepository.findAllSamplesByRequest(requestId)) {
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

    @Override
    public PublishedMetadbSample getPublishedMetadbSample(UUID metadbSampleId) throws Exception {
        MetadbSample sample = getMetadbSample(metadbSampleId);
        return new PublishedMetadbSample(sample);
    }

    @Override
    public List<PublishedMetadbSample> getPublishedMetadbSampleListByCmoPatientId(
            String cmoPatientId) throws Exception {
        List<SampleMetadata> sampleMetadataList = sampleRepository
                .findSampleMetadataListByCmoPatientId(cmoPatientId);
        List<PublishedMetadbSample> samples = new ArrayList<>();
        for (SampleMetadata sample: sampleMetadataList) {
            MetadbSample metadbSample = sampleRepository.findSampleByRequestAndIgoId(
                    sample.getRequestId(), sample.getIgoId());
            PublishedMetadbSample publishedSample = getPublishedMetadbSample(
                    metadbSample.getMetaDbSampleId());
            samples.add(publishedSample);
        }
        return samples;
    }
}
