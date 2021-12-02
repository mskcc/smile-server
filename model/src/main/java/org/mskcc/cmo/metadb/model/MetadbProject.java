package org.mskcc.cmo.metadb.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonAlias;

@NodeEntity(label = "Project")
public class MetadbProject implements Serializable {
    @Id
    @JsonAlias("projectId")
    private String igoProjectId;
    private String namespace;
    @Relationship(type = "HAS_REQUEST", direction = Relationship.OUTGOING)
    private List<MetadbRequest> requestList;

    public MetadbProject() {}

    public MetadbProject(String igoProjectId) {
        this.igoProjectId = igoProjectId;
    }

    /**
     * MetaDbProject constructor.
     * @param igoProjectId
     * @param namespace
     * @param requestList
     */
    public MetadbProject(String igoProjectId, String namespace, List<MetadbRequest> requestList) {
        this.igoProjectId = igoProjectId;
        this.namespace = namespace;
        this.requestList = requestList;
    }

    public String getIgoProjectId() {
        return igoProjectId;
    }

    public void setIgoProjectId(String igoProjectId) {
        this.igoProjectId = igoProjectId;
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
