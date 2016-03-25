/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.tsdr.impl;

import static java.lang.Thread.sleep;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mtsdr.rev160210.onem2m.tsdr.config.Onem2mTargetDesc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mTsdrAsyncManager {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mTsdrAsyncManager.class);
    private Onem2mTsdrSender onem2mTsdrSender;
    private HashMap<String,Onem2mTargetDesc> tsdrMap;

    public Onem2mTsdrAsyncManager(Onem2mTsdrSender onem2mTsdrSender) {
        this.onem2mTsdrSender = onem2mTsdrSender;
        tsdrMap = new HashMap<String,Onem2mTargetDesc>();

        LOG.info("Created Onem2mTsdrAsyncManager");
    }

    public void close() {
    }

    /*
    ** These crud routines handle changes to the onem2mtsdr.yang descriptor changes in the datastore
     */
    public void onem2mTargetDescCreated(Onem2mTargetDesc onem2mTargetDesc) {
        tsdrMap.put(onem2mTargetDesc.getOnem2mTargetUri(), onem2mTargetDesc);
    }
    public void onem2mTargetDescChanged(Onem2mTargetDesc onem2mTargetDesc) {
        tsdrMap.put(onem2mTargetDesc.getOnem2mTargetUri(), onem2mTargetDesc);
    }
    public void onem2mTargetDescDeleted(Onem2mTargetDesc onem2mTargetDesc) {
        tsdrMap.remove(onem2mTargetDesc.getOnem2mTargetUri());
    }

    /*
    ** When a database update occurs, see if falls under an async onem2mTargetDesc, then send the data to the TSDR
     */
    public void onem2mResourceUpdate(String h, Onem2mResource onem2mResource) {
        for (Map.Entry<String, Onem2mTargetDesc> entry : tsdrMap.entrySet()) {
            Onem2mTargetDesc targetDesc = entry.getValue();
            // ... see if there are any target's in the tsdrMap that contain this resource
            String targetResourceId = Onem2mDb.getInstance().findResourceIdUsingURI(targetDesc.getOnem2mTargetUri());
            if (Onem2mDb.getInstance().isResourceIdUnderTargetId(targetResourceId, onem2mResource.getResourceId())) {
                onem2mTsdrSender.sendDataToTsdr(targetDesc, h, onem2mResource);
                break;
            }
        }
    }
}
