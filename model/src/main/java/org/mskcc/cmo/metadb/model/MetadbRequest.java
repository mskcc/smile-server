package org.mskcc.cmo.metadb.model;

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
public class MetadbRequest implements Serializable {
    @Id @GeneratedValue(strategy = UuidStrategy.class)
    @Convert(UuidStringConverter.class)
    private UUID metaDbRequestId;
    @Relationship(type = "HAS_SAMPLE", direction = Relationship.OUTGOING)
    private List<MetadbSample> metaDbSampleList;
    @Relationship(type = "HAS_REQUEST", direction = Relationship.INCOMING)
    private MetadbProject metaDbProject;
    @JsonIgnore
    @Relationship(type = "HAS_METADATA", direction = Relationship.OUTGOING)
    private List<RequestMetadata> requestMetadataList;
    @JsonIgnore
    private String namespace;
    // need this field to deserialize message from IGO_NEW_REQUEST properly
    private String projectId;
    private String requestJson;
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
    private List<String> pooledNormals;
    private boolean isCmoRequest;
    private boolean bicAnalysis;

    public MetadbRequest() {}

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
     * @param isCmoRequest
     */
    public MetadbRequest(String requestId, String recipe, String projectManagerName,
            String piEmail, String labHeadName, String labHeadEmail,
            String investigatorName, String investigatorEmail, String dataAnalystName,
            String dataAnalystEmail, String otherContactEmails, String dataAccessEmails,
            String qcAccessEmails, String strand, String libraryType,
            List<MetadbSample> metaDbSampleList, String requestJson,
            boolean bicAnalysis, boolean isCmoRequest) {
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
        this.metaDbProject = new MetadbProject(requestId.split("_")[0]);
        this.requestJson = requestJson;
        this.bicAnalysis = bicAnalysis;
        this.isCmoRequest = isCmoRequest;
    }

    public UUID getMetaDbRequestId() {
        return metaDbRequestId;
    }

    public void setMetaDbRequestId(UUID metaDbRequestId) {
        this.metaDbRequestId = metaDbRequestId;
    }

    public List<MetadbSample> getMetaDbSampleList() {
        return metaDbSampleList;
    }

    public void setMetaDbSampleList(List<MetadbSample> metaDbSampleList) {
        this.metaDbSampleList = metaDbSampleList;
    }

    public MetadbProject getMetaDbProject() {
        return metaDbProject;
    }

    public void setMetaDbProject(MetadbProject metaDbProject) {
        this.metaDbProject = metaDbProject;
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
     * Adds a MetadbSample to the sample list.
     * @param metadbSample
     */
    public void addMetadbSample(MetadbSample metadbSample) {
        if (metaDbSampleList == null) {
            metaDbSampleList = new ArrayList<>();
        }
        metaDbSampleList.add(metadbSample);
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

    /**
     * Updates the RequestMetadata with provided request.
     * @param updatedRequest
     */
    public void updateRequestMetadata(MetadbRequest updatedRequest) {
        this.requestId = updatedRequest.getRequestId();
        this.recipe = updatedRequest.getRecipe();
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
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     */
    public void updateRequestMetadata(RequestMetadata requestMetadata)
            throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> metadataMap =
                mapper.readValue(requestMetadata.getRequestMetadataJson(), Map.class);

        this.requestId = metadataMap.get("requestId");
        this.recipe = metadataMap.get("recipe");
        this.projectManagerName = metadataMap.get("projectManagerName");
        this.piEmail = metadataMap.get("piEmail");
        this.labHeadName = metadataMap.get("labHeadName");
        this.labHeadEmail = metadataMap.get("labHeadEmail");
        this.investigatorName = metadataMap.get("investigatorName");
        this.investigatorEmail = metadataMap.get("investigatorEmail");
        this.dataAnalystName = metadataMap.get("dataAnalystName");
        this.otherContactEmails = metadataMap.get("otherContactEmails");
        this.dataAccessEmails = metadataMap.get("dataAccessEmails");
        this.qcAccessEmails = metadataMap.get("qcAccessEmails");
        this.strand = metadataMap.get("strand");
        this.libraryType = metadataMap.get("libraryType");
        this.bicAnalysis = Boolean.getBoolean(metadataMap.get("bicAnalysis"));
        this.isCmoRequest = Boolean.getBoolean(metadataMap.get("isCmoRequest"));
        addRequestMetadata(requestMetadata);
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
