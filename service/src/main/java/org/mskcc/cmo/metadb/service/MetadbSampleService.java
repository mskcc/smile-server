package org.mskcc.cmo.metadb.service;

import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleMetadata;

public interface MetadbSampleService {

    MetadbSample saveSampleMetadata(MetadbSample metaDbSample) throws Exception;

    MetadbSample setUpMetaDbSample(MetadbSample metaDbSample) throws Exception;

    List<MetadbSample> getMatchedNormalsBySample(MetadbSample metaDbSample)
            throws Exception;

    List<String> getPooledNormalsBySample(MetadbSample metaDbSample) throws Exception;

    MetadbSample getMetaDbSample(UUID metaDbSampleId) throws Exception;

    MetadbSample getMetaDbSampleByRequestAndAlias(String requestId,
            SampleAlias igoId) throws Exception;

    MetadbSample getMetaDbSampleByRequestAndIgoId(String requestId, String igoId) throws Exception;

    List<SampleMetadata> getSampleMetadataListByCmoPatientId(String cmoPatientId) throws Exception;

    List<MetadbSample> getAllMetadbSamplesByRequestId(String requestId) throws Exception;

    List<SampleMetadata> getSampleMetadataHistoryByIgoId(String igoId) throws Exception;

    Boolean sampleHasMetadataUpdates(SampleMetadata existingSampleMetadata, SampleMetadata sampleMetadata)
            throws Exception;
}
