package org.mskcc.smile.web;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mskcc.smile.model.web.PublishedSmileRequest;
import org.mskcc.smile.model.web.RequestSummary;
import org.mskcc.smile.service.SmileRequestService;
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
@Api(tags = "request-controller", description = "Request Controller")
@PropertySource("classpath:/maven.properties")
public class RequestController {

    @Value("${smile.schema_version}")
    private String smileSchemaVersion;

    @Autowired
    private SmileRequestService requestService;

    @Autowired
    public RequestController(SmileRequestService requestService) {
        this.requestService = requestService;
    }

    /**
     * Returns a SmileRequest given a Request ID.
     * If the given request is not found then returns a 'not found' error.
     * @param requestId
     * @return ResponseEntity
     * @throws Exception
     */
    @ApiOperation(value = "Returns a SmileRequest given a Request ID",
        nickname = "fetchSmileRequestGET")
    @RequestMapping(value = "/request/{requestId}",
        method = RequestMethod.GET,
        produces = "application/json")
    public ResponseEntity<PublishedSmileRequest> fetchSmileRequestGET(@ApiParam(value =
        "Request ID to retrieve",
        required = true)
        @PathVariable String requestId) throws Exception {
        PublishedSmileRequest request = requestService.getPublishedSmileRequestById(requestId);
        if (request == null) {
            return requestNotFoundHandler("Request does not exist by id: " + requestId);
        }
        return ResponseEntity.ok()
                .headers(responseHeaders())
                .body(request);
    }

    /**
     * Returns a list of SmileRequest's given a list of Request IDs.
     * Only requests which exist in the database will be returned.
     * @param requestIds
     * @return ResponseEntity
     * @throws Exception
     */
    @ApiOperation(value = "Returns a list of SmileRequest given a list of Request IDs.",
        nickname = "fetchSmileRequestListPOST")
    @RequestMapping(value = "/request",
        method = RequestMethod.POST,
        produces = "application/json")
    public ResponseEntity<List<PublishedSmileRequest>> fetchSmileRequestPOST(@ApiParam(value =
        "List of Request IDs", required = true, allowMultiple = true)
        @RequestBody List<String> requestIds) throws Exception {
        List<PublishedSmileRequest> requestList = new ArrayList<>();
        for (String requestId: requestIds) {
            PublishedSmileRequest request = requestService.getPublishedSmileRequestById(requestId);
            if (request != null) {
                requestList.add(request);
            }
        }
        return ResponseEntity.ok()
                .headers(responseHeaders())
                .body(requestList);
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
            "JSON with 'startDate' (required) and 'endDate' (optional) to query for.", required = true)
            @RequestBody DateRange dateRange,  ReturnTypeDetails returnType) throws Exception {
        // get request summary for given date range
        List<RequestSummary> requestSummaryList;
        try {
            requestSummaryList = requestService.getRequestsByDate(
                    dateRange.getStartDate(), dateRange.getEndDate());
        } catch (Exception e) {
            throw new SmileWebServiceException(e.getMessage());
        }
        // nothing to do if response is null
        if (requestSummaryList == null || requestSummaryList.isEmpty()) {
            requestNotFoundHandler("No requests were imported in provided date range.");
        }

        // make list of request ids if specified
        if (returnType.equals(ReturnTypeDetails.REQUEST_ID_LIST)) {
            List<String> requestIds = new ArrayList<>();
            for (RequestSummary request: requestSummaryList) {
                requestIds.add(request.getRequestId());
            }
            return sendResponse(requestIds);
        }
        return sendResponse(requestSummaryList);
    }

    private ResponseEntity<Object> sendResponse(Object toReturn) {
        return ResponseEntity.ok()
                .headers(responseHeaders())
                .body(toReturn);
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

    static class DateRange {
        String startDate;
        String endDate;

        DateRange(){}

        String getStartDate() {
            return startDate;
        }

        void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        String getEndDate() {
            return endDate;
        }

        void setEndDate(String endDate) {
            this.endDate = endDate;
        }
    }

    public enum ReturnTypeDetails {
        REQUEST_ID_LIST("REQUEST_ID_LIST"),
        REQUEST_SUMMARY_LIST("REQUEST_SUMMARY_LIST");

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
