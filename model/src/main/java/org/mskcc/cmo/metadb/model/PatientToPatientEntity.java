package org.mskcc.cmo.metadb.model;

import java.io.Serializable;
import java.util.Collection;
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

/**
 *
 * @author ochoaa
 */

@RelationshipEntity(type = "IS_ALIAS")
public class PatientToPatientEntity implements Serializable {
    @Id @GeneratedValue
    private Long id;
    @Property(name = "value")
    private Collection<String> patientIds;
    @StartNode
    private PatientAlias patientAlias;
    @EndNode
    private PatientMetadata patientMetadata;

    public PatientToPatientEntity() {}

    public Collection<String> getPatientIds() {
        return patientIds;
    }

    public void setPatientIds(Collection<String> patientIds) {
        this.patientIds = patientIds;
    }

    public PatientAlias getPatientAlias() {
        return patientAlias;
    }

    public void setPatientAlias(PatientAlias patientAlias) {
        this.patientAlias = patientAlias;
    }

    public PatientMetadata getPatientMetadata() {
        return patientMetadata;
    }

    public void setPatientMetadata(PatientMetadata patientMetadata) {
        this.patientMetadata = patientMetadata;
    }

}
