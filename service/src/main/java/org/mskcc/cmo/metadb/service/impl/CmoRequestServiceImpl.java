package org.mskcc.cmo.metadb.service.impl;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import org.mskcc.cmo.metadb.model.CmoProjectEntity;
import org.mskcc.cmo.metadb.model.CmoRequestEntity;
import org.mskcc.cmo.metadb.model.NormalSampleManifestEntity;
import org.mskcc.cmo.metadb.model.SampleManifestEntity;
import org.mskcc.cmo.metadb.persistence.CmoRequestRepository;
import org.mskcc.cmo.metadb.service.CmoRequestService;
import org.mskcc.cmo.metadb.service.NormalSampleService;
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

    @Autowired
    private NormalSampleService normalSampleService;

    @Override
    public void saveRequest(CmoRequestEntity request) throws Exception {
        CmoProjectEntity project = new CmoProjectEntity();
        project.setprojectId(request.getProjectId());
        request.setProjectEntity(project);
        CmoRequestEntity savedRequest = cmoRequestRepository.findByRequestId(request.getRequestId());
        if (savedRequest == null) {
            if (request.getSampleManifestList() != null) {
                List<SampleManifestEntity> removeSamples = new ArrayList<>();
                for (SampleManifestEntity s: request.getSampleManifestList()) {
                    if (s.getTumorOrNormal().equals("Normal")) {
                        removeSamples.add(s);
                        Gson gson = new Gson();
                        String normalSampleJson = gson.toJson(s);
                        NormalSampleManifestEntity normalSampleManifest = gson.fromJson(
                                normalSampleJson, NormalSampleManifestEntity.class);
                        request.addNormalSampleManifest(normalSampleManifest);
                        normalSampleService.saveNormalSampleManifest(normalSampleManifest);
                    } else {
                        sampleService.saveSampleManifest(s);
                    }
                }
                for (SampleManifestEntity sample: removeSamples) {
                    request.getSampleManifestList().remove(sample);
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
