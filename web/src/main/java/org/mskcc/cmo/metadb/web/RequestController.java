package org.mskcc.cmo.metadb.web;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.mskcc.cmo.metadb.service.MetaDbRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/")
@Api(tags = "request-controller", description = "Request Controller")
public class RequestController {
    @Autowired
    private MetaDbRequestService metaDbRequestService;

    @Autowired
    public RequestController(MetaDbRequestService metaDbRequestService) {
        this.metaDbRequestService = metaDbRequestService;
    }

    /**
     * fetchRequestGET
     * @param requestId
     */
    @ApiOperation(value = "Retrieve MetaDbRequest",
        nickname = "fetchMetaDbRequestGET")
    @RequestMapping(value = "/request/{requestId}",
        method = RequestMethod.GET,
        produces = "application/json")
    @ResponseBody
    public Map<String, Object> fetchMetaDbRequestGET(@ApiParam(value =
        "Retrieves MetaDbRequest from a RequestId.",
        required = true)
        @PathVariable String requestId) throws Exception {
        return metaDbRequestService.getMetaDbRequestMap(requestId);
    }

    /**
     * fetchRequestListPOST
     * @param requestIds
     * TODO properly set-up POST
     */
    @ApiOperation(value = "Retrieves list of MetaDbRequest from a list of RequestIds.",
        nickname = "fetchMetaDbRequestListPOST")
    @RequestMapping(value = "/request",
        method = RequestMethod.POST,
        produces = "application/json")
    public List<Map<String, Object>> fetchMetaDbRequestPOST(@ApiParam(value =
        "List of request ids", required = true, allowMultiple = true)
        @RequestBody List<String> requestIds) throws Exception {
        List<Map<String, Object>> requestList = new ArrayList<>();
        for (String requestId: requestIds) {
            requestList.add(metaDbRequestService.getMetaDbRequestMap(requestId));
        }
        return requestList;
    }
}
