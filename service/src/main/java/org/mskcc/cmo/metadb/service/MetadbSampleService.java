package org.mskcc.cmo.metadb.service;

import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.model.web.PublishedMetadbSample;

public interface MetadbSampleService {
    MetadbSample saveSampleMetadata(MetadbSample metaDbSample) throws Exception;
    MetadbSample fetchAndLoadSampleDetails(MetadbSample metaDbSample) throws Exception;
    List<MetadbSample> getMatchedNormalsBySample(MetadbSample metaDbSample)
            throws Exception;
    List<String> getPooledNormalsBySample(MetadbSample metaDbSample) throws Exception;
    MetadbSample getMetadbSample(UUID metaDbSampleId) throws Exception;
    MetadbSample getMetadbSampleByRequestAndAlias(String requestId,
            SampleAlias igoId) throws Exception;
    MetadbSample getMetadbSampleByRequestAndIgoId(String requestId, String igoId) throws Exception;
    List<SampleMetadata> getSampleMetadataListByCmoPatientId(String cmoPatientId) throws Exception;
    List<MetadbSample> getAllSamplesByRequestId(String requestId) throws Exception;
    List<SampleMetadata> getSampleMetadataHistoryByIgoId(String igoId) throws Exception;
    Boolean sampleHasMetadataUpdates(SampleMetadata existingSampleMetadata, SampleMetadata sampleMetadata)
            throws Exception;
    PublishedMetadbSample getPublishedMetadbSamplebyUUID(UUID metadbSampleId) throws Exception;
    List<PublishedMetadbSample> getPublishedMetadbSampleListByCmoId(String cmoPatientId) throws Exception;
}
