package org.mskcc.cmo.metadb.model;

import java.io.Serializable;
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

/**
 *
 * @author ochoaa
 */
@RelationshipEntity("PX_TO_NORMAL")
public class PatientToNormalSampleEntity implements Serializable {
    @Id @GeneratedValue
    private Long id;
    @StartNode
    private PatientMetadata patient;
    @EndNode
    private NormalSampleManifestEntity normalSampleManifestEntity;

    public PatientToNormalSampleEntity() {}

    public PatientMetadata getPatient() {
        return patient;
    }

    public void setPatient(PatientMetadata patient) {
        this.patient = patient;
    }

    public NormalSampleManifestEntity getSampleMetadata() {
        return normalSampleManifestEntity;
    }

    public void setSampleMetadata(NormalSampleManifestEntity normalSampleManifestEntity) {
        this.normalSampleManifestEntity = normalSampleManifestEntity;
    }

}
