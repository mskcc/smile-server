package org.mskcc.cmo.metadb.persistence;

import javax.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author ochoaa
 */
public class Neo4jRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID> {
  private final EntityManager entityManager;

  Neo4jRepositoryImpl(JpaEntityInformation entityInformation,
                          EntityManager entityManager) {
    super(entityInformation, entityManager);
    // Keep the EntityManager around to used from the newly introduced methods.
    this.entityManager = entityManager;
  }
  
    @Transactional
    public <MetaDbRequest extends T> MetaDbRequest save(MetaDbRequest metaDbRequest) {
        System.out.println("some implementation here...");
        return metaDbRequest;
    }
    
}
