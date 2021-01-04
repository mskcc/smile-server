package org.mskcc.cmo.metadb.service.impl;

import org.mskcc.cmo.metadb.persistence.SampleMetadataRepository;
import org.mskcc.cmo.metadb.service.SampleService;
import org.mskcc.cmo.shared.neo4j.SampleManifestEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SampleServiceImpl implements SampleService {

    @Autowired
    private SampleMetadataRepository sampleMetadataRepository;

    @Override
    public SampleManifestEntity insertSampleMetadata(SampleManifestEntity sample) {
        // create prerequisite request, patient nodes with the help of other services
        // sample, metadata, nodes etc
        return sampleMetadataRepository.insertSampleMetadata(sample);
    }

    @Override
    public SampleManifestEntity updateSampleMetadata(SampleManifestEntity sample) {
        // create prerequisite request, patient nodes with the help of other services
        // sample, metadata, nodes etc
        return sampleMetadataRepository.updateSampleMetadata(sample);
    }

    @Override
    public SampleManifestEntity findSampleByIgoId(String igoId) {
        return sampleMetadataRepository.findSampleByIgoId(igoId);
    }
}
