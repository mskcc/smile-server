package org.mskcc.cmo.metadb.model.web;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.cmo.metadb.model.Library;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.QcReport;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.model.converter.LibrariesStringConverter;
import org.mskcc.cmo.metadb.model.converter.MapStringConverter;
import org.mskcc.cmo.metadb.model.converter.QcReportsStringConverter;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.typeconversion.UuidStringConverter;

public class PublishedMetadbSample {
    @Convert(UuidStringConverter.class)
    private UUID metaDbSampleId;
    private String importDate;
    private String cmoInfoIgoId;
    private String cmoSampleName;
    private String sampleName;
    private String cmoSampleClass;
    private String oncoTreeCode;
    private String collectionYear;
    private String tubeId;
    private String cfDNA2dBarcode;
    @Convert(QcReportsStringConverter.class)
    private List<QcReport> qcReports;
    @Convert(LibrariesStringConverter.class)
    private List<Library> libraries;
    private String mrn;
    private String cmoPatientId;
    private UUID metaDbPatientId;
    private String igoId;
    private String investigatorSampleId;
    private String species;
    private String sex;
    private String tumorOrNormal;
    private String sampleType;
    private String preservation;
    private String tumorType;
    private String parentTumorType;
    private String specimenType;
    private String sampleOrigin;
    private String tissueSource;
    private String tissueLocation;
    private String recipe;
    private String baitSet;
    private String fastqPath;
    private String principalInvestigator;
    private String ancestorSample;
    private String sampleStatus;
    private String requestId;
    @Convert(MapStringConverter.class)
    private Map<String, String> cmoSampleIdFields;
    private List<SampleAlias> sampleAliases;

    public PublishedMetadbSample() {}

    /**
     * MetadbSampleWeb constructor
     * @param metaDbSample
     * @throws ParseException
     */
    public PublishedMetadbSample(MetadbSample metaDbSample) throws ParseException {
        SampleMetadata latestSampleMetadata = metaDbSample.getLatestSampleMetadata();
        this.metaDbSampleId = metaDbSample.getMetaDbSampleId();
        this.mrn = latestSampleMetadata.getMrn();
        this.setCmoInfoIgoId(latestSampleMetadata.getCmoInfoIgoId());
        this.cmoSampleName = latestSampleMetadata.getCmoSampleName();
        this.sampleName = latestSampleMetadata.getSampleName();
        this.cmoSampleClass = latestSampleMetadata.getCmoSampleClass();
        this.cmoPatientId = latestSampleMetadata.getCmoPatientId();
        this.igoId = latestSampleMetadata.getIgoId();
        this.investigatorSampleId = latestSampleMetadata.getInvestigatorSampleId();
        this.species = latestSampleMetadata.getSpecies();
        this.sex = latestSampleMetadata.getSex();
        this.tumorOrNormal = latestSampleMetadata.getTumorOrNormal();
        this.sampleType = latestSampleMetadata.getSampleType();
        this.preservation = latestSampleMetadata.getPreservation();
        this.tumorType = latestSampleMetadata.getTumorType();
        this.parentTumorType = latestSampleMetadata.getParentTumorType();
        this.specimenType = latestSampleMetadata.getSpecimenType();
        this.sampleOrigin = latestSampleMetadata.getSampleOrigin();
        this.tissueSource = latestSampleMetadata.getTissueSource();
        this.tissueLocation = latestSampleMetadata.getTissueLocation();
        this.recipe = latestSampleMetadata.getRecipe();
        this.baitSet = latestSampleMetadata.getBaitSet();
        this.principalInvestigator = latestSampleMetadata.getPrincipalInvestigator();
        this.fastqPath = latestSampleMetadata.getFastqPath();
        this.ancestorSample = latestSampleMetadata.getAncestorSample();
        this.sampleStatus = latestSampleMetadata.getSampleStatus();
        this.setImportDate(latestSampleMetadata.getImportDate());
        this.oncoTreeCode = latestSampleMetadata.getOncoTreeCode();
        this.collectionYear = latestSampleMetadata.getCollectionYear();
        this.tubeId = latestSampleMetadata.getTubeId();
        this.cfDNA2dBarcode = latestSampleMetadata.getCfDNA2dBarcode();
        this.qcReports = latestSampleMetadata.getQcReports();
        this.libraries = latestSampleMetadata.getLibraries();
        this.sampleAliases = metaDbSample.getSampleAliases();
    }

