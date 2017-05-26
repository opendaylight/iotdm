/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx;

import java.util.Collection;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.descriptors.IotdmChannelEndpointDescriptor;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPlugin;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;

public interface IotdmRxPluginsRegistry<Tplugin extends IotdmPlugin, Tdstendpoint>
                    extends IotdmRxPluginsRegistryReadOnly<Tplugin, Tdstendpoint> {

    void registerPlugin(IotdmPlugin plugin, IotdmChannelEndpointDescriptor endpoint) throws IotdmPluginRegistrationException;
    void deregisterPlugin(IotdmPlugin plugin) throws IotdmPluginRegistrationException;

    boolean isEmpty();

    IotdmPlugin getPluginOfEndpoint(IotdmChannelEndpointDescriptor endpoint);

    Collection<IotdmPlugin> getAllPlugins();
}
