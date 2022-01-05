package org.mskcc.cmo.metadb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
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
        }
    }
}
