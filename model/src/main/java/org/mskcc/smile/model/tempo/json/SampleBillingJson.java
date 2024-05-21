package org.mskcc.smile.model.tempo.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author ochoaa
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SampleBillingJson implements Serializable {
    @JsonProperty("primaryId")
    private String primaryId;
    @JsonProperty("billed")
    private Boolean billed;
    @JsonProperty("billedBy")
    private String billedBy;
    @JsonProperty("costCenter")
    private String costCenter;
    @JsonProperty("custodianInformation")
    private String custodianInformation;
    @JsonProperty("accessLevel")
    private String accessLevel;

    public SampleBillingJson() {}

    public String getPrimaryId() {
        return primaryId;
    }

    public void setPrimaryId(String primaryId) {
        this.primaryId = primaryId;
    }

    public Boolean getBilled() {
        return billed;
    }

    public void setBilled(Boolean billed) {
        this.billed = billed;
    }

    public String getBilledBy() {
        return billedBy;
    }

    public void setBilledBy(String billedBy) {
        this.billedBy = billedBy;
    }

    public String getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(String costCenter) {
        this.costCenter = costCenter;
    }

    public String getCustodianInformation() {
        return custodianInformation;
    }

    public void setCustodianInformation(String custodianInformation) {
        this.custodianInformation = custodianInformation;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
