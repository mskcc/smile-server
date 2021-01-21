package org.mskcc.cmo.metadb.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.shared.Library;
import org.mskcc.cmo.shared.QcReport;
import org.mskcc.cmo.shared.SampleManifest;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.id.UuidStrategy;
import org.neo4j.ogm.typeconversion.UuidStringConverter;

@NodeEntity(label = "cmo_metadb_sample_metadata")
public class SampleManifestEntity extends SampleManifest {
    @Id @GeneratedValue(strategy = UuidStrategy.class)
    @Convert(UuidStringConverter.class)
    private UUID uuid;
    @Relationship(type = "SP_TO_SP", direction = Relationship.INCOMING)
    private List<Sample> sampleList;
    @Relationship(type = "PX_TO_SP", direction = Relationship.INCOMING)
    private PatientMetadata patient;
    @Relationship(type = "SAMPLE_MANIFEST", direction = Relationship.OUTGOING)
    private SampleManifestJsonEntity sampleManifestJsonEntity;

    public SampleManifestEntity() {
        super();
    }

    /**
     * SampleManifestEntity constructor.
     * @param sampleManifest
     */
    public SampleManifestEntity(SampleManifest sampleManifest) {
        this.mrn = sampleManifest.getMrn();
        this.cmoPatientId = sampleManifest.getCmoPatientId();
        this.cmoSampleId = sampleManifest.getCmoSampleId();
        this.igoId = sampleManifest.getIgoId();
        this.investigatorSampleId = sampleManifest.getInvestigatorSampleId();
        this.species = sampleManifest.getSpecies();
        this.sex = sampleManifest.getSex();
        this.tumorOrNormal = sampleManifest.getTumorOrNormal();
        this.sampleType = sampleManifest.getSampleType();
        this.preservation = sampleManifest.getPreservation();
        this.tumorType = sampleManifest.getTumorType();
        this.parentTumorType = sampleManifest.getParentTumorType();
        this.specimenType = sampleManifest.getSpecimenType();
        this.sampleOrigin = sampleManifest.getSampleOrigin();
        this.tissueSource = sampleManifest.getTissueSource();
        this.tissueLocation = sampleManifest.getTissueLocation();
        this.recipe = sampleManifest.getRecipe();
        this.baitset = sampleManifest.getBaitset();
        this.fastqPath = sampleManifest.getFastqPath();
        this.principalInvestigator = sampleManifest.getPrincipalInvestigator();
        this.ancestorSample = sampleManifest.getAncestorSample();
        this.doNotUse = sampleManifest.isDoNotUse();
        this.sampleStatus = sampleManifest.getSampleStatus();
        this.cmoInfoIgoId = sampleManifest.getCmoInfoIgoId();
        this.cmoSampleName = sampleManifest.getCmoSampleName();
        this.sampleName = sampleManifest.getSampleName();
        this.cmoSampleClass = sampleManifest.getCmoSampleClass();
        this.oncotreeCode = sampleManifest.getOncotreeCode();
        this.collectionYear = sampleManifest.getCollectionYear();
        this.tubeId = sampleManifest.getTubeId();
        this.cfDNA2dBarcode = sampleManifest.getCfDNA2dBarcode();
    }

    /**
     * SampleManifestEntity constructor
     * @param uuid
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
     * @param sampleList
     * @param patient
     */
    public SampleManifestEntity(UUID uuid, String igoId, String cmoInfoIgoId, String cmoSampleName,
            String sampleName, String cmoSampleClass, String cmoPatientId, String investigatorSampleId,
            String oncotreeCode, String tumorOrNormal, String tissueLocation, String specimenType,
            String sampleOrigin, String preservation, String collectionYear, String sex,
            String species, String tubeId, String cfDNA2dBarcode, String baitSet,
            List<QcReport> qcReports, List<Library> libraries,
            List<Sample> sampleList, PatientMetadata patient) {
        super(igoId,
                cmoInfoIgoId,
                cmoSampleName,
                sampleName,
                cmoSampleClass,
                cmoPatientId,
                investigatorSampleId,
                oncotreeCode,
                tumorOrNormal,
                tissueLocation,
                specimenType,
                sampleOrigin,
                preservation,
                collectionYear,
                sex,
                species,
                tubeId,
                cfDNA2dBarcode,
                baitSet,
                qcReports,
                libraries);
        this.uuid = uuid;
        this.sampleList = sampleList;
        this.patient = patient;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setSampleList(List<Sample> sampleList) {
        this.sampleList = sampleList;
    }

    public void getSampleList(List<Sample> s) {
        this.sampleList = s;
    }

    public PatientMetadata getPatient() {
        return patient;
    }

    public void setPatient(PatientMetadata patient) {
        this.patient = patient;
    }

    public void setSampleManifestJsonEntity(SampleManifestJsonEntity s) {
        this.sampleManifestJsonEntity = s;
    }

    public SampleManifestJsonEntity getSampleManifestJsonEntity() {
        return sampleManifestJsonEntity;
    }

    public void setPatientUuid(UUID uuid) {
        this.patient.setUuid(uuid);
    }

    /**
     *
     * @return SampleIgoId
     */
    public Sample getSampleIgoId() {
        if (sampleList == null) {
            this.sampleList = new ArrayList<>();
        }
        for (Sample s: sampleList) {
            if (s.getIdSource() == "igoId") {
                return s;
            }
        }
        return null;
    }

    /**
     * Add sample to array.
     * @param sample
     */
    public void addSample(Sample sample) {
        if (sampleList == null) {
            sampleList = new ArrayList<>();
        }
        sampleList.add(sample);
    }
}
