package org.mskcc.smile.persistence.jpa;

import org.mskcc.smile.model.internal.CrdbMappingModel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CrdbRepository extends CrudRepository<CrdbMappingModel, Long> {
    @Query(value = "SELECT CMO_ID FROM CRDB_CMO_DMP_MAP WHERE DMP_ID = :dmpId", nativeQuery = true)
    Object getCmoPatientIdbyDmpId(@Param("dmpId") String dmpId);

    @Query(value = "SELECT CMO_ID FROM CRDB_CMO_DMP_MAP WHERE DMP_ID = :inputId OR "
            + "PT_MRN = :inputId", nativeQuery = true)
    Object getCmoPatientIdByInputId(@Param("inputId") String inputId);
}
