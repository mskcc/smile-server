package org.mskcc.cmo.metadb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import junit.framework.Assert;
import org.junit.jupiter.api.Test;
import org.mskcc.cmo.metadb.model.MetadbRequest;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.model.dmp.DmpSampleMetadata;
import org.mskcc.cmo.metadb.service.util.RequestDataFactory;
import org.mskcc.cmo.metadb.service.util.SampleDataFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 *
 * @author ochoaa
 */
@SpringBootTest
@Import(MockDataUtils.class)
public class SampleDataFactoryUtilTest {
    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private MockDataUtils mockDataUtils;
    private Map<String, SampleMetadata> expectedConvertedDmpSampleValues
            = initExpectedConvertedDmpSampleValues();

    /**
     * Tests that deserialization of igo sample jsons succeeds and that the sample
     * data factory can convert the igo sample into an instance of SampleMetadata.
     * @throws Exception
     */
    @Test
    public void testResearchSamplesAndRequestDataLoading() throws Exception {
        for (Map.Entry<String, MockJsonTestData> entry : mockDataUtils.mockedRequestJsonDataMap.entrySet()) {
            if (!entry.getKey().startsWith("mockIncoming")) {
                continue;
            }
            String jsonString = entry.getValue().getJsonString();
            MetadbRequest request = RequestDataFactory.buildNewLimsRequestFromJson(jsonString);
        }
    }

    /**
     * Tests that deserialization of dmp sample jsons succeeds and that the sample
     * data factory can convert the dmp sample into an instance of SampleMetadata.
     * @throws Exception
     */
    @Test
    public void testClinicalSampleDataLoading() throws Exception {
        for (Map.Entry<String, MockJsonTestData> entry : mockDataUtils.mockedDmpMetadataMap.entrySet()) {
            String jsonString = entry.getValue().getJsonString();
            DmpSampleMetadata dmpSample = mapper.readValue(jsonString, DmpSampleMetadata.class);
            String cmoPatientId = mockDataUtils.mockedDmpPatientMapping.get(dmpSample.getDmpPatientId());
            SampleMetadata sampleMetadata =
                    SampleDataFactory.buildNewSampleMetadataFromDmpSample(cmoPatientId, dmpSample);

            // verify that certain values were resolved correctly
            SampleMetadata expected = expectedConvertedDmpSampleValues.get(dmpSample.getDmpSampleId());
            if (!sampleMetadata.getCollectionYear().equals(expected.getCollectionYear())
                    || !sampleMetadata.getSampleClass().equals(expected.getSampleClass())
                    || !sampleMetadata.getSampleType().equals(expected.getSampleType())
                    || !sampleMetadata.getSex().equals(expected.getSex())
                    || !sampleMetadata.getTumorOrNormal().equals(expected.getTumorOrNormal())) {
                Assert.fail("Resolved values in dmp sample " + dmpSample.getDmpSampleId() + " do not match "
                        + "expected values in: " + expected.toString());
            }
        }
    }

    private Map<String, SampleMetadata> initExpectedConvertedDmpSampleValues() {
        Map<String, SampleMetadata> map = new HashMap<>();
        map.putAll(getSampleMetadataMapEntry("P-0000001-N01-IM3", "Normal", "Primary", "Male", "2019"));
        map.putAll(getSampleMetadataMapEntry("P-0000001-T01-IM3", "Tumor", "Metastasis", "Male", "2019"));
        map.putAll(getSampleMetadataMapEntry("P-0000002-N01-IM3", "Normal", "Primary", "Female", "2020"));
        map.putAll(getSampleMetadataMapEntry("P-0000002-T01-IM3", "Tumor", "Primary", "Female", "2020"));
        map.putAll(getSampleMetadataMapEntry("P-0000222-N01-IM3", "Normal", "Primary", "Female", "2019"));
        map.putAll(getSampleMetadataMapEntry("P-0000222-T01-IM3", "Tumor", "Primary", "Female", "2019"));
        map.putAll(getSampleMetadataMapEntry("P-0000333-N01-IM3", "Normal", "Primary", "Male", "2019"));
        map.putAll(getSampleMetadataMapEntry("P-0000333-T01-IM3", "Tumor", "Primary", "Male", "2019"));
        map.putAll(getSampleMetadataMapEntry("P-0004000-N01-IM3", "Normal", "Primary", "Male", "2019"));
        map.putAll(getSampleMetadataMapEntry("P-0004000-T01-IM3", "Tumor", "Primary", "Male", "2019"));
        map.putAll(getSampleMetadataMapEntry("P-0004444-N01-IM3", "Normal", "Primary", "Female", "2020"));
        map.putAll(getSampleMetadataMapEntry("P-0007111-N01-IM3", "Normal", "Primary", "Male", "2017"));
        map.putAll(getSampleMetadataMapEntry("P-0007111-T01-IM3", "Tumor", "Metastasis", "Male", "2017"));
        map.putAll(getSampleMetadataMapEntry("P-0008999-N01-IM3", "Normal", "Primary", "Female", "2019"));
        map.putAll(getSampleMetadataMapEntry("P-0008999-T01-IM3", "Tumor", "Primary", "Female", "2019"));
        map.putAll(getSampleMetadataMapEntry("P-6660000-N01-IM3", "Normal", "Primary", "Female", "2020"));
        map.putAll(getSampleMetadataMapEntry("P-7778999-N01-IM3", "Normal", "Primary", "Female", "2018"));
        map.putAll(getSampleMetadataMapEntry("P-7778999-T01-IM3", "Tumor", "Primary", "Female", "2018"));
        map.putAll(getSampleMetadataMapEntry("P-8882444-N01-IM3", "Normal", "Primary", "Male", "2018"));
        map.putAll(getSampleMetadataMapEntry("P-8882444-T01-IM3", "Tumor", "Primary", "Male", "2018"));
        map.putAll(getSampleMetadataMapEntry("P-9990000-N01-IM3", "Normal", "Primary", "Female", "2019"));
        map.putAll(getSampleMetadataMapEntry("P-9990000-T01-IM3", "Tumor", "Metastasis", "Female", "2019"));
        map.putAll(getSampleMetadataMapEntry("P-9992811-N01-IM3", "Normal", "Primary", "Male", "2018"));
        map.putAll(getSampleMetadataMapEntry("P-9992811-T01-IM3", "Tumor", "Metastasis", "Male", "2018"));
        map.putAll(getSampleMetadataMapEntry("P-9998808-N01-IM3", "Normal", "Primary", "Female", "2018"));
        map.putAll(getSampleMetadataMapEntry("P-9998808-T01-IM3", "Tumor", "Primary", "Female", "2018"));
        return map;
    }

    private Map<String, SampleMetadata> getSampleMetadataMapEntry(String dmpId, String sampleClass,
            String sampleType, String sex, String collectionYear) {
        SampleMetadata sm = new SampleMetadata();
        sm.setPrimaryId(dmpId);
        sm.setSampleClass(sampleClass);
        sm.setSampleType(sampleType);
        sm.setSex(sex);
        sm.setTumorOrNormal(sampleClass);
        sm.setCollectionYear(collectionYear);

        Map<String, SampleMetadata> map = new HashMap<>();
        map.put(dmpId, sm);
        return map;
    }
}
