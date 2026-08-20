package org.mskcc.smile.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.typeconversion.AttributeConverter;

/**
 *
 * @author ochoaa
 */
public class ArrayMapConverter implements AttributeConverter<List<Map>, String> {
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Log LOG = LogFactory.getLog(ArrayMapConverter.class);

    @Override
    public String toGraphProperty(List<Map> value) {
        if (value == null) {
            value = new ArrayList<>();
        }
        String toReturn = "";
        try {
            toReturn = mapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            LOG.error(ex);
        }
        return toReturn;
    }

    @Override
    public List<Map> toEntityAttribute(String value) {
        if (value == null) {
            value = "[]";
        }
        List<Map> toReturn = new ArrayList<>();
        try {
            toReturn = Arrays.asList(mapper.readValue(value, Map[].class));
        } catch (Exception ex) {
            LOG.error(ex);
        }
        return toReturn;
    }

}
