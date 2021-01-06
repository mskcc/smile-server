package org.mskcc.cmo.metadb.service.impl;

import java.util.List;
import java.util.UUID;

import org.mskcc.cmo.metadb.persistence.RequestRepository;
import org.mskcc.cmo.metadb.service.RequestService;
import org.mskcc.cmo.metadb.service.SampleService;
import org.mskcc.cmo.shared.neo4j.CmoRequestEntity;
import org.mskcc.cmo.shared.neo4j.SampleManifestEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@Component
@ComponentScan(basePackages="org.mskcc.cmo.metadb.persistence")
@EntityScan(basePackages = "org.mskcc.cmo.shared.neo4j")
public class RequestServiceImpl implements RequestService {
    
    @Autowired
    RequestRepository requestRepository;
    
    @Autowired 
    SampleService sampleService;
    
    @Override
    public void saveRequest(CmoRequestEntity request) throws Exception {
       if (requestRepository == null) {
           throw new RuntimeException("request repository null, exiting...");
       }
       if (request.getRequestId() == null) {
           throw new RuntimeException("request id null, exiting...");
       }
       CmoRequestEntity savedRequest = requestRepository.findByRequestId(request.getRequestId());
       if (savedRequest == null) {
           for (SampleManifestEntity sample: request.getSampleManifestList()) {
               sample.setUuid(UUID.randomUUID());   
           }
           requestRepository.save(request);
       } else {
           for (SampleManifestEntity s: request.getSampleManifestList()) {
               //TODO this would be replaced by a MERGE statement
               if (!savedRequest.getSampleManifestList().contains(s)) {
                   s.setUuid(UUID.randomUUID());
                   sampleService.saveSampleMetadata(s);
                   requestRepository.addSampleManifest(s.getUuid(), request.getRequestId());
               }
           }
       }
        
    }
    
    @Override
    public List<SampleManifestEntity> findIgoSamples(CmoRequestEntity request) {
        //return requestRepository.findAllSampleManifestList(request.getRequestId());
        return null;
    }

}
   
