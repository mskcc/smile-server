package org.mskcc.cmo.metadb.service.impl;

import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.metadb.model.MetadbPatient;
import org.mskcc.cmo.metadb.model.PatientAlias;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.mskcc.cmo.metadb.service.MetadbPatientService;
import org.mskcc.cmo.metadb.persistence.MetadbPatientRepository;

@Component
public class PatientServiceImpl implements MetadbPatientService {

    @Autowired
    private MetadbPatientRepository patientRepository;

    @Override
    public UUID savePatientMetadata(MetadbPatient patient) {
        MetadbPatient result = patientRepository.save(patient);
        return result.getMetaDbPatientId();
    }

    @Override
    public MetadbPatient getPatientByCmoPatientId(String cmoPatientId) {
        MetadbPatient patient = patientRepository.findPatientByCmoPatientId(cmoPatientId);
        if (patient != null) {
            List<PatientAlias> aliases = patientRepository.findPatientAliasesByPatient(patient);
            patient.setPatientAliases(aliases);
        }
        return patient;
    }

    @Override
    public UUID getPatientIdBySample(UUID metadbSampleId) {
        return patientRepository.findPatientIdBySample(metadbSampleId);
    }

}
