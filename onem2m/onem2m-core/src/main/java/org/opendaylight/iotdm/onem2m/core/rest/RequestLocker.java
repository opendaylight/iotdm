/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.rest;

import com.google.common.util.concurrent.Monitor;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Avoid concurrent write access to a resource
 */
public class RequestLocker {

    private static final Logger LOG = LoggerFactory.getLogger(RequestLocker.class);
    private static RequestLocker rl;
    private Monitor gMonitor;
    private ConcurrentHashMap<String,ResourceInfo> resourceInfoMap;
    private static final Integer INITIAL_MONITOR_COUNT = 200;

    public static RequestLocker getInstance() {
        if (rl == null) {
            rl = new RequestLocker();
        }
        return rl;
    }

    private RequestLocker() {
        gMonitor = new Monitor();
        resourceInfoMap = new ConcurrentHashMap<>(INITIAL_MONITOR_COUNT);
    }

    public void LockResource(String resourceId) {
        ResourceInfo resourceInfo;
        gMonitor.enter();
        resourceInfo = resourceInfoMap.get(resourceId);
        if (resourceInfo == null) {
            resourceInfo = new ResourceInfo();
            resourceInfoMap.put(resourceId, resourceInfo);
        } else {
            resourceInfo.numLocks++;
        }
        gMonitor.leave();
        //LOG.info("LockResource: entering res: {}", resourceId);
        resourceInfo.monitor.enter();
        //LOG.info("LockResource: in monitor res: {}", resourceId);
    }

    public void UnlockResource(String resourceId) {
        ResourceInfo resourceInfo;
        gMonitor.enter();
        resourceInfo = resourceInfoMap.get(resourceId);
        if (resourceInfo != null) {
            //LOG.info("UnockResource: leaving res: {}", resourceId);
            resourceInfo.monitor.leave();
            //LOG.info("UnockResource: left monitor res: {}", resourceId);
            resourceInfo.numLocks--;
            if (resourceInfo.numLocks == 0) {
                resourceInfoMap.remove(resourceId);
            }
        } else {
            LOG.error("UnlockResource: resource lock not found: {}", resourceId);
        }
        gMonitor.leave();
    }

    private class ResourceInfo {
        protected int numLocks;
        protected Monitor monitor;
        private ResourceInfo() {
            this.numLocks = 1;
            this.monitor = new Monitor();
        }
    }
}

