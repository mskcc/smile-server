package org.mskcc.smile.persistence.jpa;

import org.mskcc.smile.model.internal.CrdbMappingModel;
import org.mskcc.smile.model.web.CrdbCrosswalkTriplet;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CrdbRepository extends CrudRepository<CrdbMappingModel, Long> {
    @Query(value = "SELECT CMO_ID FROM CRDB_CMO_DMP_MAP WHERE DMP_ID = :dmpId", nativeQuery = true)
    Object getCmoPatientIdbyDmpId(@Param("dmpId") String dmpId);
    
    @Query(value = "SELECT CMO_ID, DMP_ID, 'MRNREDACTED', API_DMP_ID "
            + "FROM CRDB_CMO_DMP_MAP WHERE :inputId IN (DMP_ID, PT_MRN, CMO_ID)",
            nativeQuery = true)
    Object getCmoPatientIdByInputId(@Param("inputId") String inputId);
    // another query option
    @Query(value = "SELECT CMO_ID, DMP_ID, PT_MRN "
           + "FROM CRDB_CMO_LOJ_DMP_MAP WHERE :inputId IN (DMP_ID, PT_MRN, CMO_ID)",
           nativeQuery = true)
    Object getCrdbCrosswalkTripletByInputId(@Param("inputId") String inputId);
}
