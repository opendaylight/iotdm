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

    public Onem2mOicIPE(Onem2mService onem2mService) {
        this.onem2mService = onem2mService;
    }

    /**
     * Creates an AE that maps to OIC Device
     *
     * @param device    Oic device that needs to added to M2M resource tree
     * @return          true on sucessful creation of AE, failse on unsuccessful attempt
     */
    public boolean createOicAe(Onem2mOicClient.OicDevice device, String cseName) {
        AE oicAe;

        if (cseName == null || cseName.isEmpty()) {
            /* Its too chatty to log error in this api */
            //LOG.error("CSE Name is null, cant create AE");
            return false;
        }

        oicAe = mapOicTom2m(device, cseName);

        if (getCse(cseName)) {
            if (getAE(cseName, oicAe.getPrimitiveValue("nm")) || createAE(oicAe)) {
                //LOG.error("AE creation successfull");
                return true;
            }
        } else {
            //LOG.error("CSE with cseName does not exist");
        }

        return false;
    }

    /**
     * Maps OIC device attributes to OneM2M AE attributes as follows:
     *  n - Name, AppName
     *  di - AppId
     *  icv
     *  dmv
     *  if[]
     *
     * @param device    Oic device that needs to added to M2M resource tree
     * @return
     */
    private AE mapOicTom2m(Onem2mOicClient.OicDevice device, String cseName) {
        AE oicAe;

        oicAe = new AE();
        oicAe.setTo(cseName);
        oicAe.setOperationCreate();
        oicAe.setName(device.di);
        oicAe.setAppName(device.di);
        oicAe.setAppId(device.di);
        oicAe.setRequestReachability(false);

        return oicAe;
    }

    /**
     * Verifies whether the CSE with cseName exists
     *
     * @param cseName   Name of the cse
     * @return          true if it exists else false
     */
    private boolean getCse(String cseName) {

        Onem2mRequestPrimitiveClient req = new CSE().setOperationRetrieve().setTo(cseName).build();
        Onem2mResponsePrimitiveClient res = req.send(onem2mService);
        if (!res.responseOk()) {
            LOG.error("CSE get request failed: {}", res.getContent());
            return false;
        }

        Onem2mCSEResponse cseResponse = new Onem2mCSEResponse(res.getContent());
        if (!cseResponse.responseOk()) {
            LOG.error("CSE get response not ok: {}", res.getError());
            return false;
        }

        String resourceId = cseResponse.getResourceId();
        if (resourceId == null) {
            LOG.error("Create cannot parse resourceId for CSE get");
            return false;
        }

        return true;
    }

    /**
     * Creates/Provisions new CSE resource in Onem2m database
     *
     * @param cseName   Name of the cse to be created
     * @return          true if successful else false
     */
    private boolean provisionCse(String cseName) {
        CSE oicCse;

        oicCse = new CSE();
        oicCse.setCseId(cseName);
        oicCse.setCseType(Onem2m.CseType.INCSE);
        oicCse.setOperationCreate();
        Onem2mRequestPrimitiveClient req = oicCse.build();
        Onem2mResponsePrimitiveClient res = req.send(onem2mService);
        if (!res.responseOk()) {
            LOG.error("CSE create request failed: {}", res.getError());
            return false;
        }

        Onem2mCSEResponse cseResponse = new Onem2mCSEResponse(res.getContent());

        if (!cseResponse.responseOk()) {
            LOG.error("CSE create response not ok: {}", res.getError());
            return false;
        }

        String resourceId = cseResponse.getResourceId();
        if (resourceId == null) {
            LOG.error("Create cannot parse resourceId for CSE provision");
            return false;
        }

        return true;
    }

    /**
     * Creates/Provisions new AE resource
     *
     * @param oicDeviceAe   AE object with attributes mapped from OIC Devices
     * @return          true if successful else false
     */
    private boolean createAE(AE oicDeviceAe) {
        Onem2mRequestPrimitiveClient req = oicDeviceAe.build();
        Onem2mResponsePrimitiveClient res = req.send(onem2mService);
        if (!res.responseOk()) {
            LOG.error("AE create request failed: {}", res.getError());
            return false;
        }

        Onem2mAEResponse aeResponse = new Onem2mAEResponse(res.getContent());

        if (!aeResponse.responseOk()) {
            LOG.error("AE create response not ok: {}", aeResponse.getError());
            return false;
        }

        String resourceId = aeResponse.getResourceId();
        if (resourceId == null) {
            LOG.error("Create cannot parse resourceId for AE create");
            return false;
         }

        return true;
    }

    /**
     * Verifies whether the AE with aeName exists under cseName resource tree
     *
     * @param cseName   Name of the cse
     * @param aeName    Name of the ae
     * @return          true if it exists else false
     */
    private boolean getAE(String cseName, String aeName) {

        AE oicDevAe;

        oicDevAe = new AE();
        oicDevAe.setOperationRetrieve();
        oicDevAe.setTo("/" + cseName + "/" + aeName);
        Onem2mRequestPrimitiveClient req = oicDevAe.build();
        Onem2mResponsePrimitiveClient res = req.send(onem2mService);
        if (!res.responseOk()) {
            LOG.error("AE get request failed: {}", res.getError());
            return false;
        }
        Onem2mAEResponse aeResponse = new Onem2mAEResponse(res.getContent());
        if (!aeResponse.responseOk()) {
            LOG.error("AE get response error: {}", aeResponse.getError());
            return false;
        }

        String resourceId = aeResponse.getResourceId();
        if (resourceId == null) {
            LOG.error("Create cannot parse resourceId for AE retrieve");
            return false;
        }

        if (!aeName.contentEquals(aeResponse.getAppName())) {
            LOG.error("ae_app_name mismatch: expected: {}, received: {}",
                    aeName, aeResponse.getAppName());
            return false;
        }

        return true;
    }

    /**
     * Delete the AE aeName resource under cseName resource tree
     *
     * @param cseName   Name of the cse
     * @param aeName    Name of the ae
     * @return          true if it exists else false
     */
    private boolean deleteAE(String cseName, String aeName) {

        AE oicDevAe;

        oicDevAe = new AE();
        oicDevAe.setOperationDelete();
        oicDevAe.setTo("/" + cseName + "/" + aeName);
        Onem2mRequestPrimitiveClient req = oicDevAe.build();
        Onem2mResponsePrimitiveClient res = req.send(onem2mService);
        if (!res.responseOk()) {
            LOG.error("AE delete request failed: {}", res.getError());
            return false;
        }
        Onem2mAEResponse aeResponse = new Onem2mAEResponse(res.getContent());
        if (!aeResponse.responseOk()) {
            LOG.error("AE delete response not ok: {}", aeResponse.getError());
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
