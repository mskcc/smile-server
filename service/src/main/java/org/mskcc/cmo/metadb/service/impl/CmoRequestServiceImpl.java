package org.mskcc.cmo.metadb.service.impl;

import org.mskcc.cmo.metadb.model.CmoRequestEntity;
import org.mskcc.cmo.metadb.model.SampleManifestEntity;
import org.mskcc.cmo.metadb.persistence.CmoRequestRepository;
import org.mskcc.cmo.metadb.persistence.SampleManifestRepository;
import org.mskcc.cmo.metadb.service.CmoRequestService;
import org.mskcc.cmo.metadb.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author ochoaa
 */
@Component
public class CmoRequestServiceImpl implements CmoRequestService {

    @Autowired
    private CmoRequestRepository cmoRequestRepository;

    @Autowired
    private SampleManifestRepository sampleManifestRepository;
    
    @Autowired
    private SampleService sampleService;


    @Override
    public void saveRequest(CmoRequestEntity request) {
        //TODO: NEED TO PERSIST THE PROJECT DATA AS WELL
        CmoRequestEntity savedRequest = getCmoRequest(request.getRequestId());
        if (savedRequest == null) {
            if (request.getSampleManifestList() != null) {
                for (SampleManifestEntity s: request.getSampleManifestList()) {
                    sampleManifestRepository.save(s);
                }
            }
            cmoRequestRepository.save(request);
        } else {
            for (SampleManifestEntity s: request.getSampleManifestList()) {
                if (s.getSampleIgoId() != null
                        && cmoRequestRepository.findSampleManifest(request.getRequestId(),
                        s.getSampleIgoId().getSampleId()) == null) {
                    savedRequest.addSampleManifest(s);
                    cmoRequestRepository.save(savedRequest);
                }
            }
        }
    }

    @Override
    public CmoRequestEntity getCmoRequest(String requestId) {
        CmoRequestEntity request = cmoRequestRepository.findByRequestId(requestId);
        for (SampleManifestEntity sample:cmoRequestRepository.findAllSampleManifests(requestId)) {
            sample = sampleService.findSampleManifest(sample.getUuid());
            request.addSampleManifest(sample);
        }
        request.setProjectEntity(cmoRequestRepository.findProjectEntity(requestId));
        return request;
    }
}
