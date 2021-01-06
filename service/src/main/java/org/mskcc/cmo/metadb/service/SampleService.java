package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.shared.neo4j.SampleManifestEntity;

public interface SampleService {

    SampleManifestEntity saveSampleManifest(SampleManifestEntity sample) throws Exception;
}
