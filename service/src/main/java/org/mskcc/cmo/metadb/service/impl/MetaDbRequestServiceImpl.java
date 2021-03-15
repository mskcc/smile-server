package org.mskcc.cmo.metadb.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.log4j.Logger;
import org.mskcc.cmo.metadb.model.MetaDbProject;
import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.persistence.MetaDbRequestRepository;
import org.mskcc.cmo.metadb.persistence.MetaDbSampleRepository;
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

    private final ObjectMapper mapper = new ObjectMapper();
    private Logger LOG = Logger.getLogger(MetaDbRequestServiceImpl.class);


    @Override
    public boolean saveRequest(MetaDbRequest request) throws Exception {
        request.setMetaDbProject(new MetaDbProject(request.getProjectId()));
        
        MetaDbRequest savedRequest = metaDbRequestRepository.findMetaDbRequestById(request.getRequestId());
        if (savedRequest == null) {
            if (request.getMetaDbSampleList() != null) {
                List<MetaDbSample> updatedSamples = new ArrayList<>();
                for (MetaDbSample s: request.getMetaDbSampleList()) {
                    updatedSamples.add(sampleService.saveSampleMetadata(s));
                }
                request.setMetaDbSampleList(updatedSamples);
            }
            metaDbRequestRepository.save(request);
            return true;
        } 
        else {
            return updateRequest(request, savedRequest);
        }      
    }
    
    @Override
    public boolean updateRequest(MetaDbRequest request, MetaDbRequest savedRequest) throws Exception{
        savedRequest = getMetaDbRequest(savedRequest.getRequestId());
        boolean returnValue = false;
        for (MetaDbSample s: request.getMetaDbSampleList()) {
            MetaDbSample foundSample = metaDbRequestRepository.findMetaDbSampleByRequestAndIgoId(
                    savedRequest.getRequestId(), s.getLatestSampleMetadata().getIgoId());
            foundSample = sampleService.getMetaDbSample(foundSample.getMetaDbSampleId());

            if (foundSample == null) {
                s = sampleService.setUpMetaDbSample(s);
                savedRequest.addMetaDbSampleList(s);
                returnValue = true;
            }
            
            System.out.println(foundSample.getLatestSampleMetadata().checkIfEqual(s.getLatestSampleMetadata()));
            if (!foundSample.getLatestSampleMetadata().checkIfEqual(s.getLatestSampleMetadata())) {  
                SampleMetadata newSampleMetadata = s.getLatestSampleMetadata();
                System.out.println(foundSample.getMetaDbSampleId());
                System.out.println(savedRequest.getMetaDbSampleList());
                MetaDbSample savedMetaDbSample = savedRequest.findMetaDbSample(foundSample.getMetaDbSampleId());
                savedMetaDbSample.addSampleMetadata(newSampleMetadata);
                returnValue = true;
            }
         }
        if (returnValue) {
            metaDbRequestRepository.save(savedRequest);
        }
        return returnValue;
    }
    
    

    @Override
    public Map<String, Object> getMetaDbRequestMap(String requestId) throws Exception {
        MetaDbRequest metaDbRequest = metaDbRequestRepository.findMetaDbRequestById(requestId);
        if (metaDbRequest == null) {
            LOG.error("Couldn't find a request with requestId " + requestId);
            return null;
        }
        List<SampleMetadata> samples = new ArrayList<>();
        for (MetaDbSample metaDbSample: metaDbRequestRepository.findAllMetaDbSamplesByRequest(requestId)) {
            samples.addAll(sampleService.getMetaDbSample(metaDbSample.getMetaDbSampleId())
                    .getSampleMetadataList());
        }
        Map<String, Object> metaDbRequestMap = mapper.readValue(
                mapper.writeValueAsString(metaDbRequest), Map.class);
        metaDbRequestMap.put("samples", samples);
        return metaDbRequestMap;
    }

    @Override
    public MetaDbRequest getMetaDbRequest(String requestId) throws Exception {
        Map<String, Object> requestMap = getMetaDbRequestMap(requestId);
        SampleMetadata[] sampleList = mapper.convertValue(requestMap.get("samples"),
                SampleMetadata[].class);
        List<MetaDbSample> metaDbSampleList = new ArrayList<>();
        for (SampleMetadata sample: sampleList) {
            MetaDbSample metaDbSample = metaDbRequestRepository.findMetaDbSampleByRequestAndIgoId(
                    requestId, sample.getIgoId());
            metaDbSample = sampleService.getMetaDbSample(metaDbSample.getMetaDbSampleId());
            metaDbSampleList.add(metaDbSample);
        }
        MetaDbRequest request = mapper.readValue(mapper.writeValueAsString(requestMap), MetaDbRequest.class);
        request.setMetaDbSampleList(metaDbSampleList);
        return request;
    }
}
