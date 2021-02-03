package org.mskcc.cmo.metadb.service.impl;

import org.mskcc.cmo.metadb.model.MetaDbProject;
import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.persistence.CmoRequestRepository;
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
    private SampleService sampleService;


    @Override
    public void saveRequest(MetaDbRequest request) throws Exception {
        MetaDbProject project = new MetaDbProject();
        project.setprojectId(request.getProjectId());
        request.setProjectEntity(project);

        MetaDbRequest savedRequest = getCmoRequest(request.getRequestId());
        if (savedRequest == null) {
            if (request.getSampleManifestList() != null) {
                for (MetaDbSample s: request.getSampleManifestList()) {
                    sampleService.saveSampleManifest(s);
                }
            }
            cmoRequestRepository.save(request);
        } else {
            for (MetaDbSample s: request.getSampleManifestList()) {
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
    public MetaDbRequest getCmoRequest(String requestId) {
        return cmoRequestRepository.findByRequestId(requestId);
    }
}
