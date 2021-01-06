package org.mskcc.cmo.metadb.persistence;

import java.util.UUID;
import org.mskcc.cmo.shared.neo4j.SampleManifestEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */

@Repository
public interface SampleMetadataRepository extends Neo4jRepository<SampleManifestEntity, UUID> {
    
    SampleManifestEntity findSampleByIgoId(String igoId);
    
}
