package org.mskcc.smile.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author aochoa
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PoolMetadataJson {
    @JsonProperty("pool_id")
    private String poolId;
    @JsonProperty("pool_data_type")
    private String poolDataType;
    @JsonProperty("samples")
    private List<PooledSampleMetadataJson> samples;

    public PoolMetadataJson() {}

    public String getPoolId() {
        return poolId;
    }

    public void setPoolId(String poolId) {
        this.poolId = poolId;
    }

    public String getPoolDataType() {
        return poolDataType;
    }

    public void setPoolDataType(String poolDataType) {
        this.poolDataType = poolDataType;
    }

    public List<PooledSampleMetadataJson> getSamples() {
        return samples;
    }

    public void setSamples(List<PooledSampleMetadataJson> samples) {
        this.samples = samples;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
