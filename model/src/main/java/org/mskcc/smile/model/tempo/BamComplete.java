package org.mskcc.smile.model.tempo;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 *
 * @author ochoaa
 */
@NodeEntity
public class BamComplete {
    @Id @GeneratedValue
    private Long id;
    private String timestamp;
    private String status;

    public BamComplete() {}

    public BamComplete(String timestamp, String status) {
        this.timestamp = timestamp;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDate() {
        return timestamp;
    }

    public void setDate(String date) {
        this.timestamp = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
