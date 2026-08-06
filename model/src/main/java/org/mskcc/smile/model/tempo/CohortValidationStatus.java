package org.mskcc.smile.model.tempo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.smile.model.converter.ArrayMapConverter;
import org.mskcc.smile.model.tempo.json.CohortValidationResultsJson;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.typeconversion.Convert;

/**
 *
 * @author aochoa
 */
@NodeEntity(label = "CohortValidationStatus")
public class CohortValidationStatus {
    @Id @GeneratedValue
    @JsonIgnore
    private Long id;
    private Boolean jsonSchemaValidated;
    private Boolean passesAllChecks;
    private List<String> invalidEndUsers;
    private List<String> invalidPmUsers;
    @Convert(ArrayMapConverter.class)
    private List<Map<String, String>> invalidTempoSamples;

    public CohortValidationStatus() {}

    /**
     * Constructor with input validation results json.
     * @param vrJson
     */
    public CohortValidationStatus(CohortValidationResultsJson vrJson) {
        this.jsonSchemaValidated = vrJson.getJsonSchemaValidated();
        this.passesAllChecks = vrJson.getPassesAllChecks();
        this.invalidEndUsers = vrJson.getEndUsers();
        this.invalidPmUsers = vrJson.getPmUsers();
        this.invalidTempoSamples = vrJson.getSamples();
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

    public List<String> getInvalidEndUsers() {
        return invalidEndUsers;
    }

    public void setInvalidEndUsers(List<String> invalidEndUsers) {
        this.invalidEndUsers = invalidEndUsers;
    }

    public List<String> getInvalidPmUsers() {
        return invalidPmUsers;
    }

    public void setInvalidPmUsers(List<String> invalidPmUsers) {
        this.invalidPmUsers = invalidPmUsers;
    }

    public List<Map<String, String>> getInvalidTempoSamples() {
        return invalidTempoSamples;
    }

    public void setInvalidTempoSamples(List<Map<String, String>> invalidTempoSamples) {
        this.invalidTempoSamples = invalidTempoSamples;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
