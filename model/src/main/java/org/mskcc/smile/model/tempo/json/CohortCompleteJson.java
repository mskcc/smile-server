package org.mskcc.smile.model.tempo.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author ochoaa
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CohortCompleteJson implements Serializable {
    @JsonProperty("cohortId")
    private String cohortId;
    @JsonProperty("date")
    private String date;
    @JsonProperty("type")
    private String type;
    @JsonProperty("endUsers")
    private List<String> endUsers;
    @JsonProperty("pmUsers")
    private List<String> pmUsers;
    @JsonProperty("projectTitle")
    private String projectTitle;
    @JsonProperty("projectSubtitle")
    private String projectSubtitle;
    @JsonProperty("samples")
    private List<Map<String, String>> tumorNormalPairs;
    @JsonProperty("status")
    private String status;
    @JsonProperty("pipelineVersion")
    private String pipelineVersion;

    public CohortCompleteJson() {}

    public String getCohortId() {
        return cohortId;
    }

    public void setCohortId(String cohortId) {
        this.cohortId = cohortId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public String getProjectSubtitle() {
        return projectSubtitle;
    }

    public void setProjectSubtitle(String projectSubtitle) {
        this.projectSubtitle = projectSubtitle;
    }

    public List<Map<String, String>> getTumorNormalPairs() {
        return tumorNormalPairs;
    }

    public void setTumorNormalPairs(List<Map<String, String>> tumorNormalPairs) {
        this.tumorNormalPairs = tumorNormalPairs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPipelineVersion() {
        return pipelineVersion;
    }

    public void setPipelineVersion(String pipelineVersion) {
        this.pipelineVersion = pipelineVersion;
    }

    /**
     * Returns a set of tumor and normal primary ids.
     * @return
     */
    public Set<String> getTumorNormalPairsAsSet() {
        Set<String> samplePrimaryIds = new HashSet<>();
        tumorNormalPairs.forEach((pairs) -> {
            pairs.entrySet().forEach((entry) -> {
                samplePrimaryIds.add(entry.getValue());
            });
        });
        return samplePrimaryIds;
    }

    /**
     * Returns a set of tumor primary ids.
     * @return
     */
    public Set<String> getTumorPrimaryIdsAsSet() {
        Set<String> primaryIds = new HashSet<>();
        if (tumorNormalPairs != null) {
            for (Map<String, String> pair : tumorNormalPairs) {
                String primaryId = pair.get("primaryId");
                if (!StringUtils.isBlank(primaryId)) {
                    primaryIds.add(primaryId);
                }
            }
        }
        return primaryIds;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
