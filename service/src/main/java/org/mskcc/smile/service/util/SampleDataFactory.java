package org.mskcc.smile.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.smile.model.PatientAlias;
import org.mskcc.smile.model.SampleAlias;
import org.mskcc.smile.model.SampleMetadata;
import org.mskcc.smile.model.SmilePatient;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.Status;
import org.mskcc.smile.model.dmp.DmpSampleMetadata;
import org.mskcc.smile.model.igo.IgoSampleManifest;

public class SampleDataFactory {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Pattern DMP_ACCESS_REGEX = Pattern.compile("P-\\d*-T\\d*-XS\\d*");
    private static final Pattern DMP_NORMAL_REGEX = Pattern.compile("P-\\d*-N\\d*-.*$");
    private static final SimpleDateFormat DMP_DATE_TUMOR_SEQ_FORMAT =
            new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss zzz");
    private static final Log LOG = LogFactory.getLog(SampleDataFactory.class);

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
     * Method factory returns an instance of SmileSample with sampleCategory "research"
     * from an instance of SampleMetadata and the provided request id.
     * @param requestId
     * @param sampleMetadata
     * @param isCmoRequest
     * @param sampleStatus
     * @return SmileSample
     */
    public static SmileSample buildNewResearchSampleFromMetadata(String requestId,
            SampleMetadata sampleMetadata, Boolean isCmoRequest, Status sampleStatus) {
        sampleMetadata.setIgoRequestId(requestId);
        if (sampleMetadata.getImportDate() == null) {
            sampleMetadata.setImportDate(Instant.now().toEpochMilli());
        }
        sampleMetadata.addAdditionalProperty("igoRequestId", requestId);
        if (isCmoRequest != null) {
            sampleMetadata.addAdditionalProperty("isCmoSample", String.valueOf(isCmoRequest));
        }
        sampleMetadata.setStatus(sampleStatus);

        // standardize value for sex (M -> Male, F -> Female)
        String sex = resolveIgoSampleSex(sampleMetadata.getSex());
        sampleMetadata.setSex(sex);

        SmileSample sample = new SmileSample();
        sample.addSampleMetadata(sampleMetadata);
        sample.setSampleCategory("research");
        sample.setDatasource("igo");
        sample.setSampleClass(sampleMetadata.getTumorOrNormal());
        sample.addSampleAlias(new SampleAlias(sampleMetadata.getPrimaryId(), "igoId"));
        if (!StringUtils.isBlank(sampleMetadata.getInvestigatorSampleId())) {
            sample.addSampleAlias(
                    new SampleAlias(sampleMetadata.getInvestigatorSampleId(), "investigatorId"));
        }

        // only create patient-sample relationship if the request is cmo and the patient id is non-empty
        if (isCmoRequest != null && !StringUtils.isBlank(sampleMetadata.getCmoPatientId())) {
            SmilePatient patient = new SmilePatient();
            patient.addPatientAlias(new PatientAlias(sampleMetadata.getCmoPatientId(), "cmoId"));
            sample.setPatient(patient);
        }

        return sample;
    }

    /**
     * Method factory returns an instance of SmileSample with sampleCategory "research"
     * from an instance of SampleMetadata and the provided request id.
     * @param requestId
     * @param igoSampleManifest
     * @param isCmoRequest
     * @param sampleStatus
     * @return SmileSample
     * @throws JsonProcessingException
     */
    public static SmileSample buildNewResearchSampleFromMetadata(String requestId,
            IgoSampleManifest igoSampleManifest, Boolean isCmoRequest, Status sampleStatus)
            throws JsonProcessingException {
        SampleMetadata sampleMetadata = new SampleMetadata(igoSampleManifest);
        return buildNewResearchSampleFromMetadata(requestId, sampleMetadata, isCmoRequest, sampleStatus);
    }

