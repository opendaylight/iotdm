/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.odlclient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OdlOnem2mHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(OdlOnem2mHttpClient.class);

    private HttpClient httpClient;

    public OdlOnem2mHttpClient() {
        httpClient = new HttpClient();
        try {
            httpClient.start();
        } catch (Exception e) {
            LOG.error("Issue starting httpClient: {}", e.toString());
        }
    }

    public ContentExchange sendRequest(String url, OdlOnem2mHttpRequestPrimitive onem2mRequest) {

        onem2mRequest.httpRequest.setURL(url + onem2mRequest.to + "?" + onem2mRequest.uriQueryString);
        try {
            httpClient.send(onem2mRequest.httpRequest);
        } catch (IOException e) {
            LOG.error("Issues with httpClient.send: {}", e.toString());
        }
        int ex = HttpExchange.STATUS_EXCEPTED;
       // Waits until the exchange is terminated
        try {
            ex = onem2mRequest.httpRequest.waitForDone();
        } catch (InterruptedException e) {
            LOG.error("Issues with waitForDone: {}", e.toString());
        }
        return onem2mRequest.httpRequest;
    }
}

