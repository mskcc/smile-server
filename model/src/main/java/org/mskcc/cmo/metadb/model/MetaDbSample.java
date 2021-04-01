package org.mskcc.cmo.metadb.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.neo4j.driver.internal.shaded.io.netty.util.internal.StringUtil;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.id.UuidStrategy;
import org.neo4j.ogm.typeconversion.UuidStringConverter;

@NodeEntity(label = "Sample")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetaDbSample implements Serializable {
    @Id @GeneratedValue(strategy = UuidStrategy.class)
    @Convert(UuidStringConverter.class)
    private UUID metaDbSampleId;
    @Relationship(type = "IS_ALIAS", direction = Relationship.INCOMING)
    private List<SampleAlias> sampleAliases;
    @Relationship(type = "HAS_SAMPLE", direction = Relationship.INCOMING)
    private MetaDbPatient patient;
    @Relationship(type = "HAS_METADATA", direction = Relationship.OUTGOING)
    private List<SampleMetadata> sampleMetadataList;
    private String sampleClass;

    public MetaDbSample() {}

    public UUID getMetaDbSampleId() {
        return metaDbSampleId;
    }

    public void setMetaDbSampleId(UUID metaDbSampleId) {
        this.metaDbSampleId = metaDbSampleId;
    }

    public void setSampleAliases(List<SampleAlias> sampleAliases) {
        this.sampleAliases = sampleAliases;
    }

    public void getSampleAliases(List<SampleAlias> sampleAlias) {
        this.sampleAliases = sampleAlias;
    }

    /**
     * Add sample to array.
     * @param sampleAlias
     */
    public void addSample(SampleAlias sampleAlias) {
        if (sampleAliases == null) {
            sampleAliases = new ArrayList<>();
        }
        sampleAliases.add(sampleAlias);
    }

    public MetaDbPatient getPatient() {
        return patient;
    }

    public void setPatient(MetaDbPatient patient) {
        this.patient = patient;
    }

    public void setSampleMetadataList(List<SampleMetadata> sampleMetadataList) {
        this.sampleMetadataList = sampleMetadataList;
    }

    public List<SampleMetadata> getSampleMetadataList() {
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

    public void setPatientUuid(UUID uuid) {
        this.patient.setMetaDbPatientId(uuid);
    }

    /**
     *
     * @return SampleIgoId
     */
    public SampleAlias getSampleIgoId() {
        if (sampleAliases == null) {
            this.sampleAliases = new ArrayList<>();
        }
        for (SampleAlias s: sampleAliases) {
            if (s.getNamespace().equalsIgnoreCase("igoId")) {
                return s;
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
            LocalDate latest = null;
            SampleMetadata smLatest = null;
            for (int i = 0; i < sampleMetadataList.size(); i++) {
                SampleMetadata sm = sampleMetadataList.get(i);
                // if null or empty import date then set it to current date
                if (StringUtil.isNullOrEmpty(sm.getImportDate())) {
                    sm.setImportDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
                    sampleMetadataList.set(i, sm);
                }
                // compare current date with 'latest' date encountered
                // let's not assume that the most recent sample metadata will
                // always be the first element in the sampleMetadataList
                LocalDate current = LocalDate.parse(sm.getImportDate(),
                        DateTimeFormatter.ISO_LOCAL_DATE);
                if (latest == null) {
                    latest = current;
                    smLatest = sm;
                } else if (current.isAfter(latest)) {
                    // if current is later than the 'latest' then update 'latest' date and sample metadata
                    latest = current;
                    smLatest = sm;
                }
            }
            return smLatest;
        }
        return null;
    }

}
