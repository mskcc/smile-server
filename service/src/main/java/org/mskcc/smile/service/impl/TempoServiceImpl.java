package org.mskcc.smile.service.impl;

import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.tempo.BamComplete;
import org.mskcc.smile.model.tempo.MafComplete;
import org.mskcc.smile.model.tempo.QcComplete;
import org.mskcc.smile.model.tempo.Tempo;
import org.mskcc.smile.persistence.neo4j.TempoRepository;
import org.mskcc.smile.service.TempoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author ochoaa
 */
@Component
public class TempoServiceImpl implements TempoService {

    @Autowired
    private TempoRepository tempoRepository;

    @Override
    public Tempo saveTempoData(Tempo tempo) {
        return tempoRepository.save(tempo);
    }

    @Override
    public Tempo getTempoDataBySampleId(SmileSample smileSample) {
        Tempo tempo = tempoRepository.findTempoBySmileSampleId(smileSample.getSmileSampleId());
        return getDetailedTempoData(tempo);
    }

    @Override
    public Tempo getTempoDataBySamplePrimaryId(String primaryId) {
        Tempo tempo = tempoRepository.findTempoBySamplePrimaryId(primaryId);
        return getDetailedTempoData(tempo);
    }

    @Override
    public Tempo mergeBamCompleteEventBySamplePrimaryId(String primaryId, BamComplete bamCompleteEvent) {
        Tempo tempo = tempoRepository.mergeBamCompleteEventBySamplePrimaryId(primaryId, bamCompleteEvent);
        return getDetailedTempoData(tempo);
    }

    @Override
    public Tempo mergeQcCompleteEventBySamplePrimaryId(String primaryId, QcComplete qcCompleteEvent) {
        Tempo tempo = tempoRepository.mergeQcCompleteEventBySamplePrimaryId(primaryId, qcCompleteEvent);
        return getDetailedTempoData(tempo);
    }

    @Override
    public Tempo mergeMafCompleteEventBySamplePrimaryId(String primaryId, MafComplete mafCompleteEvent) {
        Tempo tempo = tempoRepository.mergeMafCompleteEventBySamplePrimaryId(primaryId, mafCompleteEvent);
        return getDetailedTempoData(tempo);
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
}
