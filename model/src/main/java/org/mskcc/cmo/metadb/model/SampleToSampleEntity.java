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

@RelationshipEntity(type = "SP_TO_SP")
public class SampleToSampleEntity implements Serializable {
    @Id @GeneratedValue
    private Long id;
    @StartNode
    private Sample sample;
    @EndNode
    private SampleManifestEntity sampleManifestEntity;

    public SampleToSampleEntity() {}

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public SampleManifestEntity getSampleManifestEntity() {
        return sampleManifestEntity;
    }

    public void setSampleManifestEntity(SampleManifestEntity sampleManifestEntity) {
        this.sampleManifestEntity = sampleManifestEntity;
    }

}
