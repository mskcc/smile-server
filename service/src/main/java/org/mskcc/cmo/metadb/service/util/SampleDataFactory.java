package org.mskcc.cmo.metadb.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.mskcc.cmo.metadb.model.MetadbPatient;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.PatientAlias;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleMetadata;

public class SampleDataFactory {
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Method factory returns an instance of MetadbSample with sampleCategory "research"
     * from an instance of SampleMetadata.
     * @param sampleMetadata
     * @return MetadbSample
     */
    public static MetadbSample buildNewResearchSampleFromMetadata(SampleMetadata sampleMetadata) {
        return buildNewResearchSampleFromMetadata(sampleMetadata.getIgoRequestId(), sampleMetadata);
    }

    /**
     * Method factory returns an instance of MetadbSample with sampleCategory "research"
     * from an instance of SampleMetadata and the provided request id.
     * @param sampleMetadata
     * @return MetadbSample
     */
    public static MetadbSample buildNewResearchSampleFromMetadata(String requestId,
            SampleMetadata sampleMetadata) {
        sampleMetadata.setIgoRequestId(requestId);
        sampleMetadata.setImportDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        MetadbSample sample = new MetadbSample();
        sample.addSampleMetadata(sampleMetadata);
        sample.setSampleCategory("research");
        sample.setSampleClass(sampleMetadata.getTumorOrNormal());
        sample.addSampleAlias(new SampleAlias(sampleMetadata.getPrimaryId(), "igoId"));
        sample.addSampleAlias(new SampleAlias(
                sampleMetadata.getInvestigatorSampleId(), "investigatorId"));

        MetadbPatient patient = new MetadbPatient();
        patient.addPatientAlias(new PatientAlias(sampleMetadata.getCmoPatientId(), "cmoId"));
        sample.setPatient(patient);
        return sample;
    }

    /**
     * Method factory returns an instance of MetadbSample with sampleCategory "clinical"
     * from an instance of SampleMetadata.
     * @param sampleMetadata
     * @return MetadbSample
     */
    public static MetadbSample buildNewClinicalSampleFromMetadata(SampleMetadata sampleMetadata) {
        sampleMetadata.setImportDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        MetadbSample sample = new MetadbSample();
        sample.addSampleMetadata(sampleMetadata);
        sample.setSampleCategory("clinical");
        sample.setSampleClass(sampleMetadata.getTumorOrNormal());
        sample.addSampleAlias(new SampleAlias(sampleMetadata.getPrimaryId(), "dmpId"));
        // 'investigatorId' isn't applicable for clinical dmp samples but we will deal with it later
        sample.addSampleAlias(new SampleAlias(
                sampleMetadata.getInvestigatorSampleId(), "investigatorId"));

        MetadbPatient patient = new MetadbPatient();
        patient.addPatientAlias(new PatientAlias(sampleMetadata.getCmoPatientId(), "cmoId"));
        sample.setPatient(patient);
        return sample;
    }

    /**
     * Method factory returns an instance of SampleMetadata from a sample metadata JSON.
     * @param sampleMetadataJson
     * @return SampleMetadata
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     */
    public static SampleMetadata buildNewSampleMetadatafromJson(String sampleMetadataJson)
            throws JsonProcessingException {
        DataTransformer dataTransformer = new DataTransformer();
        String processedSampleJson = dataTransformer.transformResearchSampleMetadata(sampleMetadataJson);
        SampleMetadata sampleMetadata = mapper.readValue(processedSampleJson, SampleMetadata.class);
        sampleMetadata.setImportDate(
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        return sampleMetadata;
    }
    
//  sampleMetadata.setSampleType(sampleJsonMap.get("cmoSampleClass"));
//  sampleMetadata.setSampleClass(sampleJsonMap.get("specimenType"));
//  sampleMetadata.setOncotreeCode(sampleJsonMap.get("oncoTreeCode"));
//  sampleMetadata.setGenePanel(sampleJsonMap.get("recipe"));
//  sampleMetadata.setIgoRequestId(sampleJsonMap.get("requestId"));
}
