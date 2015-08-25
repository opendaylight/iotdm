/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.impl;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.opendaylight.iotdm.onem2m.client.Onem2mResponse;
import org.opendaylight.iotdm.onem2m.client.ResourceContainerBuilder;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.odlclient.OdlOnem2mCoapClient;
import org.opendaylight.iotdm.onem2m.odlclient.OdlOnem2mCoapRequestPrimitive;
import org.opendaylight.iotdm.onem2m.odlclient.OdlOnem2mCoapRequestPrimitiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The PerfCoapClient here is intended to be run as a test outside of the onem2m core.  This could be run as
 * a feature running in the same karaf instance as the core(server) in which case use coap://localhost to
 * attach to the server.  Also, it can be run in a completely separate instance and it could test the coap
 * performance of another onem2m core running in another karaf instance on another server.
 *
 * The OdlOnem2mCoapClient, OdlOnem2mCoapRequestPrimitive, and OdlOnem2mCoapRequestPrimitiveBuilder are
 * classes that try to implement the oneM2M protocol but hide the nuances of CoAP and the binding rules as
 * per TS0008 ... let's see how successful I'll be at achieving this.
 *
 * At some point, I'll have to add notifications to this test suite, so I'll have to add a server inside the
 * client with a handler that I can call when a notification arrives.  Stand by for this.
 */
public class PerfCoapClient {

    private static final Logger LOG = LoggerFactory.getLogger(PerfCoapClient.class);
    public long createsPerSec, retrievesPerSec, crudsPerSec, deletesPerSec;

    public PerfCoapClient() {
    }

    /**
     * Run a performance test that create 'numResources' and records how long it took, then it will retrieve
     * each of the created resources, then update each of the resources, then finally delete each of the originally
     * created resources.  This test creates resources under a special cseBase that has been specially designed
     * to hold resources for this performance test.  The other cseBase's in the system are unaffected.
     *
     * I was thinking that when one deploys this feature, they might want to have some notion of how well it will
     * perform in their environment.  Conceivably, an administration/diagnostic function could be implemented that
     * would invoke the rpc with some number of resources, and the operator could know what performance to expect.
     * @param numResources
     * @return
     */
    public boolean runPerfTest(int numResources, String serverUri) {

        List<String> resourceList = new ArrayList<String>(numResources);

        if (createTest(resourceList, numResources, serverUri) &&
            retrieveTest(resourceList, numResources, serverUri) &&
            deleteTest(resourceList, numResources, serverUri)) {
            LOG.info("runPerfTest: all tests finished");
        } else {
            LOG.error("runPerfTest: tests failed early");
            return false;
        }
        return true;
    }

    /**
     * Create sample container resources underneath the special perfTest cse
     * @param resourceList
     * @param numResources
     * @return
     */
    private boolean createTest(List<String> resourceList, int numResources, String serverUri) {

        long startTime, endTime, delta;
        int numProcessed = 0;
        OdlOnem2mCoapRequestPrimitive coapRequest;

        OdlOnem2mCoapClient coapClient = new OdlOnem2mCoapClient(serverUri);

        String containerString = new ResourceContainerBuilder()
                .setCreator(null)
                .setMaxNrInstances(5)
                .setOntologyRef("http://ontology/ref")
                .setMaxByteSize(100)
                .build();

        startTime = System.nanoTime();
        for (int i = 0; i < numResources; i++) {

            coapRequest = new OdlOnem2mCoapRequestPrimitiveBuilder()
                    .setOperationCreate()
                    .setTo("/" + Onem2m.SYS_PERF_TEST_CSE)
                    .setFrom("PerfCoap_FROM")
                    .setRequestIdentifier("PerfCoap_RQI")
                    .setContent(containerString, MediaTypeRegistry.APPLICATION_JSON)
                    .setResultContent("1")
                    .setResourceType(3) // container
                    .build();

            CoapResponse coapResponse = coapClient.sendRequest(coapRequest);
            String responseContent = coapResponse.getResponseText();
            CoAP.ResponseCode rsc = coapResponse.getCode();
            try {
                Onem2mResponse or = new Onem2mResponse(responseContent);
                JSONObject j = or.getJSONObject();
                String resourceId = j.getString("ri");
                if (resourceId == null) {
                    LOG.error("Create cannot parse resourceId (iteration: {})", i);
                    break;
                }
                resourceList.add(resourceId);
                ++numProcessed;
            } catch (JSONException e) {
                LOG.error("Create parse responseContent error: {}", e);
                break;
            }
        }
        endTime = System.nanoTime();
        delta = (endTime-startTime);
        createsPerSec = nPerSecond(numResources, delta);
        LOG.info("Time to create ... num/total: {}/{}, delta: {}ns, ops/s: {}",
                numProcessed, numResources, delta, createsPerSec);
        return numProcessed == numResources;
    }

