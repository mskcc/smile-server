package org.mskcc.cmo.metadb.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity(label = "cmo_metadb_project")
public class CmoProjectEntity implements Serializable {
    @Id 
    private String projectId;
    @Relationship(type = "PR_TO_REQUEST", direction = Relationship.OUTGOING)
    private List<CmoRequestEntity> requestList;
    
    public CmoProjectEntity() {}
    
    public String getprojectId() {
        return projectId;
    }
    
    public void setprojectId(String projectId) {
        this.projectId = projectId;
    }
    
    public List<CmoRequestEntity> getRequestList() {
        return requestList;
    }
    
    public void setRequestList(List<CmoRequestEntity> requestList) {
        this.requestList = requestList;
    }
    
    /**
     * 
     * @param cmoRequestEntity
     */
    public void addRequest(CmoRequestEntity cmoRequestEntity) {
        if (requestList == null) {
            requestList = new ArrayList<>();
        }
        requestList.add(cmoRequestEntity);
    }
}
