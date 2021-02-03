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
@RelationshipEntity("HAS_SAMPLE")
public class PatientToSampleEntity implements Serializable {
    @Id @GeneratedValue
    private Long id;
    @StartNode
    private PatientMetadata patient;
    @EndNode
    private SampleManifestEntity sampleMetadata;

    public PatientToSampleEntity() {}

    public PatientMetadata getPatient() {
        return patient;
    }

    public void setPatient(PatientMetadata patient) {
        this.patient = patient;
    }

    public SampleManifestEntity getSampleMetadata() {
        return sampleMetadata;
    }

    public void setSampleMetadata(SampleManifestEntity sampleMetadata) {
        this.sampleMetadata = sampleMetadata;
    }

}
