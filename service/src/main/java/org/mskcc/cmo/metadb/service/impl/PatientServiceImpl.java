package org.mskcc.cmo.metadb.service.impl;

import java.util.UUID;
import org.mskcc.cmo.metadb.model.MetaDbPatient;
import org.mskcc.cmo.metadb.persistence.MetaDbPatientRepository;
import org.mskcc.cmo.metadb.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PatientServiceImpl implements PatientService {

    @Autowired
    private MetaDbPatientRepository patientRepository;

    @Override
    public MetaDbPatient savePatientMetadata(MetaDbPatient metaDbPatient) {
        return patientRepository.save(metaDbPatient);
    }

    @Override
    public MetaDbPatient findPatientByCmoPatientId(String cmoPatientId) {
        return patientRepository.findPatientByCmoPatientId(cmoPatientId);
    }

    @Override
    public UUID findPatientIdBySample(UUID metaDbSampleUuid) {
        return patientRepository.findPatientIdBySample(metaDbSampleUuid);
    }

}
