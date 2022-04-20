package org.mskcc.smile.service;

import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mskcc.smile.model.internal.CrdbMappingModel;
import org.mskcc.smile.service.impl.CrdbMappingServiceImpl;
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
    private CrdbMappingServiceImpl crdbMappingService;

    private CrdbMappingModel getCrdbMappingModel(String dmpId, String cmoId) {
        CrdbMappingModel crdbMappingModel = new CrdbMappingModel();
        crdbMappingModel.setCmoId(cmoId);
        crdbMappingModel.setDmpId(dmpId);
        return crdbMappingModel;
    }

    /**
     * Sets up test resources.
     * @throws Exception
     */
    @BeforeEach
    public void before() throws Exception {
        for (Map.Entry<String, String> entry : mockDataUtils.mockedDmpPatientMapping.entrySet()) {
            String dmpId = entry.getKey();
            String cmoId = entry.getValue();
            Mockito.when(crdbMappingService.getCmoPatientIdbyDmpId(dmpId))
                    .thenReturn(cmoId);
            Mockito.when(crdbMappingService.getCrdbMappingModelByInputId(cmoId))
                    .thenReturn(getCrdbMappingModel(dmpId, cmoId));
            Mockito.when(crdbMappingService.getCrdbMappingModelByInputId(dmpId))
                    .thenReturn(getCrdbMappingModel(dmpId, cmoId));
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

    /**
     * Tests if crdbMappingService maps a valid cmoId to a non null CrdbMappingModel
     * @throws Exception
     */
    @Test
    public void testCmoIdToCrdbModelMapping() throws Exception {
        Assertions.assertThat(crdbMappingService.getCrdbMappingModelByInputId("C-KXXL3J"))
                .isNotNull();
    }

    /**
     * Tests if crdbMappingService maps a valid cmoId to a valid CrdbMappingModel with the expected dmpId
     * @throws Exception
     */
    @Test
    public void testCmoIdToCrdbModelMappingValues() throws Exception {
        Assertions.assertThat((crdbMappingService.getCrdbMappingModelByInputId("C-XXA40X")).getDmpId())
                .isEqualTo("P-0004000");
    }

    /**
     * Tests if crdbMappingService maps a valid dmpId to a valid CrdbMappingModel with the expected cmoId
     * @throws Exception
     */
    @Test
    public void testDmpIdToCrdbModelMappingValues() throws Exception {
        Assertions.assertThat((crdbMappingService.getCrdbMappingModelByInputId("P-0000222")).getCmoId())
                .isEqualTo("C-FFX222");
    }
}
