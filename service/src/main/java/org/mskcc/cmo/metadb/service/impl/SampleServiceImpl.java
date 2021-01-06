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
    public SampleManifestEntity saveSampleMetadata(SampleManifestEntity sample) {
        // create prerequisite request, patient nodes with the help of other services
        // sample, metadata, nodes etc
        if (sampleMetadataRepository.findSampleByIgoId(sample.getIgoId()) == null) {
            return sampleMetadataRepository.save(sample);
        } else { 
            //update samplemetadata
        }
        
        return null;
    }

    @Override
    public SampleManifestEntity findSampleByIgoId(String igoId) {
        return sampleMetadataRepository.findSampleByIgoId(igoId);
    }
}
