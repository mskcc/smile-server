package org.mskcc.smile.model.tempo;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.smile.model.SmileSample;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

/**
 *
 * @author ochoaa
 */
@NodeEntity
public class Tempo {
    @Id @GeneratedValue
    private Long id;
    @Relationship(type = "HAS_EVENT", direction = Relationship.OUTGOING)
    private List<BamComplete> bamCompleteEvents;
    @Relationship(type = "HAS_EVENT", direction = Relationship.OUTGOING)
    private List<QcComplete> qcCompleteEvents;
    @Relationship(type = "HAS_TEMPO", direction = Relationship.INCOMING)
    private SmileSample sample;

    public Tempo() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns list of bam complete events.
     * @return
     */
    public List<BamComplete> getBamCompleteEvents() {
        if (bamCompleteEvents == null) {
            bamCompleteEvents = new ArrayList<>();
        }
        return bamCompleteEvents;
    }

    public void setBamCompleteEvents(List<BamComplete> bamCompleteEvents) {
        this.bamCompleteEvents = bamCompleteEvents;
    }

    /**
     * Adds a bam complete event to list.
     * @param bamComplete
     */
    public void addBamCompleteEvent(BamComplete bamComplete) {
        if (bamCompleteEvents == null) {
            bamCompleteEvents = new ArrayList<>();
        }
        bamCompleteEvents.add(bamComplete);
    }

    public Boolean hasBamCompleteEvent(BamComplete bamComplete) {
        if (bamCompleteEvents == null) {
            bamCompleteEvents = new ArrayList<>();
        }
        if (bamCompleteEvents.isEmpty()) {
            return Boolean.FALSE;
        }
        for (BamComplete event : bamCompleteEvents) {
            if (event.getDate().equalsIgnoreCase(bamComplete.getDate())
                    && event.getStatus().equalsIgnoreCase(bamComplete.getStatus())) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    /**
     * Returns list of qc complete events.
     * @return
     */
    public List<QcComplete> getQcCompleteEvents() {
        if (qcCompleteEvents == null) {
            qcCompleteEvents = new ArrayList<>();
        }
        return qcCompleteEvents;
    }

    public void setQcCompleteEvents(List<QcComplete> qcCompleteEvents) {
        this.qcCompleteEvents = qcCompleteEvents;
    }

    /**
     * Adds a qc complete event to list.
     * @param qcComplete
     */
    public void addQcCompleteEvent(QcComplete qcComplete) {
        if (qcCompleteEvents == null) {
            qcCompleteEvents = new ArrayList<>();
        }
        qcCompleteEvents.add(qcComplete);
    }

    public Boolean hasQcCompleteEvent(QcComplete qcComplete) {
        if (qcCompleteEvents == null) {
            qcCompleteEvents = new ArrayList<>();
        }
        if (qcCompleteEvents.isEmpty()) {
            return Boolean.FALSE;
        }
        for (QcComplete event : qcCompleteEvents) {
            if (event.getDate().equalsIgnoreCase(qcComplete.getDate())
                    && event.getStatus().equalsIgnoreCase(qcComplete.getStatus())
                    && event.getReason().equalsIgnoreCase(qcComplete.getReason())
                    && event.getResult().equalsIgnoreCase(qcComplete.getResult())) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    public SmileSample getSmileSample() {
        return sample;
    }

    public void setSmileSample(SmileSample sample) {
        this.sample = sample;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
