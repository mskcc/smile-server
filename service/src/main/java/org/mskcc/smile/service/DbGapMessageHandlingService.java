package org.mskcc.smile.service;

import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.smile.model.json.DbGapJson;

public interface DbGapMessageHandlingService {
    void initialize(Gateway gateway) throws Exception;
    void dbGapUpdateHandler(DbGapJson dbGapJson) throws Exception;
    void shutdown() throws Exception;
}
