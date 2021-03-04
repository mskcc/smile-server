package org.mskcc.cmo.metadb.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.mskcc.cmo.metadb.model.MetaDbProject;
import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.SampleManifestEntity;
import org.mskcc.cmo.metadb.persistence.MetaDbRequestRepository;
import org.mskcc.cmo.metadb.service.MetaDbRequestService;
import org.mskcc.cmo.metadb.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author ochoaa
 */
@Component
public class MetaDbRequestServiceImpl implements MetaDbRequestService {

    @Autowired
    private MetaDbRequestRepository metaDbRequestRepository;

    @Autowired
    private SampleService sampleService;

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private Logger LOG = Logger.getLogger(MetaDbRequestServiceImpl.class);


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
        if (metaDbRequest == null) {
            LOG.error("Couldn't find a request with requestId " + requestId);
            return null;
        }
        List<SampleManifestEntity> samples = new ArrayList<>();
        for (MetaDbSample metaDbSample: metaDbRequestRepository.findAllSampleManifests(requestId)) {
            samples.addAll(sampleService.getMetaDbSample(metaDbSample.getUuid()).getSampleManifestList());
        }
        TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};
        HashMap<String,Object> metaDbRequestMap = mapper.readValue(new ByteArrayInputStream(
                mapper.writeValueAsString(metaDbRequest).getBytes("UTF-8")), typeRef);
        metaDbRequestMap.put("sampleManifestList", samples);
        System.out.println(mapper.writeValueAsString(metaDbRequestMap));
        return metaDbRequestMap;
    }
}
