package org.mskcc.cmo.metadb.service;

import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mskcc.cmo.metadb.persistence.jpa.CrdbRepository;
import org.mskcc.cmo.metadb.service.impl.CrdbMappingServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 *
 * @author ochoaa
 */
@SpringBootTest
@Import(MockDataUtils.class)
public class CrdbMappingServiceTest {
    @Autowired
    private MockDataUtils mockDataUtils;

    @Autowired
    private CrdbRepository crdbRepository;

    @Autowired
    private CrdbMappingServiceImpl crdbMappingService;

    /**
     * Sets up test resources.
     */
    @BeforeEach
    public void before() {
        for (Map.Entry<String, String> entry : mockDataUtils.mockedDmpPatientMapping.entrySet()) {
            String dmpId = entry.getKey();
            String cmoId = entry.getValue();
            Mockito.when(crdbMappingService.getCmoPatientIdbyDmpId(dmpId))
                    .thenReturn(cmoId);
        }
    }

    /**
     * Simple unit test to ensure NPE is thrown when attempting to query
     * db for a dmp id that does not exist.
     */
    @Test
    public void testMadeUpDmpIdThrowsNullPointerException() {
        Assertions.assertThat(crdbMappingService.getCmoPatientIdbyDmpId("MADEUPVALUE"))
                .isNull();
    }

    /**
     * Simple test to ensure that the expected cmo-dmp id mappings are correct.
     */
    @Test
    public void testDmpToCmoIdMapping() {
        for (Map.Entry<String, String> entry : mockDataUtils.mockedDmpPatientMapping.entrySet()) {
            String dmpId = entry.getKey();
            String cmoId = entry.getValue();
            Assertions.assertThat(crdbMappingService.getCmoPatientIdbyDmpId(dmpId)).isEqualTo(cmoId);
        }
    }

}
