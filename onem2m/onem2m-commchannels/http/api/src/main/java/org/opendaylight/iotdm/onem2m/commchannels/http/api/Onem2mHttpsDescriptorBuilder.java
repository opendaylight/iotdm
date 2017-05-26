/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.http.api;

import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.IotdmAppProtoDefs;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors.IotdmRxDescriptorApplicationBuilder;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors.IotdmRxDescriptorApplicationEndpoint;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors.IotdmRxDescriptorApplicationInterface;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors.IotdmRxDescriptorApplicationProtocol;

public class Onem2mHttpsDescriptorBuilder extends IotdmRxDescriptorApplicationBuilder {

    private HttpsServerConfiguration configuration = null;

    public Onem2mHttpsDescriptorBuilder() {
        super(IotdmAppProtoDefs.TransportProtocolTcp,
              IotdmAppProtoDefs.RoleServer,
              Onem2mHttpChannelProviderService.PROTOCHANNELHTTP);
        setSchema(Onem2mHttpChannelProviderService.SCHEMAHTTP);
        setSecure(false);
    }

    @Override
    protected void validateConfiguration() throws IllegalStateException {
        if (null == this.configuration) {
            // TODO need to implement better verification ?
            throw new IllegalStateException("No configuration set");
        }
    }

    public Onem2mHttpsDescriptor build() throws IllegalStateException {

        this.validateAll();

        IotdmRxDescriptorApplicationInterface intDesc =
            new IotdmRxDescriptorApplicationInterface(this.getIpVersion(), this.getIpAddress(),
                                                      this.getTransportProtocol(), this.getPortNumber(),
                                                      this.getRole(), this.isShared());

        IotdmRxDescriptorApplicationProtocol<HttpsServerConfiguration> protoDesc =
            new IotdmRxDescriptorApplicationProtocol<>(this.getProtocolChannelId(), this.getSchema(),
                                                       this.isSecure(),
                                                       this.configuration);

        IotdmRxDescriptorApplicationEndpoint endpointDesc =
            new IotdmRxDescriptorApplicationEndpoint(this.getEndpoint(), this.getMode());

        return new Onem2mHttpsDescriptor(intDesc, protoDesc, endpointDesc);
    }

    public HttpsServerConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(HttpsServerConfiguration configuration) {
        this.configuration = configuration;
    }
}
