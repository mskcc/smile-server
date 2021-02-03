package org.mskcc.cmo.metadb.model;

import java.io.Serializable;
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

@RelationshipEntity(type = "PR_TO_REQUEST")
public class MetaDbProjectToMetaDbRequest implements Serializable {
    @Id @GeneratedValue
    private Long id;
    @StartNode
    private MetaDbProject metaDbProject;
    @EndNode
    private MetaDbRequest metaDbRequest;
    
    MetaDbProjectToMetaDbRequest() {}
    
    private Long getId() {
        return id;
    }
    
    private void setId(Long id) {
        this.id = id;
    }
    
    private MetaDbProject getCmoProjectEntity() {
        return metaDbProject;
    }
    
    private void setCmoProjectEntity(MetaDbProject metaDbProject) {
        this.metaDbProject = metaDbProject;
    }
    
    private MetaDbRequest getCmoRequestEntity() {
        return metaDbRequest;
    }
    
    private void setCmoRequestEntity(MetaDbRequest metaDbRequest) {
        this.metaDbRequest = metaDbRequest;
    }
}
