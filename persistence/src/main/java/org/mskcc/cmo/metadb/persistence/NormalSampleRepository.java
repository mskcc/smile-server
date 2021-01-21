package org.mskcc.cmo.metadb.persistence;

import java.util.UUID;
import org.mskcc.cmo.metadb.model.NormalSampleManifestEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */

@Repository
public interface NormalSampleRepository extends Neo4jRepository<NormalSampleManifestEntity, UUID> {
    
}
