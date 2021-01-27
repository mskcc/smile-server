package org.mskcc.cmo.metadb.service;

import java.util.UUID;
import org.mskcc.cmo.metadb.model.NormalSampleManifestEntity;

public interface NormalSampleService {

    NormalSampleManifestEntity saveNormalSampleManifest(NormalSampleManifestEntity
            normalSampleEntity) throws Exception;

    NormalSampleManifestEntity setUpNormalSampleManifest(NormalSampleManifestEntity
            normalSampleEntity) throws Exception;

    NormalSampleManifestEntity findNormalSampleManifest(UUID uuid);
}
