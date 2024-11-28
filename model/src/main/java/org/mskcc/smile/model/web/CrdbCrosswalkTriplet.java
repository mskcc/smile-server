package org.mskcc.smile.model.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.Serializable;
import java.util.ArrayList;
import javax.persistence.Entity;
import javax.persistence.Id;
import org.apache.commons.lang.builder.ToStringBuilder;

@Entity
public class CrdbCrosswalkTriplet implements Serializable {
    static String CMO_PATIENT_ID_PREFIX = "C-";
    
    @Id
    private String dmpId;
    private String cmoId;
    private String ptMrn;

    public CrdbCrosswalkTriplet() {}

    /**
     * CrdbCrosswalkTriplet constructor
     * @param crdbValues
     * @throws JsonProcessingException
     */
    public CrdbCrosswalkTriplet(ArrayList<Object> crdbValues) throws Exception {
        // the crosswalk table may contain records that are missing fields (at least dmp)
        // we need to handle cases where null is passed into this constructor
        // Jackson null values to empty string "" does not work, so lets
        // set null values to "" here in constructor
        if (crdbValues.get(1) == null) {
            this.dmpId = "";
        } else {
            this.dmpId = crdbValues.get(1).toString();
        }
        if (crdbValues.get(0) == null) {
            this.cmoId = "";
        } else {
            this.cmoId = crdbValues.get(0).toString();
            if (!this.cmoId.startsWith(CMO_PATIENT_ID_PREFIX)) {
                this.cmoId = CMO_PATIENT_ID_PREFIX + this.cmoId;
            }
        }
        if (crdbValues.get(2) == null) {
            this.ptMrn = "";
        } else {
            this.ptMrn = crdbValues.get(2).toString();
        }
    }

    public String getDmpId() {
        return dmpId;
    }

    public void setDmpId(String dmpId) {
        this.dmpId = dmpId;
    }

    public String getCmoId() {
        return cmoId;
    }

    public void setCmoId(String cmoId) {
        this.cmoId = cmoId;
    }

    public String getPtMrn() {
        return ptMrn;
    }

    public void setPtMrn(String ptMrn) {
        this.ptMrn = ptMrn;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
