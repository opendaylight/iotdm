/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.simpleadapter.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.iotdm.onem2m.client.*;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2msimpleadapter.rev160210.Onem2mSimpleAdapterConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2msimpleadapter.rev160210.SimpleAdapterParmsDesc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2msimpleadapter.rev160210.onem2m.simple.adapter.config.SimpleAdapterDesc;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of this protocol adapter is to provide a convenient way to adapt devices into the oneM2M resource
 * tree in a generic way.  No coding is required.  A very strict format is required however to conform to this
 * simple adapter.
 */
public class Onem2mSimpleAdapterManager implements ClusteredDataTreeChangeListener<SimpleAdapterDesc> {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mSimpleAdapterManager.class);

    private DataBroker dataBroker;
    private HashMap<String,SimpleAdapterDesc> simpleAdapterMap;
    private static final InstanceIdentifier<SimpleAdapterDesc> ONEM2M_SIMPLE_ADAPTER_DESC_IID =
            InstanceIdentifier.builder(Onem2mSimpleAdapterConfig.class)
                    .child(SimpleAdapterDesc.class)
                    .build();
    private ListenerRegistration<Onem2mSimpleAdapterManager> dcReg;
    private Onem2mSimpleAdapterHttpServer onem2mHttpServer = null;
    //private Onem2mSimpleAdapterMqttClient onem2mMqttClient = null;
    //private Onem2mSimpleAdapterCoapServer onem2mCoapServer = null;
    private Onem2mService onem2mService;

    public Onem2mSimpleAdapterManager(DataBroker dataBroker, Onem2mService onem2mService) {

        this.dataBroker = dataBroker;
        // listen for changes to simple adapter descriptors
        dcReg = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                ONEM2M_SIMPLE_ADAPTER_DESC_IID), this);

        // cache each of the simple adapter descriptors
        simpleAdapterMap = new HashMap<String,SimpleAdapterDesc>();

        this.onem2mService = onem2mService;

        LOG.info("Created Onem2mSimpleAdapterManager");

    }

    public void setOnem2mSimpleAdapterHttpServer(Onem2mSimpleAdapterHttpServer onem2mHttpServer) {
        this.onem2mHttpServer = onem2mHttpServer;
    }
