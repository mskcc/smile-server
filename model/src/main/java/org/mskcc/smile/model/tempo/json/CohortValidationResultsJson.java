package org.mskcc.smile.model.tempo.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author aochoa
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CohortValidationResultsJson {
    @JsonProperty("cohortId")
    private String cohortId;
    @JsonProperty("jsonSchemaValidated")
    private Boolean jsonSchemaValidated;
    @JsonProperty("passesAllChecks")
    private Boolean passesAllChecks;
    @JsonProperty("endUsers")
    private List<String> endUsers;
    @JsonProperty("pmUsers")
    private List<String> pmUsers;
    @JsonProperty("samples")
    private List<Map<String, String>> samples;

    public CohortValidationResultsJson() {}

    public String getCohortId() {
        return cohortId;
    }

    public void setCohortId(String cohortId) {
        this.cohortId = cohortId;
    }

    public Boolean getJsonSchemaValidated() {
        return jsonSchemaValidated;
    }

    public void setJsonSchemaValidated(Boolean jsonSchemaValidated) {
        this.jsonSchemaValidated = jsonSchemaValidated;
    }

    public Boolean getPassesAllChecks() {
        return passesAllChecks;
    }

    public void setPassesAllChecks(Boolean passesAllChecks) {
        this.passesAllChecks = passesAllChecks;
    }

    public List<String> getEndUsers() {
        return endUsers;
    }

    public void setEndUsers(List<String> endUsers) {
        this.endUsers = endUsers;
    }

    public List<String> getPmUsers() {
        return pmUsers;
    }

    public void setPmUsers(List<String> pmUsers) {
        this.pmUsers = pmUsers;
    }

    public List<Map<String, String>> getSamples() {
        return samples;
    }

    public void setSamples(List<Map<String, String>> samples) {
        this.samples = samples;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
