package org.mskcc.cmo.metadb.model.web;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.cmo.metadb.model.MetadbPatient;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.model.converter.LibrariesStringConverter;
import org.mskcc.cmo.metadb.model.converter.MapStringConverter;
import org.mskcc.cmo.metadb.model.converter.QcReportsStringConverter;
import org.mskcc.cmo.metadb.model.igo.Library;
import org.mskcc.cmo.metadb.model.igo.QcReport;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.typeconversion.UuidStringConverter;

public class PublishedMetadbSample {
    @Convert(UuidStringConverter.class)
    private UUID metaDbSampleId;
    private UUID metaDbPatientId;
    private String primaryId;
    private String cmoPatientId;
    private String cmoSampleName;
    private String sampleName;
    private String cmoInfoIgoId;
    private String investigatorSampleId;
    private String importDate;
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
    private String igoRequestId;
    @Convert(MapStringConverter.class)
    private Map<String, String> cmoSampleIdFields;
    @Convert(QcReportsStringConverter.class)
    private List<QcReport> qcReports;
    @Convert(LibrariesStringConverter.class)
    private List<Library> libraries;
    private List<SampleAlias> sampleAliases;
    private MetadbPatient patient;

    public PublishedMetadbSample() {}

    /**
     * PublishedMetadbSample constructor
     * @param metaDbSample
     * @throws ParseException
     */
    public PublishedMetadbSample(MetadbSample metaDbSample) throws ParseException {
        SampleMetadata latestSampleMetadata = metaDbSample.getLatestSampleMetadata();
        this.metaDbSampleId = metaDbSample.getMetaDbSampleId();
        this.cmoInfoIgoId = latestSampleMetadata.getCmoInfoIgoId();
        this.cmoSampleName = latestSampleMetadata.getCmoSampleName();
        this.sampleName = latestSampleMetadata.getSampleName();
        this.sampleType = latestSampleMetadata.getSampleType();
        this.cmoPatientId = latestSampleMetadata.getCmoPatientId();
        this.metaDbPatientId = metaDbSample.getPatient().getMetaDbPatientId();
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
        this.datasource = metaDbSample.getDatasource();
        this.igoRequestId = latestSampleMetadata.getIgoRequestId();
        this.importDate = latestSampleMetadata.getImportDate();
        this.oncotreeCode = latestSampleMetadata.getOncotreeCode();
        this.collectionYear = latestSampleMetadata.getCollectionYear();
        this.tubeId = latestSampleMetadata.getTubeId();
        this.cfDNA2dBarcode = latestSampleMetadata.getCfDNA2dBarcode();
        this.qcReports = latestSampleMetadata.getQcReports();
        this.libraries = latestSampleMetadata.getLibraries();
        this.sampleAliases = metaDbSample.getSampleAliases();
        this.cmoSampleIdFields = latestSampleMetadata.getCmoSampleIdFields();
        this.patient = metaDbSample.getPatient();
    }

    public UUID getMetaDbSampleId() {
        return metaDbSampleId;

    }

    public void setMetaDbSampleId(UUID metaDbSampleId) {
        this.metaDbSampleId = metaDbSampleId;
    }

    public String getImportDate() {
        return importDate;
    }

    public void setImportDate(String importDate) {
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

    public UUID getMetaDbPatientId() {
        return metaDbPatientId;
    }

    public void setMetaDbPatientId(UUID metaDbPatientId) {
        this.metaDbPatientId = metaDbPatientId;
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

    public String getIgoRequestId() {
        return igoRequestId;
    }

    public void setIgoRequestId(String igoRequestId) {
        this.igoRequestId = igoRequestId;
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

    public MetadbPatient getPatient() {
        return patient;
    }

    public void setPatient(MetadbPatient patient) {
        this.patient = patient;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
