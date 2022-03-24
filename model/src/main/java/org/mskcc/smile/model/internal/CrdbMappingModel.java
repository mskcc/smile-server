package org.mskcc.smile.model.internal;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class CrdbMappingModel implements Serializable {
    @Id
    private String dmpId;
    private String cmoId;
    private String ptMrn;
    private String apiDmpId;

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

    public String getApiDmpId() {
        return apiDmpId;
    }

    public void setApiDmpId(String apiDmpId) {
        this.apiDmpId = apiDmpId;
    }

}
