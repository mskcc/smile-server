package org.mskcc.smile.service;

import org.mskcc.smile.model.internal.CrdbMappingModel;
import org.mskcc.smile.model.web.CrdbCrosswalkTriplet;

public interface CrdbMappingService {
    String getCmoPatientIdbyDmpId(String dmpId);
    String getCmoPatientIdByInputId(String inputId);
    CrdbMappingModel getCrdbMappingModelByInputId(String inputId) throws Exception;
    CrdbCrosswalkTriplet getCrdbCrosswalkTripletByInputId(String inputId) throws Exception;
}
