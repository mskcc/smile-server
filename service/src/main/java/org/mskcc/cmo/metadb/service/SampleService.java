package org.mskcc.cmo.metadb.service;

import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleMetadata;

public interface SampleService {

    MetaDbSample saveSampleMetadata(MetaDbSample metaDbSample) throws Exception;

    MetaDbSample setUpMetaDbSample(MetaDbSample metaDbSample) throws Exception;

    List<MetaDbSample> findMatchedNormalSample(MetaDbSample metaDbSample)
            throws Exception;

    List<String> findPooledNormalSample(MetaDbSample metaDbSample) throws Exception;

    MetaDbSample getMetaDbSample(UUID metaDbSampleId) throws Exception;

    MetaDbSample getMetaDbSampleByRequestAndAlias(String requestId,
            SampleAlias igoId) throws Exception;

    MetaDbSample getMetaDbSampleByRequestAndIgoId(String requestId, String igoId) throws Exception;

    List<SampleMetadata> getSampleMetadataListByCmoPatientId(String cmoPatientId) throws Exception;

    List<MetaDbSample> getAllMetadbSamplesByRequestId(String requestId) throws Exception;

    List<SampleMetadata> getSampleMetadataHistoryByIgoId(String igoId) throws Exception;

    Boolean sampleHasMetadataUpdates(SampleMetadata existingSampleMetadata, SampleMetadata sampleMetadata)
            throws Exception;
}
