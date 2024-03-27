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
    Tempo saveTempoData(Tempo tempo) throws Exception;
    Tempo getTempoDataBySampleId(SmileSample smileSample) throws Exception;
    Tempo getTempoDataBySamplePrimaryId(String primaryId) throws Exception;
    Tempo mergeBamCompleteEventBySamplePrimaryId(String primaryId, BamComplete bamComplete) throws Exception;
    Tempo mergeQcCompleteEventBySamplePrimaryId(String primaryId, QcComplete qcComplete) throws Exception;
    Tempo mergeMafCompleteEventBySamplePrimaryId(String primaryId, MafComplete mafComplete) throws Exception;
    Tempo initAndSaveDefaultTempoData(String primaryId) throws Exception;
}
