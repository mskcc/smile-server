package org.mskcc.cmo.metadb.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

/**
 *
 * @author ochoaa
 */
@NodeEntity(label = "Request")
public class MetaDbRequest implements Serializable {
    @Id @GeneratedValue
    private Long id;
    @Relationship(type = "HAS_SAMPLE", direction = Relationship.OUTGOING)
    private List<MetaDbSample> metaDbSampleList;
    @Relationship(type = "HAS_REQUEST", direction = Relationship.INCOMING)
    private MetaDbProject metaDbProject;
    private String idSource;
    // need this field to deserialize message from IGO_NEW_REQUEST properly
    protected String projectId;
    private String requestJson;
    protected String requestId;
    protected String recipe;
    protected String projectManagerName;
    protected String piEmail;
    protected String labHeadName;
    protected String labHeadEmail;
    protected String investigatorName;
    protected String investigatorEmail;
    protected String dataAnalystName;
    protected String dataAnalystEmail;
    protected String otherContactEmails;
    protected String dataAccessEmails;
    protected String qcAccessEmails;
    protected String strand;
    protected String libraryType;
    protected List<RequestSample> requestSamples;
    protected List<String> pooledNormals;
    protected boolean cmoRequest;
    protected boolean bicAnalysis;

    public MetaDbRequest() {}

    /**
     * MetaDbRequest constructor
     * @param requestId
     * @param recipe
     * @param projectManagerName
     * @param piEmail
     * @param labHeadName
     * @param labHeadEmail
     * @param investigatorName
     * @param investigatorEmail
     * @param dataAnalystName
     * @param dataAnalystEmail
     * @param otherContactEmails
     * @param dataAccessEmails
     * @param qcAccessEmails
     * @param strand
     * @param libraryType
     * @param metaDbSampleList
     * @param requestJson
     * @param bicAnalysis
     * @param cmoRequest
     */
    public MetaDbRequest(String requestId, String recipe, String projectManagerName,
            String piEmail, String labHeadName, String labHeadEmail,
            String investigatorName, String investigatorEmail, String dataAnalystName,
            String dataAnalystEmail, String otherContactEmails, String dataAccessEmails,
            String qcAccessEmails, String strand, String libraryType,
            List<MetaDbSample> metaDbSampleList, String requestJson, 
            boolean bicAnalysis, boolean cmoRequest) {
        this.requestId = requestId;
        this.recipe = recipe;
        this.projectManagerName = projectManagerName;
        this.piEmail = piEmail;
        this.labHeadName = labHeadName;
        this.labHeadEmail = labHeadEmail;
        this.investigatorName = investigatorName;
        this.investigatorEmail = investigatorEmail;
        this.dataAnalystName = dataAnalystName;
        this.dataAnalystEmail = dataAnalystEmail;
        this.otherContactEmails = otherContactEmails;
        this.dataAccessEmails = dataAccessEmails;
        this.qcAccessEmails = qcAccessEmails;
        this.strand = strand;
        this.libraryType = libraryType;
        this.metaDbSampleList = metaDbSampleList;
        this.metaDbProject = new MetaDbProject(requestId.split("_")[0]);
        this.requestJson = requestJson;
        this.bicAnalysis = bicAnalysis;
        this.cmoRequest = cmoRequest;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<MetaDbSample> getMetaDbSampleList() {
        return metaDbSampleList;
    }

    public void setMetaDbSampleList(List<MetaDbSample> metaDbSampleList) {
        this.metaDbSampleList = metaDbSampleList;
    }

    public MetaDbProject getMetaDbProject() {
        return metaDbProject;
    }

    public void setMetaDbProject(MetaDbProject metaDbProject) {
        this.metaDbProject = metaDbProject;
    }

    public String getIdSource() {
        return idSource;
    }

    public void setIdSource(String idSource) {
        this.idSource = idSource;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public void setRequestJson(String requestJson) {
        this.requestJson = requestJson;
    }

    /**
     *
     * @param metaDbSample
     */
    public void addMetaDbSampleList(MetaDbSample metaDbSample) {
        if (metaDbSampleList == null) {
            metaDbSampleList = new ArrayList<>();
        }
        metaDbSampleList.add(metaDbSample);
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    public String getProjectManagerName() {
        return projectManagerName;
    }

    public void setProjectManagerName(String projectManagerName) {
        this.projectManagerName = projectManagerName;
    }

    public String getPiEmail() {
        return piEmail;
    }

    public void setPiEmail(String piEmail) {
        this.piEmail = piEmail;
    }

    public String getLabHeadName() {
        return labHeadName;
    }

    public void setLabHeadName(String labHeadName) {
        this.labHeadName = labHeadName;
    }

    public String getLabHeadEmail() {
        return labHeadEmail;
    }

    public void setLabHeadEmail(String labHeadEmail) {
        this.labHeadEmail = labHeadEmail;
    }

    public String getInvestigatorName() {
        return investigatorName;
    }

    public void setInvestigatorName(String investigatorName) {
        this.investigatorName = investigatorName;
    }

    public String getInvestigatorEmail() {
        return investigatorEmail;
    }

    public void setInvestigatorEmail(String investigatorEmail) {
        this.investigatorEmail = investigatorEmail;
    }

    public String getDataAnalystName() {
        return dataAnalystName;
    }

    public void setDataAnalystName(String dataAnalystName) {
        this.dataAnalystName = dataAnalystName;
    }

    public String getDataAnalystEmail() {
        return dataAnalystEmail;
    }

    public void setDataAnalystEmail(String dataAnalystEmail) {
        this.dataAnalystEmail = dataAnalystEmail;
    }

    public String getOtherContactEmails() {
        return otherContactEmails;
    }

    public void setOtherContactEmails(String otherContactEmails) {
        this.otherContactEmails = otherContactEmails;
    }

    public String getDataAccessEmails() {
        return dataAccessEmails;
    }

    public void setDataAccessEmails(String dataAccessEmails) {
        this.dataAccessEmails = dataAccessEmails;
    }

    public String getQcAccessEmails() {
        return qcAccessEmails;
    }

    public void setQcAccessEmails(String qcAccessEmails) {
        this.qcAccessEmails = qcAccessEmails;
    }

    public String getStrand() {
        return strand;
    }

    public void setStrand(String strand) {
        this.strand = strand;
    }

    public String getLibraryType() {
        return libraryType;
    }

    public void setLibraryType(String libraryType) {
        this.libraryType = libraryType;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<RequestSample> getSamples() {
        return requestSamples;
    }

    public void setSamples(List<RequestSample> requestSamples) {
        this.requestSamples = requestSamples;
    }

    public List<String> getPooledNormals() {
        return pooledNormals;
    }

    public void setPooledNormals(List<String> pooledNormals) {
        this.pooledNormals = pooledNormals;
    }

    public boolean getCmoRequest() {
        return cmoRequest;
    }

    public void setCmoRequest(boolean cmoRequest) {
        this.cmoRequest = cmoRequest;
    }

    public boolean getBicAnalysis() {
        return bicAnalysis;
    }

    public void setBicAnalysis(boolean bicAnalysis) {
        this.bicAnalysis = bicAnalysis;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
