package org.mskcc.smile.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.typeconversion.AttributeConverter;

/**
 *
 * @author ochoaa
 */
public class MapStringConverter implements AttributeConverter<Map<String, String>, String> {
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Log LOG = LogFactory.getLog(MapStringConverter.class);

    @Override
    public String toGraphProperty(Map<String, String> value) {
        String toReturn = null;
        try {
            toReturn = mapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            LOG.error(ex);
        }
        return toReturn;
    }

    @Override
    public Map<String, String> toEntityAttribute(String value) {
        Map<String, String> toReturn = null;
        try {
            toReturn = mapper.readValue(value, Map.class);
        } catch (Exception ex) {
            LOG.error(ex);
        }
        return toReturn;
    }
}
