package org.mskcc.cmo.metadb.service.impl;

import com.google.gson.Gson;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.mskcc.cmo.metadb.model.neo4j.MetaDbPatient;
import org.mskcc.cmo.metadb.model.neo4j.MetaDbSample;
import org.mskcc.cmo.metadb.model.neo4j.SampleAlias;
import org.mskcc.cmo.metadb.model.neo4j.SampleManifestEntity;
import org.mskcc.cmo.metadb.persistence.MetaDbSampleRepository;
import org.mskcc.cmo.metadb.persistence.PatientMetadataRepository;
import org.mskcc.cmo.metadb.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SampleServiceImpl implements SampleService {

    @Autowired
    private MetaDbSampleRepository metaDbSampleRepository;

    @Autowired
    private PatientMetadataRepository patientMetadataRepository;

    @Override
    public MetaDbSample saveSampleManifest(MetaDbSample
            metaDbSample) throws Exception {
        MetaDbSample updatedMetaDbSample = setUpMetaDbSample(metaDbSample);
        MetaDbSample foundSample =
                metaDbSampleRepository.findSampleByIgoId(updatedMetaDbSample.getSampleIgoId());
        if (foundSample == null) {
            MetaDbPatient patient = patientMetadataRepository.findPatientByInvestigatorId(
                    updatedMetaDbSample.getPatient().getInvestigatorPatientId());
            if (patient != null) {
                updatedMetaDbSample.setPatientUuid(patient.getUuid());
            }
            metaDbSampleRepository.save(updatedMetaDbSample);
        } else {
            foundSample.addSampleManifest(updatedMetaDbSample.getSampleManifestList().get(0));
            metaDbSampleRepository.save(foundSample);
        }
        return updatedMetaDbSample;
    }

    @Override
    public MetaDbSample setUpMetaDbSample(MetaDbSample
            metaDbSample) throws Exception {
        metaDbSample = setUpSampleManifestEntity(metaDbSample);
        SampleManifestEntity sampleManifestEntity = metaDbSample.getSampleManifestList().get(0);
        metaDbSample.setSampleClass(sampleManifestEntity.getTumorOrNormal());
        
        MetaDbPatient patient = new MetaDbPatient();
        patient.setInvestigatorPatientId(sampleManifestEntity.getCmoPatientId());
        metaDbSample.setPatient(patient);

        SampleAlias igoId = new SampleAlias();
        igoId.setIdSource("igoId");
        igoId.setSampleId(sampleManifestEntity.getIgoId());
        metaDbSample.addSample(igoId);

        SampleAlias investigatorId = new SampleAlias();
        investigatorId.setIdSource("investigatorId");
        investigatorId.setSampleId(sampleManifestEntity.getInvestigatorSampleId());
        metaDbSample.addSample(investigatorId);
        
        return metaDbSample;
    }
    
    @Override
    public MetaDbSample setUpSampleManifestEntity(MetaDbSample metaDbSample) throws Exception {
        for (SampleManifestEntity s: metaDbSample.getSampleManifestList()) {
            Gson gson = new Gson();
            s.setSampleManifestJson(gson.toJson(s));
            Timestamp time = Timestamp.from(Instant.now());
            s.setCreationTime(String.valueOf(time.getTime()));
        }
        return metaDbSample;
    }

    @Override
    public List<MetaDbSample> findMatchedNormalSample(
            MetaDbSample metaDbSample) throws Exception {
        return metaDbSampleRepository.findMatchedNormals(metaDbSample);
    }

    @Override
    public List<String> findPooledNormalSample(MetaDbSample metaDbSample) throws Exception {
        return metaDbSampleRepository.findPooledNormals(metaDbSample);
    }
}
