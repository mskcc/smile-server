package org.mskcc.cmo.metadb.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
    private SampleManifest sampleManifest;

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
     * SampleManifestEntity constructor.
     * @param uuid
     * @param mrn
     * @param cmoPatientId
     * @param cmoSampleId
     * @param igoId
     * @param investigatorSampleId
     * @param species
     * @param sex
     * @param tumorOrNormal
     * @param sampleType
     * @param preservation
     * @param tumorType
     * @param parentTumorType
     * @param specimenType
     * @param sampleOrigin
     * @param tissueSource
     * @param tissueLocation
     * @param recipe
     * @param baitset
     * @param fastqPath
     * @param principalInvestigator
     * @param ancestorSample
     * @param doNotUse
     * @param sampleStatus
     * @param cmoInfoIgoId
     * @param cmoSampleName
     * @param sampleName
     * @param cmoSampleClass
     * @param oncotreeCode
     * @param collectionYear
     * @param tubeId
     * @param cfDNA2dBarcode
     * @param sampleList
     * @param patient
     */
    public SampleManifestEntity(UUID uuid, String mrn, String cmoPatientId, String cmoSampleId, String igoId,
            String investigatorSampleId, String species, String sex, String tumorOrNormal, String sampleType,
            String preservation, String tumorType, String parentTumorType, String specimenType,
            String sampleOrigin, String tissueSource, String tissueLocation, String recipe, String baitset,
            String fastqPath, String principalInvestigator, String ancestorSample, boolean doNotUse,
            String sampleStatus, String cmoInfoIgoId, String cmoSampleName, String sampleName,
            String cmoSampleClass, String oncotreeCode, String collectionYear, String tubeId,
            String cfDNA2dBarcode, List<Sample> sampleList, PatientMetadata patient) {
        super(mrn,
            cmoPatientId,
            cmoSampleId,
            igoId,
            investigatorSampleId,
            species,
            sex,
            tumorOrNormal,
            sampleType,
            preservation,
            tumorType,
            parentTumorType,
            specimenType,
            sampleOrigin,
            tissueSource,
            tissueLocation,
            recipe,
            baitset,
            fastqPath,
            principalInvestigator,
            ancestorSample,
            doNotUse,
            sampleStatus,
            cmoInfoIgoId,
            cmoSampleName,
            sampleName,
            cmoSampleClass,
            oncotreeCode,
            collectionYear,
            tubeId,
            cfDNA2dBarcode);
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

    public List<Sample> getSampleList() {
        return sampleList;
    }

    public void setSampleList(List<Sample> sampleList) {
        this.sampleList = sampleList;
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

    public PatientMetadata getPatient() {
        return patient;
    }

    public void setPatient(PatientMetadata patient) {
        this.patient = patient;
    }

}
