package org.mskcc.cmo.metadb.service.exception;

import org.springframework.http.HttpStatus;

/**
 *
 * @author ochoaa
 */
public class MetadbWebServiceException extends Exception {
    private String responseBody;
    private HttpStatus statusCode;

    /**
     * MetadbWebServiceException constructor.
     * @param responseBody
     */
    public MetadbWebServiceException(String responseBody) {
        super();
        this.responseBody = responseBody;
        this.statusCode = HttpStatus.SERVICE_UNAVAILABLE;
    }

    /**
     * MetadbWebServiceException constructor.
     * @param responseBody
     * @param statusCode
     */
    public MetadbWebServiceException(String responseBody, HttpStatus statusCode) {
        super();
        this.responseBody = responseBody;
        this.statusCode = statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(HttpStatus statusCode) {
        this.statusCode = statusCode;
    }
}
