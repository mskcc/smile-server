package org.mskcc.cmo.metadb.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.cmo.metadb.model.converter.LibrariesStringConverter;
import org.mskcc.cmo.metadb.model.converter.MapStringConverter;
import org.mskcc.cmo.metadb.model.converter.QcReportsStringConverter;
import org.mskcc.cmo.metadb.model.dmp.DmpSampleMetadata;
import org.mskcc.cmo.metadb.model.igo.IgoSampleManifest;
import org.mskcc.cmo.metadb.model.igo.Library;
import org.mskcc.cmo.metadb.model.igo.QcReport;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.typeconversion.Convert;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SampleMetadata implements Serializable, Comparable<SampleMetadata> {
    @Id @GeneratedValue
    @JsonIgnore
    private Long id;
    private String primaryId;
    private String cmoPatientId;
    private String investigatorSampleId;
    private String cmoSampleName;
    private String sampleName;
    private String igoRequestId;
    private String importDate;
    private String cmoInfoIgoId;
    private String oncotreeCode;
    private String collectionYear;
    private String tubeId;
    private String cfDNA2dBarcode;
    private String species;
    private String sex;
    private String tumorOrNormal;
    private String sampleType;
    private String preservation;
    private String sampleClass;
    private String sampleOrigin;
    private String tissueLocation;
    private String genePanel;
    private String baitSet;
    @Convert(QcReportsStringConverter.class)
    private List<QcReport> qcReports;
    @Convert(LibrariesStringConverter.class)
    private List<Library> libraries;
    @Convert(MapStringConverter.class)
    private Map<String, String> cmoSampleIdFields;
    @Convert(MapStringConverter.class)
    private Map<String, String> additionalProperties = new HashMap<>();

    public SampleMetadata() {}

    /**
     * SampleMetadata constructor from igoSampleManifest.
     * @param igoSampleManifest
     * @throws JsonProcessingException
     */
    public SampleMetadata(IgoSampleManifest igoSampleManifest) throws JsonProcessingException {
        this.primaryId = igoSampleManifest.getIgoId();
        this.cmoPatientId = igoSampleManifest.getCmoPatientId();
        this.investigatorSampleId = igoSampleManifest.getInvestigatorSampleId();
        this.cmoSampleName = igoSampleManifest.getCmoSampleName();
        this.sampleName = igoSampleManifest.getSampleName();
        this.cmoInfoIgoId = igoSampleManifest.getCmoInfoIgoId();
        this.oncotreeCode = igoSampleManifest.getOncoTreeCode();
        this.collectionYear = igoSampleManifest.getCollectionYear();
        this.tubeId = igoSampleManifest.getTubeId();
        this.cfDNA2dBarcode = igoSampleManifest.getCfDNA2dBarcode();
        this.species = igoSampleManifest.getSpecies();
        this.sex = igoSampleManifest.getSex();
        this.tumorOrNormal = igoSampleManifest.getTumorOrNormal();
        this.sampleType = igoSampleManifest.getCmoSampleClass();
        this.preservation = igoSampleManifest.getPreservation();
        this.sampleClass = igoSampleManifest.getSpecimenType();
        this.sampleOrigin = igoSampleManifest.getSampleOrigin();
        this.tissueLocation = igoSampleManifest.getTissueLocation();
        this.genePanel = igoSampleManifest.getCmoSampleIdFieldValue("recipe");
        this.baitSet = igoSampleManifest.getBaitSet();
        this.qcReports =  igoSampleManifest.getQcReports();
        this.libraries = igoSampleManifest.getLibraries();
        this.cmoSampleIdFields = igoSampleManifest.getCmoSampleIdFields();
    }

    /**
     * SampleMetadata constructor from dmpSampleMetadata.
     * @param dmpSampleMetadata
     */
    public SampleMetadata(DmpSampleMetadata dmpSampleMetadata) {
        // other values are resolved in SampleDataFactory
        this.primaryId = dmpSampleMetadata.getDmpSampleId();
        this.oncotreeCode = dmpSampleMetadata.getTumorTypeCode();
        this.tissueLocation = dmpSampleMetadata.getPrimarySite();
        this.genePanel = dmpSampleMetadata.getGenePanel();
        this.baitSet = dmpSampleMetadata.getGenePanel();
        this.species = "Human";
    }

    Map<String, String> setUpMetastasisValueMapping() {
        Map<String, String> metastasisValueMapping = new HashMap<>();
        metastasisValueMapping.put("0", "Primary");
        metastasisValueMapping.put("1", "Metastasis");
        metastasisValueMapping.put("2", "Local Recurrence");
        metastasisValueMapping.put("127", "Unknown");
        return metastasisValueMapping;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCmoPatientId() {
        return cmoPatientId;
    }

    public void setCmoPatientId(String cmoPatientId) {
        this.cmoPatientId = cmoPatientId;
    }

    public String getPrimaryId() {
        return primaryId;
    }

    public void setPrimaryId(String primaryId) {
        this.primaryId = primaryId;
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
    public int compareTo(SampleMetadata sampleMetadata) {
        if (getImportDate() == null || sampleMetadata.getImportDate() == null) {
            return 0;
        }
        return getImportDate().compareTo(sampleMetadata.getImportDate());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
