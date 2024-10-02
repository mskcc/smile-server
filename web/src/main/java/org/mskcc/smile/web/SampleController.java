package org.mskcc.smile.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.model.web.PublishedSmileSample;
import org.mskcc.smile.model.web.SmileSampleIdMapping;
import org.mskcc.smile.service.SmileSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author ochoaa
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/")
@Tag(name = "sample-controller", description = "Sample Controller")
@PropertySource("classpath:/maven.properties")
public class SampleController {
    @Value("${smile.schema_version}")
    private String smileSchemaVersion;

    private final DateFormat IMPORT_DATE_FORMATTER = initDateFormatter();

    @Autowired
    private SmileSampleService sampleService;

    @Autowired
    public SampleController(SmileSampleService sampleService) {
        this.sampleService = sampleService;
    }

    /**
     * Given a CMO patient ID, returns a list of SampleMetadata.
     * @param cmoPatientId
     * @return ResponseEntity
     * @throws Exception
     */
    @Operation(description = "Fetch SampleMetadata list by CMO Patient ID")
    @RequestMapping(value = "/samples/{cmoPatientId}",
        method = RequestMethod.GET,
        produces = "application/json")
    public ResponseEntity<List<PublishedSmileSample>> fetchSampleMetadataListByCmoPatientIdGET(
            @Parameter(in = ParameterIn.PATH, description = "CMO Patient ID",
                    name = "cmoPatientId", required = true)
            @PathVariable String cmoPatientId) throws Exception {
        List<PublishedSmileSample> samples = sampleService
                .getPublishedSmileSamplesByCmoPatientId(cmoPatientId);
        if (samples == null) {
            return requestNotFoundHandler("Samples not found by CMO patient ID: " + cmoPatientId);
        }
        return ResponseEntity.ok()
                .headers(responseHeaders())
                .body(samples);
    }

    /**
     * Given an input date as yyyy-MM-dd, returns a list of sample id mappings.
     * @param importDate
     * @return ResponseEntity
     * @throws Exception
     */
    @Operation(description = "Fetch SmileSampleIdMapping list by inputDate")
    @RequestMapping(value = "/samplesByDate/{importDate}",
        method = RequestMethod.GET,
        produces = "application/json")
    public ResponseEntity<List<SmileSampleIdMapping>> fetchSampleIdMappingsByInputDateGET(
            @Parameter(in = ParameterIn.PATH, description = "Import date to search from",
                    name = "importDate", required = true)
            @PathVariable String importDate) throws Exception {
        // validate input date string before submitting db query
        try {
            IMPORT_DATE_FORMATTER.parse(importDate);
        } catch (ParseException e) {
            return badRequestHandler(e.getLocalizedMessage());
        }

        List<SmileSampleIdMapping> sampleIdsList = sampleService.getSamplesByDate(importDate);
        if (sampleIdsList == null) {
            return requestNotFoundHandler("Samples not found by import date: " + importDate);
        }

        return ResponseEntity.ok()
                .headers(responseHeaders())
                .body(sampleIdsList);
    }

    /**
     * Given a valid inputId, returns smileSample
     * @param inputId
     * @return ResponseEntity
     * @throws Exception
     */
    @Operation(description = "Fetch SmileSample by inputId")
    @RequestMapping(value = "/sampleById/{inputId}",
            method = RequestMethod.GET,
            produces = "application/json")
    public ResponseEntity<PublishedSmileSample> fetchSmileSampleByInputIdGET(
            @Parameter(in = ParameterIn.PATH, description = "input id to search with",
                    name = "inputId", required = true)
            @PathVariable String inputId) throws Exception {
        SmileSample smileSample = sampleService.getSampleByInputId(inputId);
        if (smileSample == null) {
            return requestNotFoundHandler("Sample not found by input id: " + inputId);
        }
        PublishedSmileSample publishedSample = sampleService.getPublishedSmileSample(
                smileSample.getSmileSampleId());
        return ResponseEntity.ok()
                .headers(responseHeaders())
                .body(publishedSample);
    }

    private HttpHeaders responseHeaders() {
        HttpHeaders headers  = new HttpHeaders();
        headers.set("smile-schema-version", smileSchemaVersion);
        return headers;
    }

    private ResponseEntity requestNotFoundHandler(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("message", message);
        return new ResponseEntity<>(map, HttpStatus.NOT_FOUND);
    }

    private ResponseEntity badRequestHandler(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("message", message);
        return new ResponseEntity<>(map, HttpStatus.BAD_REQUEST);
    }

    private DateFormat initDateFormatter() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df.setLenient(Boolean.FALSE);
        return df;
    }
}
