/*
 * Copyright © 2016 Cisco Systems, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2mexample.impl;

import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;
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

/**
 * This is an example plugin implementing features provided by
 * IoTDM plugin infrastructure.
 * Check out the readme file for more information:
 * onem2m/onem2m-core/src/main/java/org/opendaylight/iotdm/onem2m/plugins/README.md
 */
public class Onem2mExampleCustomProtocol implements IotdmPlugin,
                                                    IotdmPluginDbClient,
                                                    IotdmPluginSimpleConfigClient {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mExampleCustomProtocol.class);
    private static final int defaultHttpPort = 8283;
    protected DataBroker dataBroker;
    protected Onem2mService onem2mService;
    private Onem2mDataStoreChangeHandler onem2mDataStoreChangeHandler;

    private static final String ONEM2M_EXAMPLE_CSE_NAME = "ONEM2M_EXAMPLE_CSE_NAME";
    private static final String AENAME = "EXAMPLE_AE_NAME";
    private static final String CONTAINER_NAME = "EXAMPLE_CONTAINER_NAME";
    private TransactionManager transactionManager;
    private boolean cseInitialized = false;

    public Onem2mExampleCustomProtocol(DataBroker dataBroker, Onem2mService onem2mService) {
        this.onem2mService = onem2mService;
        this.dataBroker = dataBroker;
        try {

            /*
             * Example-5. Initial registrations of implemented interfaces
             */
            Onem2mPluginManager.getInstance()
                    // Register as DB API client
                    .registerDbClientPlugin(this)

                    // Register to receive requests use suitable method for required plugin (https, coap, websocket ...)
                    .registerPluginHttp(this, defaultHttpPort, Onem2mPluginManager.Mode.Exclusive, null)

                    // Register for configuration
                    .registerSimpleConfigPlugin(this);

        } catch (IotdmPluginRegistrationException e) {
            LOG.error("Failed to register plugin: {}", e);

            // Clear all possibly successful registrations
            Onem2mPluginManager.getInstance()
                    .unregisterDbClientPlugin(this)
                    .unregisterIotdmPlugin(this)
                    .unregisterSimpleConfigPlugin(this);
        }
    }


    /*
     * Example-1. Implementation of IotdmPluginCommonInterface
     */
    @Override
    public String getPluginName() {
        return "Onem2mExample";
    }

    @Override
    public void close() {
        // Clear all registrations
        Onem2mPluginManager.getInstance()
                .unregisterIotdmPlugin(this)
                .unregisterDbClientPlugin(this)
                .unregisterSimpleConfigPlugin(this);

        // Clear current configuration
        cfg = null;
    }


    /*
     * Example-2. Implementation of IotdmPlugin interface
     */

    /**
     * Handler for the HTTP registered plugin
     */
    @Override
    public void handle(IotdmPluginRequest request, IotdmPluginResponse response){
        HttpServletRequest httpRequest = ((IotdmPluginHttpRequest)request).getOriginalRequest();
        HttpServletResponse httpResponse = ((IotdmPluginHttpResponse)response).getHttpResponse();

        // We should use LOG.debug() here but for testing purposes it's OK to use LOG.info()
        // which is turned on by default
        LOG.info("Onem2mExampleCustomProtocol: method: {}, url:{}, headers: {}, payload: {}",
                 request.getMethod(), request.getUrl(),
                 ((IotdmPluginHttpRequest) request).getHeadersAll(), request.getPayLoad());

        // Only post methods are supported
        if (! request.getMethod().toUpperCase().equals(HttpMethods.POST)) {
            response.setReturnCode(HttpStatus.METHOD_NOT_ALLOWED_405);
            LOG.debug("Onem2mExampleCustomProtocol: unsupported method: {}", request.getMethod());
            return;
        }

        // Return bad request result if there's not any payload
        String payload = request.getPayLoad();
        if (null == payload || payload.isEmpty()) {
            response.setReturnCode(HttpStatus.BAD_REQUEST_400);
            LOG.debug("Onem2mExampleCustomProtocol: request with empty payload received");
            return;
        }

        /*
         * Use helper methods from Example-6 and store received payload in content instance resource.
         * This approach uses CRUD operations with resources, direct Read/Write access to DB is not needed.
         */
        if (!cseInitialized) {
            this.provisionCse();
            this.createAE();
            this.createContainer();
        }

        String resourceId = this.createContentInstance(payload);
        if (null == resourceId) {
            response.setReturnCode(HttpStatus.INTERNAL_SERVER_ERROR_500);
            LOG.error("Onem2mExampleCustomProtocol: failed to process payload");
            return;
        }

        // Set the resourceId of the create ContentInstance in the response payload as JSON
        response.setContentType(Onem2m.ContentType.APPLICATION_JSON);
        JSONObject jsonObj = new JSONObject();
        jsonObj.accumulate("rid", resourceId);
        response.setResponsePayload(jsonObj.toString());
        // Set the response status code as CREATED
        response.setReturnCode(HttpStatus.CREATED_201);

        // We should use LOG.debug() here but for testing purposes it's OK to use LOG.info()
        // which is turned on by default
        LOG.info("Onem2mExampleCustomProtocol: created new ContentInstance with resourceId: {}", resourceId);
    }


    /*
     * Example-3. Implementation of SimpleConfig client
     */
    private IotdmSimpleConfig cfg = null; // Stores current configuration

    private void changeHttpRegistration(int newPort, int oldPort) throws IotdmPluginSimpleConfigException {
        if (newPort == oldPort) {
            LOG.debug("New and old ports are the same ({}), not needed to perform re-registration", newPort);
            return;
        } else {
            LOG.info("Performing re-registration from port {} to {}", oldPort, newPort);
        }

        // Register to new port number in make before break manner
        try {
            Onem2mPluginManager.getInstance()
                    .registerPluginHttp(this, newPort, Onem2mPluginManager.Mode.Exclusive, null);
        } catch (IotdmPluginRegistrationException e) {
            LOG.error("Failed to register to new port: {}, {}", newPort, e);
            throw new IotdmPluginSimpleConfigException("Unable to register to new port number (" +
                                                       newPort +
                                                       "), error message: " +
                                                       e.toString());
        }

        // New registration passed, unregister the previous one
        Onem2mPluginManager.getInstance().unregisterIotdmPlugin(this, "http", oldPort);
    }

    @Override
    public void configure(IotdmSimpleConfig configuration) throws IotdmPluginSimpleConfigException {
        if (null == configuration) {
            if (cfg != null && null != cfg.getVal("port")) {
                // re-register with default port
                this.changeHttpRegistration(defaultHttpPort, Integer.valueOf(cfg.getVal("port")));
            }
            cfg = null;
            LOG.info("Configuration deleted");
            return;
        }

        // Walks through all key-value pairs and uses known keys, unknown are ignored
        for (Map.Entry<String,String> kv : configuration.getKeyValMap().entrySet()) {
            LOG.info("Configured KVpair: {}:{}", kv.getKey(), kv.getValue());

            // Testing failure, exception is thrown if the configuration includes key "error"
            if (kv.getKey().equals("error")) {
                throw new IotdmPluginSimpleConfigException("Configuration must not include key \"error\"");
            }

            if (kv.getKey().equals("port")) {
                int newPort = 0;
                int oldPort = 0;

                try {
                    newPort = Integer.valueOf(kv.getValue());
                } catch (NumberFormatException e) {
                    // Throw SimpleConfig exception in case of invalid port number
                    LOG.error("Invalid port number format: {}", kv.getValue());
                    throw new IotdmPluginSimpleConfigException(
                                                      "Invalid port number format configured: " + kv.getValue());
                }

                // Check the value
                if (newPort > 0xffff || newPort <= 0) {
                    LOG.error("Invalid port number configured: {}", newPort);
                    throw new IotdmPluginSimpleConfigException(
                               "Invalid port number configured: " + newPort + ", valid values are from 1 to 0xffff");
                }

                // Check with current configuration
                if (null == cfg || null == cfg.getVal("port")) {
                    if (newPort == defaultHttpPort) {
                        // We're already registered to the same port number, nothing to do
                        LOG.debug("Plugin already registered to default port: {}", newPort);
                        continue;
                    } else {
                        oldPort = defaultHttpPort;
                    }
                } else if (newPort == Integer.valueOf(cfg.getVal("port"))) {
                    LOG.debug("Plugin already registered to port: {}", newPort);
                    continue;
                } else {
                    oldPort = Integer.valueOf(cfg.getVal("port"));
                }

                this.changeHttpRegistration(newPort, oldPort);
            }
        }

        cfg = configuration;
        LOG.info("Configured new SimpleConfig: {}", configuration.getDebugString());
    }

    @Override
    public IotdmSimpleConfig getSimpleConfig() {
        return this.cfg;
    }


    /*
     * Example-4. Implementation of IotdmPluginDbClient, this interface provides access to
     * ResourceTreeReader and ResourceTreeWriter objects which allows to do direct Read/Write
     * operations with data store.
     */
    @Override
    public void dbClientStart() throws Exception {
        // DataStore Change Handler uses ResourceTreeReader so we should instantiate the change handler
        // here, when we obtain valid ResourceTreeReader
        // ResourceTreeWriter is not used in this example
        onem2mDataStoreChangeHandler = new Onem2mDataStoreChangeHandler(dataBroker);
    }

    @Override
    public void dbClientStop() {
        onem2mDataStoreChangeHandler = null;
    }


    /*
     * Example-6. Implementation of Onem2mDataStoreListener which implements methods
     * called in case of DataStore modification.
     */
    private class Onem2mDataStoreChangeHandler extends Onem2mDatastoreListener {

        public Onem2mDataStoreChangeHandler(DataBroker dataBroker) {
            super(dataBroker);
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


    /*
     * Example-7. CRUD resources from/to DataStore by resource builders and Request/Response primitive clients
     */

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

        if (null == createContentInstance("Plugin specific content")) {
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

    /**
     * Creates new content instance and returns resourceId of the
     * new ContentInstance resource.
     * @return Null in cas of failure, resourceId of the new ContentInstance if successful.
     */
    private String createContentInstance(String content) {

        ContentInstance b;

        b = new ContentInstance();
        b.setTo("/" + ONEM2M_EXAMPLE_CSE_NAME + "/" + AENAME + "/" + CONTAINER_NAME);
        b.setOperationCreate();
        b.setContent(content);
        b.setContentInfo("myContentInfo");
        b.setOntologyRef("http://ontology/ref");
        Onem2mRequestPrimitiveClient req = b.build();
        Onem2mResponsePrimitiveClient res = req.send(onem2mService);
        if (!res.responseOk()) {
            LOG.error(res.getError());
            return null;
        }
        Onem2mContentInstanceResponse ciResponse = new Onem2mContentInstanceResponse(res.getContent());
        if (!ciResponse.responseOk()) {
            LOG.error("Container create request: {}", ciResponse.getError());
            return null;
        }

        String resourceId = ciResponse.getResourceId();
        if (resourceId == null) {
            LOG.error("Create cannot parse resourceId for ContentInstance create");
            return null;
        }

        LOG.info("Curr ContentSize: {}\n", ciResponse.getContentSize());

        return resourceId;
    }

}
