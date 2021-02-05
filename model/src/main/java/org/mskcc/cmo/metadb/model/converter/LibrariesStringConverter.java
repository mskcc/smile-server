package org.mskcc.cmo.metadb.model.converter;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.List;
import org.mskcc.cmo.metadb.model.Library;
import org.neo4j.ogm.typeconversion.AttributeConverter;

public class LibrariesStringConverter implements AttributeConverter<List<Library>, String> {

    @Override
    public String toGraphProperty(List<Library> value) {
        Gson gson = new Gson();
        return gson.toJson(value);
    }

    @Override
    public List<Library> toEntityAttribute(String value) {
        Gson gson = new Gson();
        return Arrays.asList(gson.fromJson(value, Library[].class));
    }
}
