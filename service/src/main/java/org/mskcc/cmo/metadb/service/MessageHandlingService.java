package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.shared.neo4j.SampleManifestEntity;

public interface MessageHandlingService {

    void initialize(Gateway gateway) throws Exception;

    void newSampleHandler(SampleManifestEntity sample) throws Exception;

    void shutdown() throws Exception;
}
