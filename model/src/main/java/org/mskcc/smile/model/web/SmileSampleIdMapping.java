package org.mskcc.smile.model.web;

import java.util.UUID;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.typeconversion.UuidStringConverter;

/**
 *
 * @author ochoaa
 */
public class SmileSampleIdMapping {
    @Convert(UuidStringConverter.class)
    private UUID smileSampleId;
    private String importDate;
    private String primaryId;
    private String cmoSampleName;

    /**
     * SmileSampleIdMapping constructor.
     */
    public SmileSampleIdMapping() {}

    public UUID getSmileSampleId() {
        return smileSampleId;
    }

    public void setSmileSampleId(UUID smileSampleId) {
        this.smileSampleId = smileSampleId;
    }

    public String getImportDate() {
        return importDate;
    }

    public void setImportDate(String importDate) {
        this.importDate = importDate;
    }

    public String getPrimaryId() {
        return primaryId;
    }

    public void setPrimaryId(String primaryId) {
        this.primaryId = primaryId;
    }

    public String getCmoSampleName() {
        return cmoSampleName;
    }

    public void setCmoSampleName(String cmoSampleName) {
        this.cmoSampleName = cmoSampleName;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
