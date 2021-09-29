package org.mskcc.cmo.metadb.service.impl;

import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.metadb.model.MetaDbPatient;
import org.mskcc.cmo.metadb.model.PatientAlias;
import org.mskcc.cmo.metadb.persistence.MetaDbPatientRepository;
import org.mskcc.cmo.metadb.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PatientServiceImpl implements PatientService {

    @Autowired
    private MetaDbPatientRepository patientRepository;

    @Override
    public UUID savePatientMetadata(MetaDbPatient metaDbPatient) {
        MetaDbPatient result = patientRepository.save(metaDbPatient);
        return metaDbPatient.getMetaDbPatientId();
    }

    @Override
    public MetaDbPatient getPatientByCmoPatientId(String cmoPatientId) {
        MetaDbPatient patient = patientRepository.findPatientByCmoPatientId(cmoPatientId);
        if (patient != null) {
            List<PatientAlias> aliases = patientRepository.findPatientAliasesByPatient(patient);
            patient.setPatientAliases(aliases);
        }
        return patient;
    }

    @Override
    public UUID getPatientIdBySample(UUID metaDbSampleUuid) {
        return patientRepository.findPatientIdBySample(metaDbSampleUuid);
    }

}