    /**
     * MetadbSampleWeb constructor
     * @param igoId
     * @param cmoInfoIgoId
     * @param cmoSampleName
     * @param sampleName
     * @param cmoSampleClass
     * @param cmoPatientId
     * @param investigatorSampleId
     * @param oncoTreeCode
     * @param tumorOrNormal
     * @param tissueLocation
     * @param specimenType
     * @param sampleOrigin
     * @param preservation
     * @param collectionYear
     * @param sex
     * @param species
     * @param tubeId
     * @param cfDNA2dBarcode
     * @param qcReports
     * @param libraries
     * @param mrn
     * @param sampleType
     * @param tumorType
     * @param parentTumorType
     * @param tissueSource
     * @param recipe
     * @param baitSet
     * @param fastqPath
     * @param principalInvestigator
     * @param ancestorSample
     * @param sampleStatus
     * @param importDate
     * @param sampleAliases
     */
    public PublishedMetadbSample(String igoId, String cmoInfoIgoId, String cmoSampleName, String sampleName,
            String cmoSampleClass, String cmoPatientId, String investigatorSampleId, String oncoTreeCode,
            String tumorOrNormal, String tissueLocation, String specimenType, String sampleOrigin,
            String preservation, String collectionYear, String sex, String species, String tubeId,
            String cfDNA2dBarcode, List<QcReport> qcReports, List<Library> libraries,
            String mrn, String sampleType, String tumorType, String parentTumorType,
            String tissueSource, String recipe, String baitSet, String fastqPath,
            String principalInvestigator, String ancestorSample, String sampleStatus, String importDate,
            List<SampleAlias> sampleAliases) {
        this.mrn = mrn;
        this.setCmoInfoIgoId(cmoInfoIgoId);
        this.cmoSampleName = cmoSampleName;
        this.sampleName = sampleName;
        this.cmoSampleClass = cmoSampleClass;
        this.cmoPatientId = cmoPatientId;
        this.igoId = igoId;
        this.investigatorSampleId = investigatorSampleId;
        this.species = species;
        this.sex = sex;
        this.tumorOrNormal = tumorOrNormal;
        this.sampleType = sampleType;
        this.preservation = preservation;
        this.tumorType = tumorType;
        this.parentTumorType = parentTumorType;
        this.specimenType = specimenType;
        this.sampleOrigin = sampleOrigin;
        this.tissueSource = tissueSource;
        this.tissueLocation = tissueLocation;
        this.recipe = recipe;
        this.baitSet = baitSet;
        this.principalInvestigator = principalInvestigator;
        this.fastqPath = fastqPath;
        this.ancestorSample = ancestorSample;
        this.sampleStatus = sampleStatus;
        this.setImportDate(importDate);
        this.oncoTreeCode = oncoTreeCode;
        this.collectionYear = collectionYear;
        this.tubeId = tubeId;
        this.cfDNA2dBarcode = cfDNA2dBarcode;
        this.qcReports = qcReports;
        this.libraries = libraries;
        this.sampleAliases = sampleAliases;
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

    public String getCmoSampleClass() {
        return cmoSampleClass;
    }

    public void setCmoSampleClass(String cmoSampleClass) {
        this.cmoSampleClass = cmoSampleClass;
    }

    public String getOncoTreeCode() {
        return oncoTreeCode;
    }

    public void setOncoTreeCode(String oncoTreeCode) {
        this.oncoTreeCode = oncoTreeCode;
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

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
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

    public String getIgoId() {
        return igoId;
    }

    public void setIgoId(String igoId) {
        this.igoId = igoId;
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

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getPreservation() {
        return preservation;
    }

    public void setPreservation(String preservation) {
        this.preservation = preservation;
    }

    public String getTumorType() {
        return tumorType;
    }

    public void setTumorType(String tumorType) {
        this.tumorType = tumorType;
    }

    public String getParentTumorType() {
        return parentTumorType;
    }

    public void setParentTumorType(String parentTumorType) {
        this.parentTumorType = parentTumorType;
    }

    public String getSpecimenType() {
        return specimenType;
    }

    public void setSpecimenType(String specimenType) {
        this.specimenType = specimenType;
    }

    public String getSampleOrigin() {
        return sampleOrigin;
    }

    public void setSampleOrigin(String sampleOrigin) {
        this.sampleOrigin = sampleOrigin;
    }

    public String getTissueSource() {
        return tissueSource;
    }

    public void setTissueSource(String tissueSource) {
        this.tissueSource = tissueSource;
    }

    public String getTissueLocation() {
        return tissueLocation;
    }

    public void setTissueLocation(String tissueLocation) {
        this.tissueLocation = tissueLocation;
    }

    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    public String getBaitSet() {
        return baitSet;
    }

    public void setBaitSet(String baitSet) {
        this.baitSet = baitSet;
    }

    public String getFastqPath() {
        return fastqPath;
    }

    public void setFastqPath(String fastqPath) {
        this.fastqPath = fastqPath;
    }

    public String getPrincipalInvestigator() {
        return principalInvestigator;
    }

    public void setPrincipalInvestigator(String principalInvestigator) {
        this.principalInvestigator = principalInvestigator;
    }

    public String getAncestorSample() {
        return ancestorSample;
    }

    public void setAncestorSample(String ancestorSample) {
        this.ancestorSample = ancestorSample;
    }

    public String getSampleStatus() {
        return sampleStatus;
    }

    public void setSampleStatus(String sampleStatus) {
        this.sampleStatus = sampleStatus;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
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

    public List<SampleAlias> getSampleAliases() {
        return sampleAliases;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
