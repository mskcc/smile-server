package org.mskcc.smile.model;

import java.io.Serializable;
import java.util.UUID;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.smile.model.json.PooledSampleMetadataJson;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.id.UuidStrategy;
import org.neo4j.ogm.typeconversion.UuidStringConverter;

/**
 * Node entity representing a single patient/sample entry that is part of a
 * multiplexed/pooled sample (e.g., pooled microbiome samples). Each PooledSample
 * links back to the demultiplexed Sample node it belongs to, as well as the
 * Patient that it was derived from.
 * @author ochoaa
 */
@NodeEntity(label = "PooledSample")
public class PooledSample implements Serializable {
    @Id @GeneratedValue(strategy = UuidStrategy.class)
    @Convert(UuidStringConverter.class)
    private UUID smilePooledSampleId;
    private String sampleId;
    private String poolId;
    private String poolDataType;
    private String primerF;
    private String primerR;
    private String barcodeF;
    private String barcodeR;

    @Relationship(type = "IS_POOLED_SAMPLE", direction = Relationship.Direction.OUTGOING)
    private SmileSample sample;
    @Relationship(type = "HAS_POOLED_SAMPLE", direction = Relationship.Direction.INCOMING)
    private SmilePatient patient;

    public PooledSample() {}

    public PooledSample(String poolId, String poolDataType, PooledSampleMetadataJson sampleJson) {
        this.poolId = poolId;
        this.poolDataType = poolDataType;
        this.sampleId = sampleJson.getSampleId();
        this.primerF = sampleJson.getPrimerF();
        this.primerR = sampleJson.getPrimerR();
        this.barcodeF = sampleJson.getBarcodeF();
        this.barcodeR = sampleJson.getBarcodeR();
    }

    public UUID getSmilePooledSampleId() {
        return smilePooledSampleId;
    }

    public void setSmilePooledSampleId(UUID smilePooledSampleId) {
        this.smilePooledSampleId = smilePooledSampleId;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getPoolId() {
        return poolId;
    }

    public void setPoolId(String poolId) {
        this.poolId = poolId;
    }

    public String getPoolDataType() {
        return poolDataType;
    }

    public void setPoolDataType(String poolDataType) {
        this.poolDataType = poolDataType;
    }

    public String getPrimerF() {
        return primerF;
    }

    public void setPrimerF(String primerF) {
        this.primerF = primerF;
    }

    public String getPrimerR() {
        return primerR;
    }

    public void setPrimerR(String primerR) {
        this.primerR = primerR;
    }

    public String getBarcodeF() {
        return barcodeF;
    }

    public void setBarcodeF(String barcodeF) {
        this.barcodeF = barcodeF;
    }

    public String getBarcodeR() {
        return barcodeR;
    }

    public void setBarcodeR(String barcodeR) {
        this.barcodeR = barcodeR;
    }

    public SmileSample getSample() {
        return sample;
    }

    public void setSample(SmileSample sample) {
        this.sample = sample;
    }

    public SmilePatient getPatient() {
        return patient;
    }

    public void setPatient(SmilePatient patient) {
        this.patient = patient;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
