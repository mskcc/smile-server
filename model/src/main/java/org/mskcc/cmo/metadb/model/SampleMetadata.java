package org.mskcc.cmo.metadb.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.cmo.metadb.model.converter.LibrariesStringConverter;
import org.mskcc.cmo.metadb.model.converter.QcReportsStringConverter;
import org.neo4j.driver.internal.shaded.io.netty.util.internal.StringUtil;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.springframework.util.CollectionUtils;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SampleMetadata implements Serializable {
    @Id @GeneratedValue
    @JsonIgnore
    private Long id;
    private UUID sampleUuid;
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
    private UUID patientUuid;
    private String cmoSampleId;
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
    private String baitSet;
    private String fastqPath;
    private String principalInvestigator;
    private String ancestorSample;
    private String sampleStatus;
    private String requestId;

    public SampleMetadata() {}

    /**
     * SampleMetadata constructor
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
     * @param cmoSampleId
     * @param sampleType
     * @param tumorType
     * @param parentTumorType
     * @param tissueSource
     * @param baitSet
     * @param fastqPath
     * @param principalInvestigator
     * @param ancestorSample
     * @param sampleStatus
     * @param importDate
     */
    public SampleMetadata(String igoId, String cmoInfoIgoId, String cmoSampleName, String sampleName,
            String cmoSampleClass, String cmoPatientId, String investigatorSampleId, String oncoTreeCode,
            String tumorOrNormal, String tissueLocation, String specimenType, String sampleOrigin,
            String preservation, String collectionYear, String sex, String species, String tubeId,
            String cfDNA2dBarcode, List<QcReport> qcReports, List<Library> libraries,
            String mrn, String cmoSampleId, String sampleType, String tumorType, String parentTumorType,
            String tissueSource, String baitSet, String fastqPath,
            String principalInvestigator, String ancestorSample, String sampleStatus, String importDate) {
        this.mrn = mrn;
        this.cmoInfoIgoId = cmoInfoIgoId;
        this.cmoSampleName = cmoSampleName;
        this.sampleName = sampleName;
        this.cmoSampleClass = cmoSampleClass;
        this.cmoPatientId = cmoPatientId;
        this.cmoSampleId = cmoSampleId;
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
        this.baitSet = baitSet;
        this.principalInvestigator = principalInvestigator;
        this.fastqPath = fastqPath;
        this.ancestorSample = ancestorSample;
        this.sampleStatus = sampleStatus;
        this.importDate = importDate;
        this.oncoTreeCode = oncoTreeCode;
        this.collectionYear = collectionYear;
        this.tubeId = tubeId;
        this.cfDNA2dBarcode = cfDNA2dBarcode;
        this.qcReports = qcReports;
        this.libraries = libraries;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getSampleUuid() {
        return sampleUuid;
        
    }

    public void setSampleUuid(UUID sampleUuid) {
        this.sampleUuid = sampleUuid;
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
     * Adds QcReport to list.
     * @param qcReport
     */
    public void addQcReport(QcReport qcReport) {
        if (getQcReports() == null) {
            this.qcReports = new ArrayList<>();
        }
        qcReports.add(qcReport);
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

    /**
     * Adds Library to list.
     * @param library
     */
    public void addLibrary(Library library) {
        if (getLibraries() == null) {
            this.libraries = new ArrayList<>();
        }
        libraries.add(library);
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

    public UUID getPatientUuid() {
        return patientUuid;
    }

    public void setPatientUuid(UUID patientUuid) {
        this.patientUuid = patientUuid;
    }

    public String getCmoSampleId() {
        return cmoSampleId;
    }

    public void setCmoSampleId(String cmoSampleId) {
        this.cmoSampleId = cmoSampleId;
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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ancestorSample, baitSet, cfDNA2dBarcode, cmoInfoIgoId, cmoPatientId, cmoSampleClass,
                cmoSampleId, cmoSampleName, collectionYear, fastqPath, igoId, investigatorSampleId, libraries, mrn,
                oncoTreeCode, parentTumorType, preservation, principalInvestigator, qcReports, requestId, sampleName,
                sampleOrigin, sampleStatus, sampleType, sex, species, specimenType, tissueLocation, tissueSource,
                tubeId, tumorOrNormal, tumorType);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SampleMetadata other = (SampleMetadata) obj;
        if ((this.ancestorSample == null && !StringUtil.isNullOrEmpty(other.ancestorSample))|| 
                (other.ancestorSample == null && !StringUtil.isNullOrEmpty(this.ancestorSample))) {
            return false;
        } else if (!Objects.equals(this.ancestorSample, other.ancestorSample)) {
            return false;
        }
        if ((this.baitSet == null && !StringUtil.isNullOrEmpty(other.baitSet))|| 
                (other.baitSet == null && !StringUtil.isNullOrEmpty(this.baitSet))) {
            return false;
        } else if (!Objects.equals(this.baitSet, other.baitSet)){
            return false;
        }
        if ((this.cfDNA2dBarcode == null && !StringUtil.isNullOrEmpty(other.cfDNA2dBarcode))|| 
                (other.cfDNA2dBarcode == null && !StringUtil.isNullOrEmpty(this.cfDNA2dBarcode))) {
            return false;
        } else if (!Objects.equals(this.cfDNA2dBarcode, other.cfDNA2dBarcode)){
            return false;
        }
        if ((this.cmoInfoIgoId == null && !StringUtil.isNullOrEmpty(other.cmoInfoIgoId))|| 
                (other.cmoInfoIgoId == null && !StringUtil.isNullOrEmpty(this.cmoInfoIgoId))) {
            return false;
        } else if (!Objects.equals(this.cmoInfoIgoId, other.cmoInfoIgoId)){
            return false;
        }
        if ((this.cmoPatientId == null && !StringUtil.isNullOrEmpty(other.cmoPatientId))|| 
                (other.cmoPatientId == null && !StringUtil.isNullOrEmpty(this.cmoPatientId))) {
            return false;
        } else if (!Objects.equals(this.cmoPatientId, other.cmoPatientId)){
            return false;
        }
        if ((this.cmoSampleClass == null && !StringUtil.isNullOrEmpty(other.cmoSampleClass))|| 
                (other.cmoSampleClass == null && !StringUtil.isNullOrEmpty(this.cmoSampleClass))) {
            return false;
        } else if (!Objects.equals(this.cmoSampleClass, other.cmoSampleClass)){
            return false;
        }
        if ((this.cmoSampleId == null && !StringUtil.isNullOrEmpty(other.cmoSampleId))|| 
                (other.cmoSampleId == null && !StringUtil.isNullOrEmpty(this.cmoSampleId))) {
            return false;
        } else if (!Objects.equals(this.cmoSampleId, other.cmoSampleId)){
            return false;
        }
        if ((this.cmoSampleName == null && !StringUtil.isNullOrEmpty(other.cmoSampleName))|| 
                (other.cmoSampleName == null && !StringUtil.isNullOrEmpty(this.cmoSampleName))) {
            return false;
        } else if (!Objects.equals(this.cmoSampleName, other.cmoSampleName)){
            return false;
        }
        if ((this.collectionYear == null && !StringUtil.isNullOrEmpty(other.collectionYear))|| 
                (other.collectionYear == null && !StringUtil.isNullOrEmpty(this.collectionYear))) {
            return false;
        } else if (!StringUtil.isNullOrEmpty(other.collectionYear) && StringUtil.isNullOrEmpty(this.collectionYear)) {
                if(!Objects.equals(this.collectionYear, other.collectionYear)){
                    return false;
                }
        }
        if ((this.fastqPath == null && !StringUtil.isNullOrEmpty(other.fastqPath))|| 
                (other.fastqPath == null && !StringUtil.isNullOrEmpty(this.fastqPath))) {
            return false;
        } else if (!Objects.equals(this.fastqPath, other.fastqPath)){
            return false;
        }
        if ((this.igoId == null && !StringUtil.isNullOrEmpty(other.igoId))|| 
                (other.igoId == null && !StringUtil.isNullOrEmpty(this.igoId))) {
            return false;
        } else if (!Objects.equals(this.igoId, other.igoId)){
            return false;
        }
        if ((this.investigatorSampleId == null && !StringUtil.isNullOrEmpty(other.investigatorSampleId))|| 
                (other.investigatorSampleId == null && !StringUtil.isNullOrEmpty(this.investigatorSampleId))) {
            return false;
        } else if (!Objects.equals(this.investigatorSampleId, other.investigatorSampleId)){
            return false;
        }
        if (libraries == null ? other.libraries != null : !compareLibraryList(this.libraries, other.libraries)) {
            return false;
        }
        if ((this.mrn == null && !StringUtil.isNullOrEmpty(other.mrn))|| 
                (other.mrn == null && !StringUtil.isNullOrEmpty(this.mrn))) {
            return false;
        } else if (!Objects.equals(this.mrn, other.mrn)){
            return false;
        }
        if ((this.oncoTreeCode == null && !StringUtil.isNullOrEmpty(other.oncoTreeCode))|| 
                (other.oncoTreeCode == null && !StringUtil.isNullOrEmpty(this.oncoTreeCode))) {
            return false;
        } else if (!Objects.equals(this.oncoTreeCode, other.oncoTreeCode)){
            return false;
        }
        if ((this.parentTumorType == null && !StringUtil.isNullOrEmpty(other.parentTumorType))|| 
                (other.parentTumorType == null && !StringUtil.isNullOrEmpty(this.parentTumorType))) {
            return false;
        } else if (!Objects.equals(this.parentTumorType, other.parentTumorType)){
            return false;
        }
        if ((this.preservation == null && !StringUtil.isNullOrEmpty(other.preservation))|| 
                (other.preservation == null && !StringUtil.isNullOrEmpty(this.preservation))) {
            return false;
        } else if (!Objects.equals(this.preservation, other.preservation)){
            return false;
        }
        if ((this.principalInvestigator == null && !StringUtil.isNullOrEmpty(other.principalInvestigator))|| 
                (other.principalInvestigator == null && !StringUtil.isNullOrEmpty(this.principalInvestigator))) {
            return false;
        } else if (!Objects.equals(this.principalInvestigator, other.principalInvestigator)){
            return false;
        }
        if (this.qcReports == null ? other.qcReports != null : !compareQcReportList(this.qcReports, other.qcReports)) {
            return false;
        }
        if ((this.requestId == null && !StringUtil.isNullOrEmpty(other.requestId))|| 
                (other.requestId == null && !StringUtil.isNullOrEmpty(this.requestId))) {
            return false;
        } else if (!Objects.equals(this.requestId, other.requestId)){
            return false;
        }
        if ((this.sampleName == null && !StringUtil.isNullOrEmpty(other.sampleName))|| 
                (other.sampleName == null && !StringUtil.isNullOrEmpty(this.sampleName))) {
            return false;
        } else if (!Objects.equals(this.sampleName, other.sampleName)){
            return false;
        }
        if ((this.sampleOrigin == null && !StringUtil.isNullOrEmpty(other.sampleOrigin))|| 
                (other.sampleOrigin == null && !StringUtil.isNullOrEmpty(this.sampleOrigin))) {
            return false;
        } else if (!Objects.equals(this.sampleOrigin, other.sampleOrigin)){
            return false;
        }
        if ((this.sampleStatus == null && !StringUtil.isNullOrEmpty(other.sampleStatus))|| 
                (other.sampleStatus == null && !StringUtil.isNullOrEmpty(this.sampleStatus))) {
            return false;
        } else if (!Objects.equals(this.sampleStatus, other.sampleStatus)){
            return false;
        }
        if ((this.sampleType == null && !StringUtil.isNullOrEmpty(other.sampleType))|| 
                (other.sampleType == null && !StringUtil.isNullOrEmpty(this.sampleType))) {
            return false;
        } else if (!Objects.equals(this.sampleType, other.sampleType)){
            return false;
        }
        if ((this.sex == null && !StringUtil.isNullOrEmpty(other.sex))|| 
                (other.sex == null && !StringUtil.isNullOrEmpty(this.sex))) {
            return false;
        } else if (!Objects.equals(this.sex, other.sex)){
            return false;
        }
        if ((this.species == null && !StringUtil.isNullOrEmpty(other.species))|| 
                (other.species == null && !StringUtil.isNullOrEmpty(this.species))) {
            return false;
        } else if (!Objects.equals(this.species,other.species)){
            return false;
        }
        if ((this.specimenType == null && !StringUtil.isNullOrEmpty(other.specimenType))|| 
                (other.specimenType == null && !StringUtil.isNullOrEmpty(this.specimenType))) {
            return false;
        } else if (!Objects.equals(this.specimenType, other.specimenType)){
            return false;
        }
        if ((this.tissueLocation == null && !StringUtil.isNullOrEmpty(other.tissueLocation))|| 
                (other.tissueLocation == null && !StringUtil.isNullOrEmpty(this.tissueLocation))) {
            return false;
        } else if (!Objects.equals(this.tissueLocation, other.tissueLocation)){
            return false;
        }
        if ((this.tissueSource == null && !StringUtil.isNullOrEmpty(other.tissueSource))|| 
                (other.tissueSource == null && !StringUtil.isNullOrEmpty(this.tissueSource))) {
            return false;
        } else if (!Objects.equals(this.tissueSource, other.tissueSource)){
            return false;
        }
        if ((this.tubeId == null && !StringUtil.isNullOrEmpty(other.tubeId))|| 
                (other.tubeId == null && !StringUtil.isNullOrEmpty(this.tubeId))) {
            return false;
        } else if (!Objects.equals(this.tubeId, other.tubeId)){
            return false;
        }
        if ((this.tumorOrNormal == null && !StringUtil.isNullOrEmpty(other.tumorOrNormal))|| 
                (other.tumorOrNormal == null && !StringUtil.isNullOrEmpty(this.tumorOrNormal))) {
            return false;
        } else if (!Objects.equals(this.tumorOrNormal, other.tumorOrNormal)){
            return false;
        }
        if ((this.tumorType == null && !StringUtil.isNullOrEmpty(other.tumorType))|| 
                (other.tumorType == null && !StringUtil.isNullOrEmpty(this.tumorType))) {
            return false;
        } else if (!Objects.equals(this.tumorType, other.tumorType)){
            return false;
        }
        return true;
    }

    public boolean compareQcReportList(List<QcReport> foundList, List<QcReport> newList) {
        if (CollectionUtils.isEmpty(foundList) && CollectionUtils.isEmpty(newList)) {
            return true;
        }
        if (CollectionUtils.isEmpty(foundList) || CollectionUtils.isEmpty(newList)) {
            return false;
        } 
        for (QcReport qcReport: newList) {
            if(!qcReport.equalLists(foundList)) {
                return false;
            }
        }
        return true;
    }
    
    public boolean compareLibraryList(List<Library> foundList, List<Library> newList) {
        if (CollectionUtils.isEmpty(foundList) && CollectionUtils.isEmpty(newList)) {
            return true;
        }
        if (CollectionUtils.isEmpty(foundList) || CollectionUtils.isEmpty(newList)) {
            return false;
        } 
        for (Library library: newList) {
            if(!library.equalLists(foundList)) {
                return false;
            }
        }
        return true;
    }
}
