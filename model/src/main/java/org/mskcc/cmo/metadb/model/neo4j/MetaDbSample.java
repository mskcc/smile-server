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

@NodeEntity
public class MetaDbSample implements Serializable {
    @Id @GeneratedValue(strategy = UuidStrategy.class)
    @Convert(UuidStringConverter.class)
    private UUID uuid;
    @Relationship(type = "IS_ALIAS", direction = Relationship.INCOMING)
    private List<SampleAlias> sampleAliases;
    @Relationship(type = "HAS_SAMPLE", direction = Relationship.INCOMING)
    private MetaDbPatient patient;
    @Relationship(type = "HAS_METADATA", direction = Relationship.OUTGOING)
    private List<SampleManifestEntity> sampleManifestList;
    private String sampleClass;

    public MetaDbSample() {
        super();
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setSampleAliases(List<SampleAlias> sampleAliases) {
        this.sampleAliases = sampleAliases;
    }

    public void getSampleAliases(List<SampleAlias> sampleAlias) {
        this.sampleAliases = sampleAlias;
    }

    /**
     * Add sample to array.
     * @param sampleAlias
     */
    public void addSample(SampleAlias sampleAlias) {
        if (sampleAliases == null) {
            sampleAliases = new ArrayList<>();
        }
        sampleAliases.add(sampleAlias);
    }

    public MetaDbPatient getPatient() {
        return patient;
    }

    public void setPatient(MetaDbPatient patient) {
        this.patient = patient;
    }

    public void setSampleManifestList(List<SampleManifestEntity> sampleManifestList) {
        this.sampleManifestList = sampleManifestList;
    }

    public List<SampleManifestEntity> getSampleManifestList() {
        return sampleManifestList;
    }

    /**
     *
     * @param sampleManifestEntity
     */
    public void addSampleManifest(SampleManifestEntity sampleManifestEntity) {
        if (sampleManifestList == null) {
            sampleManifestList = new ArrayList<>();
        }
        sampleManifestList.add(sampleManifestEntity);
    }

    public void setPatientUuid(UUID uuid) {
        this.patient.setUuid(uuid);
    }

    /**
     *
     * @return SampleIgoId
     */
    public SampleAlias getSampleIgoId() {
        if (sampleAliases == null) {
            this.sampleAliases = new ArrayList<>();
        }
        for (SampleAlias s: sampleAliases) {
            if (s.getIdSource() == "igoId") {
                return s;
            }
        }
        return null;
    }

    public String getSampleClass() {
        return sampleClass;
    }

    public void setSampleClass(String sampleClass) {
        this.sampleClass = sampleClass;
    }

}
