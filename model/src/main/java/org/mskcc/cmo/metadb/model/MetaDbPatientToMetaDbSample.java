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
    private MetaDbPatient metaDbPatient;
    @EndNode
    private MetaDbSample metaDbSample;

    public MetaDbPatientToMetaDbSample() {}

    public MetaDbPatient getMetaDbPatient() {
        return metaDbPatient;
    }

    public void setMetaDbPatient(MetaDbPatient metaDbPatient) {
        this.metaDbPatient = metaDbPatient;
    }

    public MetaDbSample getMetaDbSample() {
        return metaDbSample;
    }

    public void setMetaDbSample(MetaDbSample metaDbSample) {
        this.metaDbSample = metaDbSample;
    }

}
