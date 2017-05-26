/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors;

import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.IotdmAppProtoDefs;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.IotdmRxAppProtocolConfigration;

public abstract class IotdmRxDescriptorApplicationBuilder<Tconfig extends IotdmRxAppProtocolConfigration>
                            extends IotdmRxDescriptorApplicationBaseBuilder<Tconfig> {

    public IotdmRxDescriptorApplicationBuilder(final String transportProtocol,
                                               final String role,
                                               final String protocolChannelId) {
        super.setTransportProtocol(transportProtocol);
        super.setRole(role);
        super.setProtocolChannelId(protocolChannelId);
    }

    public void setPortNumber(int port) {
        super.setPortNumber(port);
    }

    public int getPortNumber() {
        return super.getPortNumber();
    }

    public void setEndpoint(String endpoint) {
        super.setEndpoint(endpoint);
    }

    public String getEndpoint() {
        return super.getEndpoint();
    }

    public void setModeExclusive() {
        setShared(false);
        setMode(IotdmAppProtoDefs.EndpointRegistryMode.Exclusive);
    }

    public boolean isModeExclusive() {
        return getMode().equals(IotdmAppProtoDefs.EndpointRegistryMode.Exclusive);
    }

    public void setModeSharedExactMatch() {
        setShared(true);
        setMode(IotdmAppProtoDefs.EndpointRegistryMode.SharedExactMatch);
    }

    public boolean isModeSharedExactMatch() {
        return getMode().equals(IotdmAppProtoDefs.EndpointRegistryMode.SharedExactMatch);
    }

    public void setModeSharedPrefixMatch() {
        setShared(true);
        setMode(IotdmAppProtoDefs.EndpointRegistryMode.SharedPrefixMatch);
    }

    public boolean isModeSharedPrefixMatch() {
        return getMode().equals(IotdmAppProtoDefs.EndpointRegistryMode.SharedPrefixMatch);
    }

    protected void validateValues() throws IllegalStateException {
        int portNumber = this.getPortNumber();
        if (portNumber <= 0 || portNumber > 0xffff) {
            throw new IllegalStateException("Invalid port number");
        }
    }

    protected void validateAll() throws IllegalStateException {
        super.validateAll();
        this.validateValues();
    }
}
