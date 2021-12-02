package org.mskcc.cmo.metadb.model;

import java.io.Serializable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;

import com.fasterxml.jackson.annotation.JsonAlias;

public class RequestMetadata implements Serializable, Comparable<RequestMetadata> {
    @Id @GeneratedValue
    private Long id;
    @JsonAlias("requestId")
    private String igoRequestId;
    private String requestMetadataJson;
    private String importDate;

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

    @Override
    public int compareTo(RequestMetadata requestMetadata) {
        if (getImportDate() == null || requestMetadata.getImportDate() == null) {
            return 0;
        }
        return getImportDate().compareTo(requestMetadata.getImportDate());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
