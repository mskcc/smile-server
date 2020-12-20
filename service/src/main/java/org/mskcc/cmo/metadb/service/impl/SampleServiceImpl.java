package org.mskcc.cmo.metadb.service.impl;

import org.mskcc.cmo.metadb.persistence.SampleMetadataRepository;
import org.mskcc.cmo.metadb.service.SampleService;
import org.mskcc.cmo.shared.neo4j.SampleMetadataEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SampleServiceImpl implements SampleService {

    @Autowired
    private SampleMetadataRepository sampleMetadataRepository;

    @Override
    public SampleMetadataEntity saveSampleMetadata(SampleMetadataEntity sample) {
        // create prerequisite request, patient nodes with the help of other services
        // sample, metadata, nodes etc
        return sampleMetadataRepository.saveSampleMetadata(sample);
    }
}
