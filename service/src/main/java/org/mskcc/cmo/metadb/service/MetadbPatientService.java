package org.mskcc.cmo.metadb.service;

import java.util.UUID;
import org.mskcc.cmo.metadb.model.MetadbPatient;

public interface MetadbPatientService {
    UUID savePatientMetadata(MetadbPatient metaDbPatient);
    MetadbPatient getPatientByCmoPatientId(String cmoPatientId);
    UUID getPatientIdBySample(UUID metaDbSampleUuid);
}
