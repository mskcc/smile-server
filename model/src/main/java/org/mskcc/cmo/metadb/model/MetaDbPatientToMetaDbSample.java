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
public class MetaDbPatientToMetaDbSample implements Serializable {
    @Id @GeneratedValue
    private Long id;
    @StartNode
    private MetaDbPatient patient;
    @EndNode
    private MetaDbSample sampleMetadata;

    public MetaDbPatientToMetaDbSample() {}

    public MetaDbPatient getPatient() {
        return patient;
    }

    public void setPatient(MetaDbPatient patient) {
        this.patient = patient;
    }

    public MetaDbSample getSampleMetadata() {
        return sampleMetadata;
    }

    public void setSampleMetadata(MetaDbSample sampleMetadata) {
        this.sampleMetadata = sampleMetadata;
    }

}
