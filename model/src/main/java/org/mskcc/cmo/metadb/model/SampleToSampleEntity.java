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

@RelationshipEntity(type = "IS_ALIAS")
public class SampleToSampleEntity implements Serializable {
    @Id @GeneratedValue
    private Long id;
    @StartNode
    private SampleAlias sampleAlias;
    @EndNode
    private SampleManifestEntity sampleManifestEntity;

    public SampleToSampleEntity() {}

    public SampleAlias getSampleAlias() {
        return sampleAlias;
    }

    public void setSampleAlias(SampleAlias sampleAlias) {
        this.sampleAlias = sampleAlias;
    }

    public SampleManifestEntity getSampleManifestEntity() {
        return sampleManifestEntity;
    }

    public void setSampleManifestEntity(SampleManifestEntity sampleManifestEntity) {
        this.sampleManifestEntity = sampleManifestEntity;
    }

}
