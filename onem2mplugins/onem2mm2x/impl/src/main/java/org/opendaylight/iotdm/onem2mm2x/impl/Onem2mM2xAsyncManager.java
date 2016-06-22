/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2mm2x.impl;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;


public class Onem2mM2xAsyncManager implements AutoCloseable{

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mM2xAsyncManager.class);
    private HttpClient client;
    private String customerDeviceID = "23caa4fd11c335c73b38e5b739a82c74";
    private String customerApiKey = "ab1cb176b2178330e882feaca8c745cf";
    // use the test device as the default key

    public void setCustomerDeviceID(String customerDeviceID) {
        this.customerDeviceID = customerDeviceID;
    }

    public void setCustomerApiKey(String customerApiKey) {
        this.customerApiKey = customerApiKey;
    }



    public Onem2mM2xAsyncManager() {
        client = new HttpClient();
        LOG.info("Created Onem2mM2xAsyncManager");

        try {
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() throws Exception{
        client.stop();
        LOG.info("Onem2mM2xAsyncManager Closed");
    }


    /**
     *
     * @param url
     * @param onem2mResource
     */
    public void sendResourceUpdate(String url, Onem2mResource onem2mResource) {

        if (onem2mResource.getResourceType().contentEquals(Onem2m.ResourceType.CONTENT_INSTANCE)) {
            // only monitor the contentInstance
            String payload = onem2mResource.getResourceContentJsonString();
            ContentExchange ex = new ContentExchange();
            ex.setURL("http://api-m2x.att.com/v2/devices/" + customerDeviceID + "/streams/location/value");
            //todo: change "location" to customize
            JSONObject value = new JSONObject();
            JsonUtils.put(value, "value", payload);
            ex.setRequestContentSource(new ByteArrayInputStream(value.toString().getBytes()));
            ex.setRequestContentType("application/json");
            ex.setMethod("put");
            ex.setRequestHeader("X-M2X-KEY", customerApiKey);
            LOG.debug("HTTP: Send notification uri: {}, payload: {}:", url, payload);
            sendExchange(url, payload, ex);
        }

    }


    private void sendExchange(String url, String payload, ContentExchange ex) {
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




