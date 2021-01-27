package org.mskcc.cmo.metadb.model;

import java.io.Serializable;
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

/**
 *
 * @author ochoaa
 */
@RelationshipEntity(type = "SAMPLE_MANIFEST")
public class SampleManifestEntityToSampleManifestJsonEntity implements Serializable {
    @Id @GeneratedValue
    private Long id;
    @StartNode
    private SampleManifestEntity sampleManifestEntity;
    @EndNode
    private SampleManifestJsonEntity sampleManifestJsonEntity;

    public SampleManifestEntityToSampleManifestJsonEntity() {}
    
    private Long getId() {
        return id;
    }

    private void setId(Long id) {
        this.id = id;
    }

    public SampleManifestEntity getSampleManifestEntity() {
        return sampleManifestEntity;
    }

    public void setSampleManifestEntity(SampleManifestEntity sampleManifestEntity) {
        this.sampleManifestEntity = sampleManifestEntity;
    }

    public SampleManifestJsonEntity getSampleManifestJsonEntity() {
        return sampleManifestJsonEntity;
    }

    public void setSampleManifestJsonEntity(SampleManifestJsonEntity sampleManifestJsonEntity) {
        this.sampleManifestJsonEntity = sampleManifestJsonEntity;
    }
}
