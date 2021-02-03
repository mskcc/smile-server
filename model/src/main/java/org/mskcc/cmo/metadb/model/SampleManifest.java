package org.mskcc.cmo.metadb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.cmo.metadb.model.converter.LibrariesStringConverter;
import org.mskcc.cmo.metadb.model.converter.QcReportsStringConverter;
import org.neo4j.ogm.annotation.typeconversion.Convert;

/**
 *
 * @author ochoaa
 */
@JsonIgnoreProperties(value = { "cmoInfoIgoId" })
public class SampleManifest extends SampleMetadata {
    protected String cmoInfoIgoId;
    protected String cmoSampleName;
    protected String sampleName;
    protected String cmoSampleClass;
    protected String oncotreeCode;
    protected String collectionYear;
    protected String tubeId;
    protected String cfDNA2dBarcode;
    @Convert(QcReportsStringConverter.class)
    protected List<QcReport> qcReports;
    @Convert(LibrariesStringConverter.class)
    protected List<Library> libraries;

    public SampleManifest() {}

    /**
     * SampleManifestConstructor.
     * @param igoId
     * @param cmoInfoIgoId
     * @param cmoSampleName
     * @param sampleName
     * @param cmoSampleClass
     * @param cmoPatientId
     * @param investigatorSampleId
     * @param oncotreeCode
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
     * @param baitSet
     * @param qcReports
     * @param libraries
     */
    public SampleManifest(String igoId, String cmoInfoIgoId, String cmoSampleName, String sampleName,
            String cmoSampleClass, String cmoPatientId, String investigatorSampleId, String oncotreeCode,
            String tumorOrNormal, String tissueLocation, String specimenType, String sampleOrigin,
            String preservation, String collectionYear, String sex, String species, String tubeId,
            String cfDNA2dBarcode, String baitSet, List<QcReport> qcReports, List<Library> libraries,
            String mrn, String cmoSampleId, String sampleType, String tumorType, String parentTumorType,
            String tissueSource, String recipe, String baitset, String fastqPath,
            String principalInvestigator, String ancestorSample, Boolean doNotUse,
            String sampleStatus) {
        super(mrn, cmoPatientId, cmoSampleId, igoId,
                investigatorSampleId, species, sex, tumorOrNormal, sampleType,
                preservation, tumorType, parentTumorType, specimenType,
                sampleOrigin, tissueSource, tissueLocation, recipe, baitset,
                fastqPath, principalInvestigator, ancestorSample, doNotUse,
                sampleStatus);
        this.cmoInfoIgoId = cmoInfoIgoId;
        this.cmoSampleName = cmoSampleName;
        this.sampleName = sampleName;
        this.cmoSampleClass = cmoSampleClass;
        this.oncotreeCode = oncotreeCode;
        this.collectionYear = collectionYear;
        this.tubeId = tubeId;
        this.cfDNA2dBarcode = cfDNA2dBarcode;
        this.qcReports = qcReports;
        this.libraries = libraries;
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
     * Adds QcReport to list.
     * @param qcReport
     */
    public void addQcReport(QcReport qcReport) {
        if (qcReports == null) {
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
        if (libraries == null) {
            this.libraries = new ArrayList<>();
        }
        libraries.add(library);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
