package org.mskcc.cmo.metadb.persistence.internal;

import org.mskcc.cmo.metadb.model.internal.CRDBIdMappingModel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CRDBIdRepository extends CrudRepository<CRDBIdMappingModel, Long> {

    @Query(value = "SELECT COUNT(dmp_id)"
            + "FROM CRDB_CMO_DMP_MAP", nativeQuery = true)
    Object getCountOfClinicalSamples();


    @Query(value = "SELECT CMO_ID FROM CRDB_CMO_DMP_MAP WHERE DMP_ID = :dmpId", nativeQuery = true)
    Object getCmoPatientIdbyDmpId(@Param("dmpId") String dmpId);

}