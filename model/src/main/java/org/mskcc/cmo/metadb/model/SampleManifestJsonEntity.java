package org.mskcc.cmo.metadb.model;

import java.io.Serializable;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class SampleManifestJsonEntity implements Serializable {
    @Id @GeneratedValue
    private Long id;
    private String sampleManifestJson;
    private String creationDate;
    
    public SampleManifestJsonEntity() {}
    
    public SampleManifestJsonEntity(String sampleManifestJson, String creationDate) {
        this.sampleManifestJson = sampleManifestJson;
        this.creationDate = creationDate;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSampleManifestJson() {
        return sampleManifestJson;
    }
    
    public void setSampleManifestJson(String sampleManifestJson) {
        this.sampleManifestJson = sampleManifestJson;
    }
    
    public String getCreationDate() {
        return creationDate;
    }
    
    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }
}
