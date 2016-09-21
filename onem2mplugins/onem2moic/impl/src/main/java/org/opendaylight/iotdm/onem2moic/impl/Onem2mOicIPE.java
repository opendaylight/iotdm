/*
 * Copyright © 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2moic.impl;

import org.opendaylight.iotdm.onem2m.client.*;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mOicIPE {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mOicProvider.class);
    private Onem2mService onem2mService;
    private static final String ONEM2M_DEFAULT_CSE_NAME = "InCSE1";

    public Onem2mOicIPE(Onem2mService onem2mService) {
        this.onem2mService = onem2mService;
    }
    /******************************* IPE - OicClient interaction apis *******************************/
    public boolean createOicAe(Onem2mOicClient.OicDevice device){
        AE oicAe = mapOicTom2m(device);

        /* Check for cse */
        if(!getCse(oicAe.getPrimitiveValue("to")))
            //if (!provisionCse(oicAe.getPrimitiveValue("to")))
                return false;

        if(!createAE(oicAe)) {
            return false;
        }

        return true;
    }

    /******************************* OIC - Map apis *******************************/
    private AE mapOicTom2m(Onem2mOicClient.OicDevice device) {
        AE oicAe;

        /*
         * Map OIC device to onem2m AE
         * n - Name, AppName
         * di - AppId
         * icv
         * dmv
         * if[]
         * */
        oicAe = new AE();
        oicAe.setTo(ONEM2M_DEFAULT_CSE_NAME);
        oicAe.setOperationCreate();
        oicAe.setName(device.di);
        oicAe.setAppName(device.di);
        oicAe.setAppId(device.di);
        oicAe.setRequestReachability(false);

        return oicAe;
    }


    /******************************* OneM2M Interactions *******************************/
    private boolean getCse(String cseName) {

        Onem2mRequestPrimitiveClient req = new CSE().setOperationRetrieve().setTo(cseName).build();
        Onem2mResponsePrimitiveClient res = req.send(onem2mService);
        if (!res.responseOk()) {
            LOG.error(res.getContent());
            return false;
        }

        Onem2mCSEResponse cseResponse = new Onem2mCSEResponse(res.getContent());
        if (!cseResponse.responseOk()) {
            LOG.error(res.getError());
            return false;
        }

        String resourceId = cseResponse.getResourceId();
        if (resourceId == null) {
            LOG.error("Create cannot parse resourceId for CSE get");
            return false;
        }

        return true;
    }

    private boolean provisionCse(String cseName) {

        CSE b;

        b = new CSE();
        b.setCseId(cseName);
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

    private boolean createAE(AE b) {

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

    private boolean getAE(String cseName, String aeName) {

        AE b;

        b = new AE();
        b.setOperationRetrieve();
        b.setTo("/" + cseName + "/" + aeName);
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

        if (!aeName.contentEquals(aeResponse.getAppName())) {
            LOG.error("ae_app_name mismatch: expected: {}, received: {}", aeName, aeResponse.getAppName());
            return false;
        }

        return true;
    }

    private boolean deleteAE(String cseName, String aeName) {

        AE b;

        b = new AE();
        b.setOperationDelete();
        b.setTo("/" + cseName + "/" + aeName);
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
}
