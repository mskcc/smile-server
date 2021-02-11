package org.mskcc.cmo.metadb.service.impl;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.mskcc.cmo.metadb.model.MetaDbProject;
import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.SampleManifestEntity;
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

        MetaDbRequest savedRequest = metaDbRequestRepository.findByRequestId(request.getRequestId());
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
    public Map<String, Object> getMetaDbRequest(String requestId) throws Exception {
        MetaDbRequest metaDbRequest = metaDbRequestRepository.findByRequestId(requestId);
        List<SampleManifestEntity> samples = new ArrayList<>();
        for (MetaDbSample metaDbSample: metaDbRequestRepository.findAllSampleManifests(requestId)) {
            samples.addAll(sampleService.getMetaDbSample(metaDbSample.getUuid()).getSampleManifestList());
        }
        Gson gson = new Gson();
        Map<String, Object> metaDbRequestMap = gson.fromJson(gson.toJson(metaDbRequest), Map.class);
        metaDbRequestMap.put("sampleManifestList", samples);
        return metaDbRequestMap;
    }
}
