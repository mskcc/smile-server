package org.mskcc.cmo.metadb.model.web;

import java.util.List;
import java.util.UUID;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.typeconversion.UuidStringConverter;

/**
 *
 * @author ochoaa
 */
public class PublishedMetaDbRequest {
    @Convert(UuidStringConverter.class)
    private UUID metaDbRequestId;
    private String projectId;
    private String requestId;
    private String recipe;
    private String projectManagerName;
    private String piEmail;
    private String labHeadName;
    private String labHeadEmail;
    private String investigatorName;
    private String investigatorEmail;
    private String dataAnalystName;
    private String dataAnalystEmail;
    private String otherContactEmails;
    private String dataAccessEmails;
    private String qcAccessEmails;
    private String strand;
    private String libraryType;
    private boolean cmoRequest;
    private boolean bicAnalysis;
    private String requestJson;
    private List<String> pooledNormals;
    private List<SampleMetadata> samples;

    public PublishedMetaDbRequest() {}

    /**
     * MetaDbRequestWeb constructor.
     * @param metaDbRequest
     * @param samples
     */
    public PublishedMetaDbRequest(MetaDbRequest metaDbRequest, List<SampleMetadata> samples) {
        this.metaDbRequestId = metaDbRequest.getMetaDbRequestId();
        this.projectId = metaDbRequest.getRequestId().split("_")[0];
        this.requestId = metaDbRequest.getRequestId();
        this.recipe = metaDbRequest.getRecipe();
        this.projectManagerName = metaDbRequest.getProjectManagerName();
        this.piEmail = metaDbRequest.getPiEmail();
        this.labHeadName = metaDbRequest.getLabHeadName();
        this.labHeadEmail = metaDbRequest.getLabHeadEmail();
        this.investigatorName = metaDbRequest.getInvestigatorName();
        this.investigatorEmail = metaDbRequest.getInvestigatorEmail();
        this.dataAnalystName = metaDbRequest.getDataAnalystName();
        this.dataAnalystEmail = metaDbRequest.getDataAnalystEmail();
        this.otherContactEmails = metaDbRequest.getOtherContactEmails();
        this.dataAccessEmails = metaDbRequest.getDataAccessEmails();
        this.qcAccessEmails = metaDbRequest.getQcAccessEmails();
        this.strand = metaDbRequest.getStrand();
        this.libraryType = metaDbRequest.getLibraryType();
        this.cmoRequest = metaDbRequest.getCmoRequest();
        this.bicAnalysis = metaDbRequest.getBicAnalysis();
        this.requestJson = metaDbRequest.getRequestJson();
        this.pooledNormals = metaDbRequest.getPooledNormals();
        this.samples = samples;
    }

    /**
     * All args constructor.
     * @param metaDbRequestId
     * @param projectId
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
     * @param cmoRequest
     * @param bicAnalysis
     * @param requestJson
     * @param pooledNormals
     * @param samples
     */
    public PublishedMetaDbRequest(UUID metaDbRequestId, String projectId, String requestId,
            String recipe, String projectManagerName, String piEmail, String labHeadName,
            String labHeadEmail, String investigatorName, String investigatorEmail, String dataAnalystName,
            String dataAnalystEmail, String otherContactEmails, String dataAccessEmails,
            String qcAccessEmails, String strand, String libraryType, Boolean cmoRequest,
            Boolean bicAnalysis, String requestJson, List<String> pooledNormals,
            List<SampleMetadata> samples) {
        this.metaDbRequestId = metaDbRequestId;
        this.projectId = projectId;
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
        this.cmoRequest = cmoRequest;
        this.bicAnalysis = bicAnalysis;
        this.requestJson = requestJson;
        this.pooledNormals = pooledNormals;
        this.samples = samples;
    }

    public UUID getMetaDbRequestId() {
        return metaDbRequestId;
    }

    public void setMetaDbRequestId(UUID metaDbRequestId) {
        this.metaDbRequestId = metaDbRequestId;
    }

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

    public boolean isCmoRequest() {
        return cmoRequest;
    }

    public void setCmoRequest(boolean cmoRequest) {
        this.cmoRequest = cmoRequest;
    }

    public boolean isBicAnalysis() {
        return bicAnalysis;
    }

    public void setBicAnalysis(boolean bicAnalysis) {
        this.bicAnalysis = bicAnalysis;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public void setRequestJson(String requestJson) {
        this.requestJson = requestJson;
    }

    public List<String> getPooledNormals() {
        return pooledNormals;
    }

    public void setPooledNormals(List<String> pooledNormals) {
        this.pooledNormals = pooledNormals;
    }

    public List<SampleMetadata> getSamples() {
        return samples;
    }

    public void setSamples(List<SampleMetadata> samples) {
        this.samples = samples;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
