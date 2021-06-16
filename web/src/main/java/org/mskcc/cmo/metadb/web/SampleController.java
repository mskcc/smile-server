package org.mskcc.cmo.metadb.web;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.service.SampleService;
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
@Api(tags = "sample-controller", description = "Sample Controller")
@PropertySource("classpath:/maven.properties")
public class SampleController {
    @Value("${metadb.schema_version}")
    private String metaDbSchemaVersion;

    @Autowired
    private SampleService sampleService;

    @Autowired
    public SampleController(SampleService sampleService) {
        this.sampleService = sampleService;
    }

    /**
     * Given a CMO patient ID, returns a list of SampleMetadata.
     * @param cmoPatientId
     * @return ResponseEntity
     */
    @ApiOperation(value = "Fetch SampleMetadata list by CMO Patient ID",
        nickname = "fetchSampleMetadataListByCmoPatientIdGET")
    @RequestMapping(value = "/samples/{cmoPatientId}",
        method = RequestMethod.GET,
        produces = "application/json")
    public ResponseEntity<List<SampleMetadata>> fetchSampleMetadataListByCmoPatientIdGET(
            @ApiParam(value = "CMO Patient ID", required = true)
            @PathVariable String cmoPatientId) throws Exception {
        List<SampleMetadata> samples = sampleService.getSampleMetadataListByCmoPatientId(cmoPatientId);
        if (samples == null) {
            return requestNotFoundHandler("Samples not found by CMO patient ID: " + cmoPatientId);
        }
        return ResponseEntity.ok()
                .headers(responseHeaders())
                .body(samples);
    }


    private HttpHeaders responseHeaders() {
        HttpHeaders headers  = new HttpHeaders();
        headers.set("metadb-schema-version", metaDbSchemaVersion);
        return headers;
    }

    private ResponseEntity requestNotFoundHandler(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("message", message);
        return new ResponseEntity<>(map, HttpStatus.NOT_FOUND);
    }
}
