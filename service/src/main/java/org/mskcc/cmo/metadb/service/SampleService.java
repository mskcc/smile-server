package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.shared.neo4j.SampleMetadataEntity;

public interface SampleService {
    
    SampleMetadataEntity insertSampleMetadata(SampleMetadataEntity sample) throws Exception;
    SampleMetadataEntity updateSampleMetadata(SampleMetadataEntity sample) throws Exception;
    SampleMetadataEntity findSampleByIgoId(String igoId) throws Exception;
    
}
