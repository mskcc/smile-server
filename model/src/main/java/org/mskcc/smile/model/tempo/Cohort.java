package org.mskcc.smile.model.tempo;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.tempo.json.CohortCompleteJson;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

/**
 *
 * @author ochoaa
 */
@NodeEntity
public class Cohort implements Serializable {
    @Id @GeneratedValue
    private Long id;
    private String cohortId;
    //@Relationship(type = "HAS_COHORT_COMPLETE", direction = Relationship.OUTGOING)
    @Relationship(type = "HAS_COHORT_COMPLETE", direction = Relationship.Direction.OUTGOING)
    private List<CohortComplete> cohortCompleteList;
    //@Relationship(type = "HAS_COHORT_SAMPLE", direction = Relationship.OUTGOING)
    @Relationship(type = "HAS_COHORT_SAMPLE", direction = Relationship.Direction.OUTGOING)
    private List<SmileSample> cohortSamples;

    public Cohort() {}

    public Cohort(CohortCompleteJson ccJson) {
        this.cohortId = ccJson.getCohortId();
        addCohortComplete(new CohortComplete(ccJson));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCohortId() {
        return cohortId;
    }

    public void setCohortId(String cohortId) {
        this.cohortId = cohortId;
    }

    /**
     * Returns list of CohortComplete object instances.
     * @return
     */
    public List<CohortComplete> getCohortCompleteList() {
        if (cohortCompleteList == null) {
            this.cohortCompleteList = new ArrayList<>();
        }
        return cohortCompleteList;
    }

    /**
     * Adds instance of CohortComplete to cohortCompleteList.
     * @param cohortComplete
     */
    public final void addCohortComplete(CohortComplete cohortComplete) {
        if (cohortCompleteList == null) {
            this.cohortCompleteList = new ArrayList<>();
        }
        cohortCompleteList.add(cohortComplete);
    }

    public void setCohortCompleteList(List<CohortComplete> cohortCompleteList) {
        this.cohortCompleteList = cohortCompleteList;
    }

    /**
     * Returns list of SmileSample instances.
     * @return
     */
    public List<SmileSample> getCohortSamples() {
        if (cohortSamples == null) {
            this.cohortSamples = new ArrayList<>();
        }
        return cohortSamples;
    }

    /**
     * Adds instance of SmileSample to cohortSamples list.
     * @param sample
     */
    public final void addCohortSample(SmileSample sample) {
        if (cohortSamples == null) {
            this.cohortSamples = new ArrayList<>();
        }
        cohortSamples.add(sample);
    }

    public void setCohortSamples(List<SmileSample> cohortSamples) {
        this.cohortSamples = cohortSamples;
    }

    /**
     * Returns latest cohort complete data.
     * @return CohortComplete
     */
    public CohortComplete getLatestCohortComplete() {
        if (cohortCompleteList != null && !cohortCompleteList.isEmpty()) {
            if (cohortCompleteList.size() == 1) {
                return cohortCompleteList.get(0);
            }
            Collections.sort(cohortCompleteList);
            return cohortCompleteList.get(cohortCompleteList.size() - 1);
        }
        return null;
    }

    /**
     * Returns sample ids as set of strings.
     * @return Set
     * @throws ParseException
     */
    public Set<String> getCohortSamplePrimaryIds() throws ParseException {
        if (cohortSamples == null) {
            return new HashSet<>();
        }
        Set<String> sampleIds = new HashSet<>();
        for (SmileSample sample : cohortSamples) {
            sampleIds.add(sample.getPrimarySampleAlias());
        }
        return sampleIds;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
