package org.mskcc.smile.model.igo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author ochoaa
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IgoRequest implements Serializable {
    private String requestId;
    private String projectId;
    private String dataAccessEmails;
    private String dataAnalystEmail;
    private String dataAnalystName;
    private String investigatorEmail;
    private String investigatorName;
    private String labHeadEmail;
    private String labHeadName;
    private String libraryType;
    private String otherContactEmails;
    private String piEmail;
    private String projectManagerName;
    private String qcAccessEmails;
    private String recipe;
    private String strand;
    private Long deliveryDate;
    private Boolean bicAnalysis;
    private Boolean isCmoRequest;
    private List<IgoSampleManifest> samples;
    private List<String> pooledNormals;

    public IgoRequest() {}

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getDataAccessEmails() {
        return dataAccessEmails;
    }

    public void setDataAccessEmails(String dataAccessEmails) {
        this.dataAccessEmails = dataAccessEmails;
    }

    public String getDataAnalystEmail() {
        return dataAnalystEmail;
    }

    public void setDataAnalystEmail(String dataAnalystEmail) {
        this.dataAnalystEmail = dataAnalystEmail;
    }

    public String getDataAnalystName() {
        return dataAnalystName;
    }

    public void setDataAnalystName(String dataAnalystName) {
        this.dataAnalystName = dataAnalystName;
    }

    public String getInvestigatorEmail() {
        return investigatorEmail;
    }

    public void setInvestigatorEmail(String investigatorEmail) {
        this.investigatorEmail = investigatorEmail;
    }

    public String getInvestigatorName() {
        return investigatorName;
    }

    public void setInvestigatorName(String investigatorName) {
        this.investigatorName = investigatorName;
    }

    public String getLabHeadEmail() {
        return labHeadEmail;
    }

    public void setLabHeadEmail(String labHeadEmail) {
        this.labHeadEmail = labHeadEmail;
    }

    public String getLabHeadName() {
        return labHeadName;
    }

    public void setLabHeadName(String labHeadName) {
        this.labHeadName = labHeadName;
    }

    public String getLibraryType() {
        return libraryType;
    }

    public void setLibraryType(String libraryType) {
        this.libraryType = libraryType;
    }

    public String getOtherContactEmails() {
        return otherContactEmails;
    }

    public void setOtherContactEmails(String otherContactEmails) {
        this.otherContactEmails = otherContactEmails;
    }

    public String getPiEmail() {
        return piEmail;
    }

    public void setPiEmail(String piEmail) {
        this.piEmail = piEmail;
    }

    public String getProjectManagerName() {
        return projectManagerName;
    }

    public void setProjectManagerName(String projectManagerName) {
        this.projectManagerName = projectManagerName;
    }

    public String getQcAccessEmails() {
        return qcAccessEmails;
    }

    public void setQcAccessEmails(String qcAccessEmails) {
        this.qcAccessEmails = qcAccessEmails;
    }

    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    public String getStrand() {
        return strand;
    }

    public void setStrand(String strand) {
        this.strand = strand;
    }

    public Long getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(Long deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public Boolean getBicAnalysis() {
        return bicAnalysis;
    }

    public void setBicAnalysis(Boolean bicAnalysis) {
        this.bicAnalysis = bicAnalysis;
    }

    public Boolean getIsCmoRequest() {
        return isCmoRequest;
    }

    public void setIsCmoRequest(Boolean isCmoRequest) {
        this.isCmoRequest = isCmoRequest;
    }

    /**
     * Returns samples or empty array list if null.
     * @return List
     */
    public List<IgoSampleManifest> getSamples() {
        if (samples == null) {
            this.samples = new ArrayList<>();
        }
        return samples;
    }

    public void setSamples(List<IgoSampleManifest> samples) {
        this.samples = samples;
    }

    /**
     * Returns pooled normals or empty array list if null.
     * @return List
     */
    public List<String> getPooledNormals() {
        if (pooledNormals == null) {
            this.pooledNormals = new ArrayList<>();
        }
        return pooledNormals;
    }

    public void setPooledNormals(List<String> pooledNormals) {
        this.pooledNormals = pooledNormals;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
