package org.mskcc.cmo.metadb.service.impl;

import org.mskcc.cmo.metadb.model.SampleManifestEntity;
import org.mskcc.cmo.metadb.persistence.SampleManifestRepository;
import org.mskcc.cmo.metadb.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SampleServiceImpl implements SampleService {

    @Autowired
    private SampleManifestRepository sampleManifestRepository;

    @Override
    public SampleManifestEntity saveSampleManifest(SampleManifestEntity sample) {
        // create prerequisite request, patient nodes with the help of other services
        // sample, metadata, nodes etc
        return sampleManifestRepository.saveSampleManifest(sample);
    }
}
