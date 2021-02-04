package org.mskcc.cmo.metadb.service.impl;

import java.util.ArrayList;
import java.util.List;
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
            if (request.getMetaDbSampleList() != null) {
                List<MetaDbSample> updatedSamples = new ArrayList<>();
                for (MetaDbSample s: request.getMetaDbSampleList()) {
                    updatedSamples.add(sampleService.saveSampleManifest(s));
                    
                }
                request.setMetaDbSampleList(updatedSamples);
            }
            cmoRequestRepository.save(request);
        } else {
            for (MetaDbSample s: request.getMetaDbSampleList()) {
                if (s.getSampleIgoId() != null
                        && cmoRequestRepository.findSampleManifest(request.getRequestId(),
                        s.getSampleIgoId().getSampleId()) == null) {
                    savedRequest.addMetaDbSampleList(s);
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
