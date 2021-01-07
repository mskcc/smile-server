package org.mskcc.cmo.metadb.service.impl;

import org.mskcc.cmo.metadb.model.CmoRequestEntity;
import org.mskcc.cmo.metadb.persistence.CmoRequestRepository;
import org.mskcc.cmo.metadb.service.CmoRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author ochoaa
 */
@Component
public class CmoRequestServiceImpl implements CmoRequestService {

    @Autowired
    private CmoRequestRepository cmoRequestRepository;


    @Override
    public CmoRequestEntity saveRequest(CmoRequestEntity request) {
        return cmoRequestRepository.save(request);
    }
}
