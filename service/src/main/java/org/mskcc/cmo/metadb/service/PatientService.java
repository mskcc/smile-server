package org.mskcc.cmo.metadb.service;

import java.util.UUID;
import org.mskcc.cmo.metadb.model.MetaDbPatient;

public interface PatientService {
    MetaDbPatient savePatientMetadata(MetaDbPatient metaDbPatient);
    MetaDbPatient findPatientByPatientAlias(String cmoPatientId);
    UUID findPatientIdBySample(UUID metaDbSampleUuid);
}
