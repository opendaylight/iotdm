/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.http;

import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpPlugin;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpRegistry;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.IotdmRxPluginsRegistryAppExclusive;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.descriptors.IotdmChannelEndpointDescriptor;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPlugin;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;

public class Onem2mHttpPluginsRegistryExclusive extends IotdmRxPluginsRegistryAppExclusive<Onem2mHttpPlugin>
                                                implements Onem2mHttpRegistry {
    @Override
    public void registerPlugin(IotdmPlugin plugin, IotdmChannelEndpointDescriptor endpoint)
        throws IotdmPluginRegistrationException {

        if (! (plugin instanceof Onem2mHttpPlugin)) {
            throw new IotdmPluginRegistrationException("Invalid plugin type passed, Onem2mHttpPlugin is expected");
        }

        Onem2mHttpPlugin specificPlugin = (Onem2mHttpPlugin) plugin;
        this.registerPluginOfSpecificType(specificPlugin);
    }
}
