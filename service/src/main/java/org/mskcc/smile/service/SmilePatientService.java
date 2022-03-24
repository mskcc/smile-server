package org.mskcc.smile.service;

import java.util.UUID;
import org.mskcc.smile.model.SmilePatient;

public interface SmilePatientService {
    SmilePatient savePatientMetadata(SmilePatient patient);
    SmilePatient getPatientByCmoPatientId(String cmoPatientId);
    UUID getPatientIdBySample(UUID smileSampleId);
    SmilePatient updateCmoPatientId(String oldCmoPatientId, String newCmoPatientId);
    void deletePatient(SmilePatient patient);
}
