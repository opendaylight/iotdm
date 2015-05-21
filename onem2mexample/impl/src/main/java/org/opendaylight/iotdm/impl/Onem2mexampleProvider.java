/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.impl;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.iotdm.onem2m.client.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mexampleProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mexampleProvider.class);
    protected Onem2mService onem2mService;
    private static final String ONEM2M_EXAMPLE_CSE_NAME = "ONEM2M_EXAMPLE_CSE_NAME";
    private static final String AENAME = "EXAMPLE_AE_NAME";
    private static final String CONTAINER_NAME = "EXAMPLE_CONTAINER_NAME";

    @Override
    public void onSessionInitiated(ProviderContext session) {
        onem2mService = session.getRpcService(Onem2mService.class);
        initializeApp();
        LOG.info("Onem2mexampleProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        LOG.info("Onem2mexampleProvider Closed");
    }

    /**
     * TODO: Initialize your app here.
     */
    private void initializeApp() {

        if (!initializeOnem2m()) {
            LOG.error("ERROR: failed to init onem2m resource tree properly");
            return;
        }

        // TODO: add any app specific initialization here
    }

    /**
     * TODO: Initialize onem2m.  This is where you must think about how you wish to use the onem2m resource tree.
     * The onem2m resource tree can be accessed via HTTP or CoAP protocols from outside karaf.  Your app can access
     * the onem2m resorouce tree via API's that internally call onem2m's internal RPCs defined in the onem2m yang
     * file.  A set of convenient API's are provided to CRUD resources and to recieve notifications as resources
     * are modified inside the onem2m resource tree.
     *
     * 1) Design how the resource tree will be created.
     * 2) Test to see if your app has already created its onem2m resource tree
     * 3) If the tree needs initialization, then add any resources you need to the tree including possibly the
     * base CSE.
     * 4) Use API's to CRUD onem2m application specific resources.
     */
    private boolean initializeOnem2m() {

        // see if the resource tree exists, if not provision it.
        if (!getCse()) {
            if (!provisionCse()) {
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

        Onem2mRequestPrimitiveClient req = new Onem2mCSERequestBuilder().setOperationRetrieve().setTo(ONEM2M_EXAMPLE_CSE_NAME).build();
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

        Onem2mCSERequestBuilder b;

        b = new Onem2mCSERequestBuilder();
        b.setTo(ONEM2M_EXAMPLE_CSE_NAME);    // M
        b.setCseId(ONEM2M_EXAMPLE_CSE_NAME); // M
        b.setCseType("IN_CSE");              // O
        b.setName(ONEM2M_EXAMPLE_CSE_NAME);  // M
        b.setOperationCreate();              // M
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

        Onem2mAERequestBuilder b;

        b = new Onem2mAERequestBuilder();
        b.setTo(ONEM2M_EXAMPLE_CSE_NAME);
        b.setOperationCreate();
        b.setName(AENAME);
        b.setAppName(AENAME);
        b.setAEId(AENAME);
        b.setAppId(AENAME);
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

        if (!AENAME.contentEquals(aeResponse.getAppName())) {
            LOG.error("ae_app_name mismatch: expected: {}, received: {}", AENAME, aeResponse.getAppName());
            return false;
        }

        return true;
    }

    private boolean getAE() {

        Onem2mAERequestBuilder b;

        b = new Onem2mAERequestBuilder();
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

        Onem2mAERequestBuilder b;

        b = new Onem2mAERequestBuilder();
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

        if (!AENAME.contentEquals(aeResponse.getAppName())) {
            LOG.error("ae_app_name mismatch: expected: {}, received: {}", AENAME, aeResponse.getAppName());
            return false;
        }

        return true;
    }

    private boolean createContainer() {

        Onem2mContainerRequestBuilder b;

        b = new Onem2mContainerRequestBuilder();
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

        Onem2mContentInstanceRequestBuilder b;

        b = new Onem2mContentInstanceRequestBuilder();
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

}
