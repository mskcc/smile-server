package org.mskcc.cmo.metadb.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class CmoProjectEntity implements Serializable {
    @Id
    private String projectId;
    @Relationship(type = "PR_TO_REQUEST", direction = Relationship.OUTGOING)
    private List<CmoRequestEntity> requestList;

    public CmoProjectEntity() {}

    public CmoProjectEntity(String projectId) {
        this.projectId = projectId;
    }

    public CmoProjectEntity(String projectId, List<CmoRequestEntity> requestList) {
        this.projectId = projectId;
        this.requestList = requestList;
    }

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
