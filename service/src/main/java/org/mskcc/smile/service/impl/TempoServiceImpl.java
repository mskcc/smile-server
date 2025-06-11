package org.mskcc.smile.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.util.Strings;
import org.mskcc.smile.commons.generated.Smile.TempoSample;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.tempo.BamComplete;
import org.mskcc.smile.model.tempo.MafComplete;
import org.mskcc.smile.model.tempo.QcComplete;
import org.mskcc.smile.model.tempo.Tempo;
import org.mskcc.smile.model.tempo.json.SampleBillingJson;
import org.mskcc.smile.persistence.neo4j.TempoRepository;
import org.mskcc.smile.service.SmileRequestService;
import org.mskcc.smile.service.SmileSampleService;
import org.mskcc.smile.service.TempoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author ochoaa
 */
@Component
public class TempoServiceImpl implements TempoService {

    @Autowired
    private TempoRepository tempoRepository;

    @Autowired @Lazy // prevents circular dependencies and initializes when component is first needed
    private SmileSampleService sampleService;

    @Autowired @Lazy // prevents circular dependencies and initializes when component is first needed
    private SmileRequestService requestService;

    private static final Log LOG = LogFactory.getLog(TempoServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int EMBARGO_PERIOD_MONTHS = 18;

    public static final String ACCESS_LEVEL_EMBARGO = "MSK Embargo";
    public static final String ACCESS_LEVEL_PUBLIC = "MSK Public";


    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Tempo saveTempoData(Tempo tempo) throws Exception {
        SmileSample sample = tempo.getSmileSample();
        Tempo existingTempo = tempoRepository.findTempoBySmileSampleId(sample.getSmileSampleId());
        if (existingTempo != null) {
            tempo.setSmileTempoId(existingTempo.getSmileTempoId());
        }

        // if sample is a normal sample then no need to set values for custodian
        // information or access level. Normal samples do not require this info.
        if (!sample.getSampleClass().equalsIgnoreCase("Normal")) {
            populateTempoData(sample, tempo);
        }
        return tempoRepository.save(tempo);
    }

    @Override
    public Tempo getTempoDataBySampleId(SmileSample smileSample) throws Exception {
        Tempo tempo = tempoRepository.findTempoBySmileSampleId(smileSample.getSmileSampleId());
        if (tempo == null) {
            return null;
        }
        tempo.setSmileSample(smileSample);
        return getDetailedTempoData(tempo);
    }

    @Override
    public Tempo getTempoDataBySamplePrimaryId(String primaryId) throws Exception {
        Tempo tempo = tempoRepository.findTempoBySamplePrimaryId(primaryId);
        if (tempo == null) {
            return null;
        }
        return getDetailedTempoData(tempo, primaryId);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Tempo mergeBamCompleteEventBySamplePrimaryId(String primaryId, BamComplete bamCompleteEvent)
            throws Exception {
        if (getTempoDataBySamplePrimaryId(primaryId) == null) {
            initAndSaveDefaultTempoData(primaryId);
        }
        Tempo tempo = tempoRepository.mergeBamCompleteEventBySamplePrimaryId(primaryId, bamCompleteEvent);
        return getDetailedTempoData(tempo, primaryId);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Tempo mergeQcCompleteEventBySamplePrimaryId(String primaryId, QcComplete qcCompleteEvent)
            throws Exception {
        if (getTempoDataBySamplePrimaryId(primaryId) == null) {
            initAndSaveDefaultTempoData(primaryId);
        }
        Tempo tempo = tempoRepository.mergeQcCompleteEventBySamplePrimaryId(primaryId, qcCompleteEvent);
        return getDetailedTempoData(tempo, primaryId);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Tempo mergeMafCompleteEventBySamplePrimaryId(String primaryId, MafComplete mafCompleteEvent)
            throws Exception {
        if (getTempoDataBySamplePrimaryId(primaryId) == null) {
            initAndSaveDefaultTempoData(primaryId);
        }
        Tempo tempo = tempoRepository.mergeMafCompleteEventBySamplePrimaryId(primaryId, mafCompleteEvent);
        return getDetailedTempoData(tempo, primaryId);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Tempo initAndSaveDefaultTempoData(String primaryId, CohortComplete cc) throws Exception {
        SmileSample sample = sampleService.getSampleByInputId(primaryId);
        Tempo tempo = new Tempo(sample);

        // if sample is a normal sample then no need to set values for custodian
        // information or access level. Normal samples do not require this info.
        if (!sample.getSampleClass().equalsIgnoreCase("Normal")) {
            populateTempoData(sample, tempo, cc);
        }
        return tempoRepository.save(tempo);
    }

    private Tempo getDetailedTempoData(Tempo tempo, String inputSampleId) throws Exception {
        if (tempo == null || tempo.getSmileTempoId() == null) {
            return null;
        }
        SmileSample sample = sampleService.getDetailedSampleByInputId(inputSampleId);
        tempo.setSmileSample(sample);
        tempo.setBamCompleteEvents(tempoRepository.findBamCompleteEventsByTempoId(tempo.getSmileTempoId()));
        tempo.setQcCompleteEvents(tempoRepository.findQcCompleteEventsByTempoId(tempo.getSmileTempoId()));
        tempo.setMafCompleteEvents(tempoRepository.findMafCompleteEventsByTempoId(tempo.getSmileTempoId()));
        return tempo;
    }

    private Tempo getDetailedTempoData(Tempo tempo) throws Exception {
        if (tempo == null || tempo.getSmileTempoId() == null) {
            return null;
        }
        tempo.setBamCompleteEvents(tempoRepository.findBamCompleteEventsByTempoId(tempo.getSmileTempoId()));
        tempo.setQcCompleteEvents(tempoRepository.findQcCompleteEventsByTempoId(tempo.getSmileTempoId()));
        tempo.setMafCompleteEvents(tempoRepository.findMafCompleteEventsByTempoId(tempo.getSmileTempoId()));
        return tempo;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void updateSampleBilling(SampleBillingJson billing) throws Exception {
        tempoRepository.updateSampleBilling(billing);
    }

    private LocalDate getInitialPipelineRunDateBySamplePrimaryId(String primaryId) throws Exception {
        String dateString = tempoRepository.findInitialPipelineRunDateBySamplePrimaryId(primaryId);
        if (StringUtils.isEmpty(dateString)) {
            LOG.debug("No Initial Pipeline Run Date found for sample with Primary ID: " + primaryId);
            return null;
        }
        DateTimeFormatter runDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime initialPipelineRunDate = LocalDateTime.parse(dateString, runDateFormat);
        return initialPipelineRunDate.toLocalDate(); // return date only
    }

    private void populateTempoData(SmileSample sample, Tempo tempo, CohortComplete cc) throws Exception {
        SmileRequest request = requestService.getRequestBySample(sample);
        String custodianInformation = Strings.isBlank(request.getLabHeadName())
                ? request.getLabHeadName() : request.getInvestigatorName();
        tempo.setCustodianInformation(custodianInformation);

        String accessLevel = tempo.getAccessLevel();
        String primaryId = sample.getPrimarySampleAlias();
        LocalDate initialPipelineRunDate = getInitialPipelineRunDateBySamplePrimaryId(primaryId); // from database
        // if initial pipeline run date from database is null (sample not part of existing cohort) then
        // fall back on cohort complete date value and set initial pipeline run date/embargo date based on that

        if (initialPipelineRunDate != null) {
            LocalDate embargoDate = initialPipelineRunDate.plusMonths(EMBARGO_PERIOD_MONTHS);
            tempo.setInitialPipelineRunDate(initialPipelineRunDate.format(DATE_FORMATTER));
            tempo.setEmbargoDate(embargoDate.format(DATE_FORMATTER));
            if (!sampleIsMarkedAsPublic(accessLevel)) {
                // we release the sample the day after the embargo ends (confirmed with PMs)
                tempo.setAccessLevel(LocalDate.now().isAfter(embargoDate)
                        ? ACCESS_LEVEL_PUBLIC : ACCESS_LEVEL_EMBARGO);
            }
        } else {
            if (!sampleIsMarkedAsPublic(accessLevel)) {
                tempo.setAccessLevel(ACCESS_LEVEL_EMBARGO);
            }
        }
    }

    /**
     * Check whether the sample's access level indicates that it is marked as public.
     * Applicable values are often "MSK Public" or "Published (PMID 12345678)".
     * "MSK Public" can be set dynamically as well as manually assigned by the PMs based on
     * who paid for the sequencing.
     *
    */
    private Boolean sampleIsMarkedAsPublic(String accessLevel) {
        if (StringUtils.isEmpty(accessLevel)) {
            return Boolean.FALSE;
        }
        String accessLevelLower = accessLevel.toLowerCase();
        return accessLevelLower.contains("public") || accessLevelLower.contains("publish")
            || accessLevelLower.contains("pmid");
    }

    @Override
    public List<UUID> getTempoIdsNoLongerEmbargoed() throws Exception {
        return tempoRepository.findTempoIdsNoLongerEmbargoed();
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void updateTempoAccessLevel(List<String> samplePrimaryIds, String accessLevel) throws Exception {
        tempoRepository.updateTempoAccessLevelBySamplePrimaryIds(samplePrimaryIds, accessLevel);
    }

    @Override
    public TempoSample getTempoSampleDataBySamplePrimaryId(String primaryId) throws Exception {
        Map<String, Object> tempoSampleMap = tempoRepository.findTempoSampleDataBySamplePrimaryId(primaryId);
        if (tempoSampleMap == null) {
            return null;
        }

        // build tempo sample object
        TempoSample tempoSample = TempoSample.newBuilder()
            .setPrimaryId(primaryId)
            .setCmoSampleName(
                    StringUtils.defaultIfBlank((String) tempoSampleMap.get("cmoSampleName"), ""))
            .setAccessLevel(
                    StringUtils.defaultIfBlank((String) tempoSampleMap.get("accessLevel"), ""))
            .setCustodianInformation(
                    StringUtils.defaultIfBlank((String) tempoSampleMap.get("custodianInformation"), ""))
            .setBaitSet(
                    StringUtils.defaultIfBlank((String) tempoSampleMap.get("baitSet"), ""))
            .setGenePanel(
                    StringUtils.defaultIfBlank((String) tempoSampleMap.get("genePanel"), ""))
            .setOncotreeCode(
                    StringUtils.defaultIfBlank((String) tempoSampleMap.get("oncotreeCode"), ""))
            .setCmoPatientId(
                    StringUtils.defaultIfBlank((String) tempoSampleMap.get("cmoPatientId"), ""))
            .setDmpPatientId(
                    StringUtils.defaultIfBlank((String) tempoSampleMap.get("dmpPatientId"), ""))
            .setRecapture((Boolean) tempoSampleMap.get("recapture"))
            .build();
        return tempoSample;
    }

}
