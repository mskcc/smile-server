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
    @JsonProperty("instrumentModel")
    private String instrumentModel;
    @JsonProperty("platform")
    private String platform;

    public DbGapJson() {}

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

    public String getInstrumentModel() {
        return instrumentModel;
    }

    public void setInstrumentModel(String instrumentModel) {
        this.instrumentModel = instrumentModel;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
