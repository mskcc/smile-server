package org.mskcc.cmo.metadb.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.cmo.metadb.model.igo.IgoQcReport;
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

    public QcReport() {}

    /**
     * QcReport constructor.
     * TODO: Decide whether to keep or just
     * make the List of 'QcReport' a string for
     * SampleMetadata.qcReports since we are
     * storing in the graph db as a string anyway.
     * Replacing 'QcReport' with string will allow us to remove
     * the QcReportsStringConverter
     * @param igoQcReport
     */
    public QcReport(IgoQcReport igoQcReport) {
        this.qcReportType = QcReportType.valueOf(igoQcReport.getQcReportType().name());
        this.igoRecommendation = igoQcReport.getIgoRecommendation();
        this.comments = igoQcReport.getComments();
        this.investigatorDecision = igoQcReport.getInvestigatorDecision();
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

}
