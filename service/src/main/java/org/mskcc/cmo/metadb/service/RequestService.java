package org.mskcc.cmo.metadb.service;

import org.springframework.stereotype.Service;
import org.mskcc.cmo.shared.neo4j.CmoRequestEntity;

@Service
public interface RequestService {
    
    void saveRequest(CmoRequestEntity cre);
    void findIgoSamples();
    
}