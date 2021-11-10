package org.mskcc.cmo.metadb.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
/**
 * Node entity representing the linked sample entity from an external system.
 * @author ochoaa
 */

@NodeEntity
public class SampleAlias implements Serializable {
    @Id @GeneratedValue
    private Long id;
    private String value;
    private String namespace;
    @JsonIgnore
    @Relationship(type = "IS_ALIAS", direction = Relationship.OUTGOING)
    private MetadbSample sampleMetadata;

    public SampleAlias() {}

    /**
     * Sample constructor.
     * @param sampleId
     * @param namespace
     */
    public SampleAlias(String value, String namespace) {
        this.value = value;
        this.namespace = namespace;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public MetadbSample getSampleMetadata() {
        return sampleMetadata;
    }

    public void setSampleMetadata(MetadbSample sampleMetadata) {
        this.sampleMetadata = sampleMetadata;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
