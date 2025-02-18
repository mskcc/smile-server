package org.mskcc.smile.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.logging.log4j.util.Strings;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author ochoaa
 */
@Component
public class TempoServiceImpl implements TempoService {

    @Autowired
    private TempoRepository tempoRepository;

    @Autowired
    private SmileSampleService sampleService;

    @Autowired
    private SmileRequestService requestService;

    private static final Log LOG = LogFactory.getLog(TempoServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String RUN_DATE_FORMAT = "yyyy-MM-dd HH:mm";
    private static final String ACCESS_LEVEL_EMBARGO = "MSK Embargo";
    private static final String ACCESS_LEVEL_PUBLIC = "MSK Public";
    // approximate number of days to add to the initial pipeline run date to calculate the embargo date.
    // we use days instead of the 18 months to avoid issues with months of different lengths.
    private static final int EMBARGO_PERIOD_DAYS = 547;


    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Tempo saveTempoData(Tempo tempo) throws Exception {
        // first instance of tempo data for a given sample means we need to resolve the
        // custodian information and data access level only for non-normal samples
        SmileSample sample = tempo.getSmileSample();

        // if normal sample then do not init Tempo data with custodian information or access level
        if (!sample.getSampleClass().equalsIgnoreCase("Normal")) {
            SmileRequest request = requestService.getRequestBySample(sample);

            String custodianInformation = Strings.isBlank(request.getLabHeadName())
                    ? request.getLabHeadName() : request.getInvestigatorName();
            tempo.setCustodianInformation(custodianInformation);

            String primaryId = sample.getPrimarySampleAlias();
            LocalDateTime initialPipelineRunDate = getInitialPipelineRunDateBySamplePrimaryId(primaryId);
            if (initialPipelineRunDate != null) {
                LocalDateTime embargoDate = initialPipelineRunDate.plusDays(EMBARGO_PERIOD_DAYS);
                tempo.setInitialPipelineRunDate(initialPipelineRunDate.format(DATE_FORMATTER));
                tempo.setEmbargoDate(embargoDate.format(DATE_FORMATTER));
                // only update access level if it is not already set from backfilling
                if (StringUtils.isEmpty(tempo.getAccessLevel())) {
                    String accessLevel = embargoDate.isAfter(LocalDateTime.now())
                            ? ACCESS_LEVEL_EMBARGO : ACCESS_LEVEL_PUBLIC;
                    tempo.setAccessLevel(accessLevel);
                }
            } else {
                // explicitly set dates to empty strings if no initial pipeline run date is found
                tempo.setInitialPipelineRunDate("");
                tempo.setEmbargoDate("");
                // only update access level if it is not already set from backfilling
                if (StringUtils.isEmpty(tempo.getAccessLevel())) {
                    tempo.setAccessLevel(ACCESS_LEVEL_EMBARGO);
                }
            }
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
    public Tempo initAndSaveDefaultTempoData(String primaryId) throws Exception {
        SmileSample sample = sampleService.getSampleByInputId(primaryId);
        Tempo tempo = new Tempo(sample);

        // if sample is a normal sample then no need to set values for custodian
        // information or access level. Normal samples do not require this info.
        if (!sample.getSampleClass().equalsIgnoreCase("Normal")) {
            SmileRequest request = requestService.getRequestBySample(sample);

            String custodianInformation = Strings.isBlank(request.getLabHeadName())
                    ? request.getLabHeadName() : request.getInvestigatorName();
            tempo.setCustodianInformation(custodianInformation);

            LocalDateTime initialPipelineRunDate = getInitialPipelineRunDateBySamplePrimaryId(primaryId);
            if (initialPipelineRunDate != null) {
                LocalDateTime embargoDate = initialPipelineRunDate.plusDays(EMBARGO_PERIOD_DAYS);
                String accessLevel = embargoDate.isAfter(LocalDateTime.now())
                        ? ACCESS_LEVEL_EMBARGO : ACCESS_LEVEL_PUBLIC;
                tempo.setInitialPipelineRunDate(initialPipelineRunDate.format(DATE_FORMATTER));
                tempo.setEmbargoDate(embargoDate.format(DATE_FORMATTER));
                tempo.setAccessLevel(accessLevel);
            } else {
                // explicitly set dates to empty strings if no initial pipeline run date is found
                tempo.setInitialPipelineRunDate("");
                tempo.setEmbargoDate("");
                tempo.setAccessLevel(ACCESS_LEVEL_EMBARGO);
            }
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

    private LocalDateTime getInitialPipelineRunDateBySamplePrimaryId(String primaryId) throws Exception {
        String dateString = tempoRepository.findInitialPipelineRunDateBySamplePrimaryId(primaryId);
        if (StringUtils.isEmpty(dateString)) {
            LOG.warn("No Initial Pipeline Run Date found for sample with Primary ID: " + primaryId);
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(RUN_DATE_FORMAT);
        return LocalDateTime.parse(dateString, formatter);
    }
}
