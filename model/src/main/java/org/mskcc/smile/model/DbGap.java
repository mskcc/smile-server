package org.mskcc.smile.model;

import java.io.Serializable;
import java.util.UUID;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.id.UuidStrategy;
import org.neo4j.ogm.typeconversion.UuidStringConverter;

@NodeEntity
public class DbGap implements Serializable {
    @Id @GeneratedValue(strategy = UuidStrategy.class)
    @Convert(UuidStringConverter.class)
    private UUID smileDgGapId;
    private String dbGapStudy;

    public DbGap() {}

    /**
     * DbGap constructor.
     * @param dbGapStudy
     */
    public DbGap(String dbGapStudy) {
        this.dbGapStudy = dbGapStudy;
    }

    public UUID getSmileDgGapId() {
        return smileDgGapId;
    }

    public String getDbGapStudy() {
        return dbGapStudy;
    }

    public void setDbGapStudy(String dbGapStudy) {
        this.dbGapStudy = dbGapStudy;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
