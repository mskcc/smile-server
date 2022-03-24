package org.mskcc.smile.service.exception;

import org.springframework.http.HttpStatus;

/**
 *
 * @author ochoaa
 */
public class SmileWebServiceException extends Exception {
    private String responseBody;
    private HttpStatus statusCode;

    /**
     * SmileWebServiceException constructor.
     * @param responseBody
     */
    public SmileWebServiceException(String responseBody) {
        super();
        this.responseBody = responseBody;
        this.statusCode = HttpStatus.SERVICE_UNAVAILABLE;
    }

    /**
     * SmileWebServiceException constructor.
     * @param responseBody
     * @param statusCode
     */
    public SmileWebServiceException(String responseBody, HttpStatus statusCode) {
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
