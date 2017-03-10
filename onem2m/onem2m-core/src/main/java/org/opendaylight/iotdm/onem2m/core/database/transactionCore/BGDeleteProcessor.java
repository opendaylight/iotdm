/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.database.transactionCore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.parent.child.list.Onem2mParentChild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by gguliash on 4/21/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public class BGDeleteProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(BGDeleteProcessor.class);

    //private final static int SLEEP_TIME_MILLISECOND = 1000000;
    private final static int SLEEP_TIME_MILLISECOND = 10000;
    private final static int MAX_TO_DELETE_PER_CLEANUP = 50;
    private final Thread t;
    private LinkedBlockingQueue<String> q = new LinkedBlockingQueue<>();

    public BGDeleteProcessor() {
        t = new Thread(new Runnable() {
            @Override
            public void run() {

                Thread.currentThread().setName("db-grbg-cltr");

                String resourceId;
                Onem2mDb db = Onem2mDb.getInstance();
                boolean useSleepTimer = true;
                long then = System.currentTimeMillis();

                while (true) {
                    try {

                        // process any q'ed resources to be moved to the delete parent in the database
                        if (q.size() != 0 || useSleepTimer) {
                            resourceId = q.poll(30, TimeUnit.SECONDS);
                            if (resourceId != null) {
                                Onem2mResource onem2mResource = db.getResource(resourceId);
                                if (onem2mResource != null) {
                                    if (!db.moveParentChildLinkToDeleteParent(
                                            onem2mResource.getParentId(),
                                            onem2mResource.getName(),
                                            onem2mResource.getResourceId())) {
                                        LOG.error("TTLGarbageCollector: cannot move child: res{}, name {} from oldparent: {} to delete parent",
                                                onem2mResource.getResourceId(), onem2mResource.getName(), onem2mResource.getParentId());
                                    }
                                }
                            }
                        }

                        long now = System.currentTimeMillis();
                        if (!useSleepTimer || ((now - then) >= SLEEP_TIME_MILLISECOND)) {
                            LOG.trace("GC epoch starting.");
                            int numDeleted = cleanupResourcesFromDeleteParent();
                            useSleepTimer = numDeleted != MAX_TO_DELETE_PER_CLEANUP;
                            LOG.trace("GC epoch finished. Number of elements(approx) = {}", numDeleted);
                            then = now;
                        }

                    } catch (Exception e) {
                        StackTraceElement[] st = e.getStackTrace();
                        for (int i = 0; i < 4 && i < st.length; i++) {
                            LOG.error("TTLGarbageCollector: e: {}/{}, st:{}/{}",
                                    e.toString(), e.getMessage(), i, st[i].toString());
                        }
                    }
                }
            }
        });
        t.start();
    }

    /*
     * Read from the parent-child database where the parentId == the-delete-resource-id ... each of these
     * resources is potentially the root of a hierarchy of resources ... need to remove from the bottom of the
     * tree so records are not orphaned.
     */

    private int cleanupResourcesFromDeleteParent() {

        int numDeleted = 0;

        try {
            Onem2mDb db = Onem2mDb.getInstance();
            List<String> hierarchyList = new ArrayList<String>();
            Object t = db.startWriteTransaction();

            while (numDeleted < MAX_TO_DELETE_PER_CLEANUP) {

                List<Onem2mParentChild> childResourceList = db.getChildrenForResourceLimitN(Onem2m.SYS_DELETE_RESOURCE_ID, 1);
                if (childResourceList.size() == 0) break;

                hierarchyList.add(childResourceList.get(0).getResourceId());
                int resourceListLen = 1;
                for (int i = 0; i < resourceListLen; i++) {
                    childResourceList = db.getChildrenForResourceLimitN(hierarchyList.get(i), 1);
                    if (childResourceList.size() == 0) break;

                    hierarchyList.add(childResourceList.get(0).getResourceId());
                    resourceListLen++;
                }
                String resToDeleteId = hierarchyList.get(hierarchyList.size() - 1);
                Onem2mResource onem2mResource = db.getResource(resToDeleteId);
                if (onem2mResource != null) {
                    if (hierarchyList.size() == 1) {
                        db.twc.deleteResource(t, resToDeleteId, Onem2m.SYS_DELETE_RESOURCE_ID, onem2mResource.getResourceId());

                    } else {
                        db.twc.deleteResource(t, resToDeleteId, onem2mResource.getParentId(), onem2mResource.getName());
                    }
                } else {
                    LOG.error("cleanupResourcesFromDeleteParent: onem2mresource is null: {}", resToDeleteId);
                }
                hierarchyList.clear();
                ++numDeleted;
            }

            if (!db.endWriteTransaction(t)) {
                LOG.error("cleanupResourcesFromDeleteParent: issues deleting records, endTransaction failed");
            }

        } catch (Exception e) {
            LOG.error("cleanupResourcesFromDeleteParent: {}", e.toString(), e);
        }

        return numDeleted;
    }

    public void moveResourceToDeleteParent(String resourceId) {
        q.add(resourceId);
    }
}
