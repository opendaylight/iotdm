/*
 * Copyright © 2016 Cisco Systems, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2mexample.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.iotdm.onem2m.client.*;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.TransactionManager;
import org.opendaylight.iotdm.onem2m.plugins.*;
import org.opendaylight.iotdm.onem2m.plugins.channels.http.IotdmPluginHttpRequest;
import org.opendaylight.iotdm.onem2m.plugins.channels.http.IotdmPluginHttpResponse;
import org.opendaylight.iotdm.onem2m.plugins.simpleconfig.IotdmPluginSimpleConfigClient;
import org.opendaylight.iotdm.onem2m.plugins.simpleconfig.IotdmPluginSimpleConfigException;
import org.opendaylight.iotdm.onem2m.plugins.simpleconfig.IotdmSimpleConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Created by bjanosik on 9/9/16.
 */
public class Onem2mExampleCustomProtocol implements IotdmPlugin,
                                                    IotdmPluginDbClient,
                                                    IotdmPluginSimpleConfigClient {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mExampleCustomProtocol.class);
    protected DataBroker dataBroker;
    protected Onem2mService onem2mService;
    private Onem2mDataStoreChangeHandler onem2mDataStoreChangeHandler;

    private static final String ONEM2M_EXAMPLE_CSE_NAME = "ONEM2M_EXAMPLE_CSE_NAME";
    private static final String AENAME = "EXAMPLE_AE_NAME";
    private static final String CONTAINER_NAME = "EXAMPLE_CONTAINER_NAME";
    private TransactionManager transactionManager;


    public Onem2mExampleCustomProtocol(DataBroker dataBroker, Onem2mService onem2mService) {
        this.onem2mService = onem2mService;
        this.dataBroker = dataBroker;
        try {
            Onem2mPluginManager.getInstance()
                    .registerDbClientPlugin(this)
                    .registerSimpleConfigPlugin(this);
        } catch (IotdmPluginRegistrationException e) {
            LOG.error("Failed to register plugin as DB API client: {}", e);
        }
    }

    @Override
    public void dbClientStart(final ResourceTreeWriter twc, final ResourceTreeReader trc) throws Exception {
        onem2mDataStoreChangeHandler = new Onem2mDataStoreChangeHandler(trc, dataBroker);

        // Now the plugin can be registered at plugin manager and can start to handle requests

        //use suitable method for required plugin (https, coap, websocket ...)
        Onem2mPluginManager.getInstance()
                              .registerPluginHttp(this, 8283, Onem2mPluginManager.Mode.Exclusive, null);
    }

    @Override
    public void dbClientStop() {
        onem2mDataStoreChangeHandler = null;
        // Unregister from the plugin manager because the DB is not accessible
        Onem2mPluginManager.getInstance().unregisterIotdmPlugin(this);
    }

    private class Onem2mDataStoreChangeHandler extends Onem2mDatastoreListener {

        public Onem2mDataStoreChangeHandler(ResourceTreeReader trc, DataBroker dataBroker) {
            super(trc, dataBroker);
        }

        @Override
        public void onem2mResourceCreated(String hierarchicalResourceName, Onem2mResource onem2mResource) {
            LOG.info("Onem2mExampleCustomProtocol: onem2mResourceCreated h={}, id:{}, type:{}",
                    hierarchicalResourceName,
                    onem2mResource.getResourceId(),
                    onem2mResource.getResourceType());

            // do custom protocol notification (maybe)
        }

        @Override
        public void onem2mResourceChanged(String hierarchicalResourceName, Onem2mResource onem2mResource) {
            LOG.info("Onem2mExampleCustomProtocol: onem2mResourceChanged h={}, id:{}, type:{}",
                    hierarchicalResourceName,
                    onem2mResource.getResourceId(),
                    onem2mResource.getResourceType());

            // do custom protocol notification (maybe)

        }

        @Override
        public void onem2mResourceDeleted(String hierarchicalResourceName, Onem2mResource onem2mResource) {
            LOG.info("Onem2mExampleCustomProtocol: onem2mResourceDeleted h={}, id:{}, type:{}",
                    hierarchicalResourceName,
                    onem2mResource.getResourceId(),
                    onem2mResource.getResourceType());

            // do custom protocol notification (maybe)

        }
    }

    @Override
    public void close() {
        Onem2mPluginManager.getInstance()
                .unregisterIotdmPlugin(this)
                .unregisterDbClientPlugin(this)
                .unregisterSimpleConfigPlugin(this);
    }

    @Override
    public String getPluginName() {
        return "Onem2mExample";
    }

    // handler for the HTTP registered plugin, see Onem2mHttpProvider.java for more info
    @Override
    public void handle(IotdmPluginRequest request, IotdmPluginResponse response){
        HttpServletRequest httpRequest = ((IotdmPluginHttpRequest)request).getOriginalRequest();
        HttpServletResponse httpResponse = ((IotdmPluginHttpResponse)response).getHttpResponse();

        LOG.info("Onem2mExampleCustomProtocol: method: {}, url:{}, headers: {}, payload: {}",
                request.getMethod(), request.getUrl(), ((IotdmPluginHttpRequest) request).getHeadersAll(), request.getPayLoad());
    }

    /**
     * Based on the HTTP plugin handler, upon receviing a message, you may need to add onem2m resource objects to
     * the datastore.  This routine shows how some sample resources can be added.  Alos, some get routines show how
     * to retrive onem2m reosurces from the datastore.
     */
    private boolean exampleOfHowToAddSomeOnem2mResourceTypes() {

        // see if the resource tree exists, if not provision it.
        if (!getCse()) {
            if (!provisionCse()) {
                return false;
            }
            if (!getCse()) {
                return false;
            }
        }

        // TODO: replace this with code that you need to build your resource tree.  These are examples ONLY.

        if (!createAE() || !getAE()) {
            return false;
        }

        if (!createContainer()) {
            return false;
        }

        if (!createContentInstance()) {
            return false;
        }

        // delete the AE and its children will automatically be deleted.
        if (!deleteAE())
            return false;

        return true;
    }

    private boolean getCse() {

        Onem2mRequestPrimitiveClient req = new CSE().setOperationRetrieve().setTo(ONEM2M_EXAMPLE_CSE_NAME).build();
        Onem2mResponsePrimitiveClient res = req.send(onem2mService);
        if (!res.responseOk()) {
            LOG.error(res.getContent());
            return false;
        }
        Onem2mCSEResponse cseResponse = new Onem2mCSEResponse(res.getContent());
        if (!cseResponse.responseOk()) {
            LOG.error(res.getError());
            return false;
        }        String resourceId = cseResponse.getResourceId();
        if (resourceId == null) {
            LOG.error("Create cannot parse resourceId for CSE get");
            return false;
        }

        return true;
    }

    private boolean provisionCse() {

        CSE b;

        b = new CSE();
        b.setCseId(ONEM2M_EXAMPLE_CSE_NAME);
        b.setCseType(Onem2m.CseType.INCSE);
        b.setOperationCreate();
        Onem2mRequestPrimitiveClient req = b.build();
        Onem2mResponsePrimitiveClient res = req.send(onem2mService);
        if (!res.responseOk()) {
            LOG.error(res.getError());
            return false;
        }
        Onem2mCSEResponse cseResponse = new Onem2mCSEResponse(res.getContent());
        if (!cseResponse.responseOk()) {
            LOG.error(res.getError());
            return false;
        }
        String resourceId = cseResponse.getResourceId();
        if (resourceId == null) {
            LOG.error("Create cannot parse resourceId for CSE provision");
            return false;
        }

        return true;
    }

    private boolean createAE() {

        AE b;

        b = new AE();
        b.setTo(ONEM2M_EXAMPLE_CSE_NAME);
        b.setOperationCreate();
        b.setName(AENAME);
        b.setAppName(AENAME);
        b.setAppId(AENAME);
        b.setRequestReachability(false);
        Onem2mRequestPrimitiveClient req = b.build();
        Onem2mResponsePrimitiveClient res = req.send(onem2mService);
        if (!res.responseOk()) {
            LOG.error(res.getError());
            return false;
        }
        Onem2mAEResponse aeResponse = new Onem2mAEResponse(res.getContent());
        if (!aeResponse.responseOk()) {
            LOG.error("AE create request: {}", aeResponse.getError());
            return false;
        }

        String resourceId = aeResponse.getResourceId();
        if (resourceId == null) {
            LOG.error("Create cannot parse resourceId for AE create");
            return false;
        }

        return true;
    }

    private boolean getAE() {

        AE b;

        b = new AE();
        b.setOperationRetrieve();
        b.setTo("/" + ONEM2M_EXAMPLE_CSE_NAME + "/" + AENAME);
        Onem2mRequestPrimitiveClient req = b.build();
        Onem2mResponsePrimitiveClient res = req.send(onem2mService);
        if (!res.responseOk()) {
            LOG.error(res.getError());
            return false;
        }
        Onem2mAEResponse aeResponse = new Onem2mAEResponse(res.getContent());
        if (!aeResponse.responseOk()) {
            LOG.error("AE create request error: {}", aeResponse.getError());
            return false;
        }

        String resourceId = aeResponse.getResourceId();
        if (resourceId == null) {
            LOG.error("Create cannot parse resourceId for AE retrieve");
            return false;
        }

        if (!AENAME.contentEquals(aeResponse.getAppName())) {
            LOG.error("ae_app_name mismatch: expected: {}, received: {}", AENAME, aeResponse.getAppName());
            return false;
        }

        return true;
    }

    private boolean deleteAE() {

        AE b;

        b = new AE();
        b.setOperationDelete();
        b.setTo("/" + ONEM2M_EXAMPLE_CSE_NAME + "/" + AENAME);
        Onem2mRequestPrimitiveClient req = b.build();
        Onem2mResponsePrimitiveClient res = req.send(onem2mService);
        if (!res.responseOk()) {
            LOG.error(res.getError());
            return false;
        }
        Onem2mAEResponse aeResponse = new Onem2mAEResponse(res.getContent());
        if (!aeResponse.responseOk()) {
            LOG.error("AE create request error: {}", aeResponse.getError());
            return false;
        }

        String resourceId = aeResponse.getResourceId();
        if (resourceId == null) {
            LOG.error("Create cannot parse resourceId for AE delete");
            return false;
        }

        return true;
    }

    private boolean createContainer() {

        Container b;

        b = new Container();
        b.setTo("/" + ONEM2M_EXAMPLE_CSE_NAME + "/" + AENAME);
        b.setOperationCreate();
        b.setName(CONTAINER_NAME);
        b.setCreator(null);
        b.setMaxNrInstances(10);
        b.setMaxByteSize(500);
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

        String resourceId = ctrResponse.getResourceId();
        if (resourceId == null) {
            LOG.error("Create cannot parse resourceId for Container create");
            return false;
        }

        LOG.info("Curr/Max Nr Instances: {}/{}, curr/Max ByteSize: {}/{}\n",
                ctrResponse.getCurrNrInstances(),
                ctrResponse.getMaxNrInstances(),
                ctrResponse.getCurrByteSize(),
                ctrResponse.getMaxByteSize());

        return true;
    }

    private boolean createContentInstance() {

        ContentInstance b;

        b = new ContentInstance();
        b.setTo("/" + ONEM2M_EXAMPLE_CSE_NAME + "/" + AENAME + "/" + CONTAINER_NAME);
        b.setOperationCreate();
        b.setContent("myContent");
        b.setContentInfo("myContentInfo");
        b.setOntologyRef("http://ontology/ref");
        Onem2mRequestPrimitiveClient req = b.build();
        Onem2mResponsePrimitiveClient res = req.send(onem2mService);
        if (!res.responseOk()) {
            LOG.error(res.getError());
            return false;
        }
        Onem2mContentInstanceResponse ciResponse = new Onem2mContentInstanceResponse(res.getContent());
        if (!ciResponse.responseOk()) {
            LOG.error("Container create request: {}", ciResponse.getError());
            return false;
        }

        String resourceId = ciResponse.getResourceId();
        if (resourceId == null) {
            LOG.error("Create cannot parse resourceId for ContentInstance create");
            return false;
        }

        LOG.info("Curr ContentSize: {}\n", ciResponse.getContentSize());

        return true;
    }

    /*
     * Testing implementation of SimpleConfig client
     */
    private IotdmSimpleConfig cfg = null;

    @Override
    public void configure(IotdmSimpleConfig configuration) throws IotdmPluginSimpleConfigException {
        if (null == configuration) {
            cfg = null;
            LOG.info("Configuration deleted");
            return;
        }

        LOG.info("Configured new SimpleConfig: {}", configuration.getDebugString());

        for (Map.Entry<String,String> kv : configuration.getKeyValMap().entrySet()) {
            LOG.info("Configured KVpair: {}:{}", kv.getKey(), kv.getValue());

            // Testing failure, exception is thrown if the configuration includes key "error"
            if (kv.getKey().equals("error")) {
                throw new IotdmPluginSimpleConfigException("Configuration must not include key \"error\"");
            }
        }
        cfg = configuration;
    }

    @Override
    public IotdmSimpleConfig getSimpleConfig() {
        return this.cfg;
    }

}
