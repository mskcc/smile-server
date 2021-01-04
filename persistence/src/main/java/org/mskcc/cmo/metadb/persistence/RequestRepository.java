package org.mskcc.cmo.metadb.persistence;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestRepository extends Neo4jRepository<Object,String> {
    
    @Query()
    void saveRequest();
    
    
}