package org.mskcc.smile.model.web;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.smile.model.PatientAlias;
import org.mskcc.smile.model.SampleAlias;
import org.mskcc.smile.model.SampleMetadata;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.Status;
import org.mskcc.smile.model.converter.LibrariesStringConverter;
import org.mskcc.smile.model.converter.MapStringConverter;
import org.mskcc.smile.model.converter.QcReportsStringConverter;
import org.mskcc.smile.model.igo.Library;
import org.mskcc.smile.model.igo.QcReport;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.typeconversion.UuidStringConverter;

public class PublishedSmileSample {
    @Convert(UuidStringConverter.class)
    private UUID smileSampleId;
    private UUID smilePatientId;
    private String primaryId;
    private String cmoPatientId;
    private String cmoSampleName;
    private String sampleName;
    private String cmoInfoIgoId;
    private String investigatorSampleId;
    private Long importDate;
    private String sampleType;
    private String oncotreeCode;
    private String collectionYear;
    private String tubeId;
    private String cfDNA2dBarcode;
    private String species;
    private String sex;
    private String tumorOrNormal;
    private String preservation;
    private String sampleClass;
    private String sampleOrigin;
    private String tissueLocation;
    private String genePanel;
    private String baitSet;
    private String datasource;
    private Boolean igoComplete;
    private Status status;
    @Convert(MapStringConverter.class)
    private Map<String, String> cmoSampleIdFields;
    @Convert(QcReportsStringConverter.class)
    private List<QcReport> qcReports;
    @Convert(LibrariesStringConverter.class)
    private List<Library> libraries;
    private List<SampleAlias> sampleAliases;
    private List<PatientAlias> patientAliases;
    private Map<String, String> additionalProperties = new HashMap<>();

    public PublishedSmileSample() {}

    /**
     * PublishedSmileSample constructor
     * @param smileSample
     * @throws ParseException
     */
    public PublishedSmileSample(SmileSample smileSample) throws ParseException {
        SampleMetadata latestSampleMetadata = smileSample.getLatestSampleMetadata();
        this.smileSampleId = smileSample.getSmileSampleId();
        this.cmoInfoIgoId = latestSampleMetadata.getCmoInfoIgoId();
        this.cmoSampleName = latestSampleMetadata.getCmoSampleName();
        this.sampleName = latestSampleMetadata.getSampleName();
        this.sampleType = latestSampleMetadata.getSampleType();
        // leaving this here in case this field ends up supporting non-cmo patient ids instead
        this.cmoPatientId = latestSampleMetadata.getCmoPatientId();
        this.primaryId = latestSampleMetadata.getPrimaryId();
        this.investigatorSampleId = latestSampleMetadata.getInvestigatorSampleId();
        this.species = latestSampleMetadata.getSpecies();
        this.sex = latestSampleMetadata.getSex();
        this.tumorOrNormal = latestSampleMetadata.getTumorOrNormal();
        this.preservation = latestSampleMetadata.getPreservation();
        this.sampleClass = latestSampleMetadata.getSampleClass();
        this.sampleOrigin = latestSampleMetadata.getSampleOrigin();
        this.tissueLocation = latestSampleMetadata.getTissueLocation();
        this.genePanel = latestSampleMetadata.getGenePanel();
        this.baitSet = latestSampleMetadata.getBaitSet();
        this.datasource = smileSample.getDatasource();
        this.igoComplete = latestSampleMetadata.getIgoComplete();
        this.importDate = latestSampleMetadata.getImportDate();
        this.oncotreeCode = latestSampleMetadata.getOncotreeCode();
        this.collectionYear = latestSampleMetadata.getCollectionYear();
        this.tubeId = latestSampleMetadata.getTubeId();
        this.cfDNA2dBarcode = latestSampleMetadata.getCfDNA2dBarcode();
        this.qcReports = latestSampleMetadata.getQcReports();
        this.libraries = latestSampleMetadata.getLibraries();
        this.sampleAliases = smileSample.getSampleAliases();
        this.cmoSampleIdFields = latestSampleMetadata.getCmoSampleIdFields();
        this.additionalProperties = latestSampleMetadata.getAdditionalProperties();
        this.status = latestSampleMetadata.getStatus();
        if (smileSample.getPatient() != null) {
            this.smilePatientId = smileSample.getPatient().getSmilePatientId();
            this.patientAliases = smileSample.getPatient().getPatientAliases();
        }
    }

    public UUID getSmileSampleId() {
        return smileSampleId;

    }

    public void setSmileSampleId(UUID smileSampleId) {
        this.smileSampleId = smileSampleId;
    }

    public Long getImportDate() {
        return importDate;
    }

    public void setImportDate(Long importDate) {
        this.importDate = importDate;
    }

