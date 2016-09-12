/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.database.lock;

import com.google.common.util.concurrent.Monitor;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;

/**
 * Created by gguliash on 5/17/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public class DumbLocker implements Locker {
    Monitor monitor = new Monitor();

    /**
     * Locks everything
     * @return null
     */
    @Override
    public Object lockEverything() {
        monitor.enter();
        return null;
    }

    /**
     * Locks everything
     * @param trc DB reader
     * @param resourceId id of the resource
     * @return null
     */
    @Override
    public Object lockUpdate(ResourceTreeReader trc, String resourceId) {
        return lockEverything();
    }

    /**
     * Locks everything
     * @param trc DB reader
     * @param resourceId id of the resource
     * @param resourceName name of the resource
     * @return null
     */
    @Override
    public Object lockCreate(ResourceTreeReader trc, String resourceId, String resourceName) {
        return lockEverything();
    }

    /**
     * Locks everything
     * @param trc DB reader
     * @param resourceId id of the resource
     * @return null
     */
    @Override
    public Object lockDelete(ResourceTreeReader trc, String resourceId) {
        return lockEverything();
    }

    /**
     * Unlocks everything
     * @param o null
     */
    @Override
    public void unlockEverything(Object o) {
        monitor.leave();
    }

    /**
     * Unlocks everything
     * @param o null
     */
    @Override
    public void unlockUpdate(Object o) {
        unlockEverything(o);
    }

    /**
     * Unlocks everything
     * @param o null
     */
    @Override
    public void unlockCreate(Object o) {
        unlockEverything(o);
    }

    /**
     * Unlocks everything
     * @param o null
     */
    @Override
    public void unlockDelete(Object o) {
        unlockEverything(o);
    }

}
