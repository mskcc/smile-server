package org.mskcc.cmo.metadb.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.id.UuidStrategy;
import org.neo4j.ogm.typeconversion.UuidStringConverter;

/**
 *
 * @author ochoaa
 */

@NodeEntity(label = "Patient")
public class MetaDbPatient implements Serializable {
    @Id @GeneratedValue(strategy = UuidStrategy.class)
    @Convert(UuidStringConverter.class)
    private UUID metaDbPatientId;
    private String investigatorPatientId;
    @Relationship(type = "HAS_SAMPLE", direction = Relationship.OUTGOING)
    private List<MetaDbSample> metaDbSampleList;
    @Relationship(type = "IS_ALIAS", direction = Relationship.INCOMING)
    private List<PatientAlias>  patientAliases;

    public MetaDbPatient() {}

    public MetaDbPatient(String investigatorPatientId) {
        this.investigatorPatientId = investigatorPatientId;
    }

    public UUID getMetaDbPatientId() {
        return metaDbPatientId;
    }

    public void setMetaDbPatientId(UUID metaDbPatientId) {
        this.metaDbPatientId = metaDbPatientId;
    }

    public String getInvestigatorPatientId() {
        return investigatorPatientId;
    }

    public void setInvestigatorPatientId(String investigatorPatientId) {
        this.investigatorPatientId = investigatorPatientId;
    }

    public List<MetaDbSample> getMetaDbSampleList() {
        return metaDbSampleList;
    }

    public void setMetaDbSampleList(List<MetaDbSample> metaDbSampleList) {
        this.metaDbSampleList = metaDbSampleList;
    }

    /**
     * Add sample to array list.
     * @param metaDbSample
     */
    public void addMetaDbSample(MetaDbSample metaDbSample) {
        if (metaDbSampleList == null) {
            metaDbSampleList = new ArrayList<>();
        }
        metaDbSampleList.add(metaDbSample);
    }

    public List<PatientAlias> getPatientAliases() {
        return patientAliases;
    }

    public void setPatientAliases(List<PatientAlias> patientAliases) {
        this.patientAliases = patientAliases;
    }

    /**
     * Add patient to array list.
     * @param patientAlias
     */
    public void addPatientAlias(PatientAlias patientAlias) {
        if (patientAliases == null) {
            patientAliases = new ArrayList<>();
        }
        patientAliases.add(patientAlias);
    }

}
