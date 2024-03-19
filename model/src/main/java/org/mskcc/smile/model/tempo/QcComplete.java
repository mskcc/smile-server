package org.mskcc.smile.model.tempo;

import java.io.Serializable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 *
 * @author ochoaa
 */
@NodeEntity
public class QcComplete implements Serializable {
    @Id @GeneratedValue
    private Long id;
    private String date;
    private String result;
    private String reason;
    private String status;

    public QcComplete() {}

    /**
     * Basic QcComplete constructor.
     * @param date
     * @param result
     * @param reason
     * @param status
     */
    public QcComplete(String date, String result, String reason, String status) {
        this.date = date;
        this.result = result;
        this.reason = reason;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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
