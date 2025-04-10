package org.mskcc.smile.service;

import org.mskcc.smile.model.DbGap;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.json.DbGapJson;

public interface DbGapService {
    DbGap getDbGapBySampleId(SmileSample smileSample) throws Exception;
    DbGap getDbGapBySamplePrimaryId(String primaryId) throws Exception;
    void updateDbGap(DbGapJson dbGap) throws Exception;
}
