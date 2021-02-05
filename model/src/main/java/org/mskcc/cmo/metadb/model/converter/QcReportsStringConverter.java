package org.mskcc.cmo.metadb.model.converter;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.List;
import org.mskcc.cmo.metadb.model.QcReport;
import org.neo4j.ogm.typeconversion.AttributeConverter;

public class QcReportsStringConverter implements AttributeConverter<List<QcReport>, String> {

    @Override
    public String toGraphProperty(List<QcReport> value) {
        Gson gson = new Gson();
        return gson.toJson(value);
    }

    @Override
    public List<QcReport> toEntityAttribute(String value) {
        Gson gson = new Gson();
        return Arrays.asList(gson.fromJson(value, QcReport[].class));
    }
}
