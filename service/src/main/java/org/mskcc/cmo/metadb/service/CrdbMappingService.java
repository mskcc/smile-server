package org.mskcc.cmo.metadb.service;

public interface CrdbMappingService {
    String getCmoPatientIdbyDmpId(String dmpId);
    String getCmoPatientIdByInputId(String inputId);
}
