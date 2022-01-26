package org.mskcc.cmo.metadb.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang.builder.ToStringBuilder;
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
public class MetadbPatient implements Serializable {
    @Id @GeneratedValue(strategy = UuidStrategy.class)
    @Convert(UuidStringConverter.class)
    private UUID metaDbPatientId;
    @Relationship(type = "HAS_SAMPLE", direction = Relationship.OUTGOING)
    private List<MetadbSample> metaDbSampleList;
    @Relationship(type = "IS_ALIAS", direction = Relationship.INCOMING)
    private List<PatientAlias>  patientAliases;

    public MetadbPatient() {}

    public UUID getMetaDbPatientId() {
        return metaDbPatientId;
    }

    public void setMetaDbPatientId(UUID metaDbPatientId) {
        this.metaDbPatientId = metaDbPatientId;
    }

    /**
     * Returns CMO PatientAlias.
     * @return
     */
    public PatientAlias getCmoPatientId() {
        if (patientAliases == null) {
            this.patientAliases = new ArrayList<>();
        }
        for (PatientAlias p : patientAliases) {
            if (p.getNamespace().equalsIgnoreCase("cmoId")) {
                return p;
            }
        }
        return null;
    }

    public List<MetadbSample> getMetaDbSampleList() {
        return metaDbSampleList;
    }

    public void setMetaDbSampleList(List<MetadbSample> metaDbSampleList) {
        this.metaDbSampleList = metaDbSampleList;
    }

    /**
     * Add sample to array list.
     * @param metaDbSample
     */
    public void addMetaDbSample(MetadbSample metaDbSample) {
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

    /**
     * Determines whether Patient already has the provided patientAlias.
     * @param patientAlias
     * @return Boolean
     */
    public Boolean hasPatientAlias(PatientAlias patientAlias) {
        if (patientAliases == null) {
            patientAliases = new ArrayList<>();
        }
        for (PatientAlias alias : patientAliases) {
            if (patientAlias.getNamespace().equalsIgnoreCase(alias.getNamespace())
                    && patientAlias.getValue().equalsIgnoreCase(alias.getValue())) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
