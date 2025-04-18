package org.mskcc.smile.service.impl;

import java.util.UUID;
import org.mskcc.smile.model.DbGap;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.json.DbGapJson;
import org.mskcc.smile.persistence.neo4j.DbGapRepository;
import org.mskcc.smile.service.DbGapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DbGapServiceImpl implements DbGapService {

    @Autowired
    private DbGapRepository dbGapRepository;

    @Override
    public DbGap saveDbGap(DbGap dbGap) throws Exception {
        return dbGapRepository.save(dbGap);
    }
    
    @Override
    public DbGap getDbGapBySampleId(SmileSample smileSample) throws Exception {
        return dbGapRepository.findDbGapBySmileSampleId(smileSample.getSmileSampleId());
    }

    @Override
    public DbGap getDbGapBySamplePrimaryId(String primaryId) throws Exception {
        return dbGapRepository.findDbGapBySamplePrimaryId(primaryId);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public DbGap updateDbGap(DbGapJson dbGap) throws Exception {
        UUID smileDbGapId;
        DbGap existingDbGap = dbGapRepository.findDbGapBySamplePrimaryId(dbGap.getPrimaryId());
        if (existingDbGap == null) {
            smileDbGapId = saveDbGap(new DbGap(dbGap)).getSmileDgGapId();
        } else {
            smileDbGapId = existingDbGap.getSmileDgGapId();
        }
        return dbGapRepository.updateDbGap(smileDbGapId, dbGap);
    }

}
