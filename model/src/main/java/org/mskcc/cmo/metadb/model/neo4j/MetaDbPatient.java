package org.mskcc.cmo.metadb.model.neo4j;

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

@NodeEntity
public class MetaDbPatient implements Serializable {
    @Id @GeneratedValue(strategy = UuidStrategy.class)
    @Convert(UuidStringConverter.class)
    private UUID uuid;
    private String investigatorPatientId;
    @Relationship(type = "HAS_SAMPLE", direction = Relationship.OUTGOING)
    private List<MetaDbSample> sampleManifestList;
    @Relationship(type = "IS_ALIAS", direction = Relationship.INCOMING)
    private List<PatientAlias>  patientAliases;

    public MetaDbPatient() {}

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

    public List<MetaDbSample> getSampleManifestList() {
        return sampleManifestList;
    }

    public void setSampleManifestList(List<MetaDbSample> sampleManifestList) {
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
     * @param patientAlias
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
    public void addSampleManifest(MetaDbSample sampleManifest) {
        if (sampleManifestList == null) {
            sampleManifestList = new ArrayList<>();
        }
        sampleManifestList.add(sampleManifest);
    }
}
