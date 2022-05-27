package org.mskcc.smile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.smile.commons.JsonComparator;
import org.mskcc.smile.model.PatientAlias;
import org.mskcc.smile.model.SampleMetadata;
import org.mskcc.smile.model.SmilePatient;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.web.PublishedSmileSample;
import org.mskcc.smile.model.web.SmileSampleIdMapping;
import org.mskcc.smile.persistence.neo4j.SmileSampleRepository;
import org.mskcc.smile.service.SmilePatientService;
import org.mskcc.smile.service.SmileRequestService;
import org.mskcc.smile.service.SmileSampleService;
import org.mskcc.smile.service.util.SampleDataFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
public class SampleServiceImpl implements SmileSampleService {
    @Autowired
    private JsonComparator jsonComparator;

    @Autowired
    private SmileRequestService requestService;

    @Autowired
    private SmileSampleRepository sampleRepository;

    @Autowired
    private SmilePatientService patientService;

    private static final Log LOG = LogFactory.getLog(SampleServiceImpl.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public SmileSample saveSmileSample(SmileSample
            smileSample) throws Exception {
        SmileSample sample = fetchAndLoadPatientDetails(smileSample);
        SmileSample existingSample =
                sampleRepository.findSampleByPrimaryId(sample.getPrimarySampleAlias());
        if (existingSample == null) {
            UUID newSampleId = sampleRepository.save(sample).getSmileSampleId();
            sample.setSmileSampleId(newSampleId);
            return sample;
        } else {
            SmileSample detailedExistingSample = getDetailedSmileSample(existingSample);

            detailedExistingSample.updateSampleMetadata(sample.getLatestSampleMetadata());
            detailedExistingSample.setPatient(sample.getPatient());
            sampleRepository.save(detailedExistingSample);

            return detailedExistingSample;
        }
    }

    @Override
    public SmileSample fetchAndLoadPatientDetails(SmileSample sample) throws Exception {
        SampleMetadata sampleMetadata = sample.getLatestSampleMetadata();
        SmilePatient patient = sample.getPatient();
        if (patientService.getPatientByCmoPatientId(
               sample.getPatient().getCmoPatientId().getValue()) == null) {
            patientService.savePatientMetadata(patient);
            sample.setPatient(patient);
        }
        // find or save new patient for sample
        SmilePatient patientByLatestCmoId = patientService.getPatientByCmoPatientId(
                sampleMetadata.getCmoPatientId());

        // Case 2: For a new sample, patient and patientByLatestCmoId are both null --> set up a new sample
        // with the new cmoPatientId and save this patient to db
        // Case 1: For sample updates, if patient exists but doesn't match
        //patientByLatestCmoId --> cmoPatientId swap
        // Case 1a: If patientByLatestCmoId is null, perisit it to db and update the sample with
        //the new patient and remove the old relationship
        // Case 1b: If patientByLatestCmoId is not null, update the sample with the patient
        //and remove the old relationship
        // Case 3: For a new sample, patient is not null and patientByLatestCmoId is null
        // Case 3a: patient and patientByLatestCmoId have different cmoPatientIds --> set up a
        //new patient with cmoPatientId
        // from patientByLatestCmoId and persist this patient
        // Case 3b: patient and patientByLatestCmoId have the same cmoPatientIds --> persist patient to db
        // Case 4: For a new sample, patient is null and patientByLatestCmoId is not null
        if (patient == null) {
            throw new RuntimeException("Patient object assigned to the sample is null");
        } else if (patientByLatestCmoId == null) {
            // Former sample-patient relationship will be removed in saveSmileSample
            //set up a new patient with the latest cmoPatientId and save it
            SmilePatient newPatient = new SmilePatient();
            newPatient.addPatientAlias(new PatientAlias(sampleMetadata.getCmoPatientId(), "cmoId"));
            SmilePatient savedPatient = patientService.savePatientMetadata(newPatient);
            sample.setPatient(savedPatient);
            sampleRepository.removeSamplePatientRelationship(sample.getSmileSampleId(),
                    patient.getSmilePatientId());
        } else if (patient.getCmoPatientId().getValue().equals(sampleMetadata.getCmoPatientId())) {
            // go through the new patient aliases and indicator for whether a
            // new patient alias was added to the existing patient
            Boolean patientUpdated = Boolean.FALSE;
            for (PatientAlias pa : patient.getPatientAliases()) {
                if (!patientByLatestCmoId.hasPatientAlias(pa)) {
                    patientByLatestCmoId.addPatientAlias(pa);
                    patientUpdated = Boolean.TRUE;
                }
            }
            if (patientUpdated) {
                sample.setPatient(patientService.savePatientMetadata(patientByLatestCmoId));
            } else {
                sample.setPatient(patientByLatestCmoId);
            }
        } else if (!patient.getCmoPatientId().getValue().equals(sampleMetadata.getCmoPatientId())) {
            // Former sample-patient relationship will be removed in saveSmileSample
            sample.setPatient(patientByLatestCmoId);
            sampleRepository.removeSamplePatientRelationship(sample.getSmileSampleId(),
                    patient.getSmilePatientId());

        }
        return sample;
    }

    @Override
    public Boolean updateSampleMetadata(SampleMetadata sampleMetadata) throws Exception {
        SmileSample existingSample = getResearchSampleByRequestAndIgoId(
                        sampleMetadata.getIgoRequestId(), sampleMetadata.getPrimaryId());
        // new samples may come from IGO_NEW_REQUEST which also invokes this method
        // so if a new sample is encountered we should persist it to the database
        if (existingSample == null) {
            LOG.info("Persisting new sample to db: " + sampleMetadata.getPrimaryId());
            SmileSample sample = SampleDataFactory.buildNewResearchSampleFromMetadata(
                    sampleMetadata.getIgoRequestId(), sampleMetadata);
            saveSmileSample(sample);
            return Boolean.TRUE;
        }
        // save updates to sample if applicable
        SampleMetadata existingMetadata = existingSample.getLatestSampleMetadata();
        if (sampleHasMetadataUpdates(existingMetadata, sampleMetadata)
                || (!sampleHasMetadataUpdates(
                        existingMetadata, sampleMetadata))
                && !existingMetadata.getCmoSampleName()
                        .equals(sampleMetadata.getCmoSampleName())) {
            LOG.info("Persisting updates for sample: " + sampleMetadata.getPrimaryId());
            existingSample.updateSampleMetadata(sampleMetadata);
            saveSmileSample(existingSample);
            return Boolean.TRUE;
        }
        // no updates to persist to sample, log and return false
        LOG.info("There are no updates to persist for research sample: "
                + sampleMetadata.getPrimaryId());
        return Boolean.FALSE;
    }

    @Override
    public List<SmileSample> getMatchedNormalsBySample(
            SmileSample sample) throws Exception {
        return sampleRepository.findMatchedNormalsByResearchSample(sample);
    }

    @Override
    public List<String> getPooledNormalsBySample(SmileSample sample) throws Exception {
        SmileRequest request = requestService.getRequestBySample(sample);
        return request.getPooledNormals();
    }

    @Override
    public SmileSample getSmileSample(UUID smileSampleId) throws Exception {
        SmileSample sample = sampleRepository.findSampleById(smileSampleId);
        if (sample == null) {
            return null;
        }
        return getDetailedSmileSample(sample);
    }

    @Override
    public SmileSample getResearchSampleByRequestAndIgoId(String requestId, String igoId)
            throws Exception {
        if (requestId == null) {
            LOG.error("Cannot query for research sample without a request ID "
                    + "- confirm that request ID is provided in the incoming data for sample: " + igoId);
            return null;
        }
        SmileSample sample = sampleRepository.findResearchSampleByRequestAndIgoId(requestId, igoId);
        if (sample == null) {
            return null;
        }
        return getDetailedSmileSample(sample);
    }

    @Override
    public List<SmileSample> getResearchSamplesByRequestId(String requestId) throws Exception {
        List<SmileSample> requestSamples = new ArrayList<>();
        for (SmileSample s : sampleRepository.findResearchSamplesByRequest(requestId)) {
            requestSamples.add(getSmileSample(s.getSmileSampleId()));
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
            jsonComparator.isConsistent(currentMetadata, existingMetadata);
        } catch (AssertionError e) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public PublishedSmileSample getPublishedSmileSample(UUID smileSampleId) throws Exception {
        SmileSample sample = getSmileSample(smileSampleId);
        return new PublishedSmileSample(sample);
    }

    @Override
    public List<PublishedSmileSample> getPublishedSmileSamplesByCmoPatientId(
            String cmoPatientId) throws Exception {
        List<SmileSample> sampleList = sampleRepository
                .findAllSamplesByCmoPatientId(cmoPatientId);
        List<PublishedSmileSample> samples = new ArrayList<>();
        for (SmileSample sample: sampleList) {
            PublishedSmileSample publishedSample = getPublishedSmileSample(
                    sample.getSmileSampleId());
            samples.add(publishedSample);
        }
        return samples;
    }

    @Override
    public List<SmileSample> getSamplesByCmoPatientId(String cmoPatientId) throws Exception {
        List<SmileSample> samples = new ArrayList<>();
        for (SmileSample sample: sampleRepository.findAllSamplesByCmoPatientId(cmoPatientId)) {
            samples.add(getDetailedSmileSample(sample));
        }
        return samples;
    }

    @Override
    public SmileSample getDetailedSmileSample(SmileSample sample) throws ParseException {
        sample.setSampleMetadataList(sampleRepository.findAllSampleMetadataListBySampleId(
                sample.getSmileSampleId()));
        String cmoPatientId = sample.getLatestSampleMetadata().getCmoPatientId();
        SmilePatient patient = patientService.getPatientByCmoPatientId(cmoPatientId);
        sample.setPatient(patient);
        sample.setSampleAliases(sampleRepository.findAllSampleAliases(sample.getSmileSampleId()));
        return sample;
    }

    @Override
    public SmileSample getClinicalSampleByDmpId(String dmpId) throws Exception {
        SmileSample smileSample = sampleRepository.findSampleByPrimaryId(dmpId);
        if (smileSample != null) {
            return getDetailedSmileSample(smileSample);
        }
        return smileSample;
    }

    @Override
    public List<SmileSample> getSamplesByCategoryAndCmoPatientId(String cmoPatientId, String sampleCategory)
            throws Exception {
        List<SmileSample> samples = new ArrayList<>();
        for (SmileSample sample: sampleRepository.findAllSamplesByCategoryAndCmoPatientId(cmoPatientId,
                sampleCategory)) {
            samples.add(getDetailedSmileSample(sample));
        }
        return samples;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void updateSamplePatientRelationship(UUID smileSampleId, UUID smilePatientId) {
        sampleRepository.updateSamplePatientRelationship(smileSampleId, smilePatientId);
    }

    @Override
    public List<SmileSampleIdMapping> getSamplesByDate(String importDate) {
        if (Strings.isNullOrEmpty(importDate)) {
            throw new RuntimeException("Start date " + importDate + " cannot be null or empty");
        }
        // return latest sample metadata for each sample uuid returned
        List<UUID> sampleIds = sampleRepository.findSamplesByLatestImportDate(importDate);
        if (sampleIds == null) {
            return null;
        }
        List<SmileSampleIdMapping> sampleIdsList = new ArrayList<>();
        for (UUID smileSampleId : sampleIds) {
            SampleMetadata sm = sampleRepository.findLatestSampleMetadataBySmileId(smileSampleId);
            sampleIdsList.add(new SmileSampleIdMapping(smileSampleId, sm));
        }
        return sampleIdsList;
    }

    @Override
    public SmileSample getSampleByInputId(String inputId) {
        return sampleRepository.findSampleByInputId(inputId);
    }
}
