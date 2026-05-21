package org.mskcc.smile.model;

import java.io.Serializable;
import java.util.UUID;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.smile.model.json.DbGapJson;
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
    private UUID smileDbGapId;
    private String dbGapStudy;
    private String irbConsentProtocol;
    private String collectionStudy;
    private String dateOfConsent;
    private String genomicResearchUseStudy;
    private String consentVersion;

    public DbGap() {}

    /**
     * DbGap constructor.
     * @param dbGapJson
     */
    public DbGap(DbGapJson dbGapJson) {
        this.dbGapStudy = dbGapJson.getDbGapStudy();
        this.irbConsentProtocol = dbGapJson.getIrbConsentProtocol();
        this.collectionStudy = dbGapJson.getCollectionStudy();
        this.dateOfConsent = dbGapJson.getDateOfConsent();
        this.genomicResearchUseStudy = dbGapJson.getGenomicResearchUseStudy();
        this.consentVersion = dbGapJson.getConsentVersion();
    }

    /**
     * DbGap constructor.
     * @param dbGapStudy
     */
    public DbGap(String dbGapStudy) {
        this.dbGapStudy = dbGapStudy;
    }

    public UUID getSmileDgGapId() {
        return smileDbGapId;
    }

    public void setSmileDgGapId(UUID smileDbGapId) {
        this.smileDbGapId = smileDbGapId;
    }

    public String getDbGapStudy() {
        return dbGapStudy;
    }

    public void setDbGapStudy(String dbGapStudy) {
        this.dbGapStudy = dbGapStudy;
    }

    public String getIrbConsentProtocol() {
        return irbConsentProtocol;
    }

    public void setIrbConsentProtocol(String irbConsentProtocol) {
        this.irbConsentProtocol = irbConsentProtocol;
    }

    public String getCollectionStudy() {
        return collectionStudy;
    }

    public void setCollectionStudy(String collectionStudy) {
        this.collectionStudy = collectionStudy;
    }

    public String getDateOfConsent() {
        return dateOfConsent;
    }

    public void setDateOfConsent(String dateOfConsent) {
        this.dateOfConsent = dateOfConsent;
    }

    public String getGenomicResearchUseStudy() {
        return genomicResearchUseStudy;
    }

    public void setGenomicResearchUseStudy(String genomicResearchUseStudy) {
        this.genomicResearchUseStudy = genomicResearchUseStudy;
    }

    public String getConsentVersion() {
        return consentVersion;
    }

    public void setConsentVersion(String consentVersion) {
        this.consentVersion = consentVersion;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
