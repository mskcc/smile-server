package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.shared.neo4j.SampleMetadataEntity;

public interface SampleService {

    SampleMetadataEntity saveSampleMetadata(SampleMetadataEntity sample) throws Exception;
}
