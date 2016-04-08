/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.Monitor;
import java.util.List;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.RequestPrimitiveProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.primitive.list.Onem2mPrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.core.rev141210.Onem2mCoreRuntimeMXBean;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mCoreProvider implements Onem2mService, Onem2mCoreRuntimeMXBean, BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCoreProvider.class);
    private BindingAwareBroker.RpcRegistration<Onem2mService> rpcReg;
    private DataBroker dataBroker;
    private Onem2mStats stats;
    private Onem2mDb db;
    private static NotificationPublishService notifierService;




    private Monitor crudMonitor;

    public static NotificationPublishService getNotifier() {
        return Onem2mCoreProvider.notifierService;

    }

    /**
     * Perform session initialization
     * @param session the session
     */
    @Override
    public void onSessionInitiated(ProviderContext session) {
        this.rpcReg = session.addRpcImplementation(Onem2mService.class, this);
        this.dataBroker = session.getSALService(DataBroker.class);
        this.notifierService = session.getSALService(NotificationPublishService.class);
        crudMonitor = new Monitor();

        stats = Onem2mStats.getInstance();
        db = Onem2mDb.getInstance();
        db.initializeDatastore(dataBroker);
        initializePerfCse();
        LOG.info("Session Initiated");
    }

    /**
     * Shutdown the session
     * @throws Exception general exception if bad things happen
     */
    @Override
    public void close() throws Exception {
        if (this.rpcReg != null) {
            this.rpcReg.close();
        }
        if (db != null) {
            db.close();
        }
        LOG.info("Session Closed");
    }

    private void initializePerfCse() {
        if (!Onem2mDb.getInstance().findCseByName(Onem2m.SYS_PERF_TEST_CSE)) {
            RequestPrimitiveProcessor onem2mRequest = new RequestPrimitiveProcessor();
            onem2mRequest.createUpdateDeleteMonitorSet(crudMonitor);
            onem2mRequest.setPrimitive("CSE_ID", Onem2m.SYS_PERF_TEST_CSE);
            onem2mRequest.setPrimitive("CSE_TYPE", "IN-CSE");
            ResponsePrimitive onem2mResponse = new ResponsePrimitive();
            onem2mRequest.provisionCse(onem2mResponse);
        }
    }

    /**
     * This is the requestPrimitive RPC, it can be called from restconf directly, or it can be called from
     * onem2m-protocol-coap/http/mqtt.  Each of those onem2m-protocols have used the protocol specific bindings
     * to extract primitives from protocol specific fields and placed them in the List of Onem2mPrimitives.
     * See Onem2m.RequestPrimitive for the list of possible primitives.  For the most part, these primitives
     * are format independent and as such are strings.  There is one exception, ie the Onem2m.RequestPrimitive.CONTENT
     * parameter which has been serialized as a string by the onenm2m-protocols which was encoded in the payload
     * of those protocols.  The CONTENT is encoded by the Onem2m.RequestPrimitive.CONTENT_FORMAT=json/xml).  The
     * code that ultimately cares about the CONTENT will decode it using the appropriate content parser.
     * Based on the operation, mandatory fields have to be checked.
     * @param input the input request primitives
     * @return the response primitives
     */
    @Override
    public Future<RpcResult<Onem2mRequestPrimitiveOutput>> onem2mRequestPrimitive(Onem2mRequestPrimitiveInput input) {

        //LOG.info("RPC: begin handle op ...");

        List<Onem2mPrimitive> onem2mPrimitiveList = input.getOnem2mPrimitive();
        // todo: if it is a group/fanoutpoint, new a GroupRequestPrimitiveProcessor then called a lot of single processor?
        RequestPrimitiveProcessor onem2mRequest = new RequestPrimitiveProcessor();
        onem2mRequest.setPrimitivesList(onem2mPrimitiveList);
        onem2mRequest.createUpdateDeleteMonitorSet(crudMonitor);
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();
        // todo: new Group ResponsePrimitive?
        onem2mRequest.handleOperation(onem2mResponse);

        onem2mPrimitiveList = onem2mResponse.getPrimitivesList();
        Onem2mRequestPrimitiveOutput output = new Onem2mRequestPrimitiveOutputBuilder()
                .setOnem2mPrimitive(onem2mPrimitiveList).build();

        //LOG.info("RPC: end handle op ...");

        return RpcResultBuilder.success(output).buildFuture();
    }

    /**
     * This is the cse provisioning RPC, it can be called from restconf directly.  When a cse is initially configured
     * it needs to be provisioned with the parameters of the CSE.  This can be done via an RPC, or perhaps a CONFIG
     * file in the file system?
     *
     * For now the provisioning parms supported are
     * 1 CSE_ID - the name of the cse Base
     * 2 CSE_TYPE - "IN-CSE", "...
     * @param input cse parms
     * @return cse output parms
     */
    @Override
    public Future<RpcResult<Onem2mCseProvisioningOutput>> onem2mCseProvisioning(Onem2mCseProvisioningInput input) {

        LOG.info("RPC: begin handle onem2mCseProvisioning ...");

        List<Onem2mPrimitive> csePrimitiveList = input.getOnem2mPrimitive();
        RequestPrimitiveProcessor onem2mRequest = new RequestPrimitiveProcessor();
        onem2mRequest.setPrimitivesList(csePrimitiveList);
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();
        onem2mRequest.createUpdateDeleteMonitorSet(crudMonitor);

        onem2mRequest.provisionCse(onem2mResponse);

        if (onem2mResponse.getPrimitive("rsc").contains("Provisioned")) {
            RequestPrimitiveProcessor onem2mRequest1 = new RequestPrimitiveProcessor();
            onem2mRequest1.createUpdateDeleteMonitorSet(crudMonitor);
            onem2mRequest1.setPrimitivesList(csePrimitiveList);
            onem2mRequest1.createDefaultACP(onem2mResponse);
        }

        csePrimitiveList = onem2mResponse.getPrimitivesList();

        Onem2mCseProvisioningOutput output = new Onem2mCseProvisioningOutputBuilder()
                .setOnem2mPrimitive(csePrimitiveList).build();
        // TODO: modify the default acp response
        LOG.info("RPC: end handle onem2mCseProvisioning ...");
        return RpcResultBuilder.success(output).buildFuture();
    }

    /**
     * Cleanup the data store RPC
     * @return VOID
     */
    @Override
    public Future<RpcResult<java.lang.Void>> onem2mCleanupStore() {
        Onem2mDb.getInstance().cleanupDataStore();
        initializePerfCse();
        Onem2mDb.getInstance().dumpResourceIdLog(null);
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    /**
     * Dump the data store to karaf log RPC
     * @param input all or start resource
     * @return VOID
     */
    @Override
    public Future<RpcResult<java.lang.Void>> onem2mDumpResourceTree(Onem2mDumpResourceTreeInput input) {
        LOG.info("RPC: onem2mDumpResourceTree dumping ...");

        RequestPrimitiveProcessor onem2mRequest = new RequestPrimitiveProcessor();
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();

        String resourceId;
        if (input == null || input.getResourceUri() == null || input.getResourceUri().trim().contentEquals("")) {
            resourceId = null;
        } else {
            String resourceUri = input.getResourceUri().trim();
            if (!Onem2mDb.getInstance().findResourceUsingURI(resourceUri, onem2mRequest, onem2mResponse)) {
                LOG.error("Cannot find resourceUri: {}", resourceUri);
                return Futures.immediateFuture(RpcResultBuilder.<Void>failed().build());
            }
            resourceId = onem2mRequest.getResourceId();
            LOG.info("Dumping resourceUri: {}, resourceId: {}", resourceUri, resourceId);
        }
        switch (input.getDumpMethod()) {
            case RAW:
                Onem2mDb.getInstance().dumpResourceIdLog(resourceId);
                break;
            case HIERARCHICAL:
                Onem2mDb.getInstance().dumpHResourceIdToLog(resourceId);
                break;
            default:
                LOG.error("Unknown dump method: {}", input.getDumpMethod());
                break;
        }
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    /**
     * This is a generic debug function ... it allows input and output parameters
     * @param input Onem2mDebugInput
     * @return returnRPC
     */
    @Override
    public Future<RpcResult<Onem2mDebugOutput>> onem2mDebug(Onem2mDebugInput input) {

        LOG.info("RPC: begin handle onem2mDebug ...");

        List<Onem2mPrimitive> onem2mPrimitiveList = input.getOnem2mPrimitive();
        RequestPrimitiveProcessor onem2mRequest = new RequestPrimitiveProcessor();
        onem2mRequest.setPrimitivesList(onem2mPrimitiveList);
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();

        String op = onem2mRequest.getPrimitive("op");
        if (op == null) {
            onem2mResponse.setRSC("Unknown op", "(op) not set in input list");
        } else if (op.contentEquals("stats-get")) {
            onem2mResponse.setPrimitive("stats", getOnem2mStats());
        } else {
            onem2mResponse.setRSC("unknown op", op);
        }

        onem2mPrimitiveList = onem2mResponse.getPrimitivesList();
        Onem2mDebugOutput output = new Onem2mDebugOutputBuilder()
                .setOnem2mPrimitive(onem2mPrimitiveList).build();

        LOG.info("RPC: end handle onem2mDebug ...");

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public String getOnem2mStats() {
        return stats.getStats().toString();
    }
}
