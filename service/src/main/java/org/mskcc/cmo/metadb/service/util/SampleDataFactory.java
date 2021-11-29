package org.mskcc.cmo.metadb.service.util;

import java.text.ParseException;
import org.mskcc.cmo.metadb.model.MetadbPatient;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.PatientAlias;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.persistence.neo4j.MetadbSampleRepository;
import org.mskcc.cmo.metadb.service.MetadbPatientService;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleDataFactory {
    
    private MetadbSample metadbSample;
    
    @Autowired
    private MetadbSampleRepository sampleRepository;

    @Autowired
    private MetadbPatientService patientService;
    
    public SampleDataFactory() {}
    
    public MetadbSample setResearchMetadbSampleFields(SampleMetadata sampleMetadata) throws Exception {
        metadbSample.addSampleMetadata(sampleMetadata);
        metadbSample.setSampleCategory("research");
        metadbSample.setSampleClass(sampleMetadata.getTumorOrNormal());
        metadbSample.addSampleAlias(new SampleAlias(sampleMetadata.getPrimaryId(), "igoId"));
        metadbSample.addSampleAlias(new SampleAlias(sampleMetadata.getInvestigatorSampleId(), "investigatorId"));
        return metadbSample;
    }
    
    public MetadbSample setClinicalMetadbSampleFields(SampleMetadata sampleMetadata) throws Exception {
        metadbSample.addSampleMetadata(sampleMetadata);
        metadbSample.setSampleCategory("clinical");
        metadbSample.setSampleClass(sampleMetadata.getTumorOrNormal());
        metadbSample.addSampleAlias(new SampleAlias(sampleMetadata.getPrimaryId(), "dmpId"));
        metadbSample.addSampleAlias(new SampleAlias(sampleMetadata.getInvestigatorSampleId(), "investigatorId"));
        return metadbSample;
    }
    
    public MetadbSample setMetadbSamplePatientNode(MetadbSample sample) throws Exception  {
        SampleMetadata sampleMetadata = sample.getLatestSampleMetadata();
        
        MetadbPatient patient = new MetadbPatient();
        patient.addPatientAlias(new PatientAlias(sampleMetadata.getCmoPatientId(), "cmoId"));
        MetadbPatient existingPatient = patientService.getPatientByCmoPatientId(
                sampleMetadata.getCmoPatientId());
        if (existingPatient == null) {
            patientService.savePatientMetadata(patient);
            metadbSample.setPatient(patient);
        } else {
            metadbSample.setPatient(existingPatient);
        }
        return metadbSample;
    }
    
    public MetadbSample getAllMetadbSampleFields(MetadbSample sample) throws ParseException {
        metadbSample.setSampleMetadataList(sampleRepository.findSampleMetadataListBySampleId(sample.getMetaDbSampleId()));
        String cmoPatientId = sample.getLatestSampleMetadata().getCmoPatientId();
        MetadbPatient patient = patientService.getPatientByCmoPatientId(cmoPatientId);
        metadbSample.setPatient(patient);
        metadbSample.setSampleAliases(sampleRepository.findAllSampleAliases(sample.getMetaDbSampleId()));
        return metadbSample;
    }
    
}