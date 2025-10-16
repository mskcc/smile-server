package org.mskcc.smile.model.tempo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.smile.model.converter.ArrayStringConverter;
import org.mskcc.smile.model.tempo.json.CohortCompleteJson;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.typeconversion.Convert;

/**
 *
 * @author ochoaa
 */
@NodeEntity
public class CohortComplete implements Serializable, Comparable<CohortComplete> {
    @Id @GeneratedValue
    @JsonIgnore
    private Long id;
    private String date;
    private String status;
    private String type;
    @Convert(ArrayStringConverter.class)
    private List<String> endUsers;
    @Convert(ArrayStringConverter.class)
    private List<String> pmUsers;
    private String projectTitle;
    private String projectSubtitle;
    private String pipelineVersion;

    public CohortComplete() {}

    /**
     * Basic constructor from CohortCompleteJson.
     * @param ccJson
     */
    public CohortComplete(CohortCompleteJson ccJson) {
        this.date = ccJson.getDate();
        this.status = ccJson.getStatus();
        this.type = ccJson.getType();
        this.endUsers = ccJson.getEndUsers();
        this.pmUsers = ccJson.getPmUsers();
        this.projectTitle = ccJson.getProjectTitle();
        this.projectSubtitle = ccJson.getProjectSubtitle();
        this.pipelineVersion = ccJson.getPipelineVersion();
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns list of end users.
     * @return
     */
    public List<String> getEndUsers() {
        if (endUsers == null) {
            this.endUsers = new ArrayList<>();
        }
        Collections.sort(endUsers);
        return endUsers;
    }

    public void setEndUsers(List<String> endUsers) {
        this.endUsers = endUsers;
    }

    /**
     * Returns list of PM users.
     * @return
     */
    public List<String> getPmUsers() {
        if (pmUsers == null) {
            this.pmUsers = new ArrayList<>();
        }
        Collections.sort(pmUsers);
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

    public String getPipelineVersion() {
        return pipelineVersion;
    }

    public void setPipelineVersion(String pipelineVersion) {
        this.pipelineVersion = pipelineVersion;
    }

    /**
     * Override to enable Collections.sorting
     * @param cohortComplete
     * @return
     */
    @Override
    public int compareTo(CohortComplete cohortComplete) {
        if (date == null || cohortComplete.getDate() == null) {
            return 0;
        }
        return date.compareTo(cohortComplete.getDate());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
