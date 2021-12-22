package org.mskcc.cmo.metadb.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.mskcc.cmo.metadb.model.MetadbPatient;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.PatientAlias;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.model.igo.IgoSampleManifest;

public class SampleDataFactory {
    private static final ObjectMapper mapper = new ObjectMapper();

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
        sample.setDatasource("igo");

        MetadbPatient patient = new MetadbPatient();
        patient.addPatientAlias(new PatientAlias(sampleMetadata.getCmoPatientId(), "cmoId"));
        sample.setPatient(patient);
        return sample;
    }

    /**
     * Method factory returns an instance of MetadbSample with sampleCategory "research"
     * from an instance of SampleMetadata and the provided request id.
     * @param requestId
     * @param igoSampleManifest
     * @return MetadbSample
     * @throws JsonProcessingException
     */
    public static MetadbSample buildNewResearchSampleFromMetadata(String requestId,
            IgoSampleManifest igoSampleManifest) throws JsonProcessingException {
        SampleMetadata sampleMetadata = new SampleMetadata(igoSampleManifest);
        return buildNewResearchSampleFromMetadata(requestId, sampleMetadata);
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
        //should we remove this alias?
        sample.addSampleAlias(new SampleAlias(
                sampleMetadata.getInvestigatorSampleId(), "investigatorId"));
        //need to add datasource, would it be dmp?
        sample.setDatasource("dmp");
        MetadbPatient patient = new MetadbPatient();
        patient.addPatientAlias(new PatientAlias(sampleMetadata.getCmoPatientId(), "cmoId"));
        sample.setPatient(patient);
        return sample;
    }

    /**
     * Method factory returns an instance of SampleMetadata from a sample metadata JSON.
     * @param sampleMetadataJson
     * @return SampleMetadata
     * @throws JsonProcessingException
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
