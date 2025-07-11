package org.mskcc.smile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.smile.commons.JsonComparator;
import org.mskcc.smile.model.PatientAlias;
import org.mskcc.smile.model.SampleMetadata;
import org.mskcc.smile.model.SmilePatient;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.internal.PatientIdTriplet;
import org.mskcc.smile.model.web.PublishedSmileSample;
import org.mskcc.smile.model.web.SmileSampleIdMapping;
import org.mskcc.smile.persistence.neo4j.SmileSampleRepository;
import org.mskcc.smile.service.PatientIdMappingService;
import org.mskcc.smile.service.SmilePatientService;
import org.mskcc.smile.service.SmileRequestService;
import org.mskcc.smile.service.SmileSampleService;
import org.mskcc.smile.service.util.SampleDataFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SampleServiceImpl implements SmileSampleService {
    private static final Pattern DMP_PATIENT_ID = Pattern.compile("P-\\d*");

    @Autowired
    private JsonComparator jsonComparator;

    @Autowired
    private SmileSampleRepository sampleRepository;

    @Autowired @Lazy // prevents circular dependencies and initializes when component is first needed
    private SmileRequestService requestService;

    @Autowired @Lazy // prevents circular dependencies and initializes when component is first needed
    private SmilePatientService patientService;

    @Autowired
    private PatientIdMappingService patientIdMappingService;

    private static final Log LOG = LogFactory.getLog(SampleServiceImpl.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public SmileSample saveSmileSample(SmileSample
            sample) throws Exception {
        // sample to return
        SmileSample toReturn;
        if (sample.getPatient() != null) {
            sample = fetchAndLoadPatientDetails(sample);
        }

        SmileSample existingSample =
                sampleRepository.findSampleByPrimaryId(sample.getPrimarySampleAlias());
        if (existingSample == null) {
            UUID newSampleId = sampleRepository.save(sample).getSmileSampleId();
            sample.setSmileSampleId(newSampleId);
            toReturn = sample;
        } else {
            // populate existing sample details and check if there are actual updates to persist
            // note that a patient swap may have happened already at this point
            existingSample = getSmileSample(existingSample.getSmileSampleId());
            SampleMetadata existingMetadata = existingSample.getLatestSampleMetadata();
            SampleMetadata sampleMetadata = sample.getLatestSampleMetadata();

            // Updates SmileSample metadata history with the latest sampleMetadata updates
            existingSample.updateSampleMetadata(sampleMetadata);

            // Updates investigatorId sampleAlias if sampleMetadata investigatorSampleId has been updated
            if (existingSample.getDatasource().equals("igo")
                    && !existingMetadata.getInvestigatorSampleId()
                        .equals(sampleMetadata.getInvestigatorSampleId())) {
                existingSample.updateSampleAlias("investigatorId",
                        sampleMetadata.getInvestigatorSampleId());
            }

            // If there is a TumorOrNormal update in SampleMetadata level,
            // then sampleClass should also be updated in the SmileSample level
            if (!existingSample.getSampleClass().equals(sampleMetadata.getTumorOrNormal())) {
                existingSample.setSampleClass(sampleMetadata.getTumorOrNormal());
            }

            // determine whether a patient swap is required also
            if ((sample.getPatient() != null && existingSample.getPatient() != null)
                    && !sample.getPatient().getSmilePatientId().equals(
                            existingSample.getPatient().getSmilePatientId())) {
                LOG.info("Updating sample-to-patient relationship and removing connection to patient: "
                        + existingSample.getPatient().getSmilePatientId());
                sampleRepository.removeSamplePatientRelationship(existingSample.getSmileSampleId(),
                        existingSample.getPatient().getSmilePatientId());
                // merge aliases from existing patient to the patient we are swapping to
                SmilePatient existingPatient = existingSample.getPatient();
                SmilePatient patientToSwapTo = sample.getPatient();
                Boolean updatedPatientAliases = Boolean.FALSE;
                for (PatientAlias pa : existingPatient.getPatientAliases()) {
                    if (!patientToSwapTo.hasPatientAlias(pa)) {
                        patientToSwapTo.addPatientAlias(pa);
                        updatedPatientAliases = Boolean.TRUE;
                    }
                }
                // save updates to patient we are swapping to if applicable
                if (updatedPatientAliases) {
                    patientToSwapTo = patientService.savePatientMetadata(patientToSwapTo);
                }
                existingSample.setPatient(patientToSwapTo);
            }

            sampleRepository.save(existingSample);
            toReturn = existingSample;
        }
        // update revisable to true for sample
        sampleRepository.updateRevisableBySampleId(toReturn.getSmileSampleId(), Boolean.TRUE);
        return toReturn;
    }

    /**
     * Fetching and loading patient details explained.
     *
     * <p>Scenario #1: new sample, new patient
     *  --> new sample and patient are persisted to the database
     *
     * <p>Scenario #2: existing sample with updates and patient swap
     *  a) patient by cmo id in the incoming metadata updates does not already exist and does not match
     *      the patient linked to the existing sample:
     *      --> patient by the new id is persisted to the database, sample-to-patient relationship
     *      is updated to match the newly persisted patient and the former sample-to-patient relationship
     *      is removed
     * b) patient by cmo id in the incoming metadata updates already exists but does not match
     *      the patient linked to the existing sample:
     *      --> sample-to-patient relationship is updated to match the patient referenced in the incoming
     *      sample updates and the former sample-to-patient relationship is removed
     *
     * <p>Scenario #3: special case where new sample is added to database but there's a mismatch
     *  between the patient that the canonical sample is pointing to and the patient referenced in the
     *  latest sample metadata
     *  a) cmo patient ids do not match
     *      --> construct and persist a new patient node with the cmo id from the latest metadata
     *  b) cmo patient ids match
     *      --> persist patient from sample.getPatient() to database
     *
     * <p>Scenario #4: new sample where sample.getPatient() is null and cmo patient id in latest
     *  metadata is not null and exists in the database
     *  --> throws exception, this is a case that should never happen and would result from malformed data
     * @param sample
     * @return SmileSample
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public SmileSample fetchAndLoadPatientDetails(SmileSample sample) throws Exception {
        SampleMetadata sampleMetadata = sample.getLatestSampleMetadata();
        SmilePatient patient = sample.getPatient();
        if (!patient.hasPatientAlias("dmpId")) {
            PatientIdTriplet result = patientIdMappingService.getPatientIdTripletByInputId(
                    patient.getCmoPatientId().getValue());
            if (result != null) {
                PatientAlias alias = new PatientAlias(result.getDmpPatientId(), "dmpId");
                patient.addPatientAlias(alias);
            }
        }

        // handle the scenario where a patient node does not already exist in the database
        // to prevent any null pointer exceptions (a situation that had arose in some test dmp sample cases)
        SmilePatient patientByPatientCmoId
                = patientService.getPatientByCmoPatientId(patient.getCmoPatientId().getValue());
        if (patientByPatientCmoId == null) {
            patient = patientService.savePatientMetadata(patient);
            sample.setPatient(patient);
        }

        // get patient by cmo id from latest sample metadata
        // note: the patient according to the cmo id in the sample's latest metadata may
        // not match the patient that is linked to an existing sample in the event
        // that a patient swap is needed and is the reason for the incoming sample update
        SmilePatient patientByLatestCmoId = patientService.getPatientByCmoPatientId(
                sampleMetadata.getCmoPatientId());

        // again this is something that should never happen and would arise from some error
        // in the data construction/parsing
        if (patient == null) {
            throw new IllegalStateException("Patient object assigned to the sample is null "
                    + "- confirm whether data construction and parsing is being handled correctly");
        }

        // scenario where we are checking for an update to the existing patient and is linked to the
        // sample in the database but may contain updates (i.e., a new patient alias)
        if (patient.getCmoPatientId().getValue().equals(sampleMetadata.getCmoPatientId())
                && patientByLatestCmoId != null) {
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
                patientByLatestCmoId = patientService.savePatientMetadata(patientByLatestCmoId);
                sample.setPatient(patientByLatestCmoId);
            } else {
                sample.setPatient(patientByLatestCmoId);
            }
            return sample;
        }

        // scenario that requires a patient swap and updating the sample-to-patient relationship
        // in the database and removing the former sample-to-patient relationship
        if (patientByLatestCmoId == null) {
            SmilePatient newPatient = new SmilePatient(sampleMetadata.getCmoPatientId(), "cmoId");
            //if this sample is a clinical sample, we would also need to add dmpId
            if (sample.getSampleCategory().equals("clinical")
                    && !sample.getPatient().hasPatientAlias("dmpId")) {
                Matcher matcher = DMP_PATIENT_ID.matcher(sampleMetadata.getPrimaryId());
                if (matcher.find()) {
                    newPatient.addPatientAlias(new PatientAlias(matcher.group(), "dmpId"));
                }
            }
            newPatient = patientService.savePatientMetadata(newPatient);
            sample.setPatient(newPatient);
            return sample;
        }

        // scenario where the patient that the sample-to-patient relationship points to in the database
        // does not match the cmo patient id referenced in the latest sample metadata updates
        // and the former sample-to-patient relationship needs to be removed
        if (!patient.getCmoPatientId().getValue().equals(sampleMetadata.getCmoPatientId())) {
            sample.setPatient(patientByLatestCmoId);
            return sample;

        }
        return sample;
    }

    @Override
    public Boolean updateSampleMetadata(SampleMetadata sampleMetadata, Boolean fromLims) throws Exception {
        SmileSample existingSample = getResearchSampleByRequestAndIgoId(
                        sampleMetadata.getIgoRequestId(), sampleMetadata.getPrimaryId());
        // new samples may come from IGO_NEW_REQUEST which also invokes this method
        // so if a new sample is encountered we should persist it to the database
        // a new sample without an existing request will not be persisted
        if (existingSample == null) {
            SmileRequest request = requestService.getSmileRequestById(sampleMetadata.getIgoRequestId());
            if (request == null) {
                LOG.error("Failed to persist sample metadata updates, "
                        + "request does not exist " + sampleMetadata.toString());
                return Boolean.FALSE;
            }
            LOG.info("Persisting new sample to db: " + sampleMetadata.getPrimaryId());
            SmileSample sample = SampleDataFactory.buildNewResearchSampleFromMetadata(
                    sampleMetadata.getIgoRequestId(), sampleMetadata,
                    request.getIsCmoRequest(), sampleMetadata.getStatus());
            saveSmileSample(sample);
            createSampleRequestRelationship(sample.getSmileSampleId(), request.getSmileRequestId());
            return Boolean.TRUE;
        }
        // save updates to sample if applicable
        SampleMetadata existingMetadata = existingSample.getLatestSampleMetadata();

        Boolean isResearchSample = existingSample.getSampleCategory().equals("research");
        if (sampleHasMetadataUpdates(existingMetadata, sampleMetadata, isResearchSample,
                fromLims)) {
            // create a sampleMetadata with existing sampleMetadata with accepted field updates
            // from the new sampleMetadata. Set ID to null
            if (fromLims) {
                LOG.info("Persisting igo property updates for sample: " + sampleMetadata.getPrimaryId());
                existingSample.applyIgoLimsUpdates(sampleMetadata);
            } else {
                LOG.info("Persisting updates for sample: " + sampleMetadata.getPrimaryId());
                existingSample.updateSampleMetadata(sampleMetadata);
            }
            saveSmileSample(existingSample);
            return Boolean.TRUE;
        }
        // if sample revisable is false then return true so that message handler
        // publishes message downstream (allows dashboard to make changes to db directly
        // without having to wait or poll for updates)
        if (!existingSample.getRevisable()) {
            sampleRepository.updateRevisableBySampleId(existingSample.getSmileSampleId(), Boolean.TRUE);
            return Boolean.TRUE;
        }

        // no updates to persist to sample, log and return false
        LOG.info("There are no updates to persist for sample: "
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
        SmileSample sample = sampleRepository.findSampleBySampleSmileId(smileSampleId);
        if (sample == null) {
            return null;
        }
        SmilePatient patient = patientService.getPatientBySampleSmileId(smileSampleId);
        sample.setPatient(patient);
        return sample;
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
        return getSmileSample(sample.getSmileSampleId());
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
            SampleMetadata sampleMetadata, Boolean isResearchSample, Boolean fromLims) throws Exception {
        String existingMetadata = mapper.writeValueAsString(existingSampleMetadata);
        String currentMetadata = mapper.writeValueAsString(sampleMetadata);
        // if sample is from LIMS, look for updates by igo properties
        if (fromLims && !jsonComparator.isConsistentByIgoProperties(currentMetadata, existingMetadata)) {
            return Boolean.TRUE;
        }
        // if sample is not from LIMS, look for updates by all properties
        if (!fromLims && !jsonComparator.isConsistent(currentMetadata, existingMetadata)) {
            return Boolean.TRUE;
        }
        // if there is a change to the cmo sample label..
        if (isResearchSample && !existingSampleMetadata.getCmoSampleName()
                .equals(sampleMetadata.getCmoSampleName())) {
            return Boolean.TRUE;
        }
        // if there needs to be a patient swap..
        if (!existingSampleMetadata.getCmoPatientId()
                .equals(sampleMetadata.getCmoPatientId())) {
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
        List<SmileSample> samplesFound = sampleRepository.findAllSamplesByCmoPatientId(cmoPatientId);
        for (SmileSample sample: samplesFound) {
            samples.add(getSmileSample(sample.getSmileSampleId()));
        }
        return samples;
    }

    @Override
    public SmileSample getClinicalSampleByDmpId(String dmpId) throws Exception {
        SmileSample smileSample = sampleRepository.findSampleByPrimaryId(dmpId);
        if (smileSample != null) {
            return getSmileSample(smileSample.getSmileSampleId());
        }
        return smileSample;
    }

    @Override
    public List<SmileSample> getSamplesByCategoryAndCmoPatientId(String cmoPatientId, String sampleCategory)
            throws Exception {
        List<SmileSample> samples = new ArrayList<>();
        for (SmileSample sample: sampleRepository.findAllSamplesByCategoryAndCmoPatientId(cmoPatientId,
                sampleCategory)) {
            samples.add(getSmileSample(sample.getSmileSampleId()));
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
        if (StringUtils.isBlank(importDate)) {
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
    public SmileSample getDetailedSampleByInputId(String inputId) throws Exception {
        SmileSample sample = sampleRepository.findSampleByInputId(inputId);
        if (sample != null) {
            return getSmileSample(sample.getSmileSampleId());
        }
        return null;
    }

    @Override
    public SmileSample getSampleByInputId(String inputId) throws Exception {
        return sampleRepository.findSampleByInputId(inputId);
    }

    @Override
    public void createSampleRequestRelationship(UUID smileSampleId, UUID smileRequestId) {
        sampleRepository.createSampleRequestRelationship(smileSampleId, smileRequestId);
    }

    @Override
    public Boolean sampleExistsByInputId(String inputId) {
        return (sampleRepository.findSampleByInputId(inputId) != null);
    }

    @Override
    public List<SmileSample> getSamplesByCohortId(String cohortId) throws Exception {
        List<SmileSample> samples = sampleRepository.findSamplesByCohortId(cohortId);

        List<SmileSample> detailedSamples = new ArrayList<>();
        for (SmileSample s: samples) {
            detailedSamples.add(getSmileSample(s.getSmileSampleId()));
        }
        return detailedSamples;
    }

    @Override
    public List<SmileSample> getSamplesByCmoSampleName(String cmoSampleName) throws Exception {
        List<SmileSample> samples = sampleRepository.findSamplesByCmoSampleName(cmoSampleName);
        if  (samples == null) {
            return new ArrayList<>();
        }

        List<SmileSample> toReturn = new ArrayList<>();
        for (SmileSample s : samples) {
            SampleMetadata sm = sampleRepository.findLatestSampleMetadataBySmileId(s.getSmileSampleId());
            // ignore cases where latest sample metadata does not match the cmo sample name we're looking for
            if (!sm.getCmoSampleName().equals(cmoSampleName)) {
                continue;
            }
            SmileSample detailedSample = getSmileSample(s.getSmileSampleId());
            toReturn.add(detailedSample);
        }
        return toReturn;
    }

    @Override
    public List<SmileSample> getSamplesByAltId(String altId) throws Exception {
        // alt id is stored in the sample metadata 'additionalProperties' map which
        // is stored in the database as a string so the query needs to specify the
        // altId property name as well: "altId":"<altId>"
        String altIdSearchTerm = new StringBuilder("\"altId\":\"")
                .append(altId)
                .append("\"").toString();

        List<SmileSample> samples = sampleRepository.findSamplesByAltId(altIdSearchTerm);
        if  (samples == null) {
            return new ArrayList<>();
        }

        List<SmileSample> toReturn = new ArrayList<>();
        for (SmileSample s : samples) {
            SmileSample detailedSample = getSmileSample(s.getSmileSampleId());
            toReturn.add(detailedSample);
        }
        return toReturn;
    }

    @Override
    public SampleMetadata getLatestSampleMetadataByPrimaryId(String primaryId) {
        return sampleRepository.findLatestSampleMetadataByPrimaryId(primaryId);
    }

    @Override
    public List<String> getSamplePrimaryIdsBySmileTempoIds(List<UUID> smileTempoIds) throws Exception {
        return sampleRepository.findSamplePrimaryIdsBySmileTempoIds(smileTempoIds);
    }

    @Override
    public String getSamplePrimaryIdBySampleInputId(String inputId) throws Exception {
        return sampleRepository.findSamplePrimaryIdByInputId(inputId);
    }
}
