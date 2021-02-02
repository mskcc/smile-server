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

@NodeEntity(label = "cmo_metadb_patient_metadata")
public class PatientMetadata implements Serializable {
    @Id @GeneratedValue(strategy = UuidStrategy.class)
    @Convert(UuidStringConverter.class)
    private UUID uuid;
    private String investigatorPatientId;
    @Relationship(type = "HAS_SAMPLE", direction = Relationship.OUTGOING)
    private List<SampleManifestEntity> sampleManifestList;
    @Relationship(type = "IS_ALIAS", direction = Relationship.INCOMING)
    private List<PatientAlias>  patientAliases;

    public PatientMetadata() {}

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getInvestigatorPatientId() {
        return investigatorPatientId;
    }

    public void setInvestigatorPatientId(String investigatorPatientId) {
        this.investigatorPatientId = investigatorPatientId;
    }

    public List<SampleManifestEntity> getSampleManifestList() {
        return sampleManifestList;
    }

    public void setSampleManifestList(List<SampleManifestEntity> sampleManifestList) {
        this.sampleManifestList = sampleManifestList;
    }

    public List<PatientAlias> getPatientAliases() {
        return patientAliases;
    }

    public void setPatientAliases(List<PatientAlias> patientAliases) {
        this.patientAliases = patientAliases;
    }

    /**
     * Add patient to array list.
     * @param patient
     */
    public void addPatientAlias(PatientAlias patientAlias) {
        if (patientAliases == null) {
            patientAliases = new ArrayList<>();
        }
        patientAliases.add(patientAlias);
    }

    /**
     * Add sample to array list.
     * @param sampleManifest
     */
    public void addSampleManifest(SampleManifestEntity sampleManifest) {
        if (sampleManifestList == null) {
            sampleManifestList = new ArrayList<>();
        }
        sampleManifestList.add(sampleManifest);
    }
}
