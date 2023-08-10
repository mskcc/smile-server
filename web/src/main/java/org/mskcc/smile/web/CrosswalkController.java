package org.mskcc.smile.web;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mskcc.smile.model.web.CrdbCrosswalkTriplet;
import org.mskcc.smile.service.CrdbMappingService;
import org.mskcc.smile.service.exception.SmileWebServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/")
@Api(tags = "crosswalk-controller", description = "Crosswalk Controller")
@PropertySource("classpath:/maven.properties")
public class CrosswalkController {

    @Value("${smile.schema_version}")
    private String smileSchemaVersion;

    @Autowired
    private CrdbMappingService crdbMappingService;

    @Autowired
    public CrosswalkController(CrdbMappingService crdbMappingService) {
        this.crdbMappingService = crdbMappingService;
    }

    /**
     * Returns a list of mrn-dmp-cmo patient id triplets given a list of MRNs.
     * Only triplets that exist in the crdb crosswalk table are returned.
     * @param mrns
     * @return ResponseEntity
     * @throws Exception
     */
    @ApiOperation(value = "Returns a list of mrn-dmp-cmo patient id triplets given a list of MRNs.",
        nickname = "fetchIDTripletListPOST")
    @RequestMapping(value = "/crosswalk",
        method = RequestMethod.POST,
        produces = "application/json")
    public ResponseEntity<List<CrdbCrosswalkTriplet>> fetchIDTripletListPOST(@ApiParam(value =
        "List of MRNs", required = true, allowMultiple = true)
        @RequestBody List<String> mrns) throws Exception {
        List<CrdbCrosswalkTriplet> tripletList = new ArrayList<>();
        for (String mrn: mrns) {
            CrdbCrosswalkTriplet triplet = crdbMappingService.getCrdbCrosswalkTripletByInputId(mrn);
            if (triplet != null) {
                tripletList.add(triplet);
            }
        }
        return ResponseEntity.ok()
                .headers(responseHeaders())
                .body(tripletList);
    }

    private HttpHeaders responseHeaders() {
        HttpHeaders headers  = new HttpHeaders();
        headers.set("smile-schema-version", smileSchemaVersion);
        return headers;
    }
}
