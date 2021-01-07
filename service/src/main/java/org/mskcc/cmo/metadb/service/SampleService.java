package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.metadb.model.SampleManifestEntity;

public interface SampleService {

    SampleManifestEntity saveSampleManifest(SampleManifestEntity sample) throws Exception;
}
