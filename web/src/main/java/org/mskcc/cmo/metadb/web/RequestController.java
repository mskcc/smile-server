package org.mskcc.cmo.metadb.web;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mskcc.cmo.metadb.model.web.PublishedMetadbRequest;
import org.mskcc.cmo.metadb.service.MetadbRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.DefaultValue;
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
    private String metadbSchemaVersion;

    @Autowired
    private MetadbRequestService requestService;

    @Autowired
    public RequestController(MetadbRequestService requestService) {
        this.requestService = requestService;
    }

    /**
     * Returns a MetadbRequest given a Request ID.
     * If the given request is not found then returns a 'not found' error.
     * @param requestId
     * @return ResponseEntity
     * @throws Exception
     */
    @ApiOperation(value = "Returns a MetadbRequest given a Request ID",
        nickname = "fetchMetadbRequestGET")
    @RequestMapping(value = "/request/{requestId}",
        method = RequestMethod.GET,
        produces = "application/json")
    public ResponseEntity<PublishedMetadbRequest> fetchMetadbRequestGET(@ApiParam(value =
        "Request ID to retrieve",
        required = true)
        @PathVariable String requestId) throws Exception {
        PublishedMetadbRequest request = requestService.getPublishedMetadbRequestById(requestId);
        if (request == null) {
            return requestNotFoundHandler("Request does not exist by id: " + requestId);
        }
        return ResponseEntity.ok()
                .headers(responseHeaders())
                .body(request);
    }

    /**
     * Returns a list of MetadbRequest's given a list of Request IDs.
     * Only requests which exist in the database will be returned.
     * @param requestIds
     * @return ResponseEntity
     * @throws Exception
     */
    @ApiOperation(value = "Returns a list of MetadbRequest given a list of Request IDs.",
        nickname = "fetchMetadbRequestListPOST")
    @RequestMapping(value = "/request",
        method = RequestMethod.POST,
        produces = "application/json")
    public ResponseEntity<List<PublishedMetadbRequest>> fetchMetadbRequestPOST(@ApiParam(value =
        "List of Request IDs", required = true, allowMultiple = true)
        @RequestBody List<String> requestIds) throws Exception {
        List<PublishedMetadbRequest> requestList = new ArrayList<>();
        for (String requestId: requestIds) {
            PublishedMetadbRequest request = requestService.getPublishedMetadbRequestById(requestId);
            if (request != null) {
                requestList.add(request);
            }
        }
        return ResponseEntity.ok()
                .headers(responseHeaders())
                .body(requestList);
    }

    /**
     * Returns the Request JSON payload as it was received from pub-sub channels,
     * such as by LimsRest publisher.
     * @param requestId
     * @return ResponseEntity
     * @throws Exception
     */
    @ApiOperation(value = "Returns the unprocessed Request JSON for a given request ID",
        nickname = "fetchMetadbRequestJsonGET")
    @RequestMapping(value = "/requestJson/{requestId}",
        method = RequestMethod.GET,
        produces = "application/json")
    public ResponseEntity<String> fetchMetadbRequestJsonGET(@ApiParam(value =
        "Request ID",
        required = true)
        @PathVariable String requestId) throws Exception {
        PublishedMetadbRequest request = requestService.getPublishedMetadbRequestById(requestId);
        if (request == null) {
            return requestNotFoundHandler("Request not found by id: " + requestId);
        }
        return ResponseEntity.ok()
                .headers(responseHeaders())
                .body(request.getRequestJson());
    }

    /**
     * Returns a list of request summaries or a list of request IDs for the provided
     * date range. If an end date is not provided then the current local timestamp
     * will be used as the end date.
     * @param dateRange
     * @param returnType
     * @return ResponseEntity
     * @throws Exception
     */
    @ApiOperation(value = "Returns a list of request summaries or list of request IDs imported"
            + "into the database within the provided date range.",
            nickname = "fetchRequestListByImportDatePOST")
    @RequestMapping(value = "/requestsByImportDate",
            method = RequestMethod.POST,
            produces = "application/json")
    public ResponseEntity<Object> fetchRequestsByImportDatePOST(@ApiParam(value =
            "JSON containg a start date and (optionally) an end date to query requests by", required = true,
            example = "{\"startDate\": \"YYYY/MM/DD\", \"endDate\": \"YYYY/MM/DD\" [OPTIONAL]}")
            @RequestBody Map<String, String> dateRange,
            @ApiParam(value = "Selects level of detail to return", required = true)
                    @DefaultValue("Request ID list") ReturnTypeDetails returnType) throws Exception {
        List<List<String>> requestSummaryList = requestService.getRequestsByDate(
                dateRange.get("startDate"), dateRange.get("endDate"));

        // TODO - sanity check the provided dates here instead of at the service layer?

        // definitely would want to know in the response body if the provided date(s) is
        // invalid similar to how we are returning a 'requestNotFoundHandler()' with an
        // appropriate HttpStatus like HttpStatus.BAD_REQUEST

        // if there are no requests imported within the provided range then what
        // does the response body look like? ideally an empty list

        if (returnType.equals(ReturnTypeDetails.REQUEST_ID_LIST)) {
            List<String> requestIds = new ArrayList<>();
            for (List<String> request: requestSummaryList) {
                requestIds.add(request.get(1));
            }
            return ResponseEntity.ok()
                    .headers(responseHeaders())
                    .body(requestIds);
        }
        return ResponseEntity.ok()
                .headers(responseHeaders())
                .body(requestSummaryList);
    }

    private HttpHeaders responseHeaders() {
        HttpHeaders headers  = new HttpHeaders();
        headers.set("metadb-schema-version", metadbSchemaVersion);
        return headers;
    }

    private ResponseEntity requestNotFoundHandler(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("message", message);
        return new ResponseEntity<>(map, HttpStatus.NOT_FOUND);
    }

    public enum ReturnTypeDetails {
        REQUEST_ID_LIST("Request ID list"),
        REQUEST_SUMMARY_LIST("Request Summary list");

        private String value;

        ReturnTypeDetails(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
