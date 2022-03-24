package org.mskcc.smile.model.web;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.typeconversion.UuidStringConverter;

/**
 *
 * @author ochoaa
 */
public class RequestSummary implements Serializable {
    @Convert(UuidStringConverter.class)
    private UUID smileRequestId;
    private String projectId;
    private String requestId;
    private String importDate;

    /**
     * RequestSummary default constructor.
     */
    public RequestSummary(){}

    /**
     * RequestSummary args constructor.
     * @param smileRequestId
     * @param projectId
     * @param requestId
     * @param importDate
     */
    public RequestSummary(UUID smileRequestId, String projectId, String requestId, String importDate) {
        this.smileRequestId = smileRequestId;
        this.projectId = projectId;
        this.requestId = requestId;
        this.importDate = importDate;
    }

    /**
     * RequestSummary cypher query results constructor.
     * @param values
     */
    public RequestSummary(List<String> values) {
        this.smileRequestId = UUID.fromString(values.get(0));
        this.projectId = values.get(1);
        this.requestId = values.get(2);
        this.importDate = values.get(3);
    }

    public UUID getSmileRequestId() {
        return smileRequestId;
    }

    public void setSmileRequestId(UUID smileRequestId) {
        this.smileRequestId = smileRequestId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getImportDate() {
        return importDate;
    }

    public void setImportDate(String importDate) {
        this.importDate = importDate;
    }
}
