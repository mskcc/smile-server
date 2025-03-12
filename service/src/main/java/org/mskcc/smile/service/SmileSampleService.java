package org.mskcc.smile.service;

import java.util.List;
import java.util.UUID;
import org.mskcc.smile.model.SampleMetadata;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.web.PublishedSmileSample;
import org.mskcc.smile.model.web.SmileSampleIdMapping;

public interface SmileSampleService {
    SmileSample saveSmileSample(SmileSample smileSample) throws Exception;
    SmileSample fetchAndLoadPatientDetails(SmileSample smileSample) throws Exception;
    Boolean updateSampleMetadata(SampleMetadata sampleMetadata, Boolean fromLims) throws Exception;
    List<SmileSample> getMatchedNormalsBySample(SmileSample smileSample)
            throws Exception;
    List<String> getPooledNormalsBySample(SmileSample smileSample) throws Exception;
    SmileSample getSmileSample(UUID smileSampleId) throws Exception;
    SmileSample getResearchSampleByRequestAndIgoId(String requestId, String igoId) throws Exception;
    List<SmileSample> getResearchSamplesByRequestId(String requestId) throws Exception;
    List<SampleMetadata> getResearchSampleMetadataHistoryByIgoId(String igoId) throws Exception;
    Boolean sampleHasMetadataUpdates(SampleMetadata existingSampleMetadata,
            SampleMetadata sampleMetadata, Boolean isResearchSample, Boolean fromLims)
            throws Exception;
    PublishedSmileSample getPublishedSmileSample(UUID smileSampleId) throws Exception;
    List<PublishedSmileSample> getPublishedSmileSamplesByCmoPatientId(String cmoPatientId)
            throws Exception;
    List<SmileSample> getSamplesByCmoPatientId(String cmoPatientId) throws Exception;
    SmileSample getClinicalSampleByDmpId(String dmpId) throws Exception;
    List<SmileSample> getSamplesByCategoryAndCmoPatientId(String cmoPatientId,
            String sampleCategory) throws Exception;
    void updateSamplePatientRelationship(UUID smileSampleId, UUID smilePatientId);
    List<SmileSampleIdMapping> getSamplesByDate(String inputDate);
    SmileSample getDetailedSampleByInputId(String inputId) throws Exception;
    SmileSample getSampleByInputId(String inputId) throws Exception;
    void createSampleRequestRelationship(UUID smileSampleId, UUID smileRequestId);
    Boolean sampleExistsByInputId(String primaryId);
    List<SmileSample> getSamplesByCohortId(String cohortId) throws Exception;
    List<SmileSample> getSamplesByCmoSampleName(String cmoSampleName) throws Exception;
    List<SmileSample> getSamplesByAltId(String altId) throws Exception;
    SampleMetadata getLatestSampleMetadataByPrimaryId(String primaryId) throws Exception;
    Boolean sampleIsRecapture(String investigatorSampleId) throws Exception;
}
