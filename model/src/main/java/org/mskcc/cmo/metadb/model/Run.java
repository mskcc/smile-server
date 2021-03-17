package org.mskcc.cmo.metadb.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;

/**
 *
 * @author ochoaa
 */

public class Run implements Serializable {
    @Id @GeneratedValue
    private Long id;
    private String runMode;
    private String runId;
    private String flowCellId;
    private String readLength;
    private String runDate;
    private List<Integer> flowCellLanes;
    private List<String> fastqs;

    public Run(){}

    /**
     * Run constructor.
     * @param runId
     * @param flowCellId
     * @param runDate
     */
    public Run(String runId, String flowCellId, String runDate) {
        this.runId = runId;
        this.flowCellId = flowCellId;
        this.runDate = runDate;
    }

    /**
     * Run constructor.
     * @param runMode
     * @param runId
     * @param flowCellId
     * @param readLength
     * @param runDate
     */
    public Run(String runMode, String runId, String flowCellId, String readLength, String runDate) {
        this.runMode = runMode;
        this.runId = runId;
        this.flowCellId = flowCellId;
        this.readLength = readLength;
        this.runDate = runDate;
    }

    /**
     * Run constructor.
     * @param fastqs
     */
    public Run(List<String> fastqs) {
        this.fastqs = fastqs;
    }

    public String getRunMode() {
        return runMode;
    }

    public void setRunMode(String runMode) {
        this.runMode = runMode;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getFlowCellId() {
        return flowCellId;
    }

    public void setFlowCellId(String flowCellId) {
        this.flowCellId = flowCellId;
    }

    public String getReadLength() {
        return readLength;
    }

    public void setReadLength(String readLength) {
        this.readLength = readLength;
    }

    public String getRunDate() {
        return runDate;
    }

    public void setRunDate(String runDate) {
        this.runDate = runDate;
    }

    /**
     * Returns empty array list if field is null.
     * @return
     */
    public List<Integer> getFlowCellLanes() {
        if (flowCellLanes ==  null) {
            this.flowCellLanes = new ArrayList<>();
        }
        return flowCellLanes;
    }

    public void setFlowCellLanes(List<Integer> flowCellLanes) {
        this.flowCellLanes = flowCellLanes;
    }

    /**
     * Adds lane to flow cell lanes and sorts.
     * @param lane
     */
    public void addLane(Integer lane) {
        if (flowCellLanes == null) {
            this.flowCellLanes = new ArrayList<>();
        }
        flowCellLanes.add(lane);
        Collections.sort(flowCellLanes);
    }

    public List<String> getFastqs() {
        return fastqs;
    }

    public void setFastqs(List<String> fastqs) {
        this.fastqs = fastqs;
    }

    /**
     * Adds FastQ to list.
     * @param fastq
     */
    public void addFastq(String fastq) {
        if (fastqs == null) {
            this.fastqs = new ArrayList<>();
        }
        fastqs.add(fastq);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fastqs == null) ? 0 : fastqs.hashCode());
        result = prime * result + ((flowCellId == null) ? 0 : flowCellId.hashCode());
        result = prime * result + ((flowCellLanes == null) ? 0 : flowCellLanes.hashCode());
        result = prime * result + ((readLength == null) ? 0 : readLength.hashCode());
        result = prime * result + ((runDate == null) ? 0 : runDate.hashCode());
        result = prime * result + ((runId == null) ? 0 : runId.hashCode());
        result = prime * result + ((runMode == null) ? 0 : runMode.hashCode());
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
        Run other = (Run) obj;
        if (this.fastqs == null ? other.fastqs != null : !this.fastqs.equals(other.fastqs)) {
                return false;
        }
        if (this.flowCellId == null ? other.flowCellId != null : !this.flowCellId.equals(other.flowCellId)) {
            return false;
        }
        if (this.flowCellLanes == null ? other.flowCellLanes != null : !this.flowCellLanes.equals(other.flowCellLanes)) {
            return false;
        }
        if (this.readLength == null ? other.readLength != null : !this.readLength.equals(other.readLength)) {
            return false;
        }
        if (this.runDate == null ? other.runDate != null : !this.runDate.equals(other.runDate)) {
            return false;
        }
        if (this.runId == null ? other.runId != null : !this.runId.equals(other.runId)) {
            return false;
        }
        if (this.runMode == null ? other.runMode != null : !this.runMode.equals(other.runMode)) {
            return false;
        }
        return true;
    }
    
    public boolean equalLists(List<Run> runList) {
        for (Run run: runList) {
            if(!this.equals(run)) {
                return false;
            }
        }
        return true;
    }
}
