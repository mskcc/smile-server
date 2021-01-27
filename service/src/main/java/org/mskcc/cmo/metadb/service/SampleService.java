package org.mskcc.cmo.metadb.service;

import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.metadb.model.NormalSampleManifestEntity;
import org.mskcc.cmo.metadb.model.SampleManifestEntity;

public interface SampleService {

    SampleManifestEntity saveSampleManifest(SampleManifestEntity sampleManifestEntity) throws Exception;

    List<NormalSampleManifestEntity> findMatchedNormalSample(SampleManifestEntity sampleManifestEntity)
            throws Exception;

    List<String> findPooledNormalSample(SampleManifestEntity sampleManifestEntity) throws Exception;

    SampleManifestEntity setUpSampleManifest(SampleManifestEntity sample) throws Exception;

    SampleManifestEntity findSampleManifest(UUID uuid);
}
