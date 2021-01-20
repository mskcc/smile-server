package org.mskcc.cmo.metadb.model;

import java.util.ArrayList;
import java.util.List;
import org.mskcc.cmo.shared.IgoRequest;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

/**
 *
 * @author ochoaa
 */
@NodeEntity(label = "cmo_metadb_request")
public class CmoRequestEntity extends IgoRequest {
    @Id @GeneratedValue
    private Long id;
    @Relationship(type = "REQUEST_TO_SP", direction = Relationship.OUTGOING)
    private List<SampleManifestEntity> sampleManifestList;
    @Relationship(type = "PR_TO_REQUEST", direction = Relationship.INCOMING)
    private CmoProjectEntity projectEntity;
    private String requestJson;

    public CmoRequestEntity() {}

    /**
     * CmoRequestEntity constructor.
     * @param requestId
     * @param sampleManifestList
     * @param requestJson
     */
    public CmoRequestEntity(String requestId, List<SampleManifestEntity> sampleManifestList,
            CmoProjectEntity projectEntity, String requestJson) {
        super(requestId);
        this.sampleManifestList = sampleManifestList;
        this.projectEntity = projectEntity;
        this.requestJson = requestJson;
    }

    /**
     * CmoRequestEntity constructor
     * @param requestId
     * @param recipe
     * @param projectManagerName
     * @param piEmail
     * @param labHeadName
     * @param labHeadEmail
     * @param investigatorName
     * @param investigatorEmail
     * @param dataAnalystName
     * @param dataAnalystEmail
     * @param otherContactEmails
     * @param dataAccessEmails
     * @param qcAccessEmails
     * @param strand
     * @param libraryType
     * @param sampleManifestList
     * @param projectEntity
     * @param requestJson
     */
    public CmoRequestEntity(String requestId, String recipe, String projectManagerName,
            String piEmail, String labHeadName, String labHeadEmail,
            String investigatorName, String investigatorEmail, String dataAnalystName,
            String dataAnalystEmail, String otherContactEmails, String dataAccessEmails,
            String qcAccessEmails, String strand, String libraryType,
            List<SampleManifestEntity> sampleManifestList,
            CmoProjectEntity projectEntity, String requestJson) {
        super(requestId,
                recipe,
                projectManagerName,
                piEmail,
                labHeadName,
                labHeadEmail,
                investigatorName,
                investigatorEmail,
                dataAnalystName,
                dataAnalystEmail,
                otherContactEmails,
                dataAccessEmails,
                qcAccessEmails,
                strand,
                libraryType);
        this.sampleManifestList = sampleManifestList;
        this.projectEntity = projectEntity;
        this.requestJson = requestJson;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<SampleManifestEntity> getSampleManifestList() {
        return sampleManifestList;
    }

    public void setSampleManifestList(List<SampleManifestEntity> sampleManifestList) {
        this.sampleManifestList = sampleManifestList;
    }

    public CmoProjectEntity getProjectEntity() {
        return projectEntity;
    }

    public void setProjectEntity(CmoProjectEntity projectEntity) {
        this.projectEntity = projectEntity;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public void setRequestJson(String requestJson) {
        this.requestJson = requestJson;
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
}
