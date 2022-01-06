package org.mskcc.cmo.metadb.model.dmp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author divyamadala
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"dt_alys_end_time", "dt_dms_start_time",
    "mrev_comments", "mrev_status_name", "tmb_cohort", "tmb_tt_cohort"})
public class DmpSampleMetadata {
    @JsonProperty("alys2sample_id")
    private Integer alys2sampleId;
    @JsonProperty("cbx_patient_id")
    private Integer cbxPatientId;
    @JsonProperty("cbx_sample_id")
    private Integer cbxSampleId;
    @JsonProperty("date_tumor_sequencing")
    private String dateTumorSequencing;
    @JsonProperty("linked_mskimpact_case")
    private String linkedMskimpactCase;
    @JsonProperty("dmp_alys_task_id")
    private Integer dmpAlysTaskId;
    @JsonProperty("dmp_alys_task_name")
    private String dmpAlysTaskName;
    @JsonProperty("dmp_patient_id")
    private String dmpPatientId;
    @JsonProperty("dmp_sample_id")
    private String dmpSampleId;
    @JsonProperty("dmp_sample_so_id")
    private Integer dmpSampleSoId;
    @JsonProperty("gender")
    private Integer gender;
    @JsonProperty("gene-panel")
    private String genePanel;
    @JsonProperty("is_metastasis")
    private Integer isMetastasis;
    @JsonProperty("legacy_patient_id")
    private String legacyPatientId;
    @JsonProperty("legacy_sample_id")
    private String legacySampleId;
    @JsonProperty("metastasis_site")
    private String metastasisSite;
    @JsonProperty("msi-comment")
    private String msiComment;
    @JsonProperty("msi-score")
    private String msiScore;
    @JsonProperty("msi-type")
    private String msiType;
    @JsonProperty("outside_institute")
    private String outsideInstitute;
    @JsonProperty("primary_site")
    private String primarySite;
    @JsonProperty("retrieve_status")
    private Integer retrieveStatus;
    @JsonProperty("sample_coverage")
    private Integer sampleCoverage;
    @JsonProperty("so_comments")
    private String soComments;
    @JsonProperty("so_status_name")
    private String soStatusName;
    @JsonProperty("somatic_status")
    private String somaticStatus;
    @JsonProperty("tmb_cohort_percentile")
    private Double tmbCohortPercentile;
    @JsonProperty("tmb_score")
    private Double tmbScore;
    @JsonProperty("tmb_tt_percentile")
    private Double tmbTtPercentile;
    @JsonProperty("tumor_purity")
    private String tumorPurity;
    @JsonProperty("tumor_type_code")
    private String tumorTypeCode;
    @JsonProperty("tumor_type_name")
    private String tumorTypeName;
    @JsonProperty("consent-parta")
    private String consentPartA;
    @JsonProperty("consent-partc")
    private String consentPartC;
    @JsonProperty("slide-viewer-id")
    private String wholeSlideViewerId;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public DmpSampleMetadata() {}

