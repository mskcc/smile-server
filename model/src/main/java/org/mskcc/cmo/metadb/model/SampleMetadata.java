package org.mskcc.cmo.metadb.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.cmo.metadb.model.converter.LibrariesStringConverter;
import org.mskcc.cmo.metadb.model.converter.MapStringConverter;
import org.mskcc.cmo.metadb.model.converter.QcReportsStringConverter;
import org.mskcc.cmo.metadb.model.dmp.DmpSampleMetadata;
import org.mskcc.cmo.metadb.model.igo.IgoLibrary;
import org.mskcc.cmo.metadb.model.igo.IgoQcReport;
import org.mskcc.cmo.metadb.model.igo.IgoSampleManifest;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.typeconversion.Convert;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SampleMetadata implements Serializable, Comparable<SampleMetadata> {
    private final ObjectMapper mapper = new ObjectMapper();

    @Id @GeneratedValue
    @JsonIgnore
    private Long id;
    private String primaryId;
    private String cmoPatientId;
    private String cmoSampleName;
    private String sampleName;
    private String importDate;
    private String cmoInfoIgoId;
    private String cmoSampleClass;
    private String oncoTreeCode;
    private String collectionYear;
    private String tubeId;
    private String cfDNA2dBarcode;
    // TODO: REMOVE (not using but part of IGO LIMS sample metadata, not manifest)
    private String mrn;
    private String investigatorSampleId;
    private String species;
    private String sex;
    private String tumorOrNormal;
    // TODO: RESOLVE (only part of IGO LIMS sample metadata, not manifest but we
    // need this field for generating CMO sample labels)
    private String sampleType;
    private String preservation;
    private String tumorType;
    // TODO: RESOLVE (only part of IGO LIMS sample metadata, not manifest but we
    // need this field for generating CMO sample labels)
    private String parentTumorType;
    private String specimenType;
    private String sampleOrigin;
    // TODO: RESOLVE (only part of IGO LIMS sample metadata, not manifest but we
    // need this field for generating CMO sample labels)
    private String tissueSource;
    private String tissueLocation;
    // TODO: RESOLVE (only part of IGO LIMS sample metadata, not manifest but we
    // need this field for generating CMO sample labels)
    private String recipe;
    private String baitSet;
    // TODO: RESOLVE (only part of IGO LIMS sample metadata, not manifest but we
    // need this field for generating CMO sample labels)
    private String fastqPath;
    // TODO: RESOLVE (only part of IGO LIMS sample metadata, not manifest but we
    // need this field for generating CMO sample labels)
    private String principalInvestigator;
    // TODO: RESOLVE (only part of IGO LIMS sample metadata, not manifest but we
    // need this field for generating CMO sample labels)
    private String ancestorSample;
    // TODO: RESOLVE (only part of IGO LIMS sample metadata, not manifest but we
    // need this field for generating CMO sample labels)
    private String sampleStatus;
    private String requestId;
    @Convert(QcReportsStringConverter.class)
    private List<QcReport> qcReports;
    @Convert(LibrariesStringConverter.class)
    private List<Library> libraries;
    @Convert(MapStringConverter.class)
    private Map<String, String> cmoSampleIdFields;
    @Convert(MapStringConverter.class)
    private Map<String, Object> additionalProperties  = new HashMap<String, Object>();

    public SampleMetadata() {}

    /**
     * SampleMetadata constrcutor from igoSampleManifest.
     *
     * <p>There are quite a few fields that we had taken from IGO LIMS SampleMetadata instead
     * of SampleManifest. `get-sample-manifests` returns SampleManifest type which does
     * not have all of the same data available but migrating towards using this datatype
     * instead might be something worth considering.
     *
     * <p>Most notably however we definitely need recipe in the SampleManifest and sample type.
     *
     * <p>Other thing to consider is whether to change the types of `qcReports`, `libraries`,
     * and `cmoSampleIdFields` to strings instead since that's how we are storing them
     * in the database anyway.
     * @param igoSampleManifest
     * @throws JsonProcessingException
     */
    public SampleMetadata(IgoSampleManifest igoSampleManifest) throws JsonProcessingException {
        this.primaryId = igoSampleManifest.getIgoId();
        this.cmoPatientId = igoSampleManifest.getCmoPatientId();
        this.cmoSampleName = igoSampleManifest.getCmoSampleName();
        this.sampleName = igoSampleManifest.getSampleName();
        this.cmoInfoIgoId = igoSampleManifest.getCmoInfoIgoId();
        this.cmoSampleClass = igoSampleManifest.getCmoSampleClass();
        this.oncoTreeCode = igoSampleManifest.getOncoTreeCode();
        this.collectionYear = igoSampleManifest.getCollectionYear();
        this.tubeId = igoSampleManifest.getTubeId();
        this.cfDNA2dBarcode = igoSampleManifest.getCfDNA2dBarcode();
        this.investigatorSampleId = igoSampleManifest.getInvestigatorSampleId();
        this.species = igoSampleManifest.getSpecies();
        this.sex = igoSampleManifest.getSex();
        this.tumorOrNormal = igoSampleManifest.getTumorOrNormal();
        //this.sampleType = igoSampleManifest.getSampleType();
        this.preservation = igoSampleManifest.getPreservation();
        this.tumorType = igoSampleManifest.getTumorOrNormal();
        //this.parentTumorType = igoSampleManifest.getParentTumorType();
        this.specimenType = igoSampleManifest.getSpecimenType();
        this.sampleOrigin = igoSampleManifest.getSampleOrigin();
        //this.tissueSource = igoSampleManifest.getTissueSource();
        this.tissueLocation = igoSampleManifest.getTissueLocation();
        //this.recipe = igoSampleManifest.getRecipe();
        this.baitSet = igoSampleManifest.getBaitSet();
        //this.fastqPath = igoSampleManifest.getFastqPath();
        //this.principalInvestigator = igoSampleManifest.getPrincipalInvestigator();
        //this.ancestorSample = igoSampleManifest.getAncestorSample();
        //this.sampleStatus = igoSampleManifest.getSampleStatus();

        // TODO: Leaving these lists here for now but might replace type of 'qcReports'
        // and 'libraries' and 'cmoSampleIdFields' to String
        this.qcReports = new ArrayList<>();
        for (IgoQcReport r : igoSampleManifest.getQcReports()) {
            qcReports.add(new QcReport(r));
        }
        this.libraries = new ArrayList<>();
        for (IgoLibrary l : igoSampleManifest.getLibraries()) {
            libraries.add(new Library(l));
        }
        this.cmoSampleIdFields = igoSampleManifest.getCmoSampleIdFields();
    }

    /**
     * SampleMetadata constructor from dmpSampleMetadata.
     * @param dmpSampleMetadata
     */
    public SampleMetadata(DmpSampleMetadata dmpSampleMetadata) {
        this.primaryId = dmpSampleMetadata.getDmpSampleId();
        // cmo patient id is resolved by crdb mapping table
        // this.cmoPatientId = dmpSampleMetadata.getCmoPatientId();
        this.sampleType = resolveSampleType(dmpSampleMetadata.getIsMetastasis());
        this.recipe = dmpSampleMetadata.getGenePanel();
        this.baitSet = dmpSampleMetadata.getGenePanel();
        this.oncoTreeCode = dmpSampleMetadata.getTumorTypeCode();
        // specimen type will be renamed to sample class
        this.specimenType = dmpSampleMetadata.getDmpSampleId().matches("[ACCESS REGEX PATTERN]")
                ? "cfDNA" : "Tumor";
        this.sex = (dmpSampleMetadata.getGender().equals(0)) ? "Male" : "Female";
        this.tissueLocation = dmpSampleMetadata.getPrimarySite();
        this.tumorOrNormal = dmpSampleMetadata.getDmpSampleId().matches("[NORMAL ID SAMPLE PATTERN]")
                ? "Normal" : "Tumor";
        additionalProperties.put("msi-comment", dmpSampleMetadata.getMsiComment());
        additionalProperties.put("msi-score", dmpSampleMetadata.getMsiScore());
        additionalProperties.put("msi-type", dmpSampleMetadata.getMsiType());
        additionalProperties.put("date_tumor_sequencing", dmpSampleMetadata.getDateTumorSequencing());
        additionalProperties.put("metstasis_site", dmpSampleMetadata.getMetastasisSite());
        additionalProperties.put("outside_institute", dmpSampleMetadata.getOutsideInstitute());
        additionalProperties.put("sample_coverage", dmpSampleMetadata.getSampleCoverage());
        additionalProperties.put("somatic_status", dmpSampleMetadata.getSomaticStatus());
        additionalProperties.put("tumor_purity", dmpSampleMetadata.getTumorPurity());
        additionalProperties.put("consent-parta", dmpSampleMetadata.getConsentParta());
        additionalProperties.put("consent-partc", dmpSampleMetadata.getConsentPartc());
        additionalProperties.put("tumor_type_name", dmpSampleMetadata.getTumorTypeName());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImportDate() {
        return importDate;
    }

    public void setImportDate(String importDate) {
        this.importDate = importDate;
    }

    public String getCmoInfoIgoId() {
        return cmoInfoIgoId;
    }

    public void setCmoInfoIgoId(String cmoInfoIgoId) {
        this.cmoInfoIgoId = cmoInfoIgoId;
    }

    public String getCmoSampleName() {
        return cmoSampleName;
    }

    public void setCmoSampleName(String cmoSampleName) {
        this.cmoSampleName = cmoSampleName;
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public String getCmoSampleClass() {
        return cmoSampleClass;
    }

    public void setCmoSampleClass(String cmoSampleClass) {
        this.cmoSampleClass = cmoSampleClass;
    }

    public String getOncoTreeCode() {
        return oncoTreeCode;
    }

    public void setOncoTreeCode(String oncoTreeCode) {
        this.oncoTreeCode = oncoTreeCode;
    }

    public String getCollectionYear() {
        return collectionYear;
    }

    public void setCollectionYear(String collectionYear) {
        this.collectionYear = collectionYear;
    }

    public String getTubeId() {
        return tubeId;
    }

    public void setTubeId(String tubeId) {
        this.tubeId = tubeId;
    }

    public String getCfDNA2dBarcode() {
        return cfDNA2dBarcode;
    }

    public void setCfDNA2dBarcode(String cfDNA2dBarcode) {
        this.cfDNA2dBarcode = cfDNA2dBarcode;
    }

    public List<QcReport> getQcReports() {
        return qcReports;
    }

    public void setQcReports(List<QcReport> qcReports) {
        this.qcReports = qcReports;
    }

    public List<Library> getLibraries() {
        return libraries;
    }

    public void setLibraries(List<Library> libraries) {
        this.libraries = libraries;
    }

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    public String getCmoPatientId() {
        return cmoPatientId;
    }

    public void setCmoPatientId(String cmoPatientId) {
        this.cmoPatientId = cmoPatientId;
    }

    public String getPrimaryId() {
        return primaryId;
    }

    public void setPrimaryId(String primaryId) {
        this.primaryId = primaryId;
    }

    public String getInvestigatorSampleId() {
        return investigatorSampleId;
    }

    public void setInvestigatorSampleId(String investigatorSampleId) {
        this.investigatorSampleId = investigatorSampleId;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getTumorOrNormal() {
        return tumorOrNormal;
    }

    public void setTumorOrNormal(String tumorOrNormal) {
        this.tumorOrNormal = tumorOrNormal;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getPreservation() {
        return preservation;
    }

    public void setPreservation(String preservation) {
        this.preservation = preservation;
    }

    public String getTumorType() {
        return tumorType;
    }

    public void setTumorType(String tumorType) {
        this.tumorType = tumorType;
    }

    public String getParentTumorType() {
        return parentTumorType;
    }

    public void setParentTumorType(String parentTumorType) {
        this.parentTumorType = parentTumorType;
    }

    public String getSpecimenType() {
        return specimenType;
    }

    public void setSpecimenType(String specimenType) {
        this.specimenType = specimenType;
    }

    public String getSampleOrigin() {
        return sampleOrigin;
    }

    public void setSampleOrigin(String sampleOrigin) {
        this.sampleOrigin = sampleOrigin;
    }

    public String getTissueSource() {
        return tissueSource;
    }

    public void setTissueSource(String tissueSource) {
        this.tissueSource = tissueSource;
    }

    public String getTissueLocation() {
        return tissueLocation;
    }

    public void setTissueLocation(String tissueLocation) {
        this.tissueLocation = tissueLocation;
    }

    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    public String getBaitSet() {
        return baitSet;
    }

    public void setBaitSet(String baitSet) {
        this.baitSet = baitSet;
    }

    public String getFastqPath() {
        return fastqPath;
    }

    public void setFastqPath(String fastqPath) {
        this.fastqPath = fastqPath;
    }

    public String getPrincipalInvestigator() {
        return principalInvestigator;
    }

    public void setPrincipalInvestigator(String principalInvestigator) {
        this.principalInvestigator = principalInvestigator;
    }

    public String getAncestorSample() {
        return ancestorSample;
    }

    public void setAncestorSample(String ancestorSample) {
        this.ancestorSample = ancestorSample;
    }

    public String getSampleStatus() {
        return sampleStatus;
    }

    public void setSampleStatus(String sampleStatus) {
        this.sampleStatus = sampleStatus;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Map<String, String> getCmoSampleIdFields() {
        return cmoSampleIdFields;
    }

    public void setCmoSampleIdFields(Map<String, String> cmoSampleIdFields) {
        this.cmoSampleIdFields = cmoSampleIdFields;
    }

    @Override
    public int compareTo(SampleMetadata sampleMetadata) {
        if (getImportDate() == null || sampleMetadata.getImportDate() == null) {
            return 0;
        }
        return getImportDate().compareTo(sampleMetadata.getImportDate());
    }

    /**
     * Resolves the sample type given a dmp metastasis code.
     * @param isMetastasis
     * @return String
     */
    public String resolveSampleType(Integer isMetastasis) {
        if (isMetastasis != null) {
            switch (isMetastasis) {
                case 0:
                    return "Primary";
                case 1:
                    return "Metastasis";
                case 2:
                    return "Local Recurrence";
                case 127:
                    return "Unknown";
                default:
                    return "";
            }
        }
        return "";
    }
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
