package org.mskcc.cmo.metadb.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
        return buildNewResearchSampleFromMetadata(sampleMetadata.getRequestId(), sampleMetadata);
    }

    /**
     * Method factory returns an instance of MetadbSample with sampleCategory "research"
     * from an instance of SampleMetadata and the provided request id.
     * @param sampleMetadata
     * @return MetadbSample
     */
    public static MetadbSample buildNewResearchSampleFromMetadata(String requestId,
            SampleMetadata sampleMetadata) {
        sampleMetadata.setRequestId(requestId);
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
     * Method factory returns an instance of MetadbSample given a dmp sample json.
     * @param dmpSampleJson
     * @return MetadbSample
     * @throws JsonProcessingException
     */
    public static MetadbSample buildNewClinicalSampleFromJson(String dmpSampleJson) throws JsonProcessingException {
        Map<String, Object> datamap = mapper.readValue(dmpSampleJson, Map.class);
        SampleMetadata sampleMetadata = new SampleMetadata();
        sampleMetadata.setPrimaryId(datamap.get("dmp_sample_id").toString());
        sampleMetadata.setCmoPatientId("mapped patient id from crdb service");

        Integer isMetastasis = Integer.valueOf(datamap.get("is_metastasis").toString());
        // dmp isMetastasis mapping: 0 = Primary, 1 = Metastasis, 2 = Local Recurrence, 127 = Unknown
        sampleMetadata.setSampleType(datamap.get("is_metastasis").toString());
        sampleMetadata.setBaitSet(datamap.get("gene_panel").toString());
        sampleMetadata.setOncoTreeCode(datamap.get("tumor_type_code").toString());

        // setting sample clas is based on cohort id.. mskaccess = cfDNA, otherwise is Tumor
        // dmp id suffix = XS so maybe that's what we need to resolve by
        String sampleClassAsSpecimenType = sampleMetadata.getPrimaryId().matches("[ACCESS REGEX PATTERN]") ?
                "cfDNA" : "Tumor";
        sampleMetadata.setSpecimenType(sampleClassAsSpecimenType);
        String sex = datamap.get("gender").toString().equals("0") ? "Male" : "Female";
        sampleMetadata.setSex(sex);
        sampleMetadata.setTissueLocation(datamap.get("primary_site").toString());

        String tumorOrNormal = sampleMetadata.getPrimaryId().matches("DMP NORMAL SAMPLE SUFFIX PATTERN") ?
                "Normal" : "Tumor";
        sampleMetadata.setTumorOrNormal(tumorOrNormal);


        Map<String, String> additionalProperties = new HashMap<>();
        additionalProperties.put("msi-comment", datamap.get("msi-comment").toString());
        // etc and so on for the other additional properties that we would want to store for samples

        MetadbSample sample = new MetadbSample();
        sample.addSampleMetadata(sampleMetadata);
        sample.addSampleAlias(new SampleAlias(sampleMetadata.getPrimaryId(), "dmpId"));

        MetadbPatient patient = new MetadbPatient();
        patient.addPatientAlias(new PatientAlias(datamap.get("dmp_patient_id").toString(), "dmpId"));
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
        SampleMetadata sampleMetadata =
                mapper.readValue(sampleMetadataJson, SampleMetadata.class);
        sampleMetadata.setImportDate(
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        return sampleMetadata;
    }
}
