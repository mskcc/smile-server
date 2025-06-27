package org.mskcc.smile.service;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mskcc.smile.model.internal.PatientIdTriplet;
import org.mskcc.smile.service.impl.PatientIdMappingServiceImpl;
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
public class PatientIdMappingServiceTest {
    @Autowired
    private MockDataUtils mockDataUtils;

    @Autowired
    private PatientIdMappingServiceImpl patientIdMappingService;

    private PatientIdTriplet getPatientTriplet(String dmpId, String cmoId) {
        PatientIdTriplet patientIdTriplet = new PatientIdTriplet();
        patientIdTriplet.setCmoPatientId(cmoId);
        patientIdTriplet.setDmpPatientId(dmpId);
        return patientIdTriplet;
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
            Mockito.when(patientIdMappingService.getPatientIdTripletByInputId(cmoId))
                    .thenReturn(getPatientTriplet(dmpId, cmoId));
            Mockito.when(patientIdMappingService.getPatientIdTripletByInputId(dmpId))
                    .thenReturn(getPatientTriplet(dmpId, cmoId));
        }
    }

    /**
     * Simple unit test to ensure NPE is thrown when attempting to query
     * db for a dmp id that does not exist.
     * @throws Exception
     */
    @Test
    public void testMadeUpDmpIdThrowsNullPointerException() throws Exception {
        Assertions.assertNull(patientIdMappingService.getPatientIdTripletByInputId("MADEUPVALUE"));
    }

    /**
     * Simple test to ensure that the expected cmo-dmp id mappings are correct.
     * @throws Exception
     */
    @Test
    public void testDmpToCmoIdMapping() throws Exception {
        for (Map.Entry<String, String> entry : mockDataUtils.mockedDmpPatientMapping.entrySet()) {
            String dmpId = entry.getKey();
            String cmoId = entry.getValue();
            Assertions.assertEquals(cmoId,
                    patientIdMappingService.getPatientIdTripletByInputId(dmpId).getCmoPatientId());
        }
    }

    /**
     * Tests if patientIdMappingService maps a valid cmoId to a non null PatientIdTriplet
     * @throws Exception
     */
    @Test
    public void testCmoIdToPatientTripletMapping() throws Exception {
        Assertions.assertNotNull(patientIdMappingService.getPatientIdTripletByInputId("C-KXXL3J"));
    }

    /**
     * Tests if patientIdMappingService maps a valid cmoId to a valid PatientIdTriplet with the expected dmpId
     * @throws Exception
     */
    @Test
    public void testCmoIdToPatientTripletMappingValues() throws Exception {
        Assertions.assertEquals("P-0004000",
                patientIdMappingService.getPatientIdTripletByInputId("C-XXA40X").getDmpPatientId());
    }

    /**
     * Tests if patientIdMappingService maps a valid dmpId to a valid PatientIdTriplet with the expected cmoId
     * @throws Exception
     */
    @Test
    public void testDmpIdToPatientTripletMappingValues() throws Exception {
        Assertions.assertEquals("C-FFX222",
                patientIdMappingService.getPatientIdTripletByInputId("P-0000222").getCmoPatientId());
    }
}
