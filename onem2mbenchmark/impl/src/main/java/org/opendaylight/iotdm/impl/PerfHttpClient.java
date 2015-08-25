/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.impl;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jetty.client.ContentExchange;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.client.Onem2mResponse;
import org.opendaylight.iotdm.onem2m.client.ResourceContainerBuilder;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.odlclient.OdlOnem2mHttpClient;
import org.opendaylight.iotdm.onem2m.odlclient.OdlOnem2mHttpRequestPrimitive;
import org.opendaylight.iotdm.onem2m.odlclient.OdlOnem2mHttpRequestPrimitiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerfHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(PerfHttpClient.class);
    public long createsPerSec, retrievesPerSec, crudsPerSec, deletesPerSec;

    public PerfHttpClient() {
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
        OdlOnem2mHttpRequestPrimitive httpRequest;

        OdlOnem2mHttpClient httpClient = new OdlOnem2mHttpClient();

        String containerString = new ResourceContainerBuilder()
                .setCreator(null)
                .setMaxNrInstances(5)
                .setOntologyRef("http://ontology/ref")
                .setMaxByteSize(100)
                .build();

        startTime = System.nanoTime();
        for (int i = 0; i < numResources; i++) {

            httpRequest = new OdlOnem2mHttpRequestPrimitiveBuilder()
                    .setOperationCreate()
                    .setTo("/" + Onem2m.SYS_PERF_TEST_CSE)
                    .setFrom("Perfhttp_FROM")
                    .setRequestIdentifier("Perfhttp_RQI")
                    .setContent(containerString)
                    .setResourceType(3) // container
                    .setResultContent("1")
                    .build();

            ContentExchange httpResponse = httpClient.sendRequest(serverUri, httpRequest);
            String responseContent = null;
            int rsc = 0;
            try {
                responseContent = httpResponse.getResponseContent();
                 rsc = httpResponse.getResponseStatus();
            } catch (UnsupportedEncodingException e) {
                LOG.error("get http content exception: {}", e.toString());
                break;
            }
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
        OdlOnem2mHttpRequestPrimitive httpRequest;

        OdlOnem2mHttpClient httpClient = new OdlOnem2mHttpClient();

        startTime = System.nanoTime();
        for (String resourceId : resourceList) {

            httpRequest = new OdlOnem2mHttpRequestPrimitiveBuilder()
                    .setOperationRetrieve()
                            //.setTo("/" + Onem2m.SYS_PERF_TEST_CSE + "/" + resourceId)
                    .setTo(resourceId)
                    .setFrom("Perfhttp_FROM")
                    .setRequestIdentifier("Perfhttp_RQI")
                    .setResultContent("1")
                    .build();

            ContentExchange httpResponse = httpClient.sendRequest(serverUri, httpRequest);
            String responseContent = null;
            int rsc = 0;
            try {
                responseContent = httpResponse.getResponseContent();
                rsc = httpResponse.getResponseStatus();
            } catch (UnsupportedEncodingException e) {
                LOG.error("get http content exception: {}", e.toString());
                break;
            }
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
        OdlOnem2mHttpRequestPrimitive httpRequest;

        OdlOnem2mHttpClient httpClient = new OdlOnem2mHttpClient();

        startTime = System.nanoTime();
        for (String resourceId : resourceList) {

            httpRequest = new OdlOnem2mHttpRequestPrimitiveBuilder()
                    .setOperationDelete()
                            //.setTo("/" + Onem2m.SYS_PERF_TEST_CSE + "/" + resourceId)
                    .setTo(resourceId)
                    .setFrom("Perfhttp_FROM")
                    .setRequestIdentifier("Perfhttp_RQI")
                    .setResultContent("1")
                    .build();

            ContentExchange httpResponse = httpClient.sendRequest(serverUri, httpRequest);
            String responseContent = null;
            int rsc = 0;
            try {
                responseContent = httpResponse.getResponseContent();
                rsc = httpResponse.getResponseStatus();
            } catch (UnsupportedEncodingException e) {
                LOG.error("get http content exception: {}", e.toString());
                break;
            }
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
