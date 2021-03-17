package org.mskcc.cmo.metadb.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;

/**
 *
 * @author ochoaa
 */
public class QcReport implements Serializable {
    public enum QcReportType {
        DNA, RNA, LIBRARY;
    }

    @Id @GeneratedValue
    private Long id;
    private QcReportType qcReportType;
    @JsonProperty("IGORecommendation")
    private String igoRecommendation;
    private String comments;
    private String investigatorDecision;

    public QcReport(){}

    /**
     * QcReport constructor.
     * @param qcReportType
     * @param igoRecommendation
     * @param comments
     * @param investigatorDecision
     */
    public QcReport(QcReportType qcReportType, String igoRecommendation,
            String comments, String investigatorDecision) {
        this.qcReportType = qcReportType;
        this.igoRecommendation = igoRecommendation;
        this.comments = comments;
        this.investigatorDecision = investigatorDecision;
    }

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
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((comments == null) ? 0 : comments.hashCode());
        result = prime * result + ((igoRecommendation == null) ? 0 : igoRecommendation.hashCode());
        result = prime * result + ((investigatorDecision == null) ? 0 : investigatorDecision.hashCode());
        result = prime * result + ((qcReportType == null) ? 0 : qcReportType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        QcReport other = (QcReport) obj;
        if (this.comments == null ? other.comments != null : !this.comments.equals(other.comments)) {
            return false;
        }
        if (this.igoRecommendation == null ? other.igoRecommendation != null :
                !this.igoRecommendation.equals(other.igoRecommendation)) {
            return false;
        }
        if (this.investigatorDecision == null ? other.investigatorDecision != null : !this.investigatorDecision.equals(other.investigatorDecision)) {
            return false;
        }
        if (this.qcReportType == null ? other.qcReportType != null : !this.qcReportType.equals(other.qcReportType)) {
            return false;
        }
        return true;
    }
    
    public boolean equalLists(List<QcReport> qcReportList) {
        for (QcReport qcReport: qcReportList) {
            if(!this.equals(qcReport)) {
                return false;
            }
        }
        return true;
    }
}
