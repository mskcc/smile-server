package org.mskcc.smile.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author aochoa
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PooledSampleMetadataJson {
    @JsonProperty("sampleid")
    private String sampleId;
    @JsonProperty("patient_id")
    private String patientId;
    @JsonProperty("primerF")
    private String primerF;
    @JsonProperty("primerR")
    private String primerR;
    @JsonProperty("barcodeF")
    private String barcodeF;
    @JsonProperty("barcodeR")
    private String barcodeR;

    public PooledSampleMetadataJson() {}

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
