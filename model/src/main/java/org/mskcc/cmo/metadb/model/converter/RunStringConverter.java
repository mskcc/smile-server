package org.mskcc.cmo.metadb.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;
import org.mskcc.cmo.metadb.model.Run;
import org.neo4j.ogm.typeconversion.AttributeConverter;

public class RunStringConverter implements AttributeConverter<List<Run>, String> {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Logger LOG = Logger.getLogger(QcReportsStringConverter.class);

    @Override
    public String toGraphProperty(List<Run> value) {
        String toReturn = null;
        try {
            toReturn = mapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            LOG.error(ex);
        }
        return toReturn;
    }

    @Override
    public List<Run> toEntityAttribute(String value) {
        List<Run> toReturn = null;
        try {
            toReturn = Arrays.asList(mapper.readValue(value, Run[].class));
        } catch (Exception ex) {
            LOG.error(ex);
        }
        return toReturn;
    }
}
