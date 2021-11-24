package org.mskcc.cmo.metadb.service.impl;

import org.mskcc.cmo.metadb.persistence.jpa.CrdbRepository;
import org.mskcc.cmo.metadb.service.CrdbMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CrdbMappingServiceImpl implements CrdbMappingService {

    @Autowired
    private CrdbRepository crdbRepository;

    public CrdbMappingServiceImpl() {}

    public CrdbMappingServiceImpl(CrdbRepository crdbRepository) {
        this.crdbRepository = crdbRepository;
    }

    @Override
    public String getCmoPatientIdbyDmpId(String dmpId) {
        return crdbRepository.getCmoPatientIdbyDmpId(dmpId).toString();
    }
}
