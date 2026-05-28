package org.mskcc.smile.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbGapJson implements Serializable {
    @JsonProperty("primaryId")
    private String primaryId;
    @JsonProperty("dbGapStudy")
    private String dbGapStudy;
    @JsonProperty("irbConsentProtocol")
    private String irbConsentProtocol;
    @JsonProperty("collectionStudy")
    private String collectionStudy;
    @JsonProperty("dateOfConsent")
    private String dateOfConsent;
    @JsonProperty("genomicResearchUseStudy")
    private String genomicResearchUseStudy;
    @JsonProperty("consentVersion")
    private String consentVersion;

    public DbGapJson() {}

    /**
     * DbGapJson constructor.
     * @param primaryId
     * @param dbGapStudy
     * @param irbConsentProtocol
     */
    public DbGapJson(String primaryId, String dbGapStudy, String irbConsentProtocol) {
        this.primaryId = primaryId;
        this.dbGapStudy = dbGapStudy;
        this.irbConsentProtocol = irbConsentProtocol;
    }

    public String getPrimaryId() {
        return primaryId;
    }

    public void setPrimaryId(String primaryId) {
        this.primaryId = primaryId;
    }

    public String getDbGapStudy() {
        return dbGapStudy;
    }

    public void setDbGapStudy(String dbGapStudy) {
        this.dbGapStudy = dbGapStudy;
    }

    public String getIrbConsentProtocol() {
        return irbConsentProtocol;
    }

    public void setIrbConsentProtocol(String irbConsentProtocol) {
        this.irbConsentProtocol = irbConsentProtocol;
    }

    public String getCollectionStudy() {
        return collectionStudy;
    }

    public void setCollectionStudy(String collectionStudy) {
        this.collectionStudy = collectionStudy;
    }

    public String getDateOfConsent() {
        return dateOfConsent;
    }

    public void setDateOfConsent(String dateOfConsent) {
        this.dateOfConsent = dateOfConsent;
    }

    public String getGenomicResearchUseStudy() {
        return genomicResearchUseStudy;
    }

    public void setGenomicResearchUseStudy(String genomicResearchUseStudy) {
        this.genomicResearchUseStudy = genomicResearchUseStudy;
    }

    public String getConsentVersion() {
        return consentVersion;
    }

    public void setConsentVersion(String consentVersion) {
        this.consentVersion = consentVersion;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
