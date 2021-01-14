package org.mskcc.cmo.metadb.model;

import java.io.Serializable;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;
/**
 * Node entity representing the linked sample entity from an external system.
 * @author ochoaa
 */

@NodeEntity(label = "sample")
public class Sample implements Serializable {
    @Id @GeneratedValue
    private Long id;
    @Property(name = "value")
    private String sampleId;
    private String idSource;
    @Relationship(type = "SP_TO_SP", direction = Relationship.OUTGOING)
    private SampleManifestEntity sampleMetadata;

    public Sample() {}

    /**
     * Sample constructor.
     * @param sampleId
     * @param idSource
     */
    public Sample(String sampleId, String idSource) {
        this.sampleId = sampleId;
        this.idSource = idSource;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getIdSource() {
        return idSource;
    }

    public void setIdSource(String idSource) {
        this.idSource = idSource;
    }

    public SampleManifestEntity getSampleMetadata() {
        return sampleMetadata;
    }

    public void setSampleMetadata(SampleManifestEntity sampleMetadata) {
        this.sampleMetadata = sampleMetadata;
    }
}
