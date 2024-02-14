package org.mskcc.smile.service;

import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.tempo.BamComplete;
import org.mskcc.smile.model.tempo.MafComplete;
import org.mskcc.smile.model.tempo.QcComplete;
import org.mskcc.smile.model.tempo.Tempo;

/**
 *
 * @author ochoaa
 */
public interface TempoService {
    Tempo saveTempoData(Tempo tempo);
    Tempo getTempoDataBySampleId(SmileSample smileSample);
    Tempo getTempoDataBySamplePrimaryId(String primaryId);
    Tempo mergeBamCompleteEventBySamplePrimaryId(String primaryId, BamComplete bamComplete);
    Tempo mergeQcCompleteEventBySamplePrimaryId(String primaryId, QcComplete qcComplete);
    Tempo mergeMafCompleteEventBySamplePrimaryId(String primaryId, MafComplete mafComplete);
}
