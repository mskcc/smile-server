package org.mskcc.smile.model.tempo;

import java.io.Serializable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 * Represents a MAF Complete entity.
 * @author qu8n
 */
@NodeEntity(label = "MafComplete")
public class MafComplete implements Serializable {
    @Id @GeneratedValue
    private Long id;
    private String date;
    private String normalPrimaryId;
    private String status;

    public MafComplete() {}

    /**
     * Constructor for MafComplete.
     * @param date
     * @param normalPrimaryId
     * @param status
     */
    public MafComplete(String date, String normalPrimaryId, String status) {
        this.date = date;
        this.normalPrimaryId = normalPrimaryId;
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

    public String getNormalPrimaryId() {
        return normalPrimaryId;
    }

    public void setNormalPrimaryId(String normalPrimaryId) {
        this.normalPrimaryId = normalPrimaryId;
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
