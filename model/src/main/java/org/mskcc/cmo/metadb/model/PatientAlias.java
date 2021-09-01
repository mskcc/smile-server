package org.mskcc.cmo.metadb.model;

import java.io.Serializable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;

/**
 *
 * @author ochoaa
 */

@NodeEntity
public class PatientAlias implements Serializable {
    @Id @GeneratedValue
    private Long id;
    @Property(name = "value")
    private String patientId;
    private String namespace;
    @Relationship(type = "IS_ALIAS", direction = Relationship.OUTGOING)
    private MetadbPatient metaDbPatient;

    public PatientAlias() {}

    public PatientAlias(String patientId, String namespace) {
        this.patientId = patientId;
        this.namespace = namespace;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public MetadbPatient getPatientMetadata() {
        return metaDbPatient;
    }

    public void setPatientMetadata(MetadbPatient metaDbPatient) {
        this.metaDbPatient = metaDbPatient;
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
