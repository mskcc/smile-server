package org.mskcc.cmo.metadb.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.mskcc.cmo.metadb.model.neo4j.MetaDbProject;
import org.mskcc.cmo.metadb.model.neo4j.MetaDbRequest;
import org.mskcc.cmo.metadb.model.neo4j.MetaDbSample;
import org.mskcc.cmo.metadb.persistence.MetaDbRequestRepository;
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
    private MetaDbRequestRepository metaDbRequestRepository;

    @Autowired
    private SampleService sampleService;


    @Override
    public void saveRequest(MetaDbRequest request) throws Exception {
        MetaDbProject project = new MetaDbProject();
        project.setprojectId(request.getProjectId());
        request.setMetaDbProject(project);

        MetaDbRequest savedRequest = getMetaDbRequest(request.getRequestId());
        if (savedRequest == null) {
            if (request.getMetaDbSampleList() != null) {
                List<MetaDbSample> updatedSamples = new ArrayList<>();
                for (MetaDbSample s: request.getMetaDbSampleList()) {
                    updatedSamples.add(sampleService.saveSampleManifest(s));
                    
                }
                request.setMetaDbSampleList(updatedSamples);
            }
            metaDbRequestRepository.save(request);
        } else {
            for (MetaDbSample s: request.getMetaDbSampleList()) {
                if (s.getSampleIgoId() != null
                        && metaDbRequestRepository.findSampleManifest(request.getRequestId(),
                        s.getSampleIgoId().getSampleId()) == null) {
                    savedRequest.addMetaDbSampleList(s);
                    metaDbRequestRepository.save(savedRequest);
                }
            }
        }
    }

    @Override
    public MetaDbRequest getMetaDbRequest(String requestId) {
        return metaDbRequestRepository.findByRequestId(requestId);
    }
}
