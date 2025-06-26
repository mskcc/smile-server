package org.mskcc.smile.model.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

/**
 *
 * @author ochoaa
 */
public class PatientIdTripletMapper implements RowMapper<PatientIdTriplet> {

    @Override
    public PatientIdTriplet mapRow(ResultSet rs, int rowNum) throws SQLException {
        PatientIdTriplet patient = new PatientIdTriplet();
        patient.setMrn(rs.getString("MRN"));
        patient.setCmoPatientId(rs.getString("CMO_PATIENT_ID"));
        patient.setDmpPatientId(rs.getString("DMP_PATIENT_ID"));
        return patient;
    }

}
