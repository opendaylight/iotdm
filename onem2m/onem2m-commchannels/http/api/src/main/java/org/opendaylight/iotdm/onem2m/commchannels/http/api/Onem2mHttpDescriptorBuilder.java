/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.http.api;

import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.IotdmAppProtoDefs;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.IotdmRxAppProtocolConfigurationNone;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors.IotdmRxDescriptorApplicationBuilder;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors.IotdmRxDescriptorApplicationEndpoint;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors.IotdmRxDescriptorApplicationInterface;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors.IotdmRxDescriptorApplicationProtocol;

public class Onem2mHttpDescriptorBuilder extends IotdmRxDescriptorApplicationBuilder {

    public Onem2mHttpDescriptorBuilder() {
        super(IotdmAppProtoDefs.TransportProtocolTcp,
              IotdmAppProtoDefs.RoleServer,
              Onem2mHttpChannelProviderService.PROTOCHANNELHTTP);
        setSchema(Onem2mHttpChannelProviderService.SCHEMAHTTP);
        setSecure(false);
    }

    @Override
    protected void validateConfiguration() throws IllegalStateException {
        // HTTP doesn't use any specific protocol configuration
//        return;
    }

    public Onem2mHttpDescriptor build() throws IllegalStateException {

        this.validateAll();

        IotdmRxDescriptorApplicationInterface intDesc =
            new IotdmRxDescriptorApplicationInterface(this.getIpVersion(), this.getIpAddress(),
                                                      this.getTransportProtocol(), this.getPortNumber(),
                                                      this.getRole(), this.isShared());

        IotdmRxDescriptorApplicationProtocol<IotdmRxAppProtocolConfigurationNone> protoDesc =
            new IotdmRxDescriptorApplicationProtocol<>(this.getProtocolChannelId(), this.getSchema(),
                                                       this.isSecure(),
                                                       IotdmRxAppProtocolConfigurationNone.getInstance());

        IotdmRxDescriptorApplicationEndpoint endpointDesc =
            new IotdmRxDescriptorApplicationEndpoint(this.getEndpoint(), this.getMode());

        return new Onem2mHttpDescriptor(intDesc, protoDesc, endpointDesc);
    }
}
