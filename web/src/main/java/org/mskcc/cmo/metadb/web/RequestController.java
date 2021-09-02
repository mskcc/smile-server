package org.mskcc.cmo.metadb.web;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mskcc.cmo.metadb.model.web.PublishedMetaDbRequest;
import org.mskcc.cmo.metadb.service.MetadbRequestService;
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
@Api(tags = "request-controller", description = "Request Controller")
@PropertySource("classpath:/maven.properties")
public class RequestController {

    @Value("${metadb.schema_version}")
    private String metaDbSchemaVersion;

    @Autowired
    private MetadbRequestService requestService;

    @Autowired
    public RequestController(MetadbRequestService requestService) {
        this.requestService = requestService;
    }

    /**
     * fetchRequestGET
     * @param requestId
     * @return ResponseEntity
     */
    @ApiOperation(value = "Retrieve MetaDbRequest",
        nickname = "fetchMetaDbRequestGET")
    @RequestMapping(value = "/request/{requestId}",
        method = RequestMethod.GET,
        produces = "application/json")
    public ResponseEntity<PublishedMetaDbRequest> fetchMetaDbRequestGET(@ApiParam(value =
        "Retrieves MetaDbRequest from a RequestId.",
        required = true)
        @PathVariable String requestId) throws Exception {
        PublishedMetaDbRequest request = requestService.getPublishedMetadbRequestById(requestId);
        if (request == null) {
            return requestNotFoundHandler("Request does not exist by id: " + requestId);
        }
        return ResponseEntity.ok()
                .headers(responseHeaders())
                .body(request);
    }

    /**
     * fetchRequestListPOST
     * @param requestIds
     * TODO properly set-up POST
     * @return ResponseEntity
     */
    @ApiOperation(value = "Retrieves list of MetaDbRequest from a list of RequestIds.",
        nickname = "fetchMetaDbRequestListPOST")
    @RequestMapping(value = "/request",
        method = RequestMethod.POST,
        produces = "application/json")
    public ResponseEntity<List<PublishedMetaDbRequest>> fetchMetaDbRequestPOST(@ApiParam(value =
        "List of request ids", required = true, allowMultiple = true)
        @RequestBody List<String> requestIds) throws Exception {
        List<PublishedMetaDbRequest> requestList = new ArrayList<>();
        for (String requestId: requestIds) {
            PublishedMetaDbRequest request = requestService.getPublishedMetadbRequestById(requestId);
            if (request != null) {
                requestList.add(request);
            }
        }
        return ResponseEntity.ok()
                .headers(responseHeaders())
                .body(requestList);
    }

    /**
     * fetchRequestGET
     * @param requestId
     * @return ResponseEntity
     */
    @ApiOperation(value = "Retrieve MetaDbRequest",
        nickname = "fetchMetaDbRequestJsonGET")
    @RequestMapping(value = "/requestJson/{requestId}",
        method = RequestMethod.GET,
        produces = "application/json")
    public ResponseEntity<String> fetchMetaDbRequestJsonGET(@ApiParam(value =
        "Retrieves MetaDbRequest from a RequestId.",
        required = true)
        @PathVariable String requestId) throws Exception {
        PublishedMetaDbRequest request = requestService.getPublishedMetadbRequestById(requestId);
        if (request == null) {
            return requestNotFoundHandler("Request not found by id: " + requestId);
        }
        return ResponseEntity.ok()
                .headers(responseHeaders())
                .body(request.getRequestJson());
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