    /**
     * DmpSampleMetadata constructor.
     * @param alys2sampleId
     * @param cbxPatientId
     * @param cbxSampleId
     * @param dateTumorSequencing
     * @param linkedMskimpactCase
     * @param dmpAlysTaskId
     * @param dmpAlysTaskName
     * @param dmpPatientId
     * @param dmpSampleId
     * @param dmpSampleSoId
     * @param gender
     * @param genePanel
     * @param isMetastasis
     * @param legacyPatientId
     * @param legacySampleId
     * @param metastasisSite
     * @param msiComment
     * @param msiScore
     * @param msiType
     * @param outsideInstitute
     * @param primarySite
     * @param retrieveStatus
     * @param sampleCoverage
     * @param soComments
     * @param soStatusName
     * @param somaticStatus
     * @param tmbCohortPercentile
     * @param tmbScore
     * @param tmbTtPercentile
     * @param tumorPurity
     * @param tumorTypeCode
     * @param tumorTypeName
     * @param consentPartA
     * @param consentPartC
     * @param wholeSlideViewerId
     */
    public DmpSampleMetadata(Integer alys2sampleId, Integer cbxPatientId,
            Integer cbxSampleId, String dateTumorSequencing, String linkedMskimpactCase,
            Integer dmpAlysTaskId, String dmpAlysTaskName, String dmpPatientId,
            String dmpSampleId, Integer dmpSampleSoId, Integer gender,
            String genePanel, Integer isMetastasis, String legacyPatientId,
            String legacySampleId, String metastasisSite,
            String msiComment, String msiScore, String msiType,
            String outsideInstitute, String primarySite, Integer retrieveStatus,
            Integer sampleCoverage, String soComments, String soStatusName,
            String somaticStatus,Double tmbCohortPercentile,
            Double tmbScore, Double tmbTtPercentile, String tumorPurity,
            String tumorTypeCode, String tumorTypeName,
            String consentPartA, String consentPartC, String wholeSlideViewerId) {
        this.alys2sampleId = alys2sampleId;
        this.cbxPatientId = cbxPatientId;
        this.cbxSampleId = cbxSampleId;
        this.dateTumorSequencing = dateTumorSequencing;
        this.dmpAlysTaskId = dmpAlysTaskId;
        this.dmpAlysTaskName = dmpAlysTaskName;
        this.dmpPatientId = dmpPatientId;
        this.dmpSampleId = dmpSampleId;
        this.dmpSampleSoId = dmpSampleSoId;
        this.gender = gender;
        this.genePanel = genePanel;
        this.isMetastasis = isMetastasis;
        this.legacyPatientId = legacyPatientId;
        this.legacySampleId = legacySampleId;
        this.metastasisSite = metastasisSite;
        this.msiComment = msiComment;
        this.msiScore = msiScore;
        this.msiType = msiType;
        this.outsideInstitute = outsideInstitute;
        this.primarySite = primarySite;
        this.retrieveStatus = retrieveStatus;
        this.sampleCoverage = sampleCoverage;
        this.soComments = soComments;
        this.soStatusName = soStatusName;
        this.somaticStatus = somaticStatus;
        this.tmbCohortPercentile = tmbCohortPercentile;
        this.tmbScore = tmbScore;
        this.tmbTtPercentile = tmbTtPercentile;
        this.tumorPurity = tumorPurity;
        this.tumorTypeCode = tumorTypeCode;
        this.tumorTypeName = tumorTypeName;
        this.linkedMskimpactCase = linkedMskimpactCase;
        this.consentPartA = consentPartA;
        this.consentPartC = consentPartC;
        this.wholeSlideViewerId = wholeSlideViewerId;
    }

    public String getSomaticStatus() {
        return somaticStatus;
    }

    public void setSomatic_status(String somaticStatus) {
        this.somaticStatus = somaticStatus;
    }

    public Integer getAlys2sampleId() {
        return alys2sampleId;
    }

    public void setAlys2sampleId(Integer alys2sampleId) {
        this.alys2sampleId = alys2sampleId;
    }

    public Integer getCbxPatientId() {
        return cbxPatientId;
    }

    public void setCbxPatientId(Integer cbxPatientId) {
        this.cbxPatientId = cbxPatientId;
    }

    public Integer getCbxSampleId() {
        return cbxSampleId;
    }

    public void setCbxSampleId(Integer cbxSampleId) {
        this.cbxSampleId = cbxSampleId;
    }

    public String getDateTumorSequencing() {
        return dateTumorSequencing;
    }

    public void setDateTumorSequencing(String dateTumorSequencing) {
        this.dateTumorSequencing = dateTumorSequencing;
    }

    public Integer getDmpAlysTaskId() {
        return dmpAlysTaskId;
    }

    public String getLinkedMskimpactCase() {
        return linkedMskimpactCase;
    }

    public void setLinkedMskimpactCase(String linkedMskimpactCase) {
        this.linkedMskimpactCase = linkedMskimpactCase;
    }

    public void setDmpAlysTaskId(Integer dmpAlysTaskId) {
        this.dmpAlysTaskId = dmpAlysTaskId;
    }

    public String getDmpAlysTaskName() {
        return dmpAlysTaskName;
    }

    public void setDmpAlysTaskName(String dmpAlysTaskName) {
        this.dmpAlysTaskName = dmpAlysTaskName;
    }

    public String getDmpPatientId() {
        return dmpPatientId;
    }

    public void setDmpPatientId(String dmpPatientId) {
        this.dmpPatientId = dmpPatientId;
    }

    public String getDmpSampleId() {
        return dmpSampleId;
    }

    public void setDmpSampleId(String dmpSampleId) {
        this.dmpSampleId = dmpSampleId;
    }

