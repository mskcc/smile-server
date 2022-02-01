package org.mskcc.cmo.metadb.service.impl;

import java.util.List;
import java.util.UUID;
import org.mskcc.cmo.metadb.model.MetadbPatient;
import org.mskcc.cmo.metadb.model.PatientAlias;
import org.mskcc.cmo.metadb.persistence.neo4j.MetadbPatientRepository;
import org.mskcc.cmo.metadb.service.MetadbPatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PatientServiceImpl implements MetadbPatientService {

    @Autowired
    private MetadbPatientRepository patientRepository;

    @Override
    public MetadbPatient savePatientMetadata(MetadbPatient patient) {
        MetadbPatient result = patientRepository.save(patient);
        patient.setMetaDbPatientId(result.getMetaDbPatientId());
        return patient;
    }

    @Override
    public MetadbPatient updatePatientMetadata(MetadbPatient patient, MetadbPatient existingPatient) {
        for (PatientAlias alias : patient.getPatientAliases()) {
            if (!existingPatient.hasPatientAlias(alias)) {
                existingPatient.addPatientAlias(alias);
            }
        }
        patientRepository.save(existingPatient);
        return existingPatient;
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

    @Override
    public MetadbPatient updateCmoPatientId(String oldCmoPatientId, String newCmoPatientId) {
        if (getPatientByCmoPatientId(oldCmoPatientId) == null) {
            return null;
        }
        return patientRepository.updateCmoPatientIdInPatientNode(oldCmoPatientId, newCmoPatientId);
    }

}