    public String getCmoInfoIgoId() {
        return cmoInfoIgoId;
    }

    public void setCmoInfoIgoId(String cmoInfoIgoId) {
        this.cmoInfoIgoId = cmoInfoIgoId;
    }

    public String getCmoSampleName() {
        return cmoSampleName;
    }

    public void setCmoSampleName(String cmoSampleName) {
        this.cmoSampleName = cmoSampleName;
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getOncotreeCode() {
        return oncotreeCode;
    }

    public void setOncotreeCode(String oncotreeCode) {
        this.oncotreeCode = oncotreeCode;
    }

    public String getCollectionYear() {
        return collectionYear;
    }

    public void setCollectionYear(String collectionYear) {
        this.collectionYear = collectionYear;
    }

    public String getTubeId() {
        return tubeId;
    }

    public void setTubeId(String tubeId) {
        this.tubeId = tubeId;
    }

    public String getCfDNA2dBarcode() {
        return cfDNA2dBarcode;
    }

    public void setCfDNA2dBarcode(String cfDNA2dBarcode) {
        this.cfDNA2dBarcode = cfDNA2dBarcode;
    }

    /**
     * Returns empty array list if field is null.
     * @return
     */
    public List<QcReport> getQcReports() {
        if (qcReports == null) {
            this.qcReports = new ArrayList<>();
        }
        return qcReports;
    }

    public void setQcReports(List<QcReport> qcReports) {
        this.qcReports = qcReports;
    }

    /**
     * Returns empty array list if field is null.
     * @return
     */
    public List<Library> getLibraries() {
        if (libraries == null) {
            this.libraries = new ArrayList<>();
        }
        return libraries;
    }

    public void setLibraries(List<Library> libraries) {
        this.libraries = libraries;
    }

    public String getCmoPatientId() {
        return cmoPatientId;
    }

    public void setCmoPatientId(String cmoPatientId) {
        this.cmoPatientId = cmoPatientId;
    }

    public UUID getSmilePatientId() {
        return smilePatientId;
    }

    public void setSmilePatientId(UUID smilePatientId) {
        this.smilePatientId = smilePatientId;
    }

    public String getPrimaryId() {
        return primaryId;
    }

    public void setPrimaryId(String igoId) {
        this.primaryId = igoId;
    }

    public String getInvestigatorSampleId() {
        return investigatorSampleId;
    }

    public void setInvestigatorSampleId(String investigatorSampleId) {
        this.investigatorSampleId = investigatorSampleId;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getTumorOrNormal() {
        return tumorOrNormal;
    }

    public void setTumorOrNormal(String tumorOrNormal) {
        this.tumorOrNormal = tumorOrNormal;
    }

    public String getPreservation() {
        return preservation;
    }

    public void setPreservation(String preservation) {
        this.preservation = preservation;
    }

    public String getSampleClass() {
        return sampleClass;
    }

    public void setSampleClass(String sampleClass) {
        this.sampleClass = sampleClass;
    }

    public String getSampleOrigin() {
        return sampleOrigin;
    }

    public void setSampleOrigin(String sampleOrigin) {
        this.sampleOrigin = sampleOrigin;
    }

    public String getTissueLocation() {
        return tissueLocation;
    }

    public void setTissueLocation(String tissueLocation) {
        this.tissueLocation = tissueLocation;
    }

    public String getGenePanel() {
        return genePanel;
    }

    public void setGenePanel(String genePanel) {
        this.genePanel = genePanel;
    }

    public String getBaitSet() {
        return baitSet;
    }

    public void setBaitSet(String baitSet) {
        this.baitSet = baitSet;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public Boolean getIgoComplete() {
        return igoComplete;
    }

    public void setIgoComplete(Boolean igoComplete) {
        this.igoComplete = igoComplete;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Map<String, String> getCmoSampleIdFields() {
        return cmoSampleIdFields;
    }

    public void setCmoSampleIdFields(Map<String, String> cmoSampleIdFields) {
        this.cmoSampleIdFields = cmoSampleIdFields;
    }

    public void setSampleAliases(List<SampleAlias> sampleAliases) {
        this.sampleAliases = sampleAliases;
    }

    /**
     * Returns empty array list if field is null.
     * @return
     */
    public List<SampleAlias> getSampleAliases() {
        if (sampleAliases == null) {
            this.sampleAliases = new ArrayList<>();
        }
        return sampleAliases;
    }

    public List<PatientAlias> getPatientAliases() {
        return patientAliases;
    }

    public void setPatientAliases(List<PatientAlias> patientAliases) {
        this.patientAliases = patientAliases;
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Map<String, String> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public void addAdditionalProperty(String property, String value) {
        this.additionalProperties.put(property, value);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
