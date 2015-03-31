/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.impl;

import static org.opendaylight.iotdm.onem2m.core.Onem2m.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClient;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClientBuilder;
import org.opendaylight.iotdm.onem2m.client.ResourceContainerBuilder;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mbenchmark.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mbenchmark.rev150105.TestStatus.ExecStatus;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mbenchmarkProvider implements Onem2mbenchmarkService, BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mbenchmarkProvider.class);
    private DataBroker dataBroker;
    protected Onem2mService onem2mService;
    private final AtomicReference<ExecStatus> execStatus = new AtomicReference<TestStatus.ExecStatus>( ExecStatus.Idle );
    private static final InstanceIdentifier<TestStatus> TEST_STATUS_IID = InstanceIdentifier.builder(TestStatus.class).build();
    long createsPerSec, retrievesPerSec, updatesPerSec, deletesPerSec;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        session.addRpcImplementation(Onem2mbenchmarkService.class, this);
        onem2mService = session.getRpcService(Onem2mService.class);
        this.dataBroker = session.getSALService(DataBroker.class);
        setTestOperData(this.execStatus.get());

        LOG.info("Onem2mbenchmarkProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        LOG.info("Onem2mbenchmarkProvider Closed");
    }

    /**
     * RPC to start tests.
     * @param input
     * @return
     */
    @Override
    public Future<RpcResult<StartTestOutput>> startTest(StartTestInput input) {
        // Check if there is a test in progress
        if ( execStatus.compareAndSet(ExecStatus.Idle, ExecStatus.Executing) == false ) {
            LOG.info("Test in progress");
            return RpcResultBuilder.success(new StartTestOutputBuilder()
                    .setStatus(StartTestOutput.Status.TESTINPROGRESS)
                    .build()).buildFuture();
        }

        switch (input.getOperation()) {
            case PERFCRUD:

                long numResources = input.getNumResources();
                LOG.info("Test started: numResources: {}", numResources);

                if (runPerfTest((int) numResources)) {

                    LOG.info("Test finished");
                    setTestOperData(ExecStatus.Idle);
                    execStatus.set(ExecStatus.Idle);

                    StartTestOutput output = new StartTestOutputBuilder()
                            .setStatus(StartTestOutput.Status.OK)
                            .setCreatesPerSec(createsPerSec)
                            .setRetrievesPerSec(retrievesPerSec)
                            .setUpdatesPerSec(updatesPerSec)
                            .setDeletesPerSec(deletesPerSec)
                            .build();

                    return RpcResultBuilder.success(output).buildFuture();
                }
                break;
        }
        execStatus.set(ExecStatus.Idle);
        return RpcResultBuilder.success(new StartTestOutputBuilder()
                .setStatus(StartTestOutput.Status.FAILED)
                .build()).buildFuture();
    }

    private void setTestOperData(ExecStatus sts) {
        TestStatus status = new TestStatusBuilder()
                .setExecStatus(sts)
                .build();

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, TEST_STATUS_IID, status);

        try {
            tx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            throw new IllegalStateException(e);
        }

        LOG.info("DataStore test oper status populated: {}", status);
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
    private boolean runPerfTest(int numResources) {

        List<String> resourceList = new ArrayList<String>(numResources);

        if (!createTest(resourceList, numResources))
            return false;
        if (!retrieveTest(resourceList, numResources))
            return false;
        if (!deleteTest(resourceList, numResources))
            return false;
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
                .setOperation(Onem2m.Operation.CREATE)
                .setContent(containerString)
                .build();

        startTime = System.nanoTime();
        for (int i = 0; i < numResources; i++) {
            ResponsePrimitive onem2mResponse = serviceOnenm2mRequest(onem2mRequest, onem2mService);
            String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
            try {
                JSONObject j = new JSONObject(responseContent);
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
                    .setOperation(Onem2m.Operation.RETRIEVE)
                    .build();

            ResponsePrimitive onem2mResponse = serviceOnenm2mRequest(onem2mRequest, onem2mService);
            String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
            try {
                JSONObject j = new JSONObject(responseContent);
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
                    .setOperation(Onem2m.Operation.DELETE)
                    .build();

            ResponsePrimitive onem2mResponse = serviceOnenm2mRequest(onem2mRequest, onem2mService);
            String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
            try {
                JSONObject j = new JSONObject(responseContent);
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
