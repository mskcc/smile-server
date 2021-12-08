package org.mskcc.cmo.metadb.model.igo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author ochoaa
 */
public class IgoRun {
    private String runMode;
    private String runId;
    private String flowCellId;
    private String readLength;
    private String runDate;
    private List<Integer> flowCellLanes;
    private List<String> fastqs;

    public IgoRun() {}

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
     * Returns flow cell lanes or empty array list if null.
     * @return List
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
    public void addFlowCellLane(Integer lane) {
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

}
