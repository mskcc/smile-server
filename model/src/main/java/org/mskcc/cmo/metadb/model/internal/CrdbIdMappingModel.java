package org.mskcc.cmo.metadb.model.internal;

import javax.persistence.*;

@Entity
public class CrdbIdMappingModel {
    @Id
    private String DMP_ID;
    private String CMO_ID;
    private String PT_MRN;
    private String API_DMP_ID;

    public String getCMO_ID() {
        return CMO_ID;
    }

    public void setCMO_ID(String cMO_ID) {
        CMO_ID = cMO_ID;
    }

    public String getDMP_ID() {
        return DMP_ID;
    }

    public void setDMP_ID(String dMP_ID) {
        DMP_ID = dMP_ID;
    }

    public String getPT_MRN() {
        return PT_MRN;
    }

    public void setPT_MRN(String pT_MRN) {
        PT_MRN = pT_MRN;
    }

    public String getAPI_DMP_ID() {
        return API_DMP_ID;
    }

    public void setAPI_DMP_ID(String aPI_DMP_ID) {
        API_DMP_ID = aPI_DMP_ID;
    }
}
