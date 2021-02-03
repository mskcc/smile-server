package org.mskcc.cmo.metadb.model.converter;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.List;
import org.mskcc.cmo.metadb.model.Run;
import org.neo4j.ogm.typeconversion.AttributeConverter;

public class RunStringConverter implements AttributeConverter<List<Run>, String> {

    @Override
    public String toGraphProperty(List<Run> value) {
        Gson gson = new Gson();
        return gson.toJson(value);
    }

    @Override
    public List<Run> toEntityAttribute(String value) {
        Gson gson = new Gson();
        return Arrays.asList(gson.fromJson(value, Run[].class));
    }
}