    /**
     * Method factory returns an instance of SmileSample with sampleCategory "clinical"
     * from an instance of SampleMetadata.
     * @param cmoPatientId
     * @param dmpSampleMetadata
     * @return SmileSample
     * @throws ParseException
     */
    public static SmileSample buildNewClinicalSampleFromMetadata(String cmoPatientId,
            DmpSampleMetadata dmpSampleMetadata) throws ParseException {
        SampleMetadata sampleMetadata = buildNewSampleMetadataFromDmpSample(cmoPatientId, dmpSampleMetadata);
        sampleMetadata.setImportDate(Instant.now().toEpochMilli());

        SmileSample sample = new SmileSample();
        sample.addSampleMetadata(sampleMetadata);
        sample.setSampleCategory("clinical");
        sample.setSampleClass(sampleMetadata.getTumorOrNormal());
        sample.setDatasource("dmp");
        sample.addSampleAlias(new SampleAlias(sampleMetadata.getPrimaryId(), "dmpId"));

        SmilePatient patient = new SmilePatient();
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
        sampleMetadata.setImportDate(Instant.now().toEpochMilli());

        // standardize value for sex (M -> Male, F -> Female)
        String sex = resolveIgoSampleSex(sampleMetadata.getSex());
        sampleMetadata.setSex(sex);

        // resolve igo request id if null from additionalProperties if possible
        if (sampleMetadata.getIgoRequestId() == null
                && !sampleMetadata.getAdditionalProperties().isEmpty()) {
            Map<String, String> additionalProperties = sampleMetadata.getAdditionalProperties();
            if (additionalProperties.containsKey("requestId")) {
                sampleMetadata.setIgoRequestId(additionalProperties.get("requestId"));
            } else if (additionalProperties.containsKey("igoRequestId")) {
                sampleMetadata.setIgoRequestId(additionalProperties.get("igoRequestId"));
            }
        }
        sampleMetadata.setStatus(extractStatusFromJson(sampleMetadataJson));
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

        // conditionally set additional properties
        if (dmpSampleMetadata.getCt() != null) {
            sampleMetadata.addAdditionalProperty("ct", String.valueOf(dmpSampleMetadata.getCt()));
        }
        if (!StringUtils.isBlank(dmpSampleMetadata.getMsiComment())) {
            sampleMetadata.addAdditionalProperty("msi-comment",
                    dmpSampleMetadata.getMsiComment());
        }
        if (!StringUtils.isBlank(dmpSampleMetadata.getMsiScore())) {
            sampleMetadata.addAdditionalProperty("msi-score",
                    dmpSampleMetadata.getMsiScore());
        }
        if (!StringUtils.isBlank(dmpSampleMetadata.getMsiType())) {
            sampleMetadata.addAdditionalProperty("msi-type",
                    dmpSampleMetadata.getMsiType());
        }
        if (!StringUtils.isBlank(dmpSampleMetadata.getConsentPartA())) {
            sampleMetadata.addAdditionalProperty("consent-parta",
                    dmpSampleMetadata.getConsentPartA());
        }
        if (!StringUtils.isBlank(dmpSampleMetadata.getConsentPartC())) {
            sampleMetadata.addAdditionalProperty("consent-partc",
                    dmpSampleMetadata.getConsentPartC());
        }
        if (dmpSampleMetadata.getTmbCohortPercentile() != null) {
            sampleMetadata.addAdditionalProperty("tmb_cohort_percentile",
                    String.valueOf(dmpSampleMetadata.getTmbCohortPercentile()));
        }
        if (dmpSampleMetadata.getTmbScore() != null) {
            sampleMetadata.addAdditionalProperty("tmb_score",
                    String.valueOf(dmpSampleMetadata.getTmbScore()));
        }
        if (dmpSampleMetadata.getTmbTtPercentile() != null) {
            sampleMetadata.addAdditionalProperty("tmb_tt_percentile",
                    String.valueOf(dmpSampleMetadata.getTmbTtPercentile()));
        }
        if (!StringUtils.isBlank(dmpSampleMetadata.getStandardCoverage())) {
            sampleMetadata.addAdditionalProperty("standard_coverage",
                    dmpSampleMetadata.getStandardCoverage());
        }
        if (!StringUtils.isBlank(dmpSampleMetadata.getRecommendedCoverage())) {
            sampleMetadata.addAdditionalProperty("recommended_coverage",
                    dmpSampleMetadata.getRecommendedCoverage());
        }
        return sampleMetadata;
    }

    private static String resolveDmpCollectionYear(String dmpDateTumorSequencing) {
        if (StringUtils.isBlank(dmpDateTumorSequencing)) {
            return "";
        }
        try {
            Date dmpDateSequenced = DMP_DATE_TUMOR_SEQ_FORMAT.parse(dmpDateTumorSequencing);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dmpDateSequenced);
            return String.valueOf(calendar.get(Calendar.YEAR));
        } catch (ParseException e) {
            LOG.error("Could not resolve DMP tumor sequencing date into value for sample metadata "
                    + "'collection year': " + dmpDateTumorSequencing + " - returning empty string "
                            + "since collection year isn't an essential field.", e);
            return "";
        }
    }

    private static String resolveDmpSampleType(Integer isMetastasis) {
        return dmpClinicalMetastasisValuesMap.getOrDefault(isMetastasis, "Unknown");
    }

    private static String resolveDmpSampleClass(String dmpSampleId) {
        Matcher cfDnaMatcher = DMP_ACCESS_REGEX.matcher(dmpSampleId);
        if (cfDnaMatcher.matches()) {
            return "cfDNA";
        }
        Matcher normalMatcher = DMP_NORMAL_REGEX.matcher(dmpSampleId);
        if (normalMatcher.matches()) {
            return "Normal";
        }
        return "Tumor";
    }

    private static String resolveDmpTumorOrNormal(String dmpSampleId) {
        Matcher matcher = DMP_NORMAL_REGEX.matcher(dmpSampleId);
        return matcher.matches() ? "Normal" : "Tumor";
    }

    private static String resolveDmpGender(Integer dmpGender) {
        return dmpGender.equals(1) ? "Male" : "Female";
    }

    private static Status extractStatusFromJson(String inputJson)
            throws JsonMappingException, JsonProcessingException {
        Status status = new Status();
        Map<String, Object> jsonMap = mapper.readValue(inputJson, Map.class);
        if (jsonMap.containsKey("status")) {
            status = mapper.convertValue(jsonMap.get("status"), Status.class);
        }
        return status;
    }

    private static String resolveIgoSampleSex(String sex) {
        // standardize value for sex (M -> Male, F -> Female)
        if (!StringUtils.isBlank(sex)) {
            return switch (sex.toUpperCase()) {
                case "M" -> "Male";
                case "F" -> "Female";
                default -> sex;
            };
        }
        return "";
    }
}