//    public void setOnem2mSimpleAdapterMqttClient(Onem2mSimpleAdapterMqttClient onem2mMqttClient) {
//        this.onem2mMqttClient = onem2mMqttClient;
//    }
//    public void setOnem2mSimpleAdapterCoapServer(Onem2mSimpleAdapterCoapServer onem2mCoapServer) {
//        this.onem2mCoapServer = onem2mCoapServer;
//    }

    public void close() {
        dcReg.close();
    }
    
    /**
     * Configuration of the simple adapter has changed.
     * @param changes
     */
    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<SimpleAdapterDesc>> changes) {
        LOG.info("Onem2mSimpleAdapterNotifier: onDataTreeChanged(SimpleAdapterDesc) called");
        for (DataTreeModification<SimpleAdapterDesc> change : changes) {
            SimpleAdapterDesc simpleAdapterDescBefore = change.getRootNode().getDataBefore();
            SimpleAdapterDesc simpleAdapterDescAfter = change.getRootNode().getDataAfter();
            switch (change.getRootNode().getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    // crude way of handling changes? .. delete the old config, add the new
                    if (simpleAdapterMap.containsKey(simpleAdapterDescAfter.getSimpleAdapterName())) {
                        simpleAdapterParmsDeleted(simpleAdapterDescBefore);
                    }
                    simpleAdapterParmsCreated(simpleAdapterDescAfter);
                    break;
                case DELETE:
                    if (simpleAdapterMap.containsKey(simpleAdapterDescAfter.getSimpleAdapterName())) {
                        simpleAdapterParmsDeleted(simpleAdapterDescBefore);
                    }
                    break;
                default:
                    LOG.error("Onem2mSimpleAdapterNotifier: onDataTreeChanged(Onem2mSimpleAdapterConfig) non handled modification {}",
                            change.getRootNode().getModificationType());
                    break;
            }
        }
    }

    private void simpleAdapterParmsCreated(SimpleAdapterDesc simpleAdapterDesc) {

        LOG.info("simpleAdapterParmsCreated: {}", simpleAdapterDesc.getSimpleAdapterName());
        String descName = simpleAdapterDesc.getSimpleAdapterName();
        simpleAdapterMap.put(descName, simpleAdapterDesc);
        switch (simpleAdapterDesc.getWireProtocol()) {
            case HTTP:
                onem2mHttpServer.startHttpServer(simpleAdapterDesc);
                break;
            case MQTT:
                break;
            case COAP:
                break;
        }
    }

    private void simpleAdapterParmsDeleted(SimpleAdapterDesc simpleAdapterDesc) {

        LOG.info("simpleAdapterParmsDeleted: {}", simpleAdapterDesc.getSimpleAdapterName());

        String descName = simpleAdapterDesc.getSimpleAdapterName();
        simpleAdapterMap.remove(descName);
        switch (simpleAdapterDesc.getWireProtocol()) {
            case HTTP:
                onem2mHttpServer.stopHttpServer(simpleAdapterDesc);
                break;
            case MQTT:
                break;
            case COAP:
                break;
        }

    }

    /**
     * Lookup the URI in the map, if found then verify it is in the onem2m datastore
     * @param uri onem2m target URI
     * @return
     */
    public SimpleAdapterDesc findDescriptorUsingUri(String uri) {

        for (Map.Entry<String, SimpleAdapterDesc> entry : simpleAdapterMap.entrySet()) {
            SimpleAdapterDesc desc = entry.getValue();
            if (uri.contentEquals(trim(desc.getOnem2mTargetId()))) {
                String onem2mResourceId = Onem2mDb.getInstance().findResourceIdUsingURI(uri);
                return (onem2mResourceId != null) ? desc : null;
            }
        }

        return null;
    }

    private String trim(String stringWithSlashes) {

        stringWithSlashes = stringWithSlashes.trim();
        stringWithSlashes = stringWithSlashes.startsWith("/") ?
                stringWithSlashes.substring("/".length()) : stringWithSlashes;
        stringWithSlashes = stringWithSlashes.endsWith("/") ?
                stringWithSlashes.substring(0,stringWithSlashes.length()-1) : stringWithSlashes;
        return stringWithSlashes;
    }

    /**
     * Parse the payload.   Use the simpleAdapterDesc.getOnem2mContainerJsonKeyName() to get the json key's value
     * from the JSON payload.  Note that the syntax key1:key2:key3 ... is used if the "key" is embedded in a
     * hierarchy of JSON objects.  Typically, there is only one level.
     * @param simpleAdapterDesc
     * @param uri
     * @param payload
     * @return
     */
    public String processUriAndPayload(SimpleAdapterDesc simpleAdapterDesc,
                                       String uri,
                                       String payload,
                                       String onem2mContainerName) {

        JSONObject jsonPayloadObject;
        try {
            jsonPayloadObject = new JSONObject(payload);
        } catch (JSONException e) {
            return "Error json format:" + e.toString();
        }

        if (onem2mContainerName == null) {

            String path[] = simpleAdapterDesc.getOnem2mContainerJsonKeyName().split("/");
            Integer jsonNestedCount;

            JSONObject tempJ = jsonPayloadObject;
            for (jsonNestedCount = 1; jsonNestedCount < path.length && tempJ != null; jsonNestedCount++) {
                tempJ = tempJ.optJSONObject(path[jsonNestedCount - 1]);
            }
            if (tempJ == null) {
                return simpleAdapterDesc.getOnem2mContainerJsonKeyName() + " missing";
            }

            String jsonKeyValue = tempJ.optString(path[jsonNestedCount - 1], null);
            if (jsonKeyValue == null) {
                return simpleAdapterDesc.getOnem2mContainerJsonKeyName() + " missing";
            }
            onem2mContainerName = jsonKeyValue;
        }

        String target = "/" + uri;
        if (!getContainer(target, onem2mContainerName)) {
            LOG.info("processUriAndPayload: adding {}/{}", target, onem2mContainerName);
            if (!createContainer(target, onem2mContainerName, simpleAdapterDesc)) {
                return "Error adding " + target + "/" + onem2mContainerName;
            }
        }
        target += "/" + onem2mContainerName;
        if (!createContentInstance(target, jsonPayloadObject.toString(), simpleAdapterDesc)) {
            return "Error adding content to " + target;
        }

        return null;
    }

    private boolean getContainer(String parent, String name) {

        Onem2mContainerRequestBuilder b;

        b = new Onem2mContainerRequestBuilder();
        b.setTo(parent + "/" + name);
        b.setOperationRetrieve();
        Onem2mRequestPrimitiveClient req = b.build();
        Onem2mResponsePrimitiveClient res = req.send(onem2mService);
        if (!res.responseOk()) {
            return false;
        }
        Onem2mContainerResponse ctrResponse = new Onem2mContainerResponse(res.getContent());
        if (!ctrResponse.responseOk()) {
            LOG.error("Container get request: {}", ctrResponse.getError());
            return false;
        }

        String resourceId = ctrResponse.getResourceId();
        if (resourceId == null) {
            LOG.error("get cannot parse resourceId for Container create");
            return false;
        }

        LOG.info("getContainer {}/{}: Curr/Max Nr Instances: {}/{}, curr/Max ByteSize: {}/{}",
                parent, name,
                ctrResponse.getCurrNrInstances(),
                ctrResponse.getMaxNrInstances(),
                ctrResponse.getCurrByteSize(),
                ctrResponse.getMaxByteSize());

        return true;
    }

    private boolean createContainer(String parent, String name, SimpleAdapterDesc simpleAdapterDesc) {

        Onem2mContainerRequestBuilder b;

        b = new Onem2mContainerRequestBuilder();
        b.setTo(parent);
        b.setOperationCreate();
        b.setPrimitiveContent(simpleAdapterDesc.getOnem2mContainerJsonString());
        b.setName(name);
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

        LOG.info("createContainer {}/{}", parent, name);

        return true;
    }

    private boolean createContainer2(String parent, String name, SimpleAdapterDesc simpleAdapterDesc) {

        Onem2mContainerRequestBuilder b;

        b = new Onem2mContainerRequestBuilder();
        b.setTo(parent);
        b.setOperationCreate();
        b.setPrimitiveContent(simpleAdapterDesc.getOnem2mContainerJsonString());
        b.setMaxNrInstances(1);
        b.setName(name);
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

        LOG.info("createContainer {}/{}", parent, name);

        return true;
    }

    private boolean createContentInstance(String parent, String content, SimpleAdapterDesc simpleAdapterDesc) {

        Onem2mContentInstanceRequestBuilder b;

        b = new Onem2mContentInstanceRequestBuilder();
        b.setTo(parent);
        b.setOperationCreate();
        b.setPrimitiveContent(simpleAdapterDesc.getOnem2mContentInstanceJsonString());
        b.setContent(content);
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

        LOG.info("createContentInstance: Curr ContentSize: {}\n", ciResponse.getContentSize());

        return true;
    }
}
