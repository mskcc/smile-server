package org.mskcc.smile.service.impl;

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
    public DbGap getDbGapBySampleId(SmileSample smileSample) throws Exception {
        return dbGapRepository.findDbGapBySmileSampleId(smileSample.getSmileSampleId());
    }

    @Override
    public DbGap getDbGapBySamplePrimaryId(String primaryId) throws Exception {
        return dbGapRepository.findDbGapBySamplePrimaryId(primaryId);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void updateDbGap(DbGapJson dbGap) throws Exception {
        dbGapRepository.updateDbGap(dbGap);
    }
}
