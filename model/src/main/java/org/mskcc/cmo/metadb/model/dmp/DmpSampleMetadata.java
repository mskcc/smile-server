package org.mskcc.cmo.metadb.model.dmp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author ochoaa
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DmpSampleMetadata {
    @JsonProperty("alys2sample_id")
    private Integer alys2sampleId;
    @JsonProperty("cbx_patient_id")
    private Integer cbxPatientId;
    @JsonProperty("cbx_sample_id")
    private Integer cbxSampleId;
    @JsonProperty("date_tumor_sequencing")
    private String dateTumorSequencing;
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
    @JsonProperty("tumor_purity")
    private String tumorPurity;
    @JsonProperty("tumor_type_code")
    private String tumorTypeCode;
    @JsonProperty("tumor_type_name")
    private String tumorTypeName;
    @JsonProperty("consent-parta")
    private String consentParta;
    @JsonProperty("consent-partc")
    private String consentPartc;
    @JsonProperty("slide-viewer-id")
    private String slideViewerId;
    @JsonProperty("dt_alys_end_time")
    private String dtAlysEndTime;
    @JsonProperty("dt_dms_start_time")
    private String dtDmsStartTime;
    @JsonProperty("mrev_status_name")
    private String mrevStatusName;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public DmpSampleMetadata() {}

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

    public String getSomaticStatus() {
        return somaticStatus;
    }

    public void setSomaticStatus(String somaticStatus) {
        this.somaticStatus = somaticStatus;
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

    public String getConsentParta() {
        return consentParta;
    }

    public void setConsentParta(String consentParta) {
        this.consentParta = consentParta;
    }

    public String getConsentPartc() {
        return consentPartc;
    }

    public void setConsentPartc(String consentPartc) {
        this.consentPartc = consentPartc;
    }

    public String getSlideViewerId() {
        return slideViewerId;
    }

    public void setSlideViewerId(String slideViewerId) {
        this.slideViewerId = slideViewerId;
    }

    public String getDtAlysEndTime() {
        return dtAlysEndTime;
    }

    public void setDtAlysEndTime(String dtAlysEndTime) {
        this.dtAlysEndTime = dtAlysEndTime;
    }

    public String getDtDmsStartTime() {
        return dtDmsStartTime;
    }

    public void setDtDmsStartTime(String dtDmsStartTime) {
        this.dtDmsStartTime = dtDmsStartTime;
    }

    public String getMrevStatusName() {
        return mrevStatusName;
    }

    public void setMrevStatusName(String mrevStatusName) {
        this.mrevStatusName = mrevStatusName;
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
