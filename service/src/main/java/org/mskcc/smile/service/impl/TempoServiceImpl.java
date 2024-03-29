package org.mskcc.smile.service.impl;

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

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Tempo saveTempoData(Tempo tempo) throws Exception {
        // first instance of tempo data for a given sample means we need to resolve the
        // custodian information and data access level
        SmileSample sample = tempo.getSmileSample();
        SmileRequest request = requestService.getRequestBySample(sample);
        String custodianInformation = Strings.isBlank(request.getPiEmail())
                ? request.getInvestigatorEmail() : request.getPiEmail();
        tempo.setCustodianInformation(custodianInformation);

        // if backfilling data then access level might already be present in incoming data
        if (Strings.isBlank(tempo.getAccessLevel())) {
            tempo.setAccessLevel("MSKEmbargo");
        }

        return tempoRepository.save(tempo);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Tempo getTempoDataBySampleId(SmileSample smileSample) throws Exception {
        Tempo tempo = tempoRepository.findTempoBySmileSampleId(smileSample.getSmileSampleId());
        if (tempo == null) {
            return null;
        }
        return getDetailedTempoData(tempo);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Tempo getTempoDataBySamplePrimaryId(String primaryId) throws Exception {
        Tempo tempo = tempoRepository.findTempoBySamplePrimaryId(primaryId);
        if (tempo == null) {
            return null;
        }
        return getDetailedTempoData(tempo);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Tempo mergeBamCompleteEventBySamplePrimaryId(String primaryId, BamComplete bamCompleteEvent)
            throws Exception {
        if (getTempoDataBySamplePrimaryId(primaryId) == null) {
            initAndSaveDefaultTempoData(primaryId);
        }
        Tempo tempo = tempoRepository.mergeBamCompleteEventBySamplePrimaryId(primaryId, bamCompleteEvent);
        return getDetailedTempoData(tempo);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Tempo mergeQcCompleteEventBySamplePrimaryId(String primaryId, QcComplete qcCompleteEvent)
            throws Exception {
        if (getTempoDataBySamplePrimaryId(primaryId) == null) {
            initAndSaveDefaultTempoData(primaryId);
        }
        Tempo tempo = tempoRepository.mergeQcCompleteEventBySamplePrimaryId(primaryId, qcCompleteEvent);
        return getDetailedTempoData(tempo);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Tempo mergeMafCompleteEventBySamplePrimaryId(String primaryId, MafComplete mafCompleteEvent)
            throws Exception {
        if (getTempoDataBySamplePrimaryId(primaryId) == null) {
            initAndSaveDefaultTempoData(primaryId);
        }
        Tempo tempo = tempoRepository.mergeMafCompleteEventBySamplePrimaryId(primaryId, mafCompleteEvent);
        return getDetailedTempoData(tempo);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Tempo initAndSaveDefaultTempoData(String primaryId) throws Exception {
        SmileSample sample = sampleService.getSampleByInputId(primaryId);
        Tempo tempo = new Tempo(sample);
        SmileRequest request = requestService.getRequestBySample(sample);
        String custodianInformation = Strings.isBlank(request.getPiEmail())
                ? request.getInvestigatorEmail() : request.getPiEmail();
        // using default of MSKEmbargo since we're making a brand new tempo event
        tempo.setCustodianInformation(custodianInformation);
        tempo.setAccessLevel("MSKEmbargo");
        return tempoRepository.save(tempo);
    }

    private Tempo getDetailedTempoData(Tempo tempo) {
        if (tempo == null || tempo.getId() == null) {
            return null;
        }
        tempo.setBamCompleteEvents(tempoRepository.findBamCompleteEventsByTempoId(tempo.getId()));
        tempo.setQcCompleteEvents(tempoRepository.findQcCompleteEventsByTempoId(tempo.getId()));
        tempo.setMafCompleteEvents(tempoRepository.findMafCompleteEventsByTempoId(tempo.getId()));
        return tempo;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void updateSampleBilling(SampleBillingJson billing) throws Exception {
        tempoRepository.updateSampleBilling(billing);
    }
}
