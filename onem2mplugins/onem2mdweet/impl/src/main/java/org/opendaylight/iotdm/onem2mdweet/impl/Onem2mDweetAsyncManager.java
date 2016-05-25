/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2mdweet.impl;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.server.Server;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Onem2mDweetAsyncManager implements AutoCloseable{

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mDweetAsyncManager.class);
    private HttpClient client;

    public Onem2mDweetAsyncManager() {
        client = new HttpClient();
        LOG.info("Created Onem2mDweetAsyncManager");

        try {
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() throws Exception{
        client.stop();
        LOG.info("Onem2mDweetAsyncManager Closed");
    }


    /**
     *
     * @param url
     * @param onem2mResource
     */
    public void sendResourceUpdate(String url, Onem2mResource onem2mResource) {



        // problem, does not recognize / , do we change / to %2F as dweet website?
        String payload = onem2mResource.getResourceContentJsonString();
        ContentExchange ex = new ContentExchange();
        ex.setURL("https://dweet.io:443/dweet/for/" + onem2mResource.getName());
        ex.setRequestContentSource(new ByteArrayInputStream(payload.getBytes()));
        ex.setRequestContentType("application/json");
        ex.setMethod("post");
        LOG.debug("HTTP: Send notification uri: {}, payload: {}:", url, payload);
        try {
            client.send(ex);
            ex.waitForDone();
            String response = ex.getResponseContent();
            System.out.println(response);
        } catch (IOException e) {
            LOG.error("Dropping notification: uri: {}, payload: {}", url, payload);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

}
