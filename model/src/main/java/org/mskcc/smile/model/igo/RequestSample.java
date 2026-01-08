package org.mskcc.smile.model.igo;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import org.apache.commons.lang.builder.ToStringBuilder;

public class RequestSample implements Serializable {
    private String investigatorSampleId;
    private String igoSampleId;
    private boolean igoComplete;
    private String sampleStatus;

    public RequestSample() {}

    /**
     * RequestSample constructor
     * @param investigatorSampleId
     * @param igoSampleId
     * @param igoComplete
     * @param sampleStatus
     */
    public RequestSample(String investigatorSampleId, String igoSampleId, boolean igoComplete, String sampleStatus) {
        this.investigatorSampleId = investigatorSampleId;
        this.igoSampleId = igoSampleId;
        this.igoComplete = igoComplete;
        this.sampleStatus = sampleStatus;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getInvestigatorSampleId() {
        return investigatorSampleId;
    }

    public void setInvestigatorSampleId(String investigatorSampleId) {
        this.investigatorSampleId = investigatorSampleId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getIgoSampleId() {
        return igoSampleId;
    }

    public void setIgoSampleId(String igoSampleId) {
        this.igoSampleId = igoSampleId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public boolean isIgoComplete() {
        return igoComplete;
    }

    public void setIgoComplete(boolean igoComplete) {
        this.igoComplete = igoComplete;
    }

    public String getSampleStatus() {
        return sampleStatus;
    }

    public void setSampleStatus(String sampleStatus) {
        this.sampleStatus = sampleStatus;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
