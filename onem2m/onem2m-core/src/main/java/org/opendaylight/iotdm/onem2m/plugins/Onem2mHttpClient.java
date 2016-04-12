/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mHttpClient {

    private final Logger LOG = LoggerFactory.getLogger(Onem2mHttpClient.class);

    private HttpClient httpClient;

    public Onem2mHttpClient() {
        httpClient = new HttpClient();
        try {
            httpClient.start();
        } catch (Exception e) {
            LOG.error("Issue starting httpClient: {}", e.toString());
        }
    }

    public ContentExchange sendRequest(String url, ContentExchange httpRequest) {

        httpRequest.setURL(url);
        try {
            httpClient.send(httpRequest);
        } catch (IOException e) {
            LOG.error("Issues with httpClient.send: {}", e.toString());
        }
        int ex = HttpExchange.STATUS_EXCEPTED;
        // Waits until the exchange is terminated
        try {
            ex = httpRequest.waitForDone();
        } catch (InterruptedException e) {
            LOG.error("Issues with waitForDone: {}", e.toString());
        }
        return httpRequest;
    }

    public String processRequest(String url, ContentExchange httpRequest) {

        ContentExchange httpResponse = sendRequest(url, httpRequest);

        try {
            String responseContent = httpResponse.getResponseContent();
            int rsc = httpResponse.getResponseStatus();
            LOG.info("handleRequest: responseStatus: {}, responseContent: {}", rsc, responseContent);
            if (rsc < 200 || rsc >= 300) {
                LOG.error("handleRequest: httpStatusCode: {} ...", rsc);
                return null;
            }
            LOG.info("handleRequest:content: {}", responseContent);
            return responseContent;
        } catch (UnsupportedEncodingException e) {
            LOG.error("get http content exception: {}", e.toString());
        }

        return null;
    }

    public void stop() throws Exception {
        httpClient.stop();
    }
}