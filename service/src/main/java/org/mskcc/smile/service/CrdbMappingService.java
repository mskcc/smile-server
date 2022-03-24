package org.mskcc.smile.service;

public interface CrdbMappingService {
    String getCmoPatientIdbyDmpId(String dmpId);
    String getCmoPatientIdByInputId(String inputId);
}
