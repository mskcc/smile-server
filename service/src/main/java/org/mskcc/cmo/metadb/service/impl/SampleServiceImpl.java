package org.mskcc.cmo.metadb.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.common.MetadbJsonComparator;
import org.mskcc.cmo.metadb.model.MetadbPatient;
import org.mskcc.cmo.metadb.model.MetadbRequest;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.model.web.PublishedMetadbSample;
import org.mskcc.cmo.metadb.persistence.neo4j.MetadbSampleRepository;
import org.mskcc.cmo.metadb.service.MetadbPatientService;
import org.mskcc.cmo.metadb.service.MetadbRequestService;
import org.mskcc.cmo.metadb.service.MetadbSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


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
    @Transactional(rollbackFor = {Exception.class})
    public MetadbSample saveMetadbSample(MetadbSample
            sample) throws Exception {
        fetchAndLoadSampleDetails(sample);
        MetadbSample existingSample =
                sampleRepository.findSampleByPrimaryId(sample.getPrimarySampleAlias());
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
        MetadbPatient patient = sample.getPatient();

        // find or save new patient for sample
        MetadbPatient existingPatient = patientService.getPatientByCmoPatientId(
                sampleMetadata.getCmoPatientId());
        if (existingPatient == null) {
            patientService.savePatientMetadata(patient);
            sample.setPatient(patient);
        } else {
            sample.setPatient(existingPatient);
        }

        return sample;
    }

    @Override
    public List<MetadbSample> getMatchedNormalsBySample(
            MetadbSample sample) throws Exception {
        return sampleRepository.findMatchedNormalsByResearchSample(sample);
    }

    @Override
    public List<String> getPooledNormalsBySample(MetadbSample sample) throws Exception {
        MetadbRequest request = requestService.getRequestBySample(sample);
        return request.getPooledNormals();
    }

    @Override
    public MetadbSample getMetadbSample(UUID metadbSampleId) throws Exception {
        MetadbSample sample = sampleRepository.findAllSamplesById(metadbSampleId);
        if (sample == null) {
            return null;
        }
        return getDetailedMetadbSample(sample);
    }

    @Override
    public MetadbSample getResearchSampleByRequestAndIgoId(String requestId, String igoId)
            throws Exception {
        MetadbSample sample = sampleRepository.findResearchSampleByRequestAndIgoId(requestId, igoId);
        if (sample == null) {
            return null;
        }
        return getDetailedMetadbSample(sample);
    }

    @Override
    public List<SampleMetadata> getSampleMetadataListByCmoPatientId(String cmoPatientId) throws Exception {
        return sampleRepository.findAllSampleMetadataByCmoPatientId(cmoPatientId);
    }

    @Override
    public List<MetadbSample> getResearchSamplesByRequestId(String requestId) throws Exception {
        List<MetadbSample> requestSamples = new ArrayList<>();
        for (MetadbSample s : sampleRepository.findResearchSamplesByRequest(requestId)) {
            requestSamples.add(getMetadbSample(s.getMetaDbSampleId()));
        }
        return requestSamples;
    }

    @Override
    public List<SampleMetadata> getResearchSampleMetadataHistoryByIgoId(String igoId) throws Exception {
        return sampleRepository.findSampleMetadataHistoryByNamespaceValue("igoId", igoId);
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
                .findAllSampleMetadataByCmoPatientId(cmoPatientId);
        List<PublishedMetadbSample> samples = new ArrayList<>();
        for (SampleMetadata sample: sampleMetadataList) {
            MetadbSample metadbSample = sampleRepository.findSampleByPrimaryId(sample.getPrimaryId());
            PublishedMetadbSample publishedSample = getPublishedMetadbSample(
                    metadbSample.getMetaDbSampleId());
            samples.add(publishedSample);
        }
        return samples;
    }

    @Override
    public MetadbSample getDetailedMetadbSample(MetadbSample sample) throws ParseException {
        sample.setSampleMetadataList(sampleRepository.findAllSampleMetadataListBySampleId(
                sample.getMetaDbSampleId()));
        String cmoPatientId = sample.getLatestSampleMetadata().getCmoPatientId();
        MetadbPatient patient = patientService.getPatientByCmoPatientId(cmoPatientId);
        sample.setPatient(patient);
        sample.setSampleAliases(sampleRepository.findAllSampleAliases(sample.getMetaDbSampleId()));
        return sample;
    }

    @Override
    public MetadbSample getClinicalSampleByDmpId(String dmpId) throws Exception {
        MetadbSample metadbSample = sampleRepository.findSampleByPrimaryId(dmpId);
        if (metadbSample != null) {
            return getDetailedMetadbSample(metadbSample);
        }
        return metadbSample;
    }

}
