/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.database.transactionCore;

import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.database.lock.Locker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResourceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gguliash on 4/21/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public class TTLGarbageCollector {
    private static final Logger LOG = LoggerFactory.getLogger(TTLGarbageCollector.class);
    private final static int SLEEP_TIME_MILLISECOND = 1000000;
    private final ConcurrentHashMap<Onem2mResourceKey, Onem2mResource> trash = new ConcurrentHashMap<>();
    private final TransactionManager manager;
    private final Thread t;
    private final Locker locker;

    public TTLGarbageCollector(TransactionManager manager, Locker locker) {
        this.locker = locker;
        this.manager = manager;
        t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(SLEEP_TIME_MILLISECOND);
                        LOG.info("GC epoch starting. Number of elements(approx) = {}", trash.size());
                        removeThings();
                        LOG.info("GC epoch finished. Number of elements(approx) = {}", trash.size());
                    } catch (Exception e) {
                        LOG.error("error in GC. Very bad" + e.getMessage());
                    }
                }
            }
        });
        t.start();
    }

    private void removeThings() {
        Object lockerObject = locker.lockEverything();
        try {
            for (Iterator<Map.Entry<Onem2mResourceKey, Onem2mResource>> iter = trash.entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry<Onem2mResourceKey, Onem2mResource> elem = iter.next();
                iter.remove();

                try (ResourceTreeWriter twc = manager.getDbResourceTreeWriter()) {
                    if (!Onem2mDb.getInstance().deleteResourceUsingResource(twc, manager.getTransactionReader(), elem.getValue())) {
                        LOG.warn("GC epoch: Element could not be removed, possibly was removed someones subtree");
                    }
                }

            }
        } finally {
            locker.unlockEverything(lockerObject);
        }
    }

    /**
     * Adds expired resources into the queue. Resource will be removed from the database during next epoch.
     * @param resource to be removed
     */
    public void addGarbage(Onem2mResource resource) {
        trash.put(resource.getKey(), resource);
    }

}
