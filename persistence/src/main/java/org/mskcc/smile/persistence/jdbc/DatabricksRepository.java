package org.mskcc.smile.persistence.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.mskcc.smile.model.internal.PatientIdTriplet;
import org.mskcc.smile.model.internal.PatientIdTripletMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */
@Repository
public class DatabricksRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Returns an instance of PatientIdTriplet given an input.
     * @param inputId
     * @return PatientIdTriplet
     */
    public PatientIdTriplet findPatientIdTripletByInputId(String inputId) {
        String sqlQuery = "SELECT MRN, CMO_PATIENT_ID, DMP_PATIENT_ID "
                + "FROM cdsi_eng_phi.deng_id_mapping.mrn_cmo_dmp_patient_fullouter "
                + "WHERE MRN NOT LIKE 'P-%' AND ? IN (MRN, CMO_PATIENT_ID, DMP_PATIENT_ID)";

        List<PatientIdTriplet> patientIds = jdbcTemplate.query(
                sqlQuery,
                new PreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps) throws SQLException {
                        ps.setString(1, inputId);
                    }
                },
                new PatientIdTripletMapper()
        );
        // return first 'complete' triplet if more than one result hit
        if (patientIds.size() > 1) {
            for (PatientIdTriplet t : patientIds) {
                if (!StringUtils.isBlank(t.getCmoPatientId())
                        && !StringUtils.isBlank(t.getDmpPatientId())) {
                    return t;
                }
            }
        }
        return patientIds.isEmpty() ? null : patientIds.get(0);
    }

}
