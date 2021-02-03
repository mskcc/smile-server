package org.mskcc.cmo.metadb.service;

import java.util.List;
import org.mskcc.cmo.metadb.model.MetaDbSample;

public interface SampleService {

    MetaDbSample saveSampleManifest(MetaDbSample metaDbSample) throws Exception;

    MetaDbSample setUpSampleManifest(MetaDbSample sample) throws Exception;

    List<MetaDbSample> findMatchedNormalSample(MetaDbSample metaDbSample)
            throws Exception;

    List<String> findPooledNormalSample(MetaDbSample metaDbSample) throws Exception;
}
