package org.mskcc.smile.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NodeEntity(label = "Status")
public class Status implements Serializable {
    
    @Id @GeneratedValue
    @JsonIgnore
    private Long id;
    private Boolean validationStatus;
    private String validationReport;

    public Status() {
        this.validationStatus = Boolean.TRUE;
        this.validationReport = "";
    }

    public Status(Boolean validationStatus,
            String validationReport) {
        this.validationStatus = validationStatus;
        this.validationReport = validationReport;
    }
    
    public Status(String statusJson) throws JsonMappingException, JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        Map<String,String> statusJsonMap = mapper.readValue(statusJson, Map.class);
        this.validationStatus = Boolean.valueOf(statusJsonMap.get("validationStatus"));
        this.validationReport = statusJsonMap.get("validationReport");
    }

    public Boolean getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(Boolean validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getValidationReport() {
        return validationReport;
    }

    public void setValidationReport(String validationReport) {
        this.validationReport = validationReport;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
