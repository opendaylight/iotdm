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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mbenchmark.rev150105.Onem2mbenchmarkService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mbenchmark.rev150105.StartTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mbenchmark.rev150105.StartTestOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mbenchmark.rev150105.TestStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mbenchmark.rev150105.TestStatusBuilder;
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

        long numResources;
        String serverUri;

        switch (input.getOperation()) {

            case PERFRPC:
                numResources = input.getNumResources();
                LOG.info("Test started: numResources: {}", numResources);
                PerfCrudRpc perfCrudRpc = new PerfCrudRpc(onem2mService);
                if (perfCrudRpc.runPerfTest((int) numResources)) {
                    setTestOperData(ExecStatus.Idle);
                    execStatus.set(ExecStatus.Idle);

                    StartTestOutput output = new StartTestOutputBuilder()
                            .setStatus(StartTestOutput.Status.OK)
                            .setCreatesPerSec(perfCrudRpc.createsPerSec)
                            .setRetrievesPerSec(perfCrudRpc.retrievesPerSec)
                            .setUpdatesPerSec(perfCrudRpc.updatesPerSec)
                            .setDeletesPerSec(perfCrudRpc.deletesPerSec)
                            .build();

                    return RpcResultBuilder.success(output).buildFuture();
                }
                break;

            case PERFCOAP:
                numResources = input.getNumResources();
                serverUri = input.getServerUri();
                LOG.info("Test started: numResources: {}", numResources);
                PerfCoapClient perfCoapClient = new PerfCoapClient();
                if (perfCoapClient.runPerfTest((int) numResources, serverUri)) {
                    setTestOperData(ExecStatus.Idle);
                    execStatus.set(ExecStatus.Idle);

                    StartTestOutput output = new StartTestOutputBuilder()
                            .setStatus(StartTestOutput.Status.OK)
                            .setCreatesPerSec(perfCoapClient.createsPerSec)
                            .setRetrievesPerSec(perfCoapClient.retrievesPerSec)
                            .setUpdatesPerSec(perfCoapClient.updatesPerSec)
                            .setDeletesPerSec(perfCoapClient.deletesPerSec)
                            .build();

                    return RpcResultBuilder.success(output).buildFuture();
                }
                break;

            case PERFHTTP:
                numResources = input.getNumResources();
                LOG.info("Test started: numResources: {}", numResources);
                PerfHttpClient perfHttpClient = new PerfHttpClient();
                if (perfHttpClient.runPerfTest((int) numResources)) {
                    setTestOperData(ExecStatus.Idle);
                    execStatus.set(ExecStatus.Idle);

                    StartTestOutput output = new StartTestOutputBuilder()
                            .setStatus(StartTestOutput.Status.OK)
                            .setCreatesPerSec(perfHttpClient.createsPerSec)
                            .setRetrievesPerSec(perfHttpClient.retrievesPerSec)
                            .setUpdatesPerSec(perfHttpClient.updatesPerSec)
                            .setDeletesPerSec(perfHttpClient.deletesPerSec)
                            .build();

                    return RpcResultBuilder.success(output).buildFuture();
                }
                break;

            case BASICSANITY:
                LOG.info("Test started: ...");
                BasicSanityRpc perfBasicSanity = new BasicSanityRpc(onem2mService);
                if (perfBasicSanity.runTestSuite()) {
                    setTestOperData(ExecStatus.Idle);
                    execStatus.set(ExecStatus.Idle);

                    StartTestOutput output = new StartTestOutputBuilder()
                            .setStatus(StartTestOutput.Status.OK)
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
}
