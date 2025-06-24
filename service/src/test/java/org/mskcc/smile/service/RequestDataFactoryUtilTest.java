package org.mskcc.smile.service;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.service.util.RequestDataFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 *
 * @author ochoaa
 */
@SpringBootTest(
        classes = SmileTestApp.class,
        properties = {"spring.neo4j.authentication.username:neo4j", "databricks.url:"}
)
@Import(MockDataUtils.class)
public class RequestDataFactoryUtilTest {
    @Autowired
    private MockDataUtils mockDataUtils;

    /**
     * Tests that deserialization of igo sample jsons succeeds and that the sample
     * data factory can convert the igo sample into an instance of SampleMetadata.
     * @throws Exception
     */
    @Test
    public void testResearchSamplesAndRequestDataLoading() throws Exception {
        for (Map.Entry<String, MockJsonTestData> entry :
                mockDataUtils.mockedRequestJsonDataMap.entrySet()) {
            if (!entry.getKey().startsWith("mockIncoming")
                    && !entry.getKey().startsWith("mockValidated")) {
                continue;
            }
            String jsonString = entry.getValue().getJsonString();
            SmileRequest request = RequestDataFactory.buildNewLimsRequestFromJson(jsonString);
        }
    }
}

