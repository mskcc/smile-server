package org.mskcc.smile.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.smile.model.igo.IgoRequest;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.id.UuidStrategy;
import org.neo4j.ogm.typeconversion.UuidStringConverter;

/**
 *
 * @author ochoaa
 */
@NodeEntity(label = "Request")
@JsonIgnoreProperties({"samples"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SmileRequest implements Serializable {
    @JsonIgnore
    private final ObjectMapper mapper = new ObjectMapper();

    @Id @GeneratedValue(strategy = UuidStrategy.class)
    @Convert(UuidStringConverter.class)
    private UUID smileRequestId;
    @Relationship(type = "HAS_SAMPLE", direction = Relationship.OUTGOING)
    private List<SmileSample> smileSampleList;
    @Relationship(type = "HAS_REQUEST", direction = Relationship.INCOMING)
    private SmileProject smileProject;
    @JsonIgnore
    @Relationship(type = "HAS_METADATA", direction = Relationship.OUTGOING)
    private List<RequestMetadata> requestMetadataList;
    @JsonIgnore
    private String namespace;
    // need this field to deserialize message from IGO_NEW_REQUEST properly
    private String igoProjectId;
    private String requestJson;
    private String igoRequestId;
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
    private List<String> pooledNormals;
    private boolean isCmoRequest;
    private boolean bicAnalysis;
    private Boolean revisable;

    public SmileRequest() {}

    /**
     * SmileRequest constructor.
     * @param requestMetadata
     * @throws JsonProcessingException
     */
    public SmileRequest(RequestMetadata requestMetadata) throws JsonProcessingException {
        this.igoRequestId = requestMetadata.getIgoRequestId();
        this.igoProjectId = igoRequestId.split("_")[0];
        this.smileProject = new SmileProject(igoProjectId);
        updateRequestMetadataByMetadata(requestMetadata);
    }

    /**
     * SmileRequest constructor.
     * @param igoRequest
     * @throws JsonProcessingException
     */
    public SmileRequest(IgoRequest igoRequest) throws JsonProcessingException {
        this.igoRequestId = igoRequest.getRequestId();
        this.igoProjectId = igoRequest.getProjectId();
        this.dataAccessEmails = igoRequest.getDataAccessEmails();
        this.dataAnalystEmail = igoRequest.getDataAnalystEmail();
        this.dataAnalystName = igoRequest.getDataAnalystName();
        this.investigatorEmail = igoRequest.getInvestigatorEmail();
        this.investigatorName = igoRequest.getInvestigatorName();
        this.labHeadEmail = igoRequest.getLabHeadEmail();
        this.labHeadName = igoRequest.getLabHeadName();
        this.libraryType = igoRequest.getLibraryType();
        this.otherContactEmails = igoRequest.getOtherContactEmails();
        this.piEmail = igoRequest.getPiEmail();
        this.projectManagerName = igoRequest.getProjectManagerName();
        this.qcAccessEmails = igoRequest.getQcAccessEmails();
        this.genePanel = igoRequest.getRecipe();
        this.strand = igoRequest.getStrand();
        this.pooledNormals = igoRequest.getPooledNormals();
        this.bicAnalysis = igoRequest.getBicAnalysis();
        this.isCmoRequest = igoRequest.getIsCmoRequest();
        this.namespace = "igo";
        this.requestJson = mapper.writeValueAsString(igoRequest);
    }

    public UUID getSmileRequestId() {
        return smileRequestId;
    }

    public void setSmileRequestId(UUID smileRequestId) {
        this.smileRequestId = smileRequestId;
    }

    public List<SmileSample> getSmileSampleList() {
        return smileSampleList;
    }

    public void setSmileSampleList(List<SmileSample> smileSampleList) {
        this.smileSampleList = smileSampleList;
    }

    public SmileProject getSmileProject() {
        return smileProject;
    }

    public void setSmileProject(SmileProject smileProject) {
        this.smileProject = smileProject;
    }

    /**
     * Returns sorted RequestMetadata list.
     * @return List
     */
    public List<RequestMetadata> getRequestMetadataList() {
        if (requestMetadataList == null) {
            requestMetadataList = new ArrayList<>();
        }
        Collections.sort(requestMetadataList);
        return requestMetadataList;
    }

    public void setRequestMetadataList(List<RequestMetadata> requestMetadataList) {
        this.requestMetadataList = requestMetadataList;
    }

    /**
     * Adds new RequestMetadata to requestMetadataList
     * If the requestMetadataList is empty, a new one is instantiated.
     * Otherwise its simply added to the list
     * @param requestMetadata
     */
    public void addRequestMetadata(RequestMetadata requestMetadata) {
        if (requestMetadataList == null) {
            requestMetadataList = new ArrayList<>();
        }
        requestMetadataList.add(requestMetadata);
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getIgoProjectId() {
        return igoProjectId;
    }

    public void setIgoProjectId(String igoProjectId) {
        this.igoProjectId = igoProjectId;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public void setRequestJson(String requestJson) {
        this.requestJson = requestJson;
    }

    /**
     * Adds a SmileSample to the sample list.
     * @param smileSample
     */
    public void addSmileSample(SmileSample smileSample) {
        if (smileSampleList == null) {
            smileSampleList = new ArrayList<>();
        }
        smileSampleList.add(smileSample);
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getIgoRequestId() {
        return igoRequestId;
    }

    public void setIgoRequestId(String igoRequestId) {
        this.igoRequestId = igoRequestId;
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

    public List<String> getPooledNormals() {
        return pooledNormals;
    }

    public void setPooledNormals(List<String> pooledNormals) {
        this.pooledNormals = pooledNormals;
    }

    public boolean getIsCmoRequest() {
        return isCmoRequest;
    }

    public void setIsCmoRequest(boolean isCmoRequest) {
        this.isCmoRequest = isCmoRequest;
    }

    public boolean getBicAnalysis() {
        return bicAnalysis;
    }

    public void setBicAnalysis(boolean bicAnalysis) {
        this.bicAnalysis = bicAnalysis;
    }

    public Boolean getRevisable() {
        return revisable;
    }

    public void setRevisable(Boolean revisable) {
        this.revisable = revisable;
    }

    /**
     * Updates the RequestMetadata with provided request.
     * @param updatedRequest
     */
    public void updateRequestMetadataByRequest(SmileRequest updatedRequest) {
        this.igoRequestId = updatedRequest.getIgoRequestId();
        this.genePanel = updatedRequest.getGenePanel();
        this.projectManagerName = updatedRequest.getProjectManagerName();
        this.piEmail = updatedRequest.getPiEmail();
        this.labHeadName = updatedRequest.getLabHeadName();
        this.labHeadEmail = updatedRequest.getLabHeadEmail();
        this.investigatorName = updatedRequest.getInvestigatorName();
        this.investigatorEmail = updatedRequest.getInvestigatorEmail();
        this.dataAnalystName = updatedRequest.getDataAnalystName();
        this.otherContactEmails = updatedRequest.getOtherContactEmails();
        this.dataAccessEmails = updatedRequest.getDataAccessEmails();
        this.qcAccessEmails = updatedRequest.getQcAccessEmails();
        this.strand = updatedRequest.getStrand();
        this.libraryType = updatedRequest.getLibraryType();
        this.bicAnalysis = updatedRequest.getBicAnalysis();
        this.isCmoRequest = updatedRequest.getIsCmoRequest();
        this.requestJson = updatedRequest.getRequestJson();
        addRequestMetadata(updatedRequest.getLatestRequestMetadata());
    }

    /**
     * Updates the metadata for the current request provided a RequestMetadata object.
     * @param requestMetadata
     * @throws JsonProcessingException
     */
    public void updateRequestMetadataByMetadata(RequestMetadata requestMetadata)
            throws JsonProcessingException {
        Map<String, Object> metadataMap =
                mapper.readValue(requestMetadata.getRequestMetadataJson(), Map.class);

        this.genePanel = resolveGenePanel(metadataMap);
        this.projectManagerName = String.valueOf(metadataMap.get("projectManagerName"));
        this.piEmail = String.valueOf(metadataMap.get("piEmail"));
        this.labHeadName = String.valueOf(metadataMap.get("labHeadName"));
        this.labHeadEmail = String.valueOf(metadataMap.get("labHeadEmail"));
        this.investigatorName = String.valueOf(metadataMap.get("investigatorName"));
        this.investigatorEmail = String.valueOf(metadataMap.get("investigatorEmail"));
        this.dataAnalystName = String.valueOf(metadataMap.get("dataAnalystName"));
        this.otherContactEmails = String.valueOf(metadataMap.get("otherContactEmails"));
        this.dataAccessEmails = String.valueOf(metadataMap.get("dataAccessEmails"));
        this.qcAccessEmails = String.valueOf(metadataMap.get("qcAccessEmails"));
        this.strand = String.valueOf(metadataMap.get("strand"));
        this.libraryType = String.valueOf(metadataMap.get("libraryType"));
        this.bicAnalysis = Boolean.parseBoolean(String.valueOf(metadataMap.get("bicAnalysis")));
        this.isCmoRequest = Boolean.parseBoolean(String.valueOf(metadataMap.get("isCmoRequest")));
        this.requestJson = requestMetadata.getRequestMetadataJson();
        addRequestMetadata(requestMetadata);
    }

    /**
     * Updates by LIMS
     * Update Metadata to only include accepted updates for a list of fields
     * @param requestMetadata
     * @throws JsonProcessingException
     */
    public void applyIgoRequestMetadataUpdates(RequestMetadata requestMetadata)
            throws JsonProcessingException {
        Map<String, Object> metadataMap =
                mapper.readValue(requestMetadata.getRequestMetadataJson(), Map.class);

        this.genePanel = resolveGenePanel(metadataMap);
        this.strand = String.valueOf(metadataMap.get("strand"));
        this.libraryType = String.valueOf(metadataMap.get("libraryType"));
        this.isCmoRequest = Boolean.parseBoolean(String.valueOf(metadataMap.get("isCmoRequest")));
        this.pooledNormals = mapper.convertValue(metadataMap.get("pooledNormals"), List.class);
        addRequestMetadata(requestMetadata);
    }

    /**
     * Resolves gene panel from recipe or genePanel sample json field.
     * @param metadataMap
     * @return String
     */
    public String resolveGenePanel(Map<String, Object> metadataMap) {
        Object genePanel = (metadataMap.containsKey("recipe"))
                ? metadataMap.get("recipe") : metadataMap.get("genePanel");
        if (genePanel != null) {
            return String.valueOf(genePanel);
        }
        return null;
    }

    /**
     * Returns the latest RequestMetadata.
     * Collections is sorting by date in ascending order so the last item
     * in the list reflects the latest RequestMetadata.
     * @return RequestMetadata
     */
    public RequestMetadata getLatestRequestMetadata() {
        if (requestMetadataList != null && !requestMetadataList.isEmpty()) {
            Collections.sort(requestMetadataList);
            return requestMetadataList.get(requestMetadataList.size() - 1);
        }
        return null;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
