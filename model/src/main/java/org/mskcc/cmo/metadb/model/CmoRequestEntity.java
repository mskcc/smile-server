package org.mskcc.cmo.metadb.model;

import java.util.List;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

/**
 *
 * @author ochoaa
 */
@NodeEntity(label = "cmo_metadb_request")
public class CmoRequestEntity {
    @Id
    private String requestId;
    @Relationship(type = "REQUEST_TO_SP", direction = Relationship.OUTGOING)
    private List<SampleManifestEntity> sampleManifestList;
    private String requestJson;

    public CmoRequestEntity() {}

    /**
     * CmoRequestEntity constructor.
     * @param requestId
     * @param sampleManifestList
     * @param requestJson 
     */
    public CmoRequestEntity(String requestId, List<SampleManifestEntity> sampleManifestList,
            String requestJson) {
        this.requestId = requestId;
        this.sampleManifestList = sampleManifestList;
        this.requestJson = requestJson;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public List<SampleManifestEntity> getSampleManifestList() {
        return sampleManifestList;
    }

    public void setSampleManifestList(List<SampleManifestEntity> sampleManifestList) {
        this.sampleManifestList = sampleManifestList;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public void setRequestJson(String requestJson) {
        this.requestJson = requestJson;
    }

}
