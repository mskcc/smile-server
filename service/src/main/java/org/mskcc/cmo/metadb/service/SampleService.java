package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.shared.neo4j.SampleManifestEntity;

public interface SampleService {

    SampleManifestEntity saveSampleMetadata(SampleManifestEntity sample) throws Exception;
    SampleManifestEntity findSampleByIgoId(String igoId) throws Exception;

}
