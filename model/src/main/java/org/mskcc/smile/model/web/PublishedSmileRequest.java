package org.mskcc.smile.model.web;

import java.util.List;
import java.util.UUID;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.Status;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.typeconversion.UuidStringConverter;

/**
 *
 * @author ochoaa
 */
public class PublishedSmileRequest {
    @Convert(UuidStringConverter.class)
    private UUID smileRequestId;
    private String igoProjectId;
    private String igoRequestId;
    private Long igoDeliveryDate;
    private String ilabRequestId;
    private String genePanel;
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
    private boolean isCmoRequest;
    private boolean bicAnalysis;
    private Status status;
    private String requestJson;
    private List<String> pooledNormals;
    private List<PublishedSmileSample> samples;

    public PublishedSmileRequest() {}

    /**
     * PublishedSmileRequest constructor.
     * @param smileRequest
     * @param samples
     */
    public PublishedSmileRequest(SmileRequest smileRequest, List<PublishedSmileSample> samples) {
        this.smileRequestId = smileRequest.getSmileRequestId();
        this.igoProjectId = smileRequest.getIgoRequestId().split("_")[0];
        this.igoRequestId = smileRequest.getIgoRequestId();
        this.igoDeliveryDate = smileRequest.getIgoDeliveryDate();
        this.ilabRequestId = smileRequest.getIlabRequestId();
        this.genePanel = smileRequest.getGenePanel();
        this.projectManagerName = smileRequest.getProjectManagerName();
        this.piEmail = smileRequest.getPiEmail();
        this.labHeadName = smileRequest.getLabHeadName();
        this.labHeadEmail = smileRequest.getLabHeadEmail();
        this.investigatorName = smileRequest.getInvestigatorName();
        this.investigatorEmail = smileRequest.getInvestigatorEmail();
        this.dataAnalystName = smileRequest.getDataAnalystName();
        this.dataAnalystEmail = smileRequest.getDataAnalystEmail();
        this.otherContactEmails = smileRequest.getOtherContactEmails();
        this.dataAccessEmails = smileRequest.getDataAccessEmails();
        this.qcAccessEmails = smileRequest.getQcAccessEmails();
        this.strand = smileRequest.getStrand();
        this.libraryType = smileRequest.getLibraryType();
        this.isCmoRequest = smileRequest.getIsCmoRequest();
        this.bicAnalysis = smileRequest.getBicAnalysis();
        this.status = smileRequest.getLatestRequestMetadata().getStatus();
        this.requestJson = smileRequest.getRequestJson();
        this.pooledNormals = smileRequest.getPooledNormals();
        this.samples = samples;
    }

    /**
     * All args constructor.
     * @param smileRequestId
     * @param igoProjectId
     * @param igoRequestId
     * @param igoDeliveryDate
     * @param ilabRequestId
     * @param genePanel
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
     * @param isCmoRequest
     * @param bicAnalysis
     * @param requestJson
     * @param pooledNormals
     * @param samples
     */
    public PublishedSmileRequest(UUID smileRequestId, String igoProjectId, String igoRequestId,
            Long igoDeliveryDate, String ilabRequestId, String genePanel, String projectManagerName, 
            String piEmail, String labHeadName, String labHeadEmail, String investigatorName, 
            String investigatorEmail, String dataAnalystName, String dataAnalystEmail, 
            String otherContactEmails, String dataAccessEmails, String qcAccessEmails, String strand, 
            String libraryType, Boolean isCmoRequest, Boolean bicAnalysis, String requestJson, 
            List<String> pooledNormals, List<PublishedSmileSample> samples) {
        this.smileRequestId = smileRequestId;
        this.igoProjectId = igoProjectId;
        this.igoRequestId = igoRequestId;
        this.igoDeliveryDate = igoDeliveryDate;
        this.ilabRequestId = ilabRequestId;
        this.genePanel = genePanel;
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
        this.isCmoRequest = isCmoRequest;
        this.bicAnalysis = bicAnalysis;
        this.requestJson = requestJson;
        this.pooledNormals = pooledNormals;
        this.samples = samples;
    }

    public UUID getSmileRequestId() {
        return smileRequestId;
    }

    public void setSmileRequestId(UUID smileRequestId) {
        this.smileRequestId = smileRequestId;
    }

    public String getIgoProjectId() {
        return igoProjectId;
    }

    public void setIgoProjectId(String igoProjectId) {
        this.igoProjectId = igoProjectId;
    }

    public String getIgoRequestId() {
        return igoRequestId;
    }

    public void setIgoRequestId(String igoRequestId) {
        this.igoRequestId = igoRequestId;
    }

    public Long getIgoDeliveryDate() {
        return igoDeliveryDate;
    }

    public void setIgoDeliveryDate(Long igoDeliveryDate) {
        this.igoDeliveryDate = igoDeliveryDate;
    }

    public String getIlabRequestId() {
        return ilabRequestId;
    }

    public void setIlabRequestId(String ilabRequestId) {
        this.ilabRequestId = ilabRequestId;
    }

    public String getGenePanel() {
        return genePanel;
    }

    public void setGenePanel(String genePanel) {
        this.genePanel = genePanel;
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

    public boolean getIsCmoRequest() {
        return isCmoRequest;
    }

    public void setIsCmoRequest(boolean isCmoRequest) {
        this.isCmoRequest = isCmoRequest;
    }

    public boolean isBicAnalysis() {
        return bicAnalysis;
    }

    public void setBicAnalysis(boolean bicAnalysis) {
        this.bicAnalysis = bicAnalysis;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    public List<PublishedSmileSample> getSamples() {
        return samples;
    }

    public void setSamples(List<PublishedSmileSample> samples) {
        this.samples = samples;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
