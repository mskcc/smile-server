package org.mskcc.cmo.metadb.service;

import org.springframework.stereotype.Service;

import java.util.List;
import org.mskcc.cmo.shared.neo4j.CmoRequestEntity;
import org.mskcc.cmo.shared.neo4j.SampleManifestEntity;

@Service
public interface RequestService {
    
    void saveRequest(CmoRequestEntity request) throws Exception;
    List<SampleManifestEntity> findIgoSamples(CmoRequestEntity request);
    
}