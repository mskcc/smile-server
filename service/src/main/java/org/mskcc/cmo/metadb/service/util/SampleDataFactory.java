package org.mskcc.cmo.metadb.service.util;

import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleMetadata;

public class SampleDataFactory {

    private MetadbSample metadbSample;

    public SampleDataFactory() {}

    /**
     * SampleDataFactory contructor
     * @param sampleMetadata
     * @param sampleCategory
     * @throws Exception
     */
    public SampleDataFactory(SampleMetadata sampleMetadata, String sampleCategory) throws Exception {
        if (sampleCategory.equals("research")) {
            setResearchMetadbSampleFields(sampleMetadata);
        } else if (sampleCategory.equals("clinical")) {
            setClinicalMetadbSampleFields(sampleMetadata);
        }
    }

    public MetadbSample getMetadbSample() {
        return metadbSample;
    }

    public MetadbSample buildResearchSample(SampleMetadata sampleMetadata) throws Exception {
        setResearchMetadbSampleFields(sampleMetadata);
        return getMetadbSample();
    }

    public MetadbSample buildClinicalSample(SampleMetadata sampleMetadata) throws Exception {
        setClinicalMetadbSampleFields(sampleMetadata);
        return getMetadbSample();
    }

    private MetadbSample setResearchMetadbSampleFields(SampleMetadata sampleMetadata) throws Exception {
        getMetadbSample().addSampleMetadata(sampleMetadata);
        getMetadbSample().setSampleCategory("research");
        getMetadbSample().setSampleClass(sampleMetadata.getTumorOrNormal());
        getMetadbSample().addSampleAlias(new SampleAlias(sampleMetadata.getPrimaryId(), "igoId"));
        getMetadbSample().addSampleAlias(new SampleAlias(
                sampleMetadata.getInvestigatorSampleId(), "investigatorId"));
        return getMetadbSample();
    }

    private MetadbSample setClinicalMetadbSampleFields(SampleMetadata sampleMetadata) throws Exception {
        getMetadbSample().addSampleMetadata(sampleMetadata);
        getMetadbSample().setSampleCategory("clinical");
        getMetadbSample().setSampleClass(sampleMetadata.getTumorOrNormal());
        getMetadbSample().addSampleAlias(new SampleAlias(sampleMetadata.getPrimaryId(), "dmpId"));
        getMetadbSample().addSampleAlias(new SampleAlias(
                sampleMetadata.getInvestigatorSampleId(), "investigatorId"));
        return getMetadbSample();
    }
}
