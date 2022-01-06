package org.mskcc.cmo.metadb.service;

import java.text.ParseException;
import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.model.web.PublishedMetadbSample;

public interface MetadbSampleService {
    MetadbSample saveSampleMetadata(MetadbSample metaDbSample) throws Exception;
    MetadbSample fetchAndLoadSampleDetails(MetadbSample metaDbSample) throws Exception;
    List<MetadbSample> getMatchedNormalsBySample(MetadbSample metaDbSample)
            throws Exception;
    List<String> getPooledNormalsBySample(MetadbSample metaDbSample) throws Exception;
    MetadbSample getMetadbSample(UUID metaDbSampleId) throws Exception;
    MetadbSample getResearchSampleByRequestAndIgoId(String requestId, String igoId) throws Exception;
    List<SampleMetadata> getSampleMetadataListByCmoPatientId(String cmoPatientId) throws Exception;
    List<MetadbSample> getResearchSamplesByRequestId(String requestId) throws Exception;
    List<SampleMetadata> getResearchSampleMetadataHistoryByIgoId(String igoId) throws Exception;
    Boolean sampleHasMetadataUpdates(SampleMetadata existingSampleMetadata, SampleMetadata sampleMetadata)
            throws Exception;
    PublishedMetadbSample getPublishedMetadbSample(UUID metadbSampleId) throws Exception;
    List<PublishedMetadbSample> getPublishedMetadbSampleListByCmoPatientId(String cmoPatientId)
            throws Exception;
    MetadbSample getDetailedMetadbSample(MetadbSample sample) throws ParseException;
}
