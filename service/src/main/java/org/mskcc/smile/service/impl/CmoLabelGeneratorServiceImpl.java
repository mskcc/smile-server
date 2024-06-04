package org.mskcc.smile.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.smile.commons.enums.CmoSampleClass;
import org.mskcc.smile.commons.enums.NucleicAcid;
import org.mskcc.smile.commons.enums.SampleOrigin;
import org.mskcc.smile.commons.enums.SampleType;
import org.mskcc.smile.commons.enums.SpecimenType;
import org.mskcc.smile.model.SampleMetadata;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.Status;
import org.mskcc.smile.model.igo.IgoSampleManifest;
import org.mskcc.smile.service.CmoLabelGeneratorService;
import org.mskcc.smile.service.SmileSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author ochoaa
 */
@Service
public class CmoLabelGeneratorServiceImpl implements CmoLabelGeneratorService {
    @Autowired
    private SmileSampleService sampleService;

    private final ObjectMapper mapper = new ObjectMapper();
    private static final Log LOG = LogFactory.getLog(CmoLabelGeneratorServiceImpl.class);
    // example: C-1235-X001-d01
    public static final Pattern CMO_SAMPLE_ID_REGEX =
            Pattern.compile("^C-([a-zA-Z0-9]+)-([NTRMLUPSGXF])([0-9]{3})-([d|r])(.*$)");
    // example: JH123-12345T
    public static final Pattern CMO_CELLLINE_ID_REGEX =
            Pattern.compile("^([A-Za-z0-9]+[A-Za-z0-9_]+)-([A-Za-z0-9]+)$");
    public static final String CMO_LABEL_SEPARATOR = "-";
    public static final Integer CMO_PATIENT_ID_GROUP = 1;
    public static final Integer CMO_SAMPLE_TYPE_ABBREV_GROUP = 2;
    public static final Integer CMO_SAMPLE_COUNTER_GROUP = 3;
    public static final Integer CMO_SAMPLE_COUNTER_STRING_PADDING = 3;
    public static final Integer CMO_SAMPLE_NUCACID_ABBREV_GROUP = 4;
    public static final Integer CMO_SAMPLE_NUCACID_COUNTER_GROUP = 5;
    public static final Integer CMO_SAMPLE_NUCACID_COUNTER_PADDING = 2;

    // globals for mapping sample type abbreviations
    private static final Map<SpecimenType, String> SPECIMEN_TYPE_ABBREV_MAP = initSpecimenTypeAbbrevMap();
    private static final Map<SampleOrigin, String> SAMPLE_ORIGIN_ABBREV_MAP = initSampleOriginAbbrevMap();
    private static final Map<CmoSampleClass, String> SAMPLE_CLASS_ABBREV_MAP = initCmoSampleClassAbbrevMap();
    private static final List<SampleOrigin> KNOWN_CFDNA_SAMPLE_ORIGINS =
            Arrays.asList(SampleOrigin.URINE,
                    SampleOrigin.CEREBROSPINAL_FLUID,
                    SampleOrigin.PLASMA,
                    SampleOrigin.WHOLE_BLOOD);
    private static final String SAMPLE_ORIGIN_ABBREV_DEFAULT = "T";

    /**
     * Init specimen type abbreviation mappings.
     * @return
     */
    private static Map<SpecimenType, String> initSpecimenTypeAbbrevMap() {
        Map<SpecimenType, String> map = new HashMap<>();
        map.put(SpecimenType.PDX, "X");
        map.put(SpecimenType.XENOGRAFT, "X");
        map.put(SpecimenType.XENOGRAFTDERIVEDCELLLINE, "X");
        map.put(SpecimenType.ORGANOID, "G");
        return map;
    }

    /**
     * Init sample origin abbreviation mappings.
     * @return
     */
    private static Map<SampleOrigin, String> initSampleOriginAbbrevMap() {
        Map<SampleOrigin, String> map = new HashMap<>();
        map.put(SampleOrigin.URINE, "U");
        map.put(SampleOrigin.CEREBROSPINAL_FLUID, "S");
        map.put(SampleOrigin.PLASMA, "L");
        map.put(SampleOrigin.WHOLE_BLOOD, "L");
        return map;
    }

