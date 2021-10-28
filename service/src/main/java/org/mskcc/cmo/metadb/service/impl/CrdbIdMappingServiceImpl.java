package org.mskcc.cmo.metadb.service.impl;

import org.mskcc.cmo.metadb.persistence.jpa.CrdbIdRepository;
import org.mskcc.cmo.metadb.service.CrdbIdMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CrdbIdMappingServiceImpl implements CrdbIdMappingService {

    @Autowired
    private CrdbIdRepository crdbIdRepository;

    @Override
    public String getCmoPatientIdbyDmpId(String dmpId) {
        return crdbIdRepository.getCmoPatientIdbyDmpId(dmpId).toString();
    }
}
