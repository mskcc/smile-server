package org.mskcc.smile.model.internal;

/**
 *
 * @author laptop
 */
public class PatientIdTriplet {
    private String mrn;
    private String cmoPatientId;
    private String dmpPatientId;

    public PatientIdTriplet() {}

    /**
     * PatientIdTriplet constructor.
     * @param mrn
     * @param cmoPatientId
     * @param dmpPatientId
     */
    public PatientIdTriplet(String mrn, String cmoPatientId, String dmpPatientId) {
        this.mrn = mrn;
        this.cmoPatientId = cmoPatientId;
        this.dmpPatientId = dmpPatientId;
    }

    /**
     * @return the mrn
     */
    public String getMrn() {
        return mrn;
    }

    /**
     * @param mrn the mrn to set
     */
    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    /**
     * @return the cmoPatientId
     */
    public String getCmoPatientId() {
        return cmoPatientId;
    }

    /**
     * @param cmoPatientId the cmoPatientId to set
     */
    public void setCmoPatientId(String cmoPatientId) {
        this.cmoPatientId = cmoPatientId;
    }

    /**
     * @return the dmpPatientId
     */
    public String getDmpPatientId() {
        return dmpPatientId;
    }

    /**
     * @param dmpPatientId the dmpPatientId to set
     */
    public void setDmpPatientId(String dmpPatientId) {
        this.dmpPatientId = dmpPatientId;
    }

}
