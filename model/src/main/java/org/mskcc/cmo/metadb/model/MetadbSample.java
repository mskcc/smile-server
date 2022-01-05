package org.mskcc.cmo.metadb.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.id.UuidStrategy;
import org.neo4j.ogm.typeconversion.UuidStringConverter;

@NodeEntity(label = "Sample")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetadbSample implements Serializable {
    @Id @GeneratedValue(strategy = UuidStrategy.class)
    @Convert(UuidStringConverter.class)
    private UUID metaDbSampleId;
    @Relationship(type = "IS_ALIAS", direction = Relationship.INCOMING)
    private List<SampleAlias> sampleAliases;
    @Relationship(type = "HAS_SAMPLE", direction = Relationship.INCOMING)
    private MetadbPatient patient;
    @Relationship(type = "HAS_METADATA", direction = Relationship.OUTGOING)
    private List<SampleMetadata> sampleMetadataList;
    private String sampleClass;
    private String sampleCategory;
    private String datasource;

    public MetadbSample() {}

    public UUID getMetaDbSampleId() {
        return metaDbSampleId;
    }

    public void setMetaDbSampleId(UUID metaDbSampleId) {
        this.metaDbSampleId = metaDbSampleId;
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

    public MetadbPatient getPatient() {
        return patient;
    }

    public void setPatient(MetadbPatient patient) {
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
            Collections.sort(sampleMetadataList);
            return sampleMetadataList.get(sampleMetadataList.size() - 1);
        }
        return null;
    }

    public void updateSampleMetadata(SampleMetadata sampleMetadata) throws ParseException {
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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
