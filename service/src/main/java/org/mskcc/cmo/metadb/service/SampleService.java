package org.mskcc.cmo.metadb.service;

import java.util.List;
import org.mskcc.cmo.metadb.model.SampleManifestEntity;

public interface SampleService {

    SampleManifestEntity saveSampleManifest(SampleManifestEntity sampleManifestEntity) throws Exception;

    SampleManifestEntity setUpSampleManifest(SampleManifestEntity sample) throws Exception;

    List<SampleManifestEntity> findMatchedNormalSample(SampleManifestEntity sampleManifestEntity)
            throws Exception;

    List<String> findPooledNormalSample(SampleManifestEntity sampleManifestEntity) throws Exception;
}
