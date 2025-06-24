package org.mskcc.smile.service.impl;

import org.apache.commons.lang.StringUtils;
import org.mskcc.smile.model.internal.PatientIdTriplet;
import org.mskcc.smile.persistence.jdbc.DatabricksRepository;
import org.mskcc.smile.service.PatientIdMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PatientIdMappingServiceImpl implements PatientIdMappingService {

    @Autowired
    private DatabricksRepository databricksRepository;

    public PatientIdMappingServiceImpl() {}

    public PatientIdMappingServiceImpl(DatabricksRepository databricksRepository) {
        this.databricksRepository = databricksRepository;
    }

    @Override
    public PatientIdTriplet getPatientIdTripletByInputId(String inputId) throws Exception {
        PatientIdTriplet patientId = databricksRepository.findPatientIdTripletByInputId(inputId);
        if (patientId == null) {
            return null;
        }
        if (!StringUtils.isBlank(patientId.getCmoPatientId())
                && !patientId.getCmoPatientId().startsWith("C-")) {
            patientId.setCmoPatientId("C-" + patientId.getCmoPatientId());
        }
        return patientId;
    }
}
