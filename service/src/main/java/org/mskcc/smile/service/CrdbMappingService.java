package org.mskcc.smile.service;

import org.mskcc.smile.model.internal.CrdbMappingModel;

public interface CrdbMappingService {
    String getCmoPatientIdbyDmpId(String dmpId);
    String getCmoPatientIdByInputId(String inputId);
    CrdbMappingModel getCrdbMappingModelByInputId(String inputId) throws Exception;
}
