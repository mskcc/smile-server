package org.mskcc.smile.model;

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
public class SmilePatient implements Serializable {
    @Id @GeneratedValue(strategy = UuidStrategy.class)
    @Convert(UuidStringConverter.class)
    private UUID smilePatientId;
    @Relationship(type = "HAS_SAMPLE", direction = Relationship.Direction.OUTGOING)
    private List<SmileSample> smileSampleList;
    @Relationship(type = "IS_ALIAS", direction = Relationship.Direction.INCOMING)
    private List<PatientAlias>  patientAliases;

    public SmilePatient() {}

    public SmilePatient(String aliasValue, String aliasNamespace) {
        this.patientAliases = new ArrayList<>();
        patientAliases.add(new PatientAlias(aliasValue, aliasNamespace));
    }

    public UUID getSmilePatientId() {
        return smilePatientId;
    }

    public void setSmilePatientId(UUID smilePatientId) {
        this.smilePatientId = smilePatientId;
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

    public List<SmileSample> getSmileSampleList() {
        return smileSampleList;
    }

    public void setSmileSampleList(List<SmileSample> smileSampleList) {
        this.smileSampleList = smileSampleList;
    }

    /**
     * Add sample to array list.
     * @param smileSample
     */
    public void addSmileSample(SmileSample smileSample) {
        if (smileSampleList == null) {
            smileSampleList = new ArrayList<>();
        }
        smileSampleList.add(smileSample);
    }

    /**
     * Returns patient aliases list.
     * @return List
     */
    public List<PatientAlias> getPatientAliases() {
        if (patientAliases == null) {
            patientAliases = new ArrayList<>();
        }
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
     * Determines whether Patient has a patient alias matching the namespace provided.
     * @param patientAlias
     * @return Boolean
     */
    public Boolean hasPatientAlias(PatientAlias patientAlias) {
        if (patientAliases == null) {
            patientAliases = new ArrayList<>();
            return Boolean.FALSE;
        }
        for (PatientAlias alias : patientAliases) {
            if (alias.getNamespace().equalsIgnoreCase(patientAlias.getNamespace())) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    /**
     * Determines whether Patient has a patient alias matching the namespace provided.
     * @param patientAliasNamespace
     * @return Boolean
     */
    public Boolean hasPatientAlias(String patientAliasNamespace) {
        if (patientAliases == null) {
            patientAliases = new ArrayList<>();
            return Boolean.FALSE;
        }
        for (PatientAlias alias : patientAliases) {
            if (alias.getNamespace().equalsIgnoreCase(patientAliasNamespace)) {
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
