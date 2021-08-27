package org.mskcc.cmo.metadb.model;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;

public class RequestMetadata {
    @Id @GeneratedValue
    private int metaDbRequestId;
    private String requestMetadataJSON;
    private String importDate;
    
    public RequestMetadata(String requestMetadataJSON, String importDate) {
        this.requestMetadataJSON = requestMetadataJSON;
        this.importDate = importDate;
    }
    
    public String getRequestMetadataJSON() {
        return requestMetadataJSON;
    }
    
    public void setRequestMetadataJSON(String requestMetadataJSON) {
        this.requestMetadataJSON = requestMetadataJSON;
    }
    
    public String getImportDate() {
        return importDate;
    }
    
    public void setImportDate(String importDate) {
        this.importDate = importDate;
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
