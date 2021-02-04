package org.mskcc.cmo.metadb.model;

import java.util.ArrayList;
import java.util.List;
import org.mskcc.cmo.shared.IgoRequest;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

/**
 *
 * @author ochoaa
 */
@NodeEntity
public class MetaDbRequest extends IgoRequest {
    @Id @GeneratedValue
    private Long id;
    @Relationship(type = "REQUEST_TO_SP", direction = Relationship.OUTGOING)
    private List<MetaDbSample> metaDbSampleList;
    @Relationship(type = "PR_TO_REQUEST", direction = Relationship.INCOMING)
    private MetaDbProject projectEntity;
    // need this field to deserialize message from IGO_NEW_REQUEST properly
    protected String projectId;
    private String requestJson;

    public MetaDbRequest() {}

    /**
     * CmoRequestEntity constructor
     * @param requestId
     * @param metaDbSampleList
     * @param sampleManifestList
     * @param projectEntity
     * @param requestJson
     */
    public MetaDbRequest(String requestId, List<MetaDbSample> metaDbSampleList,
            List<SampleManifestEntity> sampleManifestList,
            MetaDbProject projectEntity, String requestJson) {
        super(requestId);
        //this.sampleManifestList = sampleManifestList;
        this.metaDbSampleList = metaDbSampleList;
        this.projectEntity = new MetaDbProject(requestId.split("_")[0]);
        this.requestJson = requestJson;
    }

    /**
     * CmoRequestEntity constructor
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
     * @param projectEntity
     * @param requestJson
     */
    public MetaDbRequest(String requestId, String recipe, String projectManagerName,
            String piEmail, String labHeadName, String labHeadEmail,
            String investigatorName, String investigatorEmail, String dataAnalystName,
            String dataAnalystEmail, String otherContactEmails, String dataAccessEmails,
            String qcAccessEmails, String strand, String libraryType,
            List<MetaDbSample> metaDbSampleList,
            MetaDbProject projectEntity, String requestJson) {
        super(requestId,
                recipe,
                projectManagerName,
                piEmail,
                labHeadName,
                labHeadEmail,
                investigatorName,
                investigatorEmail,
                dataAnalystName,
                dataAnalystEmail,
                otherContactEmails,
                dataAccessEmails,
                qcAccessEmails,
                strand,
                libraryType);
        this.metaDbSampleList = metaDbSampleList;
        this.projectEntity = new MetaDbProject(requestId.split("_")[0]);
        this.requestJson = requestJson;
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

    public MetaDbProject getProjectEntity() {
        return projectEntity;
    }

    public void setProjectEntity(MetaDbProject projectEntity) {
        this.projectEntity = projectEntity;
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
}
