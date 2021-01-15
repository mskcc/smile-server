package org.mskcc.cmo.metadb.model;

import java.util.ArrayList;
import java.util.List;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

/**
 *
 * @author ochoaa
 */
@NodeEntity(label = "cmo_metadb_request")
public class CmoRequestEntity {
    @Id
    private String requestId;
    @Relationship(type = "REQUEST_TO_SP", direction = Relationship.OUTGOING)
    private List<SampleManifestEntity> sampleManifestList;
    @Relationship(type = "PR_TO_REQUEST", direction = Relationship.INCOMING)
    private CmoProjectEntity projectEntity;
    private String requestJson;

    public CmoRequestEntity() {}

    /**
     * CmoRequestEntity constructor.
     * @param requestId
     * @param sampleManifestList
     * @param requestJson 
     */
    public CmoRequestEntity(String requestId, List<SampleManifestEntity> sampleManifestList, 
            CmoProjectEntity projectEntity, String requestJson) {
        this.requestId = requestId;
        this.sampleManifestList = sampleManifestList;
        this.projectEntity = projectEntity;
        this.requestJson = requestJson;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public List<SampleManifestEntity> getSampleManifestList() {
        return sampleManifestList;
    }

    public void setSampleManifestList(List<SampleManifestEntity> sampleManifestList) {
        this.sampleManifestList = sampleManifestList;
    }
    
    public CmoProjectEntity getProjectEntity() {
        return projectEntity;
    }

    public void setProjectEntity(CmoProjectEntity projectEntity) {
        this.projectEntity = projectEntity;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public void setRequestJson(String requestJson) {
        this.requestJson = requestJson;
    }
    
    /**
     * 
     * @param sampleManifestEntity
     */
    public void addSampleManifest(SampleManifestEntity sampleManifestEntity) {
        if (sampleManifestList == null) {
            sampleManifestList = new ArrayList<>();
        }
        sampleManifestList.add(sampleManifestEntity); 
    }
}
