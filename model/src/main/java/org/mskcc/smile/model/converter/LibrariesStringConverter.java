package org.mskcc.smile.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.smile.model.igo.Library;
import org.neo4j.ogm.typeconversion.AttributeConverter;

public class LibrariesStringConverter implements AttributeConverter<List<Library>, String> {
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Log LOG = LogFactory.getLog(LibrariesStringConverter.class);

    @Override
    public String toGraphProperty(List<Library> value) {
        String toReturn = null;
        try {
            toReturn = mapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            LOG.error(ex);
        }
        return toReturn;
    }

    @Override
    public List<Library> toEntityAttribute(String value) {
        List<Library> toReturn = null;
        try {
            toReturn = Arrays.asList(mapper.readValue(value, Library[].class));
        } catch (Exception ex) {
            LOG.error(ex);
        }
        return toReturn;
    }
}
