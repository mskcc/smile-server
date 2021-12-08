package org.mskcc.cmo.metadb.model.igo;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author ochoaa
 */
public class IgoQcReport {
    public enum QcReportType {
        DNA, RNA, LIBRARY;
    }

    @JsonProperty("IGORecommendation")
    private String igoRecommendation;
    private String comments;
    private String investigatorDecision;
    private QcReportType qcReportType;

    public IgoQcReport(){}

    public QcReportType getQcReportType() {
        return qcReportType;
    }

    public void setQcReportType(QcReportType qcReportType) {
        this.qcReportType = qcReportType;
    }

    public String getIgoRecommendation() {
        return igoRecommendation;
    }

    public void setIgoRecommendation(String igoRecommendation) {
        this.igoRecommendation = igoRecommendation;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getInvestigatorDecision() {
        return investigatorDecision;
    }

    public void setInvestigatorDecision(String investigatorDecision) {
        this.investigatorDecision = investigatorDecision;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