    /**
     * Retrieve all the created resources (this ensures they were created).
     * @param resourceList
     * @param numResources
     * @return
     */
    private boolean retrieveTest(List<String> resourceList, int numResources, String serverUri) {

        long startTime, endTime, delta;
        int numProcessed = 0;
        OdlOnem2mCoapRequestPrimitive coapRequest;

        OdlOnem2mCoapClient coapClient = new OdlOnem2mCoapClient(serverUri);

        startTime = System.nanoTime();
        for (String resourceId : resourceList) {

            coapRequest = new OdlOnem2mCoapRequestPrimitiveBuilder()
                    .setOperationRetrieve()
                    //.setTo("/" + Onem2m.SYS_PERF_TEST_CSE + "/" + resourceId)
                    .setTo(resourceId)
                    .setFrom("PerfCoap_FROM")
                    .setResultContent("1")
                    .setRequestIdentifier("PerfCoap_RQI")
                    .build();

            CoapResponse coapResponse = coapClient.sendRequest(coapRequest);
            String responseContent = coapResponse.getResponseText();
            CoAP.ResponseCode rsc = coapResponse.getCode();
            try {
                Onem2mResponse or = new Onem2mResponse(responseContent);
                JSONObject j = or.getJSONObject();
                if (!resourceId.contentEquals(j.getString("ri"))) {
                    LOG.error("Retrieve resourceId: {} not found", resourceId);
                    break;
                }
                ++numProcessed;
            } catch (JSONException e) {
                LOG.error("Retrieve parse responseContent error: {}", e);
                break;
            }
        }
        endTime = System.nanoTime();
        delta = (endTime-startTime);
        retrievesPerSec = nPerSecond(numResources, delta);
        LOG.info("Time to retrieve ... num/total: {}/{}, delta: {}ns, ops/s: {}",
                numProcessed, numResources, delta, retrievesPerSec);

        return numProcessed == numResources;
    }

    /**
     * Delete the previously created resources (again, this ensures the resources  actually existed)
     * @param resourceList
     * @param numResources
     * @return
     */
    private boolean deleteTest(List<String> resourceList, int numResources, String serverUri) {

        long startTime, endTime, delta;
        int numProcessed = 0;
        OdlOnem2mCoapRequestPrimitive coapRequest;

        OdlOnem2mCoapClient coapClient = new OdlOnem2mCoapClient(serverUri);

        startTime = System.nanoTime();
        for (String resourceId : resourceList) {

            coapRequest = new OdlOnem2mCoapRequestPrimitiveBuilder()
                    .setOperationDelete()
                    //.setTo("/" + Onem2m.SYS_PERF_TEST_CSE + "/" + resourceId)
                    .setTo(resourceId)
                    .setFrom("PerfCoap_FROM")
                    .setRequestIdentifier("PerfCoap_RQI")
                    .setResultContent("1")
                    .build();

            CoapResponse coapResponse = coapClient.sendRequest(coapRequest);
            String responseContent = coapResponse.getResponseText();
            try {
                Onem2mResponse or = new Onem2mResponse(responseContent);
                JSONObject j = or.getJSONObject();
                if (!resourceId.contentEquals(j.getString("ri"))) {
                    LOG.error("Delete resourceId: {} not found", resourceId);
                    break;
                }
                ++numProcessed;
            } catch (JSONException e) {
                LOG.error("Delete parse responseContent error: {}", e);
                break;
            }
        }
        endTime = System.nanoTime();
        delta = (endTime-startTime);
        deletesPerSec = nPerSecond(numResources, delta);
        LOG.info("Time to delete ... num/total: {}/{}, delta: {}ns, ops/s: {}",
                numProcessed, numResources, delta, deletesPerSec);

        return numProcessed == numResources;
    }

    /**
     * There must be a better way of discerning how many ops per sec.  The delta is measured in nanoseconds.
     *
     * @param num
     * @param delta
     * @return
     */
    private long nPerSecond(int num, long delta) {
        long n;

        if (delta >= 1000000000) {
            n = num / (delta / 1000000000);
        } else if (delta >= 100000000) {
            n =  (num * 10)/ (delta / 100000000);
        } else if (delta >= 10000000) {
            n =  (num * 100)/ (delta / 10000000);
        } else if (delta >= 1000000) {
            n =  (num * 1000)/ (delta / 1000000);
        } else if (delta >= 100000) {
            n =  (num * 10000)/ (delta / 100000);
        } else if (delta >= 10000) {
            n = (num * 100000)/ (delta / 10000);
        } else if (delta >= 1000) {
            n = (num * 1000000)/ (delta / 1000);
        } else if (delta >= 100) {
            n = (num * 10000000)/ (delta / 100);
        } else if (delta >= 10) {
            n = (num * 100000000)/ (delta / 10);
        } else {
            n = num * 1000000000 / delta;
        }
        return n;
    }
}
