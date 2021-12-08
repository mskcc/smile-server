package org.mskcc.cmo.metadb.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.cmo.metadb.model.converter.LibrariesStringConverter;
import org.mskcc.cmo.metadb.model.converter.MapStringConverter;
import org.mskcc.cmo.metadb.model.converter.QcReportsStringConverter;
import org.mskcc.cmo.metadb.model.dmp.DmpSampleMetadata;
import org.mskcc.cmo.metadb.model.igo.IgoSampleManifest;
import org.mskcc.cmo.metadb.model.igo.Library;
import org.mskcc.cmo.metadb.model.igo.QcReport;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.typeconversion.Convert;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SampleMetadata implements Serializable, Comparable<SampleMetadata> {
    @Id @GeneratedValue
    @JsonIgnore
    private Long id;
    private String primaryId;
    private String cmoPatientId;
    private String cmoSampleName;
    private String sampleName;
    private String importDate;
    private String cmoInfoIgoId;
    private String sampleType;
    private String oncotreeCode;
    private String collectionYear;
    private String tubeId;
    private String cfDNA2dBarcode;
    private String investigatorSampleId;
    private String species;
    private String sex;
    private String tumorOrNormal;
    private String preservation;
    private String tumorType;
    private String sampleClass;
    private String sampleOrigin;
    private String tissueLocation;
    // incl in igo sample metadata, not igo sample manifest
    // recipe --> genePanel for universal schema
    private String genePanel;
    private String baitSet;
    private String fastqPath;
    // incl in igo sample metadata but not igo sample manifest
    private String principalInvestigator;
    private String igoRequestId;
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
     * SampleMetadata constructor from igoSampleManifest.
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
        this.sampleType = igoSampleManifest.getCmoSampleClass();
        this.oncotreeCode = igoSampleManifest.getOncoTreeCode();
        this.collectionYear = igoSampleManifest.getCollectionYear();
        this.tubeId = igoSampleManifest.getTubeId();
        this.cfDNA2dBarcode = igoSampleManifest.getCfDNA2dBarcode();
        this.investigatorSampleId = igoSampleManifest.getInvestigatorSampleId();
        this.species = igoSampleManifest.getSpecies();
        this.sex = igoSampleManifest.getSex();
        this.tumorOrNormal = igoSampleManifest.getTumorOrNormal();
        this.preservation = igoSampleManifest.getPreservation();
        this.tumorType = igoSampleManifest.getTumorOrNormal();
        this.sampleClass = igoSampleManifest.getSpecimenType();
        this.sampleOrigin = igoSampleManifest.getSampleOrigin();
        this.tissueLocation = igoSampleManifest.getTissueLocation();
        // TODO need to add recipe to igo sample manifest as well
        // since baitSet != recipe (sometimes)
        //this.genePanel = igoSampleManifest.getRecipe();
        this.baitSet = igoSampleManifest.getBaitSet();
        // TODO need to add fastq path to igo sample manifest as well
        // it is only incl as part of igo sample metadata
        //this.fastqPath = igoSampleManifest.getFastqPath();

        this.qcReports =  igoSampleManifest.getQcReports();
        this.libraries = igoSampleManifest.getLibraries();
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
        this.genePanel = dmpSampleMetadata.getGenePanel();
        this.baitSet = dmpSampleMetadata.getGenePanel();
        this.oncotreeCode = dmpSampleMetadata.getTumorTypeCode();
        // specimen type will be renamed to sample class
        //P-\d+-(T|N)\d+-(IH|TB|TS|AH|AS|IM|XS)\d+
        this.sampleClass = dmpSampleMetadata.getDmpSampleId().matches("[ACCESS REGEX PATTERN]")
                ? "cfDNA" : "Tumor";
        this.sex = (dmpSampleMetadata.getGender().equals(0)) ? "Male" : "Female";
        this.tissueLocation = dmpSampleMetadata.getPrimarySite();
        this.tumorOrNormal = dmpSampleMetadata.getDmpSampleId().matches("[NORMAL ID SAMPLE PATTERN]")
                ? "Normal" : "Tumor";
        additionalProperties.put("msi-comment", dmpSampleMetadata.getMsiComment());
        additionalProperties.put("msi-score", dmpSampleMetadata.getMsiScore());
        additionalProperties.put("msi-type", dmpSampleMetadata.getMsiType());
        additionalProperties.put("date_tumor_sequencing", dmpSampleMetadata.getDateTumorSequencing());
        additionalProperties.put("metastasis_site", dmpSampleMetadata.getMetastasisSite());
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

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getOncotreeCode() {
        return oncotreeCode;
    }

    public void setOncotreeCode(String oncoTreeCode) {
        this.oncotreeCode = oncoTreeCode;
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

    public String getSampleClass() {
        return sampleClass;
    }

    public void setSampleClass(String sampleClass) {
        this.sampleClass = sampleClass;
    }

    public String getSampleOrigin() {
        return sampleOrigin;
    }

    public void setSampleOrigin(String sampleOrigin) {
        this.sampleOrigin = sampleOrigin;
    }

    public String getTissueLocation() {
        return tissueLocation;
    }

    public void setTissueLocation(String tissueLocation) {
        this.tissueLocation = tissueLocation;
    }

    public String getGenePanel() {
        return genePanel;
    }

    public void setGenePanel(String genePanel) {
        this.genePanel = genePanel;
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

    public String getIgoRequestId() {
        return igoRequestId;
    }

    public void setIgoRequestId(String igoRequestId) {
        this.igoRequestId = igoRequestId;
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
