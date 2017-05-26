/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins;

/**
 * This class of exceptions is used by PluginManager and related services.
 * These exceptions are thrown in case of failure of registration of plugin instance.
 */
public class IotdmPluginRegistrationException extends Exception {
    public IotdmPluginRegistrationException(String errMsg) {
        super(errMsg);
    }
}
