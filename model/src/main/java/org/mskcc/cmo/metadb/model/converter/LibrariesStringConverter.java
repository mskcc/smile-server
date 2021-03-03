package org.mskcc.cmo.metadb.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;
import org.mskcc.cmo.metadb.model.Library;
import org.neo4j.ogm.typeconversion.AttributeConverter;

public class LibrariesStringConverter implements AttributeConverter<List<Library>, String> {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Logger LOG = Logger.getLogger(LibrariesStringConverter.class);

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
