package org.mskcc.cmo.metadb.service.impl;

import org.mskcc.cmo.metadb.persistence.internal.CRDBIdRepository;
import org.mskcc.cmo.metadb.service.CRDBIdMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CRDBIdMappingServiceImpl implements CRDBIdMappingService{

    @Autowired
    CRDBIdRepository crdbIdRepository;

    @Override
    public Object getCountOfClinicalSamples() {
        return crdbIdRepository.getCountOfClinicalSamples();
    }

    @Override
    public Object getCmoPatientIdbyDmpId(String dmpId) {
        return crdbIdRepository.getCmoPatientIdbyDmpId(dmpId);
    }
}
