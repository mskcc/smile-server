package org.mskcc.cmo.metadb.service.impl;

import java.io.FileReader;
import java.util.List;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.mskcc.cmo.metadb.service.RequestService;
/*
public class RequestServiceImpl implements RequestService {
    
    @Override
    public void saveRequest() {
        JSONParser jsonParser = new JSONParser();
        try {
            JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader("request.json"));
            String id = (String) jsonObject.get("requestId");
            List<JSONObject> samples = (List<JSONObject>) jsonObject.get("samples");
            SampleSummary r = new SampleSummary();
            r.setId(id);
            for(JSONObject s: samples) {
                System.out.println(s);
                IgoRequest igo = new IgoRequest();
                UUID uuid = UUID.randomUUID();
                igo.setUUID(uuid);
                String igoId = (String) s.get("igoSampleId");
                igo.setIgoId(igoId);
                String investigatorId = (String) s.get("investigatorSampleId");
                igo.setInvestigatorId(investigatorId);
                r.addSample(igo);
            }
            //save r
            
            System.out.println(r);
            
        } catch(Exception e) {
            e.printStackTrace();
            
        }
    }

    @Override
    public void findIgoSamples() {
        
    }
    }
    */
