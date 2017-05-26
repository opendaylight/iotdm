/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx;

import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPlugin;

/**
 * Describes generic registry of plugins for specific endpoint identified by endpoint descriptor.
 * @param <Tdstendpoint> Endpoint descriptor type
 */
public interface IotdmRxPluginsRegistryReadOnly<Tplugin extends IotdmPlugin, Tdstendpoint> {

    /**
     * Returns plugin registered for given endpoint identified by endpoint descriptor object.
     * @param endpoint Endpoint descriptor object
     * @return Registered plugin or null is returned if there is not any plugin registered for the given endpoint.
     */
    Tplugin getPlugin(Tdstendpoint endpoint);
}
