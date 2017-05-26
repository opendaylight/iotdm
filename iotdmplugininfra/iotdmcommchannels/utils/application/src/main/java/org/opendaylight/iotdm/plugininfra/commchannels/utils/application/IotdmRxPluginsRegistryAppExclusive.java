/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.commchannels.utils.application;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxPluginsRegistry;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxPluginsRegistryReadOnly;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.descriptors.IotdmChannelEndpointDescriptor;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPlugin;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;

public abstract class IotdmRxPluginsRegistryAppExclusive<Tplugin extends IotdmPlugin>
                        implements IotdmRxPluginsRegistry<Tplugin, String> {

    private Tplugin plugin = null;

    @Override
    public abstract void registerPlugin(IotdmPlugin plugin, IotdmChannelEndpointDescriptor endpoint)
                    throws IotdmPluginRegistrationException;


    protected void registerPluginOfSpecificType(Tplugin plugin) throws IotdmPluginRegistrationException {
        // Only one plugin can be registered
        if (null != this.plugin) {
            throw new IotdmPluginRegistrationException(
                "Attempt to register multiple plugins in exclusive registry. Plugin registered: " +
                this.plugin.getDebugString() +
                " new plugin: " + plugin.getDebugString());
        }

        // endpoint can be ignored
        this.plugin = plugin;
    }

    @Override
    public void deregisterPlugin(IotdmPlugin plugin) throws IotdmPluginRegistrationException {
        // TODO

        throw new IotdmPluginRegistrationException("Not implemented");

//        try {
//            if (null == this.plugin) {
//                throw new IotdmPluginRegistrationException()
//            }
//
//            if (null != plugin) {
//                plugin = null;
//            }
//        } catch (IotdmPluginRegistrationException e) {
//            // just rethrow
//            throw e;
//        } catch (Exception e) {
//            throw new IotdmPluginRegistrationException(e);
//        }
    }

    @Override
    public boolean isEmpty() {
        return null == this.plugin;
    }

    @Override
    public IotdmPlugin getPluginOfEndpoint(IotdmChannelEndpointDescriptor endpoint) {
        // TODO do we need to check the endpoint here ?
        return this.plugin;
    }

    @Override
    public Collection<IotdmPlugin> getAllPlugins() {
        List<IotdmPlugin> list = new LinkedList<>();
        if (! isEmpty()) {
            list.add(this.plugin);
        }
        return list;
    }

    @Override
    public Tplugin getPlugin(String endpoint) {
        // TODO do we need to check the endpoint here ?
        return this.plugin;
    }
}
