package org.mskcc.cmo.metadb.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mskcc.cmo.metadb.model.MetadbPatient;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.PatientAlias;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.model.dmp.DmpSampleMetadata;
import org.mskcc.cmo.metadb.model.igo.IgoSampleManifest;

public class SampleDataFactory {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Pattern DMP_ACCESS_REGEX = Pattern.compile("P-\\d*-T\\d*-XS\\d*");
    private static final Pattern DMP_NORMAL_REGEX = Pattern.compile("P-\\d*-N\\d*-.*$");
    private static final SimpleDateFormat DMP_DATE_TUMOR_SEQ_FORMAT =
            new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss zzz");
    private static Map<Integer, String> dmpClinicalMetastasisValuesMap
            = initDmpClinicalMetastasisValuesMap();

    private static Map<Integer, String> initDmpClinicalMetastasisValuesMap() {
        Map<Integer, String> map = new HashMap<>();
        map.put(0, "Primary");
        map.put(1, "Metastasis");
        map.put(2, "Local Recurrence");
        map.put(127, "Unknown");
        return map;
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
     * @param cmoPatientId
     * @param dmpSampleMetadata
     * @return MetadbSample
     */
    public static MetadbSample buildNewClinicalSampleFromMetadata(String cmoPatientId,
            DmpSampleMetadata dmpSampleMetadata) throws ParseException {
        SampleMetadata sampleMetadata = buildNewSampleMetadataFromDmpSample(cmoPatientId, dmpSampleMetadata);
        sampleMetadata.setImportDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        MetadbSample sample = new MetadbSample();
        sample.addSampleMetadata(sampleMetadata);
        sample.setSampleCategory("clinical");
        sample.setSampleClass(sampleMetadata.getTumorOrNormal());
        sample.setDatasource("dmp");
        sample.addSampleAlias(new SampleAlias(sampleMetadata.getPrimaryId(), "dmpId"));

        MetadbPatient patient = new MetadbPatient();
        patient.addPatientAlias(new PatientAlias(cmoPatientId, "cmoId"));
        patient.addPatientAlias(new PatientAlias(dmpSampleMetadata.getDmpPatientId(), "dmpId"));
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

    /**
     * Method factory returns an instance of SampleMetadata from a cmo patient id and dmp sample metadata.
     * @param cmoPatientId
     * @param dmpSampleMetadata
     * @return SampleMetadata
     * @throws ParseException
     */
    public static SampleMetadata buildNewSampleMetadataFromDmpSample(String cmoPatientId,
            DmpSampleMetadata dmpSampleMetadata) throws ParseException {
        SampleMetadata sampleMetadata = new SampleMetadata(dmpSampleMetadata);
        sampleMetadata.setCmoPatientId(cmoPatientId);
        sampleMetadata.setSex(resolveDmpGender(dmpSampleMetadata.getGender()));
        sampleMetadata.setCollectionYear(
                resolveDmpCollectionYear(dmpSampleMetadata.getDateTumorSequencing()));
        sampleMetadata.setTumorOrNormal(
                resolveDmpTumorOrNormal(dmpSampleMetadata.getDmpSampleId()));
        sampleMetadata.setSampleClass(
                resolveDmpSampleClass(dmpSampleMetadata.getDmpSampleId()));
        sampleMetadata.setSampleType(
                resolveDmpSampleType(dmpSampleMetadata.getIsMetastasis()));
        sampleMetadata.addAdditionalProperty("msi-comment",
                dmpSampleMetadata.getMsiComment());
        sampleMetadata.addAdditionalProperty("msi-score",
                dmpSampleMetadata.getMsiScore());
        sampleMetadata.addAdditionalProperty("msi-type",
                dmpSampleMetadata.getMsiType());
        sampleMetadata.addAdditionalProperty("consent-parta",
                dmpSampleMetadata.getConsentPartA());
        sampleMetadata.addAdditionalProperty("consent-partc",
                dmpSampleMetadata.getConsentPartC());
        sampleMetadata.addAdditionalProperty("tmb_cohort_percentile",
                String.valueOf(dmpSampleMetadata.getTmbCohortPercentile()));
        sampleMetadata.addAdditionalProperty("tmb_score",
                String.valueOf(dmpSampleMetadata.getTmbScore()));
        sampleMetadata.addAdditionalProperty("tmb_tt_percentile",
                String.valueOf(dmpSampleMetadata.getTmbTtPercentile()));
        return sampleMetadata;
    }

    private static String resolveDmpCollectionYear(String dmpDateTumorSequencing) throws ParseException {
        Date dmpDateSequenced = DMP_DATE_TUMOR_SEQ_FORMAT.parse(dmpDateTumorSequencing);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dmpDateSequenced);
        return String.valueOf(calendar.get(Calendar.YEAR));
    }

    private static String resolveDmpSampleType(Integer isMetastasis) {
        return dmpClinicalMetastasisValuesMap.getOrDefault(isMetastasis, "Unknown");
    }

    private static String resolveDmpSampleClass(String dmpSampleId) {
        Matcher matcher = DMP_ACCESS_REGEX.matcher(dmpSampleId);
        return matcher.matches() ? "cfDNA" : "Tumor";
    }

    private static String resolveDmpTumorOrNormal(String dmpSampleId) {
        Matcher matcher = DMP_NORMAL_REGEX.matcher(dmpSampleId);
        return matcher.matches() ? "Normal" : "Tumor";
    }

    private static String resolveDmpGender(Integer dmpGender) {
        return dmpGender.equals(0) ? "Male" : "Female";
    }
}
