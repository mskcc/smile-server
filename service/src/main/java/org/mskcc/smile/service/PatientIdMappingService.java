package org.mskcc.smile.service;

import org.mskcc.smile.model.internal.PatientIdTriplet;

public interface PatientIdMappingService {
    PatientIdTriplet getPatientIdTripletByInputId(String inputId) throws Exception;
}
