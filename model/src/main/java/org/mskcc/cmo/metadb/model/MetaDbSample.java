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

@NodeEntity
public class MetaDbSample extends SampleManifest {
    @Id @GeneratedValue(strategy = UuidStrategy.class)
    @Convert(UuidStringConverter.class)
    private UUID uuid;
    @Relationship(type = "IS_ALIAS", direction = Relationship.INCOMING)
    private List<SampleAlias> sampleAliases;
    @Relationship(type = "HAS_SAMPLE", direction = Relationship.INCOMING)
    private MetaDbPatient patient;
    @Relationship(type = "HAS_METADATA", direction = Relationship.OUTGOING)
    private SampleManifestEntity sampleManifestEntity;

    public MetaDbSample() {
        super();
    }

    /**
     * SampleManifestEntity constructor.
     * @param sampleManifest
     */
    public MetaDbSample(SampleManifest sampleManifest) {
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
    public MetaDbSample(UUID uuid, String igoId, String cmoInfoIgoId, String cmoSampleName,
            String sampleName, String cmoSampleClass, String cmoPatientId, String investigatorSampleId,
            String oncotreeCode, String tumorOrNormal, String tissueLocation, String specimenType,
            String sampleOrigin, String preservation, String collectionYear, String sex,
            String species, String tubeId, String cfDNA2dBarcode, String baitSet,
            List<QcReport> qcReports, List<Library> libraries,
            List<SampleAlias> sampleList, MetaDbPatient patient) {
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
        this.sampleAliases = sampleList;
        this.patient = patient;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
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

    public void setSampleManifestJsonEntity(SampleManifestEntity s) {
        this.sampleManifestEntity = s;
    }

    public SampleManifestEntity getSampleManifestJsonEntity() {
        return sampleManifestEntity;
    }

    public void setPatientUuid(UUID uuid) {
        this.patient.setUuid(uuid);
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
            if (s.getIdSource() == "igoId") {
                return s;
            }
        }
        return null;
    }

}
