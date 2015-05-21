/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.impl;

import static java.lang.Thread.sleep;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.client.*;
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
    private final ExecutorService executor;
    private Integer nextResourceId = 0;
    private Integer numSuccessful = 0;
    private Integer numComplete = 0;

    private synchronized Integer getNext() {
        return ++nextResourceId;
    }
    private synchronized void incNumSuccessful() {
        ++numSuccessful;
    }
    private synchronized void incNumComplete() {
        ++numComplete;
    }

    public PerfCrudRpc(Onem2mService onem2mService) {
        this.onem2mService = onem2mService;
        executor = Executors.newFixedThreadPool(32);
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

        int totalSuccessful = 0;
        totalSuccessful += createTest(numResources);
        totalSuccessful += retrieveTest(numResources);
        totalSuccessful += deleteTest(numResources);

        return (numResources*3) == totalSuccessful;
    }

    private boolean createOneTest(Integer resourceId) {

        String containerString = new ResourceContainerBuilder()
                .setCreator(null)
                .setMaxNrInstances(5)
                .setOntologyRef("http://ontology/ref")
                .setMaxByteSize(100)
                .build();

        Onem2mRequestPrimitiveClient onem2mRequest = new Onem2mRequestPrimitiveClientBuilder()
                .setProtocol(Onem2m.Protocol.NATIVEAPP)
                .setContentFormat(Onem2m.ContentFormat.JSON)
                .setTo("/" + Onem2m.SYS_PERF_TEST_CSE)
                .setFrom("")
                .setRequestIdentifier("RQI_1234")
                .setResourceType(Onem2m.ResourceType.CONTAINER)
                .setOperationCreate()
                .setPrimitiveContent(containerString)
                .setName(resourceId.toString())
                .build();


        ResponsePrimitive onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);
        String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
        String rscString = onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE);
        try {
            Onem2mResponse or = new Onem2mResponse(responseContent);
            JSONObject j = or.getJSONObject();
            String resourceName = j.getString(ResourceContent.RESOURCE_NAME);
            String tempResourceId = "/" + Onem2m.SYS_PERF_TEST_CSE + "/" + resourceId;
            if (resourceName == null || !resourceName.contentEquals(tempResourceId)) {
                LOG.error("create: resource name error: {}, {}", tempResourceId, resourceName);
                return false;
            }
            incNumSuccessful();
        } catch (JSONException e) {
            LOG.error("Create parse responseContent error: {}", e);
            return false;
        }

        return true;
    }

    private int createTest(int numResources) {

        long startTime, endTime, delta;
        nextResourceId = 0;
        numComplete = 0;
        numSuccessful = 0;

        startTime = System.nanoTime();
        for (int i = 0; i < numResources; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    createOneTest(getNext());
                    incNumComplete();
                }
            });
        }
        while (numComplete != numResources) {
            try {
                sleep(1, 0);
            } catch (InterruptedException e) {
                LOG.error("sleep error: {}", e);
            }
        }
        endTime = System.nanoTime();
        delta = (endTime-startTime);
        createsPerSec = nPerSecond(numResources, delta);
        LOG.info("Time to create ... num/total: {}/{}, delta: {}ns, ops/s: {}", numSuccessful, numResources, delta, createsPerSec);
        return numSuccessful;
    }

    private boolean retrieveOneTest(Integer resourceId) {

        String tempResourceId = "/" + Onem2m.SYS_PERF_TEST_CSE + "/" + resourceId;

        Onem2mRequestPrimitiveClient onem2mRequest = new Onem2mRequestPrimitiveClientBuilder()
                .setProtocol(Onem2m.Protocol.NATIVEAPP)
                .setContentFormat(Onem2m.ContentFormat.JSON)
                .setTo(tempResourceId)
                .setFrom("")
                .setRequestIdentifier("RQI_1234")
                .setOperationRetrieve()
                .build();

        ResponsePrimitive onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);
        String rscString = onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE);
        String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
        try {
            Onem2mResponse or = new Onem2mResponse(responseContent);
            JSONObject j = or.getJSONObject();
            String resourceName = j.getString(ResourceContent.RESOURCE_NAME);
            if (resourceName == null || !resourceName.contentEquals(tempResourceId)) {
                LOG.error("retrieve: resource name error: {}, {}", tempResourceId, resourceName);
                return false;
            }
            incNumSuccessful();
        } catch (JSONException e) {
            LOG.error("Retrieve parse responseContent error: {}", e);
            return false;
        }
        return false;
    }


    private int retrieveTest(int numResources) {

        long startTime, endTime, delta;
        nextResourceId = 0;
        numComplete = 0;
        numSuccessful = 0;

        startTime = System.nanoTime();
        for (int i = 0; i < numResources; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    retrieveOneTest(getNext());
                    incNumComplete();
                }
            });
        }
        while (numComplete != numResources) {
            try {
                sleep(1, 0);
            } catch (InterruptedException e) {
                LOG.error("sleep error: {}", e);
            }
        }
        endTime = System.nanoTime();
        delta = (endTime-startTime);
        retrievesPerSec = nPerSecond(numResources, delta);
        LOG.info("Time to retrieve ... num/total: {}/{}, delta: {}ns, ops/s: {}", numSuccessful, numResources, delta, retrievesPerSec);

        return numSuccessful;
    }

    private boolean deleteOneTest(Integer resourceId) {

        String tempResourceId = "/" + Onem2m.SYS_PERF_TEST_CSE + "/" + resourceId;

        Onem2mRequestPrimitiveClient onem2mRequest = new Onem2mRequestPrimitiveClientBuilder()
                .setProtocol(Onem2m.Protocol.NATIVEAPP)
                .setContentFormat(Onem2m.ContentFormat.JSON)
                .setTo(tempResourceId)
                .setFrom("")
                .setRequestIdentifier("RQI_1234")
                .setOperationDelete()
                .build();

        ResponsePrimitive onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);
        String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
        String rscString = onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE);
        try {
            Onem2mResponse or = new Onem2mResponse(responseContent);
            JSONObject j = or.getJSONObject();
            String resourceName = j.getString(ResourceContent.RESOURCE_NAME);
            if (resourceName == null || !resourceName.contentEquals(tempResourceId)) {
                LOG.error("delete: resource name error: {}, {}", tempResourceId, resourceName);
                return false;
            }
            incNumSuccessful();
        } catch (JSONException e) {
            LOG.error("Delete parse responseContent error: {}", e);
            return false;
        }
        return true;
    }

    private int deleteTest(int numResources) {

        long startTime, endTime, delta;
        nextResourceId = 0;
        numComplete = 0;
        numSuccessful = 0;

        startTime = System.nanoTime();
        for (int i = 0; i < numResources; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    deleteOneTest(getNext());
                    incNumComplete();
                }
            });
        }
        while (numComplete != numResources) {
            try {
                sleep(1, 0);
            } catch (InterruptedException e) {
                LOG.error("sleep error: {}", e);
            }
        }
        endTime = System.nanoTime();
        delta = (endTime-startTime);
        deletesPerSec = nPerSecond(numResources, delta);
        LOG.info("Time to delete ... num/total: {}/{}, delta: {}ns, ops/s: {}", numSuccessful, numResources, delta, deletesPerSec);

        return numSuccessful;
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
