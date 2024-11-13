package org.mskcc.smile.service.impl;

import java.util.UUID;
import org.mskcc.smile.model.SmilePatient;
import org.mskcc.smile.persistence.neo4j.SmilePatientRepository;
import org.mskcc.smile.service.SmilePatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PatientServiceImpl implements SmilePatientService {

    @Autowired
    private SmilePatientRepository patientRepository;

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public SmilePatient savePatientMetadata(SmilePatient patient) {
        SmilePatient result = patientRepository.save(patient);
        patient.setSmilePatientId(result.getSmilePatientId());
        return patient;
    }

    @Override
    public SmilePatient getPatientByCmoPatientId(String cmoPatientId) {
        return patientRepository.findPatientByCmoPatientId(cmoPatientId);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public SmilePatient updateCmoPatientId(String oldCmoPatientId, String newCmoPatientId) {
        if (getPatientByCmoPatientId(oldCmoPatientId) == null) {
            return null;
        }
        return patientRepository.updateCmoPatientIdInPatientNode(oldCmoPatientId, newCmoPatientId);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void deletePatient(SmilePatient patient) {
        patientRepository.deletePatientAndAliases(patient);
    }

    @Override
    public SmilePatient getPatientBySampleSmileId(UUID smileSampleId) {
        return patientRepository.findPatientBySampleSmileId(smileSampleId);
    }
}
