package org.mskcc.smile.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.mskcc.smile.model.SampleMetadata;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.Status;
import org.mskcc.smile.model.igo.IgoSampleManifest;

/**
 *
 * @author ochoaa
 */
public interface CmoLabelGeneratorService {
    String generateCmoSampleLabel(String requestId,
            IgoSampleManifest sampleManifest, List<SampleMetadata> existingPatientSamples);
    String generateCmoSampleLabel(SampleMetadata sample, List<SampleMetadata> existingPatientSamples);
    Status generateSampleStatus(String requestId, IgoSampleManifest sampleManifest,
            List<SampleMetadata> existingSamples) throws JsonProcessingException;
    Status generateSampleStatus(SampleMetadata sampleMetadata,
            List<SampleMetadata> existingSamples) throws JsonProcessingException;
    Boolean igoSampleRequiresLabelUpdate(String newCmoLabel, String existingCmoLabel);
    String resolveSampleTypeAbbreviation(String specimenTypeValue, String sampleOriginValue,
            String cmoSampleClassValue);
    SmileRequest addCmoSampleLabelsToRequestSamples(SmileRequest request) throws Exception;
    SampleMetadata updateCmoSampleLabel(SampleMetadata sample) throws Exception;
}
