/*
 * Copyright(c) Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core;

import com.google.common.util.concurrent.Futures;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.RequestPrimitiveProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.primitive.list.Onem2mPrimitive;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mCoreProvider implements Onem2mService, BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCoreProvider.class);
    private BindingAwareBroker.RpcRegistration<Onem2mService> rpcReg;
    private DataBroker dataBroker;
    private Onem2mDb db;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        this.rpcReg = session.addRpcImplementation(Onem2mService.class, this);
        this.dataBroker = session.getSALService(DataBroker.class);

        db = Onem2mDb.getInstance();
        db.initializeDatastore(dataBroker);

        LOG.info("Session Initiated");
    }

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

    /**
     * This is the requestPrimitive RPC, it can be called from restconf directly, or it can be called from
     * onem2m-protocol-coap/http/mqtt.  Each of those onem2m-protocols have used the protocol specific bindings
     * to extract primitives from protocol specific fields and placed them in the List<Onem2mPrimitives>.
     * See Onem2m.RequestPrimitive for the list of possible primitives.  For the most part, these primitives
     * are format independent and as such are strings.  There is one exception, ie the Onem2m.RequestPrimitive.CONTENT
     * parameter which has been serialized as a string by the onenm2m-protocols which was encoded in the payload
     * of those protocols.  The CONTENT is encoded by the Onem2m.RequestPrimitive.CONTENT_FORMAT=json/xml).  The
     * code that ultimately cares about the CONTENT will decode it using the appropriate content parser.
     * Based on the operation, mandatory fields have to be checked.
     * @param input
     * @return
     */
    @Override
    public Future<RpcResult<Onem2mRequestPrimitiveOutput>> onem2mRequestPrimitive(Onem2mRequestPrimitiveInput input) {

        LOG.info("RPC: begin handle op ...");

        List<Onem2mPrimitive> onem2mPrimitiveList = input.getOnem2mPrimitive();
        RequestPrimitiveProcessor onem2mRequest = new RequestPrimitiveProcessor(onem2mPrimitiveList);
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();

        onem2mRequest.handleOperation(onem2mResponse);

        onem2mPrimitiveList = onem2mResponse.getPrimitivesList();
        Onem2mRequestPrimitiveOutput output = new Onem2mRequestPrimitiveOutputBuilder()
                .setOnem2mPrimitive(onem2mPrimitiveList).build();

        LOG.info("RPC: end handle op ...");

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
     * @param input
     * @return
     */
    @Override
    public Future<RpcResult<Onem2mCseProvisioningOutput>> onem2mCseProvisioning(Onem2mCseProvisioningInput input) {

        LOG.info("RPC: begin handle op ...");

        List<Onem2mPrimitive> csePrimitiveList = input.getOnem2mPrimitive();
        RequestPrimitiveProcessor onem2mRequest = new RequestPrimitiveProcessor(csePrimitiveList);
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();

        onem2mRequest.provisionCse(onem2mResponse);

        csePrimitiveList = onem2mResponse.getPrimitivesList();
        Onem2mCseProvisioningOutput output = new Onem2mCseProvisioningOutputBuilder()
                .setOnem2mPrimitive(csePrimitiveList).build();

        LOG.info("RPC: end handle op ...");

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<java.lang.Void>> onem2mCleanupStore() {
        Onem2mDb.getInstance().cleanupDataStore();
        Onem2mDb.getInstance().dumpDataStoreToLog();
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<java.lang.Void>> onem2mDumpResourceTree() {
        LOG.info("RPC: onem2mDumpResourceTree dumping ...");
        Onem2mDb.getInstance().dumpDataStoreToLog();
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }
}
