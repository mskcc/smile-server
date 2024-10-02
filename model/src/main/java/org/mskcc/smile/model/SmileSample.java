package org.mskcc.smile.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.smile.model.tempo.Tempo;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.id.UuidStrategy;
import org.neo4j.ogm.typeconversion.UuidStringConverter;
//import org.springframework.data.neo4j.core.schema.GeneratedValue;
//import org.springframework.data.neo4j.core.schema.Id;
//import org.springframework.data.neo4j.core.schema.Node;
//import org.springframework.data.neo4j.core.schema.Relationship;
//import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@NodeEntity(label = "Sample")
//@Node("Sample")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SmileSample implements Serializable {
    @Id @GeneratedValue(strategy = UuidStrategy.class)
    //@Id @GeneratedValue(UUIDStringGenerator.class)
    @Convert(UuidStringConverter.class)
    private UUID smileSampleId;
    //@Relationship(type = "IS_ALIAS", direction = Relationship.INCOMING)
    @Relationship(type = "IS_ALIAS", direction = Relationship.Direction.INCOMING)
    private List<SampleAlias> sampleAliases;
    //@Relationship(type = "HAS_SAMPLE", direction = Relationship.INCOMING)
    @Relationship(type = "HAS_SAMPLE", direction = Relationship.Direction.INCOMING)
    private SmilePatient patient;
    //@Relationship(type = "HAS_METADATA", direction = Relationship.OUTGOING)
    @Relationship(type = "HAS_METADATA", direction = Relationship.Direction.OUTGOING)
    private List<SampleMetadata> sampleMetadataList;
    //@Relationship(type = "HAS_TEMPO", direction = Relationship.OUTGOING)
    @Relationship(type = "HAS_TEMPO", direction = Relationship.Direction.OUTGOING)
    private Tempo tempo;
    private String sampleClass;
    private String sampleCategory;
    private String datasource;
    private Boolean revisable;

    public SmileSample() {}

    public UUID getSmileSampleId() {
        return smileSampleId;
    }

    public void setSmileSampleId(UUID smileSampleId) {
        this.smileSampleId = smileSampleId;
    }

    public void setSampleAliases(List<SampleAlias> sampleAliases) {
        this.sampleAliases = sampleAliases;
    }

    public List<SampleAlias> getSampleAliases() {
        return sampleAliases;
    }

    /**
     * Add sample to array.
     * @param sampleAlias
     */
    public void addSampleAlias(SampleAlias sampleAlias) {
        if (sampleAliases == null) {
            sampleAliases = new ArrayList<>();
        }
        sampleAliases.add(sampleAlias);
    }

    /**
     * Updates value of sampleAlias for a given namespace
     * @param namespace
     * @param value
     */
    public void updateSampleAlias(String namespace, String value) {
        if (sampleAliases != null) {
            for (SampleAlias sa: sampleAliases) {
                if (sa.getNamespace().equals(namespace)) {
                    sa.setValue(value);
                    break;
                }
            }
        }
    }

    public SmilePatient getPatient() {
        return patient;
    }

    public void setPatient(SmilePatient patient) {
        this.patient = patient;
    }

    public void setSampleMetadataList(List<SampleMetadata> sampleMetadataList) {
        this.sampleMetadataList = sampleMetadataList;
    }

    /**
     * Returns sorted SampleMetadata list.
     * @return List
     */
    public List<SampleMetadata> getSampleMetadataList() {
        if (sampleMetadataList == null) {
            sampleMetadataList = new ArrayList<>();
        }
        Collections.sort(sampleMetadataList);
        return sampleMetadataList;
    }

    /**
     *
     * @param sampleMetadata
     */
    public void addSampleMetadata(SampleMetadata sampleMetadata) {
        if (sampleMetadataList == null) {
            sampleMetadataList = new ArrayList<>();
        }
        sampleMetadataList.add(sampleMetadata);
    }

    /**
     *
     * @return SamplePrimaryId
     * @throws ParseException
     */
    public String getPrimarySampleAlias() throws ParseException {
        if (sampleAliases == null) {
            sampleAliases = new ArrayList<>();
        }
        SampleMetadata latestMetadata = getLatestSampleMetadata();
        for (SampleAlias s: sampleAliases) {
            if (s.getValue().equalsIgnoreCase(latestMetadata.getPrimaryId())) {
                return s.getValue();
            }
        }
        return null;
    }

    public String getSampleClass() {
        return sampleClass;
    }

    public void setSampleClass(String sampleClass) {
        this.sampleClass = sampleClass;
    }

    /**
     * Returns the latest SampleMetadata based on the import date.
     * @return SampleMetadata
     * @throws ParseException
     */
    public SampleMetadata getLatestSampleMetadata() throws ParseException {
        if (sampleMetadataList != null && !sampleMetadataList.isEmpty()) {
            if (sampleMetadataList.size() == 1) {
                return sampleMetadataList.get(0);
            }
            Collections.sort(sampleMetadataList);
            return sampleMetadataList.get(sampleMetadataList.size() - 1);
        }
        return null;
    }

    /**
     * Applies IGO LIMS updates for the following fields
     * @param sampleMetadata
     * @return
     */
    public void applyIgoLimsUpdates(SampleMetadata sampleMetadata) throws ParseException {
        SampleMetadata latestSampleMetadata = getLatestSampleMetadata();

        sampleMetadata.setId(null);
        sampleMetadata.setCmoPatientId(latestSampleMetadata.getCmoPatientId());
        sampleMetadata.setInvestigatorSampleId(latestSampleMetadata.getInvestigatorSampleId());
        sampleMetadata.setInvestigatorSampleId(latestSampleMetadata.getInvestigatorSampleId());
        sampleMetadata.setSampleName(latestSampleMetadata.getSampleName());
        sampleMetadata.setCmoInfoIgoId(latestSampleMetadata.getCmoInfoIgoId());
        sampleMetadata.setOncotreeCode(latestSampleMetadata.getOncotreeCode());
        sampleMetadata.setCollectionYear(latestSampleMetadata.getCollectionYear());
        sampleMetadata.setTubeId(latestSampleMetadata.getTubeId());
        sampleMetadata.setSpecies(latestSampleMetadata.getSpecies());
        sampleMetadata.setSex(latestSampleMetadata.getSex());
        sampleMetadata.setTumorOrNormal(latestSampleMetadata.getTumorOrNormal());
        sampleMetadata.setSampleType(latestSampleMetadata.getSampleType());
        sampleMetadata.setPreservation(latestSampleMetadata.getPreservation());
        sampleMetadata.setSampleClass(latestSampleMetadata.getSampleClass());
        sampleMetadata.setSampleOrigin(latestSampleMetadata.getSampleOrigin());
        sampleMetadata.setTissueLocation(latestSampleMetadata.getTissueLocation());
        sampleMetadata.setGenePanel(latestSampleMetadata.getGenePanel());
        sampleMetadata.setIgoComplete(latestSampleMetadata.getIgoComplete());

        addSampleMetadata(sampleMetadata);
    }

    public void updateSampleMetadata(SampleMetadata sampleMetadata) throws ParseException {
        sampleMetadata.setId(null);
        addSampleMetadata(sampleMetadata);
    }

    public String getSampleCategory() {
        return sampleCategory;
    }

    public void setSampleCategory(String sampleCategory) {
        this.sampleCategory = sampleCategory;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public Boolean getRevisable() {
        return revisable;
    }

    public void setRevisable(Boolean revisable) {
        this.revisable = revisable;
    }

    public Tempo getTempo() {
        return tempo;
    }

    public void setTempo(Tempo tempo) {
        this.tempo = tempo;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
