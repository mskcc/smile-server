package org.mskcc.smile.service.impl;

import java.text.ParseException;
import java.util.List;
import java.util.UUID;
import org.mskcc.smile.model.PatientAlias;
import org.mskcc.smile.model.SampleMetadata;
import org.mskcc.smile.model.SmilePatient;
import org.mskcc.smile.model.SmileSample;
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
    public SmilePatient setUpPatient(String cmoPatientId) {
        SmilePatient patient = new SmilePatient();
        patient.addPatientAlias(new PatientAlias(cmoPatientId, "cmoId"));
        return patient;
    }

    @Override
    public SmilePatient getPatientByCmoPatientId(String cmoPatientId) {
        SmilePatient patient = patientRepository.findPatientByCmoPatientId(cmoPatientId);
        if (patient != null) {
            List<PatientAlias> aliases = patientRepository.findPatientAliasesByPatient(patient);
            patient.setPatientAliases(aliases);
        }
        return patient;
    }

    @Override
    public UUID getPatientIdBySample(UUID smileSampleId) {
        return patientRepository.findPatientIdBySample(smileSampleId);
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
}
