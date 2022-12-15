package org.mskcc.smile.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.Relationship;

public class RequestMetadata implements Serializable, Comparable<RequestMetadata> {
    @Id @GeneratedValue
    @JsonIgnore
    private Long id;
    private String igoRequestId;
    private String requestMetadataJson;
    private String importDate;
    @Relationship(type = "HAS_STATUS", direction = Relationship.OUTGOING)
    private Status status;

    /**
     * Default constructor.
     * @param igoRequestId
     * @param requestMetadataJson
     * @param importDate
     */
    public RequestMetadata(String igoRequestId, String requestMetadataJson, String importDate) {
        this.igoRequestId = igoRequestId;
        this.requestMetadataJson = requestMetadataJson;
        this.importDate = importDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIgoRequestId() {
        return igoRequestId;
    }

    public void setIgoRequestId(String igoRequestId) {
        this.igoRequestId = igoRequestId;
    }

    public String getRequestMetadataJson() {
        return requestMetadataJson;
    }

    public void setRequestMetadataJson(String requestMetadataJson) {
        this.requestMetadataJson = requestMetadataJson;
    }

    public String getImportDate() {
        return importDate;
    }

    public void setImportDate(String importDate) {
        this.importDate = importDate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public int compareTo(RequestMetadata requestMetadata) {
        if (importDate == null || requestMetadata.getImportDate() == null) {
            return 0;
        }
        if (id != null && importDate.equals(requestMetadata.getImportDate())) {
            return id.compareTo(requestMetadata.getId());
        }
        return importDate.compareTo(requestMetadata.getImportDate());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
