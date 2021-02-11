package org.mskcc.cmo.metadb.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class MetaDbProject implements Serializable {
    @Id
    private String projectId;
    @Relationship(type = "HAS_REQUEST", direction = Relationship.OUTGOING)
    private List<MetaDbRequest> requestList;

    public MetaDbProject() {}

    public MetaDbProject(String projectId) {
        this.projectId = projectId;
    }

    public MetaDbProject(String projectId, List<MetaDbRequest> requestList) {
        this.projectId = projectId;
        this.requestList = requestList;
    }

    public String getprojectId() {
        return projectId;
    }

    public void setprojectId(String projectId) {
        this.projectId = projectId;
    }

    public List<MetaDbRequest> getRequestList() {
        return requestList;
    }

    public void setRequestList(List<MetaDbRequest> requestList) {
        this.requestList = requestList;
    }

    /**
     *
     * @param metaDbRequest
     */
    public void addRequest(MetaDbRequest metaDbRequest) {
        if (requestList == null) {
            requestList = new ArrayList<>();
        }
        requestList.add(metaDbRequest);
    }
}
