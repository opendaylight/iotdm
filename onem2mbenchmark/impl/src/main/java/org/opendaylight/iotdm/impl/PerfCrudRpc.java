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
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClient;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClientBuilder;
import org.opendaylight.iotdm.onem2m.client.ResourceContainerBuilder;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerfCrudRpc {

    private static final Logger LOG = LoggerFactory.getLogger(PerfCrudRpc.class);
    private Onem2mService onem2mService;
    public long createsPerSec, retrievesPerSec, updatesPerSec, deletesPerSec;

    public PerfCrudRpc(Onem2mService onem2mService) {
        this.onem2mService = onem2mService;
    }
    /**
     * Run a performance test that create 'numResources' and records how long it took, then it will retrieve
     * each of the created resources, then update each of the resources, then finally delete each of the originally
     * created resources.  This test creates resources under a special cseBase that has been specially designed
     * to hold resoources for this performance test.  The other cseBase's in the system are unaffected.
     *
     * I was thinking that when one deploys this feature, they might want to have some notion of how well it will
     * perform in their environment.  Conceivably, an administration/diagnostic function could be implemented that
     * would invoke the rpc with some number of resources, and the operator could know what performance to expect.
     * @param numResources
     * @return
     */
    public boolean runPerfTest(int numResources) {

        List<String> resourceList = new ArrayList<String>(numResources);

        if (createTest(resourceList, numResources) &&
            retrieveTest(resourceList, numResources) &&
            deleteTest(resourceList, numResources)) {
            LOG.info("runPerfTest: all tests finished");
        } else {
            LOG.error("runPerfTest: tests failed early");
        }
        return true;
    }

    /**
     * Create sample container resources underneath the special perfTest cse
     * @param resourceList
     * @param numResources
     * @return
     */
    private boolean createTest(List<String> resourceList, int numResources) {

        long startTime, endTime, delta;
        int numProcessed = 0;

        String containerString = new ResourceContainerBuilder()
                .setCreator("Creator")
                .setMaxNrInstances("5")
                .setOntologyRef("http://ontology/ref")
                .setMaxByteSize("100")
                .setMaxInstanceAge("1")
                .build();

        Onem2mRequestPrimitiveClient onem2mRequest = new Onem2mRequestPrimitiveClientBuilder()
                .setProtocol(Onem2m.Protocol.NATIVEAPP)
                .setContentFormat(Onem2m.ContentFormat.JSON)
                .setTo("/" + Onem2m.SYS_PERF_TEST_CSE)
                .setFrom("")
                .setRequestIdentifier("RQI_1234")
                .setResourceType("cnt")
                .setOperationCreate()
                .setContent(containerString)
                .build();

        startTime = System.nanoTime();
        for (int i = 0; i < numResources; i++) {
            ResponsePrimitive onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);
            String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
            String rscString = onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE);
            try {
                JSONObject j = new JSONObject(responseContent);
                String resourceId = j.getString(ResourceContent.RESOURCE_ID);
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
        LOG.info("Time to create ... num/total: {}/{}, delta: {}ns, ops/s: {}", numProcessed, numResources, delta, createsPerSec);
        return numProcessed == numResources;
    }

    /**
     * Retrieve all the created resources (this ensures they were created).
     * @param resourceList
     * @param numResources
     * @return
     */
    private boolean retrieveTest(List<String> resourceList, int numResources) {

        long startTime, endTime, delta;
        int numProcessed = 0;

        startTime = System.nanoTime();
        for (String resourceId : resourceList) {

            Onem2mRequestPrimitiveClient onem2mRequest = new Onem2mRequestPrimitiveClientBuilder()
                    .setProtocol(Onem2m.Protocol.NATIVEAPP)
                    .setContentFormat(Onem2m.ContentFormat.JSON)
                    .setTo("/" + Onem2m.SYS_PERF_TEST_CSE + "/" + resourceId)
                    .setFrom("")
                    .setRequestIdentifier("RQI_1234")
                    .setOperationRetrieve()
                    .build();

            ResponsePrimitive onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);
            String rscString = onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE);
            String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
            try {
                JSONObject j = new JSONObject(responseContent);
                if (!resourceId.contentEquals(j.getString(ResourceContent.RESOURCE_ID))) {
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
        LOG.info("Time to retrieve ... num/total: {}/{}, delta: {}ns, ops/s: {}", numProcessed, numResources, delta, retrievesPerSec);

        return numProcessed == numResources;
    }

    /**
     * Delete the previously created resources (again, this ensures the resources  actually existed)
     * @param resourceList
     * @param numResources
     * @return
     */
    private boolean deleteTest(List<String> resourceList, int numResources) {

        long startTime, endTime, delta;
        int numProcessed = 0;

        startTime = System.nanoTime();
        for (String resourceId : resourceList) {

            Onem2mRequestPrimitiveClient onem2mRequest = new Onem2mRequestPrimitiveClientBuilder()
                    .setProtocol(Onem2m.Protocol.NATIVEAPP)
                    .setContentFormat(Onem2m.ContentFormat.JSON)
                    .setTo("/" + Onem2m.SYS_PERF_TEST_CSE + "/" + resourceId)
                    .setFrom("")
                    .setRequestIdentifier("RQI_1234")
                    .setOperationDelete()
                    .build();

            ResponsePrimitive onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);
            String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
            String rscString = onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE);
            try {
                JSONObject j = new JSONObject(responseContent);
                if (!resourceId.contentEquals(j.getString(ResourceContent.RESOURCE_ID))) {
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
        LOG.info("Time to delete ... num/total: {}/{}, delta: {}ns, ops/s: {}", numProcessed, numResources, delta, deletesPerSec);

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
