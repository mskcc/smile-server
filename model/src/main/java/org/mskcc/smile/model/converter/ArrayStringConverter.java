package org.mskcc.smile.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.typeconversion.AttributeConverter;

/**
 *
 * @author ochoaa
 */
public class ArrayStringConverter implements AttributeConverter<List<String>, String> {
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Log LOG = LogFactory.getLog(ArrayStringConverter.class);

    @Override
    public String toGraphProperty(List<String> value) {
        String toReturn = null;
        try {
            toReturn = mapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            LOG.error(ex);
        }
        return toReturn;
    }

    @Override
    public List<String> toEntityAttribute(String value) {
        List<String> toReturn = null;
        try {
            toReturn = Arrays.asList(mapper.readValue(value, String[].class));
        } catch (Exception ex) {
            LOG.error(ex);
        }
        return toReturn;
    }

}
