package org.mskcc.cmo.metadb.model;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;

public class RequestMetadata {
    @Id @GeneratedValue
    private Long id;
    private String requestMetadataJson;
    private String importDate;
    
    /**
     * 
     * @param requestMetadataJson
     * @param importDate
     */
    public RequestMetadata(String requestMetadataJson, String importDate) {
        this.requestMetadataJson = requestMetadataJson;
        this.importDate = importDate;
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
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
