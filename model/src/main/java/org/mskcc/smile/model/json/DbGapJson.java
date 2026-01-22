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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
