/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.database.lock;

import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;

/**
 * Created by gguliash on 5/17/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public interface Locker {
    /**
     * Locks everything, nothing else can acquire lock at the same time.
     * @return Object which might be used for unlocking.
     */
    Object lockEverything();

    /**
     * Acquires lock before start of the resourceId update
     * @param trc DB reader
     * @param resourceId id of the resource
     * @return Object which might be used for unlocking.
     */
    Object lockUpdate(ResourceTreeReader trc, String resourceId);

    /**
     * Acquires lock before start of the resourceId update
     * @param trc DB reader
     * @param resourceId id of the resource
     * @param resourceName name of the resource
     * @return Object which might be used for unlocking.
     */
    Object lockCreate(ResourceTreeReader trc, String resourceId, String resourceName);

    /**
     * Acquires lock before start of the resourceId delete
     * @param trc DB reader
     * @param resourceId id of the resource
     * @return Object which might be used for unlocking.
     */
    Object lockDelete(ResourceTreeReader trc, String resourceId);

    /**
     * Releases locks acquired by lockEverything()
     * @param o object which is returned by lockEverything()
     */
    void unlockEverything(Object o);

    /**
     * Releases locks acquired by lockUpdate()
     * @param o object which is returned by lockUpdate()
     */
    void unlockUpdate(Object o);

    /**
     * Releases locks acquired by lockCreate()
     * @param o object which is returned by lockCreate()
     */
    void unlockCreate(Object o);

    /**
     * Releases locks acquired by lockDelete()
     * @param o object which is returned by lockDelete()
     */
    void unlockDelete(Object o);
}