    /**
     * Init CMO sample class abbreviation mappings.
     * @return
     */
    private static Map<CmoSampleClass, String> initCmoSampleClassAbbrevMap() {
        Map<CmoSampleClass, String> map = new HashMap<>();
        map.put(CmoSampleClass.UNKNOWN_TUMOR, "T");
        map.put(CmoSampleClass.LOCAL_RECURRENCE, "R");
        map.put(CmoSampleClass.PRIMARY, "P");
        map.put(CmoSampleClass.RECURRENCE, "R");
        map.put(CmoSampleClass.METASTASIS, "M");
        map.put(CmoSampleClass.NORMAL, "N");
        map.put(CmoSampleClass.ADJACENT_NORMAL, "N");
        map.put(CmoSampleClass.ADJACENT_TISSUE, "T");
        return map;
    }

    /**
     * Compares the regex groups for 2 CMO labels generated for the same IGO sample.
     * The padded counter strings encoded in the cmo labels being compared are ignored.
     * Note: the 'same' IGO sample is determined based on IGO sample ID matching,
     *  or primaryId if sample metadata provided as universal schema format.
     * Groups compared:
     *  1. cmo patient id prefix
     *  2. sample type abbreviation
     *  3. nucleic acid abbreviation
     * @param newCmoLabel
     * @param existingCmoLabel
     * @return Boolean
     */
    @Override
    public Boolean igoSampleRequiresLabelUpdate(String newCmoLabel, String existingCmoLabel) {
        // if the labels match then just return false
        if (newCmoLabel.equals(existingCmoLabel)) {
            return Boolean.FALSE;
        }
        // proceed with regular (non-cell line) cmo sample label checking
        Matcher matcherNewCelllineLabel = CMO_CELLLINE_ID_REGEX.matcher(newCmoLabel);
        Matcher matcherNewLabel = CMO_SAMPLE_ID_REGEX.matcher(newCmoLabel);
        Matcher matcherExistingLabel = CMO_SAMPLE_ID_REGEX.matcher(existingCmoLabel);

        // if we have a cell line sample and the existing and new label generated do not match
        // then return true so that we update to the new cmo label generated
        if (matcherNewCelllineLabel.find() && !matcherNewLabel.find()) {
            return Boolean.TRUE;
        }

        if (!matcherExistingLabel.find() || !matcherNewLabel.find()) {
            if (matcherNewLabel.find() && !matcherExistingLabel.find()) {
                return Boolean.TRUE;
            }
            throw new IllegalStateException("New CMO label and existing CMO label do not meet CMO ID "
                    + "regex requirements: new = " + newCmoLabel + ", existingLabel = " + existingCmoLabel);
        }

        // compare cmo patient id prefix
        if (!compareMatcherGroups(matcherNewLabel, matcherExistingLabel, CMO_PATIENT_ID_GROUP)) {
            LOG.info("CMO patient ID differs between incoming IGO sample and matching IGO sample "
                    + "from database. Sample will be published to IGO_SAMPLE_UPDATE topic.");
            return Boolean.TRUE;
        }
        // compare sample type abbreviation
        if (!compareMatcherGroups(matcherNewLabel, matcherExistingLabel, CMO_SAMPLE_TYPE_ABBREV_GROUP)) {
            LOG.info("Sample Type abbreviation differs between incoming IGO sample and matching IGO sample "
                    + "from database. Sample will be published to IGO_SAMPLE_UPDATE topic.");
            return Boolean.TRUE;
        }
        // compare nucleic acid abbreviation
        if (!compareMatcherGroups(matcherNewLabel, matcherExistingLabel, CMO_SAMPLE_NUCACID_ABBREV_GROUP)) {
            LOG.info("Nucleic Acid abbreviation differs between incoming IGO sample and matching IGO sample "
                    + "from database. Sample will be published to IGO_SAMPLE_UPDATE topic.");
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Boolean compareMatcherGroups(Matcher matcher1, Matcher matcher2, Integer group) {
        return matcher1.group(group).equalsIgnoreCase(matcher2.group(group));
    }

    @Override
    public String generateCmoSampleLabel(String requestId, IgoSampleManifest sampleManifest,
            List<SampleMetadata> existingSamples) {
        // if sample is a cellline sample then generate a cmo cellline label
        if (isCmoCelllineSample(sampleManifest)) {
            return generateCmoCelllineSampleLabel(requestId, sampleManifest.getInvestigatorSampleId());
        }

        // resolve sample type abbreviation
        String sampleTypeAbbreviation = resolveSampleTypeAbbreviation(sampleManifest);

        // resolve the sample counter value to use for the cmo label
        Integer sampleCounter =  resolveSampleIncrementValue(sampleManifest.getIgoId(), existingSamples);
        String paddedSampleCounter = getPaddedIncrementString(sampleCounter,
                CMO_SAMPLE_COUNTER_STRING_PADDING);

        // resolve nucleic acid abbreviation
        String nucleicAcidAbbreviation = resolveNucleicAcidAbbreviation(sampleManifest);
        if (nucleicAcidAbbreviation == null) {
            LOG.error("Could not resolve nucleic acid abbreviation from sample "
                    + "type or naToExtract: " + sampleManifest.toString());
            return null;
        }
        // get next increment for nucleic acid abbreviation
        Integer nextNucAcidCounter = getNextNucleicAcidIncrement(nucleicAcidAbbreviation, existingSamples);
        String paddedNucAcidCounter = getPaddedIncrementString(nextNucAcidCounter,
                CMO_SAMPLE_NUCACID_COUNTER_PADDING);

        String patientId = sampleManifest.getCmoPatientId();

        return getFormattedCmoSampleLabel(patientId, sampleTypeAbbreviation, paddedSampleCounter,
                nucleicAcidAbbreviation, paddedNucAcidCounter);
    }

    @Override
    public String generateCmoSampleLabel(SampleMetadata sampleMetadata,
            List<SampleMetadata> existingSamples) {
        // if sample is a cellline sample then generate a cmo cellline label
        if (isCmoCelllineSample(sampleMetadata.getSampleClass(), sampleMetadata.getCmoSampleIdFields())) {
            return generateCmoCelllineSampleLabel(sampleMetadata.getIgoRequestId(),
                    sampleMetadata.getInvestigatorSampleId());
        }

        // resolve sample type abbreviation
        String sampleTypeAbbreviation = resolveSampleTypeAbbreviation(sampleMetadata.getSampleClass(),
                sampleMetadata.getSampleOrigin(), sampleMetadata.getSampleType());
        if (sampleTypeAbbreviation == null) {
            LOG.error("Could not resolve sample type abbreviation "
                    + "from specimen type ('sampleClass'), sample origin, or sample "
                    + "class ('sampleType'): " + sampleMetadata.toString());
            return null;
        }

        // resolve the sample counter value to use for the cmo label
        Integer sampleCounter =  resolveSampleIncrementValue(sampleMetadata.getPrimaryId(), existingSamples);
        String paddedSampleCounter = getPaddedIncrementString(sampleCounter,
                CMO_SAMPLE_COUNTER_STRING_PADDING);

        // resolve nucleic acid abbreviation
        String sampleTypeString = sampleMetadata.getCmoSampleIdFields().get("sampleType");
        String recipe = sampleMetadata.getCmoSampleIdFields().get("recipe");
        String naToExtract = sampleMetadata.getCmoSampleIdFields().get("naToExtract");
        String nucleicAcidAbbreviation =
                resolveNucleicAcidAbbreviation(sampleTypeString, recipe, naToExtract);
        if (nucleicAcidAbbreviation == null) {
            LOG.error("Could not resolve nucleic acid abbreviation from sample "
                    + "type or naToExtract: " + sampleMetadata.toString());
            return null;
        }
        // get next increment for nucleic acid abbreviation
        Integer nextNucAcidCounter = getNextNucleicAcidIncrement(nucleicAcidAbbreviation, existingSamples);
        String paddedNucAcidCounter = getPaddedIncrementString(nextNucAcidCounter,
                CMO_SAMPLE_NUCACID_COUNTER_PADDING);

        String patientId = sampleMetadata.getCmoPatientId();

        return getFormattedCmoSampleLabel(patientId, sampleTypeAbbreviation, paddedSampleCounter,
                nucleicAcidAbbreviation, paddedNucAcidCounter);
    }

    @Override
    public Status generateSampleStatus(String requestId, IgoSampleManifest sampleManifest,
            List<SampleMetadata> existingSamples) throws JsonProcessingException {
        Status sampleStatus = new Status();
        Map<String, String> validationReport = new HashMap<>();

        String sampleTypeAbbreviation = resolveSampleTypeAbbreviation(sampleManifest);
        if (sampleTypeAbbreviation == null
                || sampleTypeAbbreviation.equals("F")) {
            validationReport.put("sample type abbreviation",
                    "could not resolve based on specimenType, sampleOrigin, or sampleClass");
        }
        if (resolveNucleicAcidAbbreviation(sampleManifest) == null) {
            validationReport.put("nucleic acid abbreviation",
                    "could not resolve based on sampleType or naToExtract");
        }
        if (validationReport.isEmpty()) {
            sampleStatus.setValidationStatus(Boolean.TRUE);
        } else {
            sampleStatus.setValidationStatus(Boolean.FALSE);
        }
        sampleStatus.setValidationReport(mapper.writeValueAsString(validationReport));
        return sampleStatus;
    }

    @Override
    public Status generateSampleStatus(SampleMetadata sampleMetadata,
            List<SampleMetadata> existingSamples) throws JsonProcessingException {
        Status sampleStatus = new Status();
        Map<String, String> validationReport = new HashMap<>();

        String sampleTypeAbbreviation = resolveSampleTypeAbbreviation(sampleMetadata.getSampleClass(),
                sampleMetadata.getSampleOrigin(), sampleMetadata.getSampleType());
        if (sampleTypeAbbreviation == null
                || sampleTypeAbbreviation.equals("F")) {
            validationReport.put("sample type abbreviation",
                    "could not resolve based on specimenType, sampleOrigin, or sampleClass");
        }
        String sampleTypeString = sampleMetadata.getCmoSampleIdFields().get("sampleType");
        String recipe = sampleMetadata.getCmoSampleIdFields().get("recipe");
        String naToExtract = sampleMetadata.getCmoSampleIdFields().get("naToExtract");
        if (resolveNucleicAcidAbbreviation(sampleTypeString, recipe, naToExtract) == null) {
            validationReport.put("nucleic acid abbreviation",
                    "could not resolve based on sampleType or naToExtract");
        }
        if (validationReport.isEmpty()) {
            sampleStatus.setValidationStatus(Boolean.TRUE);
        } else {
            sampleStatus.setValidationStatus(Boolean.FALSE);
        }
        sampleStatus.setValidationReport(mapper.writeValueAsString(validationReport));
        return sampleStatus;
    }

    private String getFormattedCmoSampleLabel(String patientId, String sampleTypeAbbreviation,
            String paddedSampleCounter, String nucleicAcidAbbreviation, String paddedNucAcidCounter) {
        return String.format("%s-%s%s-%s%s", patientId, sampleTypeAbbreviation, paddedSampleCounter,
                nucleicAcidAbbreviation, paddedNucAcidCounter);
    }

    private String resolveNucleicAcidAbbreviation(String sampleTypeString,
            String recipe, String naToExtract) {
        try {
            SampleType sampleType = SampleType.fromString(sampleTypeString);
            // resolve from sample type if not null
            // if pooled library then resolve value based on recipe
            switch (sampleType) {
                case POOLED_LIBRARY:
                    return (recipe.equalsIgnoreCase("RNASeq") || recipe.equalsIgnoreCase("User_RNA"))
                            ? "r" : "d";
                case DNA:
                case CFDNA:
                case DNA_LIBRARY:
                    return "d";
                case RNA:
                    return "r";
                default:
                    return "d";
            }
        } catch (Exception e) {
            LOG.debug("Could not resolve sample type acid from 'sampleType' - using default 'd'");
        }
        // if nucleic acid abbreviation is still unknown then attempt to resolve from
        // sample metadata --> cmo sample id fields --> naToExtract
        try {
            NucleicAcid nucAcid = NucleicAcid.fromString(naToExtract);
            if (nucAcid != null) {
                switch (nucAcid) {
                    case DNA:
                    case DNA_AND_RNA:
                    case CFDNA:
                        return "d";
                    case RNA:
                        return "r";
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            LOG.debug("Could not resolve nucleic acid from 'naToExtract' - using default 'd'");
            return "d";
        }

        return null;
    }

    /**
     * Resolve the nucleic acid abbreviation for the generated cmo sample label.
     * @param sampleManifest
     * @return
     */
    private String resolveNucleicAcidAbbreviation(IgoSampleManifest sampleManifest) {
        String sampleTypeString = sampleManifest.getCmoSampleIdFields().get("sampleType");
        String recipe = sampleManifest.getCmoSampleIdFields().get("recipe");
        String naToExtract = sampleManifest.getCmoSampleIdFields().get("naToExtract");
        return resolveNucleicAcidAbbreviation(sampleTypeString, recipe, naToExtract);
    }

    @Override
    public String resolveSampleTypeAbbreviation(String specimenTypeValue, String sampleOriginValue,
            String cmoSampleClassValue) {
        try {
            SpecimenType specimenType = SpecimenType.fromValue(specimenTypeValue);
            // if can be mapped directly from specimen type then use corresponding abbreviation
            if (SPECIMEN_TYPE_ABBREV_MAP.containsKey(specimenType)) {
                return SPECIMEN_TYPE_ABBREV_MAP.get(specimenType);
            }
            // if specimen type is cfDNA and sample origin is known type for cfDNA samples
            // then return corresponding abbreviation
            SampleOrigin sampleOrigin = SampleOrigin.fromValue(sampleOriginValue);
            if (sampleOrigin != null) {
                if (specimenType.equals(SpecimenType.CFDNA)
                        && KNOWN_CFDNA_SAMPLE_ORIGINS.contains(sampleOrigin)) {
                    return SAMPLE_ORIGIN_ABBREV_MAP.get(sampleOrigin);
                }
                // if specimen type is exosome then map abbreviation from sample origin or use default value
                if (specimenType.equals(SpecimenType.EXOSOME)) {
                    return SAMPLE_ORIGIN_ABBREV_MAP.getOrDefault(sampleOrigin, SAMPLE_ORIGIN_ABBREV_DEFAULT);
                }
            }
        } catch (Exception e) {
            LOG.debug("Could not resolve specimen type acid from 'specimenType': "
                    + specimenTypeValue);
        }

        // if abbreviation is still not resolved then try to resolve from sample class
        String sampleTypeAbbreviation = "F";
        try {
            CmoSampleClass sampleClass = CmoSampleClass.fromValue(cmoSampleClassValue);
            if (SAMPLE_CLASS_ABBREV_MAP.containsKey(sampleClass)) {
                sampleTypeAbbreviation = SAMPLE_CLASS_ABBREV_MAP.get(sampleClass);
            }
        } catch (Exception e) {
            // happens if cmoSampleClassValue is not found in CmoSampleClass
            // nothing to do here since since sampleTypeAbbreviation
            // is initialized to default 'F'
        }

        if (sampleTypeAbbreviation.equalsIgnoreCase("F")) {
            LOG.warn("Could not resolve sample type abbreviation from specimen type,"
                     + " sample origin, or sample class - using default 'F' ");
        }
        return sampleTypeAbbreviation;
    }

    /**
     * Resolves the sample type abbreviation for the generated cmo sample label.
     * @param sampleManifest
     * @return
     */
    private String resolveSampleTypeAbbreviation(IgoSampleManifest sampleManifest) {
        return resolveSampleTypeAbbreviation(sampleManifest.getSpecimenType(),
                sampleManifest.getSampleOrigin(), sampleManifest.getCmoSampleClass());
    }

    /**
     * Returns a padded string with the provided increment and padding size.
     * @param increment
     * @param padding
     * @return String
     */
    private String getPaddedIncrementString(Integer increment, Integer padding) {
        return StringUtils.leftPad(String.valueOf(increment), padding, "0");
    }

    /**
     * Given a primaryId and list of existing samples, returns the increment to use
     * for the padded sample counter string embedded in the cmo sample label.
     * @param primaryId
     * @param existingSamples
     * @return Integer
     */
    private Integer resolveSampleIncrementValue(String primaryId, List<SampleMetadata> existingSamples) {
        if (existingSamples.isEmpty()) {
            return 1;
        }

        // if we find a match by the primary id then return the increment parsed from
        // the matching sample's current cmo label
        for (SampleMetadata sample : existingSamples) {
            if (sample.getPrimaryId().equalsIgnoreCase(primaryId)) {
                Matcher matcher = CMO_SAMPLE_ID_REGEX.matcher(sample.getCmoSampleName());
                if (matcher.find()) {
                    Integer currentIncrement = Integer.valueOf(matcher.group(CMO_SAMPLE_COUNTER_GROUP));
                    return currentIncrement;
                }
            }
        }

        // assuming that a match by the primary id has not been identified
        // then we can use the next sample increment logic like before
        return getNextSampleIncrement(existingSamples);
    }

    /**
     * Returns the next sample increment.
     * @param samples
     * @return Integer
     */
    private Integer getNextSampleIncrement(List<SampleMetadata> samples) {
        // return 1 if samples is empty
        if (samples.isEmpty()) {
            return 1;
        }
        // otherwise extract the max counter from the current set of samples
        // do not rely on the size of the list having the exact same counter
        // to prevent accidentally giving samples the same counter
        Integer maxIncrement = 0;
        for (SampleMetadata sample : samples) {
            // skip samples without a defined cmo sample label
            if (StringUtils.isBlank(sample.getCmoSampleName())) {
                continue;
            }
            // skip cell line samples
            if (CMO_CELLLINE_ID_REGEX.matcher(sample.getCmoSampleName()).find()) {
                continue;
            }
            Matcher matcher = CMO_SAMPLE_ID_REGEX.matcher(sample.getCmoSampleName());
            // increment assigned to the current sample is in group 3 of matcher
            if (matcher.find()) {
                Integer currentIncrement = Integer.valueOf(matcher.group(CMO_SAMPLE_COUNTER_GROUP));
                if (currentIncrement > maxIncrement) {
                    maxIncrement = currentIncrement;
                }
            }
        }
        return maxIncrement + 1;
    }

    /**
     * Returns the nucleic acid increment. Counter will be a 2 digit integer value range
     * from 01-99 (values less < 10 are filled in with zeros '0' to preserve 2-digit format).
     * From the time of implementation the first sample for a particular Nucleic Acid get 01.
     * @param nucleicAcidAbbreviation
     * @param samples
     * @return Integer
     */
    private Integer getNextNucleicAcidIncrement(String nucleicAcidAbbreviation,
            List<SampleMetadata> samples) {
        if (samples.isEmpty()) {
            return 1;
        }
        // otherwise extract the max counter from the current set of samples
        // do not rely on the size of the list having the exact same counter
        // to prevent accidentally giving samples the same counter
        Integer maxIncrement = 0;
        for (SampleMetadata sample : samples) {
            // skip cell line samples
            if (CMO_CELLLINE_ID_REGEX.matcher(sample.getCmoSampleName()).find()) {
                continue;
            }
            Matcher matcher = CMO_SAMPLE_ID_REGEX.matcher(sample.getCmoSampleName());
            // increment assigned to the current sample is in group 3 of matcher
            if (matcher.find()) {
                // nucleic acid abbreviation determines which counters we consider
                // when iterating through sample list
                if (!matcher.group(CMO_SAMPLE_NUCACID_ABBREV_GROUP)
                        .equalsIgnoreCase(nucleicAcidAbbreviation)) {
                    continue;
                }
                Integer currentIncrement;
                if (matcher.group(CMO_SAMPLE_NUCACID_COUNTER_GROUP) == null
                        || matcher.group(CMO_SAMPLE_NUCACID_COUNTER_GROUP).isEmpty()) {
                    currentIncrement = 1;
                } else {
                    currentIncrement = Integer.valueOf(matcher.group(CMO_SAMPLE_NUCACID_COUNTER_GROUP));
                }
                if (currentIncrement > maxIncrement) {
                    maxIncrement = currentIncrement;
                }
            }
        }
        return maxIncrement + 1;
    }

    private String generateCmoCelllineSampleLabel(String requestId, String sampleInvestigatorId) {
        String formattedRequestId = requestId.replaceAll("[-_]", "");
        return sampleInvestigatorId + CMO_LABEL_SEPARATOR + formattedRequestId;
    }

    private Boolean isCmoCelllineSample(String specimenType, Map<String, String> cmoSampleIdFields) {
        // if specimen type is not cellline or cmo sample id fields are null then return false
        if (!specimenType.equalsIgnoreCase("CellLine")
                || cmoSampleIdFields == null) {
            return  Boolean.FALSE;
        }
        String normalizedPatientId = cmoSampleIdFields.get("normalizedPatientId");
        return (!StringUtils.isBlank(normalizedPatientId)
                && !normalizedPatientId.equalsIgnoreCase("MRN_REDACTED"));
    }

    private Boolean isCmoCelllineSample(IgoSampleManifest sample) {
        return isCmoCelllineSample(sample.getSpecimenType(), sample.getCmoSampleIdFields());
    }

    @Override
    public SmileRequest addCmoSampleLabelsToRequestSamples(SmileRequest request) throws Exception {
        List<SmileSample> requestSamples = request.getSmileSampleList();
        Map<String, List<SampleMetadata>> patientSamplesMap = getPatientSamplesMap(requestSamples);

        // udpated samples list will store samples which had a label generated successfully
        List<SmileSample> updatedSamples = new ArrayList<>();
        for (SmileSample sample : requestSamples) {
            SampleMetadata sm = sample.getLatestSampleMetadata();

            // skip over samples with missing cmo patient id - this should be getting caught
            // by the request filter but this is here as an extra precaution
            if (StringUtils.isBlank(sm.getCmoPatientId())) {
                LOG.warn("Sample is missing CMO patient ID that was not caught by the "
                        + "request filter: " + mapper.writeValueAsString(sample));
                continue;
            }

            Status sampleStatus = sm.getStatus();
            if (sampleStatus.getValidationStatus()) {
                // get existing patient samples for cmo patient id
                List<SampleMetadata> existingSamples = patientSamplesMap.getOrDefault(sm.getCmoPatientId(),
                        new ArrayList<>());

                String newCmoSampleLabel = generateCmoSampleLabel(sm, existingSamples);
                if (newCmoSampleLabel == null) {
                    sampleStatus = generateSampleStatus(sm, existingSamples);
                    LOG.error("Unable to generate CMO sample label for sample: "
                            + sm.getPrimaryId());
                } else {
                    // check if matching sample found and determine if label actually needs
                    // updating or if we can use the same label that
                    // is already persisted for this sample
                    // note that we want to continue publishing to the IGO_SAMPLE_UPDATE_TOPIC
                    // since there might be other metadata changes that need to be persisted
                    // that may not necessarily affect the cmo label generated
                    String resolvedCmoSampleLabel = resolveAndUpdateCmoSampleLabel(
                            sm.getPrimaryId(), existingSamples, newCmoSampleLabel);

                    // update patient sample map and list of updated samples for request
                    sm.setStatus(sampleStatus);
                    sm.setCmoSampleName(resolvedCmoSampleLabel);
                    patientSamplesMap.put(sm.getCmoPatientId(),
                            updatePatientSampleList(existingSamples, sm));
                }
                // update sample status
                sm.setStatus(sampleStatus);
            }
            // update sample with updated sample metadata containing possibly updated cmo sample label
            sample.setLatestSampleMetadata(sm);
            updatedSamples.add(sample);
        }
        request.setSmileSampleList(updatedSamples);
        return request;
    }

    @Override
    public SampleMetadata updateCmoSampleLabel(SampleMetadata sample) throws Exception {
        List<SampleMetadata> existingSamples =
                getExistingPatientSamples(sample.getCmoPatientId());
        // Case when sample update json doesn't have status
        if (sample.getStatus() == null) {
            Status newSampleStatus = generateSampleStatus(sample, existingSamples);
            sample.setStatus(newSampleStatus);
        }
        if (sample.getStatus().getValidationStatus()) {
            // generate new cmo sample label and update sample metadata object
            String newCmoSampleLabel = generateCmoSampleLabel(sample, existingSamples);
            if (newCmoSampleLabel == null) {
                Status newSampleStatus = generateSampleStatus(sample, existingSamples);
                sample.setStatus(newSampleStatus);
            }

            // check if matching sample found and determine if label actually needs updating
            // or if we can use the same label that is already persisted for this sample
            // note that we want to continue publishing to the IGO_SAMPLE_UPDATE_TOPIC since
            // there might be other metadata changes that need to be persisted that may not
            // necessarily affect the cmo label generated
            String resolvedCmoSampleLabel = resolveAndUpdateCmoSampleLabel(
                    sample.getPrimaryId(), existingSamples, newCmoSampleLabel);
            sample.setCmoSampleName(resolvedCmoSampleLabel);
        }
        return sample;
    }

    private String resolveAndUpdateCmoSampleLabel(String samplePrimaryId,
            List<SampleMetadata> existingSamples, String newCmoSampleLabel) {
        // check for matching sample in existing samples list and determine if label
        // actually needs updating or if we can use the same label that is alredy
        // persisted for this sample
        SampleMetadata matchingSample = null;
        for (SampleMetadata s : existingSamples) {
            if (s.getPrimaryId().equalsIgnoreCase(samplePrimaryId)) {
                matchingSample = s;
                break;
            }
        }
        // if sample does not require a label update then use the existing label from the
        // matching sample identified if applicable - otherwise use the newly generated label
        Boolean updateRequired = Boolean.FALSE;
        if (matchingSample != null) {
            try {
                updateRequired = igoSampleRequiresLabelUpdate(
                        newCmoSampleLabel, matchingSample.getCmoSampleName());
            } catch (IllegalStateException e) {
                // note: special cases where we just want to keep the existing label even if it's not
                // meeting the cmo id regex requirements only if the existing cmo sample name
                // matches the existing investigator sample id
                if (matchingSample.getCmoSampleName().equals(matchingSample.getInvestigatorSampleId())) {
                    return matchingSample.getCmoSampleName();
                } else {
                    LOG.error("Falling back on existing cmo sample name for sample.", e);
                    return matchingSample.getCmoSampleName();
                }
            } catch (NullPointerException e2) {
                LOG.error("NPE caught during label generation check: ", e2);
                LOG.error("Falling back on existing cmo sample name for sample.");
                return matchingSample.getCmoSampleName();
            }
            if (!updateRequired) {
                LOG.info("No change detected for CMO sample label metadata - using "
                        + "existing CMO label for matching IGO sample from database.");
                return matchingSample.getCmoSampleName();
            }
        }
        LOG.info("Changes detected in CMO sample label metadata - "
                    + "updating sample CMO label to newly generated label.");
        return newCmoSampleLabel;
    }

    private List<SampleMetadata> updatePatientSampleList(List<SampleMetadata> existingSamples,
            SampleMetadata sample) throws JsonProcessingException {
        Boolean foundMatching = Boolean.FALSE;
        // if sample already exists in the existing samples list then simply replace at the matching index
        for (SampleMetadata existing : existingSamples) {
            if (existing.getPrimaryId().equalsIgnoreCase(sample.getPrimaryId())) {
                existingSamples.set(existingSamples.indexOf(existing), sample);
                foundMatching = Boolean.TRUE;
                break;
            }
        }
        // if matching sample not found then append to list and return
        if (!foundMatching) {
            existingSamples.add(sample);
        }
        return existingSamples;
    }

    private Map<String, List<SampleMetadata>> getPatientSamplesMap(List<SmileSample> samples)
            throws Exception {
        Map<String, List<SampleMetadata>> patientSamplesMap = new HashMap<>();
        for (SmileSample sample : samples) {
            SampleMetadata sm = sample.getLatestSampleMetadata();
            String cmoPatientId = sm.getCmoPatientId();
            // get or request existing patient samples and update patient sample mapping
            if (!patientSamplesMap.containsKey(cmoPatientId)
                    && !StringUtils.isBlank(cmoPatientId)) {
                List<SampleMetadata> ptSamples = getExistingPatientSamples(
                        cmoPatientId);
                patientSamplesMap.put(cmoPatientId,
                        new ArrayList<>(ptSamples));
            }
        }
        return patientSamplesMap;
    }

    private List<SampleMetadata> getExistingPatientSamples(String cmoPatientId) throws Exception {
        List<SmileSample> ptSamples = sampleService.getSamplesByCmoPatientId(cmoPatientId);
        List<SampleMetadata> smList = new ArrayList<>();
        for (SmileSample sample : ptSamples) {
            smList.add(sample.getLatestSampleMetadata());
        }
        return smList;
    }

}
