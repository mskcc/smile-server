package org.mskcc.smile.model.igo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author ochoaa
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IgoSampleManifest {
    private String igoId;
    private String cmoPatientId;
    private String cmoSampleName;
    private String sampleName;
    private String baitSet;
    private String cfDNA2dBarcode;
    private String cmoInfoIgoId;
    private String cmoSampleClass;
    private String collectionYear;
    private String investigatorSampleId;
    private String oncoTreeCode;
    private String preservation;
    private String sampleOrigin;
    private String sex;
    private String species;
    private String specimenType;
    private String tissueLocation;
    private String tubeId;
    private String tumorOrNormal;
    private Boolean igoComplete;
    private List<QcReport> qcReports;
    private List<Library> libraries;
    private Map<String, String> cmoSampleIdFields = new HashMap<>();

    public IgoSampleManifest() {}

    public String getIgoId() {
        return igoId;
    }

    public void setIgoId(String igoId) {
        this.igoId = igoId;
    }

    public String getCmoPatientId() {
        return cmoPatientId;
    }

    public void setCmoPatientId(String cmoPatientId) {
        this.cmoPatientId = cmoPatientId;
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

    public String getBaitSet() {
        return baitSet;
    }

    public void setBaitSet(String baitSet) {
        this.baitSet = baitSet;
    }

    public String getCfDNA2dBarcode() {
        return cfDNA2dBarcode;
    }

    public void setCfDNA2dBarcode(String cfDNA2dBarcode) {
        this.cfDNA2dBarcode = cfDNA2dBarcode;
    }

    public String getCmoInfoIgoId() {
        return cmoInfoIgoId;
    }

    public void setCmoInfoIgoId(String cmoInfoIgoId) {
        this.cmoInfoIgoId = cmoInfoIgoId;
    }

    public String getCmoSampleClass() {
        return cmoSampleClass;
    }

    public void setCmoSampleClass(String cmoSampleClass) {
        this.cmoSampleClass = cmoSampleClass;
    }

    public String getCollectionYear() {
        return collectionYear;
    }

    public void setCollectionYear(String collectionYear) {
        this.collectionYear = collectionYear;
    }

    public String getInvestigatorSampleId() {
        return investigatorSampleId;
    }

    public void setInvestigatorSampleId(String investigatorSampleId) {
        this.investigatorSampleId = investigatorSampleId;
    }

    public String getOncoTreeCode() {
        return oncoTreeCode;
    }

    public void setOncoTreeCode(String oncoTreeCode) {
        this.oncoTreeCode = oncoTreeCode;
    }

    public String getPreservation() {
        return preservation;
    }

    public void setPreservation(String preservation) {
        this.preservation = preservation;
    }

    public String getSampleOrigin() {
        return sampleOrigin;
    }

    public void setSampleOrigin(String sampleOrigin) {
        this.sampleOrigin = sampleOrigin;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getSpecimenType() {
        return specimenType;
    }

    public void setSpecimenType(String specimenType) {
        this.specimenType = specimenType;
    }

    public String getTissueLocation() {
        return tissueLocation;
    }

    public void setTissueLocation(String tissueLocation) {
        this.tissueLocation = tissueLocation;
    }

    public String getTubeId() {
        return tubeId;
    }

    public void setTubeId(String tubeId) {
        this.tubeId = tubeId;
    }

    public String getTumorOrNormal() {
        return tumorOrNormal;
    }

    public void setTumorOrNormal(String tumorOrNormal) {
        this.tumorOrNormal = tumorOrNormal;
    }

    public Boolean getIgoComplete() {
        return igoComplete;
    }

    public void setIgoComplete(Boolean igoComplete) {
        this.igoComplete = igoComplete;
    }

    public List<QcReport> getQcReports() {
        return qcReports;
    }

    public void setQcReports(List<QcReport> qcReports) {
        this.qcReports = qcReports;
    }

    public List<Library> getLibraries() {
        return libraries;
    }

    public void setLibraries(List<Library> libraries) {
        this.libraries = libraries;
    }

    public Map<String, String> getCmoSampleIdFields() {
        return cmoSampleIdFields;
    }

    public void setCmoSampleIdFields(Map<String, String> cmoSampleIdFields) {
        this.cmoSampleIdFields = cmoSampleIdFields;
    }

    public String getCmoSampleIdFieldValue(String field) {
        return cmoSampleIdFields.getOrDefault(field, "");
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