    public Integer getDmpSampleSoId() {
        return dmpSampleSoId;
    }

    public void setDmpSampleSoId(Integer dmpSampleSoId) {
        this.dmpSampleSoId = dmpSampleSoId;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getGenePanel() {
        return genePanel;
    }

    public void setGenePanel(String genePanel) {
        this.genePanel = genePanel;
    }

    public Integer getIsMetastasis() {
        return isMetastasis;
    }

    public void setIsMetastasis(Integer isMetastasis) {
        this.isMetastasis = isMetastasis;
    }

    public String getLegacyPatientId() {
        return legacyPatientId;
    }

    public void setLegacyPatientId(String legacyPatientId) {
        this.legacyPatientId = legacyPatientId;
    }

    public String getLegacySampleId() {
        return legacySampleId;
    }

    public void setLegacySampleId(String legacySampleId) {
        this.legacySampleId = legacySampleId;
    }

    public String getMetastasisSite() {
        return metastasisSite;
    }

    public void setMetastasisSite(String metastasisSite) {
        this.metastasisSite = metastasisSite;
    }

    public String getMsiComment() {
        return msiComment;
    }

    public void setMsiComment(String msiComment) {
        this.msiComment = msiComment;
    }

    public String getMsiScore() {
        return msiScore;
    }

    public void setMsiScore(String msiScore) {
        this.msiScore = msiScore;
    }

    public String getMsiType() {
        return msiType;
    }

    public void setMsiType(String msiType) {
        this.msiType = msiType;
    }

    public String getOutsideInstitute() {
        return outsideInstitute;
    }

    public void setOutsideInstitute(String outsideInstitute) {
        this.outsideInstitute = outsideInstitute;
    }

    public String getPrimarySite() {
        return primarySite;
    }

    public void setPrimarySite(String primarySite) {
        this.primarySite = primarySite;
    }

    public Integer getRetrieveStatus() {
        return retrieveStatus;
    }

    public void setRetrieveStatus(Integer retrieveStatus) {
        this.retrieveStatus = retrieveStatus;
    }

    public Integer getSampleCoverage() {
        return sampleCoverage;
    }

    public void setSampleCoverage(Integer sampleCoverage) {
        this.sampleCoverage = sampleCoverage;
    }

    public String getSoComments() {
        return soComments;
    }

    public void setSoComments(String soComments) {
        this.soComments = soComments;
    }

    public String getSoStatusName() {
        return soStatusName;
    }

    public void setSoStatusName(String soStatusName) {
        this.soStatusName = soStatusName;
    }

    public Double getTmbCohortPercentile() {
        return tmbCohortPercentile;
    }

    public void setTmbCohortPercentile(Double tmbCohortPercentile) {
        this.tmbCohortPercentile = tmbCohortPercentile;
    }

    public Double getTmbScore() {
        return tmbScore;
    }

    public void setTmbScore(Double tmbScore) {
        this.tmbScore = tmbScore;
    }

    public Double getTmbTtPercentile() {
        return tmbTtPercentile;
    }

    public void setTmbTtPercentile(Double tmbTtPercentile) {
        this.tmbTtPercentile = tmbTtPercentile;
    }

    public String getTumorPurity() {
        return tumorPurity;
    }

    public void setTumorPurity(String tumorPurity) {
        this.tumorPurity = tumorPurity;
    }

    public String getTumorTypeCode() {
        return tumorTypeCode;
    }

    public void setTumorTypeCode(String tumorTypeCode) {
        this.tumorTypeCode = tumorTypeCode;
    }

    public String getTumorTypeName() {
        return tumorTypeName;
    }

    public void setTumorTypeName(String tumorTypeName) {
        this.tumorTypeName = tumorTypeName;
    }

    public String getConsentPartA() {
        return consentPartA;
    }

    public void setConsentPartA(String consentPartA) {
        this.consentPartA = consentPartA;
    }

    public String getConsentPartC() {
        return consentPartC;
    }

    public void setConsentPartC(String consentPartC) {
        this.consentPartC = consentPartC;
    }

    public String getWholeSlideViewerId() {
        return wholeSlideViewerId;
    }

    public void setWholeSlideViewerId(String wholeSlideViewerId) {
        this.wholeSlideViewerId = wholeSlideViewerId;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
