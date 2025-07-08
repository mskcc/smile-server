package org.mskcc.smile.service;

import java.util.List;
import java.util.UUID;
import org.mskcc.smile.commons.generated.Smile.TempoSample;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.tempo.BamComplete;
import org.mskcc.smile.model.tempo.MafComplete;
import org.mskcc.smile.model.tempo.QcComplete;
import org.mskcc.smile.model.tempo.Tempo;
import org.mskcc.smile.model.tempo.json.SampleBillingJson;

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
    Tempo initAndSaveDefaultTempoData(String primaryId, String latestCohortCompleteDate) throws Exception;
    void updateSampleBilling(SampleBillingJson billing) throws Exception;
    List<UUID> getTempoIdsNoLongerEmbargoed() throws Exception;
    void updateTempoAccessLevel(List<String> samplePrimaryIds, String accessLevel) throws Exception;
    TempoSample getTempoSampleDataBySamplePrimaryId(String primaryId) throws Exception;
    void updateSampleInitRunDate(String primaryId) throws Exception;
}
