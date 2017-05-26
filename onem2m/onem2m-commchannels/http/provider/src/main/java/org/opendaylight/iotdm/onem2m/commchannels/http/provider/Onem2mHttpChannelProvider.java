/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.http.provider;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.opendaylight.iotdm.onem2m.commchannels.http.Onem2mHttpChannelFactory;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpChannelProviderService;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpDescriptorBuilder;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpPlugin;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpsConfigBuilder;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpsDescriptorBuilder;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors.IotdmRxDescriptorApplication;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.IotdmChannelProviderRegistrationException;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.IotdmChannelProviderRegistrationService;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxChannelFactory;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxChannelProvider;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxPluginsRegistryFactory;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mHttpChannelProvider implements IotdmRxChannelProvider, Onem2mHttpChannelProviderService,
                                                  AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpChannelProvider.class);

    protected final IotdmPluginRegistrationService pluginRegService;
    protected final IotdmChannelProviderRegistrationService channelProviderRegService;

    public Onem2mHttpChannelProvider(final IotdmChannelProviderRegistrationService channelProviderRegService,
                                     final IotdmPluginRegistrationService pluginRegService) {
        this.channelProviderRegService = channelProviderRegService;
        this.pluginRegService = pluginRegService;

        try {
            channelProviderRegService.registerRxProvider(this);
        } catch (IotdmChannelProviderRegistrationException e) {
            LOG.error("Failed to register as ChannelProvider: {}, error: {}", PROTOCHANNELPRODVIDERID, e);
        }
    }

    @Override
    public void close() {
        try {
            this.channelProviderRegService.unregisterRxProvider(this);
        } catch (IotdmChannelProviderRegistrationException e) {
            LOG.error("Failed to deregister ChannelProvider: {}, error: {}", PROTOCHANNELPRODVIDERID, e);
        }
    }

    @Override
    public String getProtocolChannelProviderId() {
        return PROTOCHANNELPRODVIDERID;
    }

    @Override
    public List<String> getProtocolChannelsIds() {
        return new LinkedList<>(Arrays.asList(PROTOCHANNELHTTP, PROTOCHANNELHTTPS));
    }

    @Override
    public IotdmRxChannelFactory getRxChannelFactory() {
        return new Onem2mHttpChannelFactory();
    }

    @Override
    public IotdmRxPluginsRegistryFactory getRxChannelRegistryFactory() {
        // TODO make it singleton
        return new Onem2mHttpChannelFactory();
    }

    /*
     * Implementation of Onem2mHttpChannelProviderService
     */

    @Override
    public Onem2mHttpDescriptorBuilder getHttpDescriptorBuilder() {
        return new Onem2mHttpDescriptorBuilder();
    }

    @Override
    public Onem2mHttpsDescriptorBuilder getHttpsDescriptorBuilder() {
        return new Onem2mHttpsDescriptorBuilder();
    }

    @Override
    public Onem2mHttpsConfigBuilder getHttpsConfigBuilder() {
        return new Onem2mHttpsConfigBuilder();
    }

    @Override
    public void registerPluginInManager(final Onem2mHttpPlugin plugin, IotdmRxDescriptorApplication descriptor)
        throws IotdmPluginRegistrationException {

        this.pluginRegService.registerRxPlugin(descriptor, plugin);
    }

    @Override
    public void deregisterPluginInManager(final Onem2mHttpPlugin plugin, IotdmRxDescriptorApplication descriptor) {
        this.pluginRegService.deregisterRxPlugin(descriptor, plugin);
    }
}
