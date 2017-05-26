/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.http;


import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpChannelProviderService;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpDescriptor;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpRegistry;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpsDescriptor;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors.IotdmRxDescriptorApplicationEndpoint;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmBaseRxCommunicationChannel;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxChannelFactory;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxPluginsRegistry;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxPluginsRegistryFactory;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxPluginsRegistryReadOnly;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.descriptors.IotdmChannelDescriptor;

public class Onem2mHttpChannelFactory implements IotdmRxChannelFactory, IotdmRxPluginsRegistryFactory {

    @Override
    public String getProtocolChannelProviderId() {
        return Onem2mHttpChannelProviderService.PROTOCHANNELPRODVIDERID;
    }

    private IotdmRxPluginsRegistry createRegistryInstance(IotdmRxDescriptorApplicationEndpoint appEndpointDesc) {
        switch(appEndpointDesc.getMode()) {
            case Exclusive:
                return new Onem2mHttpPluginsRegistryExclusive();
            default:
                // TODO log
                throw new IllegalArgumentException("Unexpected endpoint descriptor mode");
        }
    }

    @Override
    public IotdmRxPluginsRegistry getEndpointRegistry(IotdmChannelDescriptor channelDescriptor) {

        if (channelDescriptor instanceof Onem2mHttpDescriptor) {
            return createRegistryInstance(((Onem2mHttpDescriptor) channelDescriptor).getEndpointDescriptor());
        }

        if (channelDescriptor instanceof Onem2mHttpsDescriptor) {
            return createRegistryInstance(((Onem2mHttpsDescriptor) channelDescriptor).getEndpointDescriptor());
        }

        // TODO log
        return null;
    }

    @Override
    public IotdmBaseRxCommunicationChannel getRxChannelInstance(IotdmChannelDescriptor channelDescriptor,
                                                                IotdmRxPluginsRegistryReadOnly registry) {

        if (! (registry instanceof Onem2mHttpRegistry)) {
            // TODO log
            return null;
        }

        Onem2mHttpRegistry reg = (Onem2mHttpRegistry) registry;

        if (channelDescriptor instanceof Onem2mHttpDescriptor) {
            // TODO check isSecure()
            return new Onem2mHttpRxChannel((Onem2mHttpDescriptor) channelDescriptor, reg);
        }

        if (channelDescriptor instanceof Onem2mHttpsDescriptor) {
            // TODO check isSecure() ?
            return new Onem2mHttpsRxChannel((Onem2mHttpsDescriptor) channelDescriptor, reg);
        }

        // TODO log
        return null;
    }
}
