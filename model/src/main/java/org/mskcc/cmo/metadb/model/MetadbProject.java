package org.mskcc.cmo.metadb.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity(label = "Project")
public class MetadbProject implements Serializable {
    @Id
    private String projectId;
    private String namespace;
    @Relationship(type = "HAS_REQUEST", direction = Relationship.OUTGOING)
    private List<MetadbRequest> requestList;

    public MetadbProject() {}

    public MetadbProject(String projectId) {
        this.projectId = projectId;
    }

    /**
     * MetaDbProject constructor.
     * @param projectId
     * @param namespace
     * @param requestList
     */
    public MetadbProject(String projectId, String namespace, List<MetadbRequest> requestList) {
        this.projectId = projectId;
        this.namespace = namespace;
        this.requestList = requestList;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public List<MetadbRequest> getRequestList() {
        return requestList;
    }

    public void setRequestList(List<MetadbRequest> requestList) {
        this.requestList = requestList;
    }

    /**
     *
     * @param metaDbRequest
     */
    public void addRequest(MetadbRequest metaDbRequest) {
        if (requestList == null) {
            requestList = new ArrayList<>();
        }
        requestList.add(metaDbRequest);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
