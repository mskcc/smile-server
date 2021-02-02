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

@NodeEntity(label = "sample_alias")
public class SampleAlias implements Serializable {
    @Id @GeneratedValue
    private Long id;
    @Property(name = "value")
    private String sampleId;
    private String idSource;
    @Relationship(type = "IS_ALIAS", direction = Relationship.OUTGOING)
    private SampleManifestEntity sampleMetadata;

    public SampleAlias() {}

    /**
     * Sample constructor.
     * @param sampleId
     * @param idSource
     */
    public SampleAlias(String sampleId, String idSource) {
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
