package org.mskcc.smile.service;

import org.mskcc.smile.model.internal.CrdbMappingModel;

public interface CrdbMappingService {
    String getCmoPatientIdbyDmpId(String dmpId);
    String getCmoPatientIdByInputId(String inputId) throws Exception;
    CrdbMappingModel getCrdbMappingModelByInputId(String inputId) throws Exception;
}
