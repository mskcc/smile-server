package org.mskcc.cmo.metadb.model;

import java.io.Serializable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;
/**
 * Node entity representing the linked sample entity from an external system.
 * @author ochoaa
 */

@NodeEntity
public class SampleAlias implements Serializable {
    @Id @GeneratedValue
    private Long id;
    @Property(name = "value")
    private String sampleId;
    private String namespace;
    @Relationship(type = "IS_ALIAS", direction = Relationship.OUTGOING)
    private MetaDbSample sampleMetadata;

    public SampleAlias() {}

    /**
     * Sample constructor.
     * @param sampleId
     * @param namespace
     */
    public SampleAlias(String sampleId, String namespace) {
        this.sampleId = sampleId;
        this.namespace = namespace;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public MetaDbSample getSampleMetadata() {
        return sampleMetadata;
    }

    public void setSampleMetadata(MetaDbSample sampleMetadata) {
        this.sampleMetadata = sampleMetadata;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
