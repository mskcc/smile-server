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
    @Relationship(type = "PX_TO_SP", direction = Relationship.OUTGOING)
    private List<SampleManifestEntity> sampleManifestList;
    @Relationship(type = "PX_TO_NORMAL", direction = Relationship.OUTGOING)
    private List<NormalSampleManifestEntity> normalSampleManifestList;
    @Relationship(type = "PX_TO_PX", direction = Relationship.INCOMING)
    private List<Patient>  patientList;

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

    public List<NormalSampleManifestEntity> getNormalSampleManifestList() {
        return normalSampleManifestList;
    }

    public void setNormalSampleManifestList(List<NormalSampleManifestEntity> normalSampleManifestList) {
        this.normalSampleManifestList = normalSampleManifestList;
    }

    public List<Patient> getPatientList() {
        return patientList;
    }

    public void setPatientList(List<Patient> linkedPatientList) {
        this.patientList = linkedPatientList;
    }

    /**
     * Add patient to array list.
     * @param patient
     */
    public void addPatient(Patient patient) {
        if (patientList == null) {
            patientList = new ArrayList<>();
        }
        patientList.add(patient);
    }
    
    /**
     * Add normal sample to array list.
     * @param normalSampleManifestEntity
     */
    public void addNormalSample(NormalSampleManifestEntity normalSampleManifestEntity) {
        if (normalSampleManifestList == null) {
            normalSampleManifestList = new ArrayList<>();
        }
        normalSampleManifestList.add(normalSampleManifestEntity);
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
