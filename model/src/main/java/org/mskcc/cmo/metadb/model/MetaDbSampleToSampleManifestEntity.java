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
@RelationshipEntity(type = "HAS_METADATA")
public class MetaDbSampleToSampleManifestEntity implements Serializable {
    @Id @GeneratedValue
    private Long id;
    @StartNode
    private MetaDbSample metaDbSample;
    @EndNode
    private SampleManifestEntity sampleManifestEntity;

    public MetaDbSampleToSampleManifestEntity() {}

    private Long getId() {
        return id;
    }

    private void setId(Long id) {
        this.id = id;
    }

    public MetaDbSample getSampleManifestEntity() {
        return metaDbSample;
    }

    public void setSampleManifestEntity(MetaDbSample metaDbSample) {
        this.metaDbSample = metaDbSample;
    }

    public SampleManifestEntity getSampleManifestJsonEntity() {
        return sampleManifestEntity;
    }

    public void setSampleManifestJsonEntity(SampleManifestEntity sampleManifestEntity) {
        this.sampleManifestEntity = sampleManifestEntity;
    }
}
