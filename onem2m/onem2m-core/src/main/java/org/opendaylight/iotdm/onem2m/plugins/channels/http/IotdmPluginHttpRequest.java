/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.plugins.channels.http;

import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPluginRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * Implementation of class wrapping original HTTP request.
 */
public class IotdmPluginHttpRequest implements IotdmPluginRequest<HttpServletRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmPluginHttpRequest.class);

    protected final HttpServletRequest httpRequest; // The original HTTP request
    protected HashMap<String, String[]> headers = null; // HashMap with header names as keys and header values as values
    protected String payload = null; // Payload of the received request

    /**
     * Constructor just sets orignal request. Headers and payload are not prepared
     * in advance, lazy initialization is used.
     * @param httpRequest Original HTTP request.
     */
    protected IotdmPluginHttpRequest(@Nonnull final HttpServletRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    @Override
    public String getOnem2mOperation() {
        switch(this.httpRequest.getMethod().toLowerCase()) {
            case "get":
                return Onem2m.Operation.RETRIEVE;
            case "post":
                return Onem2m.Operation.CREATE;
            case "put":
                return Onem2m.Operation.UPDATE;
            case "delete":
                return Onem2m.Operation.DELETE;
            default:
                return null;
        }
    }

    @Override
    public String getOnem2mUri() {
        return Onem2m.translateUriToOnem2m(httpRequest.getRequestURI());
    }

    @Override
    public String getMethod() {
        return this.httpRequest.getMethod();
    }

    /**
     * Returns URL including HTTP query string.
     * @return URL with query string if exists.
     */
    private String getFullUrl() {
        StringBuffer requestURL = this.httpRequest.getRequestURL();
        String queryString = this.httpRequest.getQueryString();

        if (queryString == null) {
            return requestURL.toString();
        } else {
            return requestURL.append('?').append(queryString).toString();
        }
    }

    @Override
    public String getUrl() {
        return this.getFullUrl();
    }

    private void getAndStorePayload() {
        try {
            StringBuilder buffer = new StringBuilder();
            BufferedReader reader = this.httpRequest.getReader();
            String line = null;
            String result = null;

            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }

            result = buffer.toString();
            if (null == result) {
                this.payload = "";
            } else {
                this.payload = result;
            }
        } catch (java.io.IOException e) {
            LOG.error("Failed to read HTTP request payload: {}", e);
            this.payload = "";
        }
    }

    @Override
    public String getPayLoad() {
        if (null == this.payload) {
            this.getAndStorePayload();
        }

        return this.payload;
    }

    @Override
    public String getContentType() {
        return this.httpRequest.getContentType();
    }

    private void getAndStoreAllHeaders() {
        Enumeration<String> headerNames = this.httpRequest.getHeaderNames();
        if (null == headerNames) {
            return;
        }

        this.headers = new HashMap<>();

        for(String headerName = null;  headerNames.hasMoreElements(); headerName = headerNames.nextElement()) {
            String[] retArray = null;
            ArrayList<String> arrayList = Collections.list(this.httpRequest.getHeaders(headerName));
            if (null == arrayList) {
                retArray = new String[0];
            } else {
                retArray = arrayList.toArray(retArray);
            }

            this.headers.put(headerName, retArray);
        }
    }

    @Override
    public HashMap<String, String[]> getHeadersAll() {
        if (null == this.headers) {
            this.getAndStoreAllHeaders();
        }

        return this.headers;
    }

    @Override
    public String[] getHeaders(String key) {
        if (null == this.headers) {
            this.getAndStoreAllHeaders();
        }
        return this.headers.get(key);
    }

    @Override
    public String getHeader(String key) {
        return this.httpRequest.getHeader(key);
    }

    @Override
    public HttpServletRequest getOriginalRequest() {
        return httpRequest;
    }
}
