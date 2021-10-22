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
     * fetchRequestGET
     * @param requestId
     * @return ResponseEntity
     */
    @ApiOperation(value = "Retrieve MetadbRequest",
        nickname = "fetchMetadbRequestGET")
    @RequestMapping(value = "/request/{requestId}",
        method = RequestMethod.GET,
        produces = "application/json")
    public ResponseEntity<PublishedMetadbRequest> fetchMetadbRequestGET(@ApiParam(value =
        "Retrieves MetadbRequest from a RequestId.",
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
     * fetchRequestListPOST
     * @param requestIds
     * TODO properly set-up POST
     * @return ResponseEntity
     */
    @ApiOperation(value = "Retrieves list of MetadbRequest from a list of RequestIds.",
        nickname = "fetchMetadbRequestListPOST")
    @RequestMapping(value = "/request",
        method = RequestMethod.POST,
        produces = "application/json")
    public ResponseEntity<List<PublishedMetadbRequest>> fetchMetadbRequestPOST(@ApiParam(value =
        "List of request ids", required = true, allowMultiple = true)
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
     * fetchRequestGET
     * @param requestId
     * @return ResponseEntity
     */
    @ApiOperation(value = "Retrieve MetadbRequest",
        nickname = "fetchMetadbRequestJsonGET")
    @RequestMapping(value = "/requestJson/{requestId}",
        method = RequestMethod.GET,
        produces = "application/json")
    public ResponseEntity<String> fetchMetadbRequestJsonGET(@ApiParam(value =
        "Retrieves MetadbRequest from a RequestId.",
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
     * fetchRequestListByImportDatePOST
     * @param dateRange
     * @param returnType
     * @return ResponseEntity
     * @throws Exception
     */
    @ApiOperation(value = "Retrieves list of metaDbRequestId, requestId and importDate"
            + "published within the given start and end date.",
            nickname = "fetchRequestListByImportDatePOST")
    @RequestMapping(value = "/requestsByImportDate",
            method = RequestMethod.POST,
            produces = "application/json")
    public ResponseEntity<Object> fetchRequestListByImportDatePOST(@ApiParam(value =
            "Retrieves list of metaDbRequestId, requestId and importDate"
            + " published between the given start and end date.", required = true)
            @RequestBody Map<String, String> dateRange,
            ReturnTypeEnum returnType) throws Exception {
        List<List<String>> requestSummaryList = requestService.getRequestsByDate(
                dateRange.get("startDate"), dateRange.get("endDate"));
        if (returnType.equals(ReturnTypeEnum.RequestId)) {
            List<String> RequestIdList = new ArrayList<>();
            for (List<String> requestData: requestSummaryList) {
                RequestIdList.add(requestData.get(1));
            }
            return ResponseEntity.ok()
                    .headers(responseHeaders())
                    .body(RequestIdList);
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

    public enum ReturnTypeEnum {
        RequestId("RequestId"),
        RequestSummary("RequestSummary");

        private String str;

        ReturnTypeEnum(String str) {
            this.str = str;
        }

        public String getStr() {
            return str;
        }
    }
}
