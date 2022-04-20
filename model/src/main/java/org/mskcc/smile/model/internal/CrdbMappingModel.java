package org.mskcc.smile.model.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.ArrayList;
import javax.persistence.Entity;
import javax.persistence.Id;
import org.apache.commons.lang.builder.ToStringBuilder;

@Entity
public class CrdbMappingModel implements Serializable {
    @Id
    private String dmpId;
    private String cmoId;
    private String ptMrn;
    private Integer apiDmpId;

    public CrdbMappingModel() {}

    /**
     * CrdbMappingModel constructor
     * @param crdbValues
     * @throws JsonProcessingException
     */
    public CrdbMappingModel(ArrayList<Object> crdbValues) throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        this.dmpId = mapper.writeValueAsString(crdbValues.get(1));
        this.cmoId = mapper.writeValueAsString(crdbValues.get(0));
        this.ptMrn = mapper.writeValueAsString(crdbValues.get(2));
        this.apiDmpId = mapper.convertValue(crdbValues.get(3), Integer.class);
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

    public Integer getApiDmpId() {
        return apiDmpId;
    }

    public void setApiDmpId(Integer apiDmpId) {
        this.apiDmpId = apiDmpId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
