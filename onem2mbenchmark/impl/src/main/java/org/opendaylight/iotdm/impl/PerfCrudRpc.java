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

    public long createsPerSec, retrievesPerSec, crudsPerSec, deletesPerSec;
    private ExecutorService executor;
    private Integer nextQueueId = 0;
    private Integer numSuccessful = 0;
    private Integer numComplete = 0;

    private synchronized Integer getNextQ() {
        return nextQueueId++;
    }
    private synchronized void incNumSuccessful() {
        ++numSuccessful;
    }
    private synchronized void incNumComplete() {
        ++numComplete;
    }

    public PerfCrudRpc(Onem2mService onem2mService) {
        this.onem2mService = onem2mService;
        executor = null;
    }

    private ArrayList<ArrayList<Integer>> resourceIdQueues;
    private void buildResourceIdQueues(int numQueues, int numResources) {
        resourceIdQueues = new ArrayList<ArrayList<Integer>>(numQueues);
        for (int i = 0; i < numQueues; ++i) {
            ArrayList<Integer> resourceArray = new ArrayList<Integer>(numResources/numQueues+1);
            resourceIdQueues.add(resourceArray);
        }
        for (int i = 0; i < numResources; i++) {
            int q = i%numQueues;
            ArrayList<Integer> resourceArray = resourceIdQueues.get(q);
            resourceArray.add(i+1);
        }
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
    public boolean runPerfTest(int numResources, int numThreads) {

        int totalSuccessful = 0;
        totalSuccessful += createTest(numResources, numThreads);
        totalSuccessful += retrieveTest(numResources, numThreads);
        totalSuccessful += deleteTest(numResources, numThreads);
        totalSuccessful += crudTest(numResources, numThreads);

        return (numResources*4) == totalSuccessful;
    }


    private int createTest(int numResources, final int numThreads) {

        long startTime, endTime, delta;
        nextQueueId = 0;
        numComplete = 0;
        numSuccessful = 0;

        executor = Executors.newFixedThreadPool(numThreads);

        buildResourceIdQueues(numThreads, numResources);

        startTime = System.nanoTime();

        for (int i = 0; i < numThreads; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    runCreateTests(getNextQ());
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

        executor.shutdown(); // kill the threads

        return numSuccessful;
    }

    private void runCreateTests(Integer q) {
        ArrayList<Integer> resourceIdArray = resourceIdQueues.get(q);
        for (Integer resourceId : resourceIdArray) {
            if (createOneTest(resourceId)) {
                incNumSuccessful();
            }
            incNumComplete();
        }
    }

    private void runRetrieveTests(Integer q) {
        ArrayList<Integer> resourceIdArray = resourceIdQueues.get(q);
        for (Integer resourceId : resourceIdArray) {
            if (retrieveOneTest(resourceId)) {
                incNumSuccessful();
            }
            incNumComplete();
        }
    }

    private void runDeleteTests(Integer q) {
        ArrayList<Integer> resourceIdArray = resourceIdQueues.get(q);
        for (Integer resourceId : resourceIdArray) {
            if (deleteOneTest(resourceId)) {
                incNumSuccessful();
            }
            incNumComplete();
        }
    }

    private boolean createOneTest(Integer resourceId) {

        Container b;

        b = new Container();
        b.setTo("/" + Onem2m.SYS_PERF_TEST_CSE);
        b.setOperationCreate();
        b.setMaxNrInstances(5);
        b.setCreator(null);
        b.setMaxByteSize(100);
        b.setOntologyRef("http:/whoa/nelly");
        b.setName("RN_" + resourceId);
        Onem2mRequestPrimitiveClient req = b.build();

        Onem2mResponsePrimitiveClient res = req.send(onem2mService);
        if (!res.responseOk()) {
            LOG.error(res.getError());
            return false;
        }
        Onem2mContainerResponse ctrResponse = new Onem2mContainerResponse(res.getContent());
        if (!ctrResponse.responseOk()) {
            LOG.error("Container create request: {}", ctrResponse.getError());
            return false;
        }

        return true;
    }

    private boolean retrieveOneTest(Integer resourceId) {

        String tempResourceId = "/" + Onem2m.SYS_PERF_TEST_CSE + "/RN_" + resourceId;

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
        if (rscString == null || rscString.charAt(0) != '2') {
            LOG.error("retrieve: error code returned: {}, {}", rscString, responseContent);
            return false;
        }
        try {
            Onem2mResponse or = new Onem2mResponse(responseContent);
            JSONObject j = or.getJSONObject();
            String resourceName = j.getString(ResourceContent.RESOURCE_NAME);
            String expectedResourceName = "RN_" + resourceId;
            if (resourceName == null || !resourceName.contentEquals(expectedResourceName)) {
                LOG.error("retrieve: resource name error: expected {}, received {}, json: {}",
                        expectedResourceName, resourceName, j.toString());
                return false;
            }
        } catch (JSONException e) {
            LOG.error("Retrieve parse responseContent error: {}", e);
            return false;
        }

        return true;
    }


    private int retrieveTest(int numResources, final int numThreads) {

        long startTime, endTime, delta;
        nextQueueId = 0;
        numComplete = 0;
        numSuccessful = 0;

        executor = Executors.newFixedThreadPool(numThreads);

        buildResourceIdQueues(numThreads, numResources);

        startTime = System.nanoTime();

        for (int i = 0; i < numThreads; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    runRetrieveTests(getNextQ());
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

        executor.shutdown(); // kill the threads

        return numSuccessful;
    }

    private boolean deleteOneTest(Integer resourceId) {

        String tempResourceId = "/" + Onem2m.SYS_PERF_TEST_CSE + "/RN_" + resourceId;

        Onem2mRequestPrimitiveClient onem2mRequest = new Onem2mRequestPrimitiveClientBuilder()
                .setProtocol(Onem2m.Protocol.NATIVEAPP)
                .setContentFormat(Onem2m.ContentFormat.JSON)
                .setTo(tempResourceId)
                .setFrom("")
                .setResultContent("1")
                .setRequestIdentifier("RQI_1234")
                .setOperationDelete()
                .build();

        ResponsePrimitive onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);
        String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
        String rscString = onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE);
        if (rscString == null || rscString.charAt(0) != '2') {
            LOG.error("delete: error code returned: {}, {}", rscString, responseContent);
            return false;
        }
        try {
            Onem2mResponse or = new Onem2mResponse(responseContent);
            JSONObject j = or.getJSONObject();
            String resourceName = j.getString(ResourceContent.RESOURCE_NAME);
            String expectedResourceName = "RN_" + resourceId;
            if (resourceName == null || !resourceName.contentEquals(expectedResourceName)) {
                LOG.error("delete: resource name error: expected {}, received {}, json: {}",
                        expectedResourceName, resourceName, j.toString());
                return false;
            }
        } catch (JSONException e) {
            LOG.error("Delete parse responseContent error: {}", e);
            return false;
        }
        return true;
    }

    private int deleteTest(int numResources, final int numThreads) {

        long startTime, endTime, delta;
        nextQueueId = 0;
        numComplete = 0;
        numSuccessful = 0;

        executor = Executors.newFixedThreadPool(numThreads);

        buildResourceIdQueues(numThreads, numResources);

        startTime = System.nanoTime();

        for (int i = 0; i < numThreads; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    runDeleteTests(getNextQ());
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

        executor.shutdown(); // kill the threads

        return numSuccessful;
    }

    private boolean crudOneTest(Integer resourceId) {

        if (createOneTest(resourceId) && retrieveOneTest(resourceId) && deleteOneTest(resourceId)) {
            return true;
        }

        return false;
    }

    private int crudTest(int numResources, final int numThreads) {

        long startTime, endTime, delta;
        nextQueueId = 0;
        numComplete = 0;
        numSuccessful = 0;

        executor = Executors.newFixedThreadPool(numThreads);

        buildResourceIdQueues(numThreads, numResources);

        startTime = System.nanoTime();

        for (int i = 0; i < numThreads; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    runCrudTests(getNextQ());
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
        crudsPerSec = nPerSecond(numResources, delta);
        LOG.info("Time to CRUD ... num/total: {}/{}, delta: {}ns, ops/s: {}", numSuccessful, numResources, delta, crudsPerSec);

        executor.shutdown(); // kill the threads

        return numSuccessful;
    }

    private void runCrudTests(Integer q) {
        ArrayList<Integer> resourceIdArray = resourceIdQueues.get(q);
        for (Integer resourceId : resourceIdArray) {
            if (crudOneTest(resourceId)) {
                incNumSuccessful();
            }
            incNumComplete();
        }
    }

    private long nPerSecond(int num, long delta) {

        double secondsTotal = (double)delta / (double)1000000000;
        return (long) (((double)num / secondsTotal));
    }
}
