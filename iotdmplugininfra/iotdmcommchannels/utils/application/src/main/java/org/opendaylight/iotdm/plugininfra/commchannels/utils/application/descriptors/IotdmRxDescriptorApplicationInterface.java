/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors;


import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.IotdmAppProtoDefs;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.descriptors.IotdmChannelInterfaceDescriptor;

public class IotdmRxDescriptorApplicationInterface implements IotdmChannelInterfaceDescriptor {

    private final String ipVersion;
    private final String ipAddress;
    private final String transportProtocol;
    private final int portNumber;
    private final String role;  // client or server
    private final boolean shared;

    public IotdmRxDescriptorApplicationInterface(final String ipVersion,
                                                    final String ipAddress,
                                                    final String transportProtocol,
                                                    final int portNumber,
                                                    final String role,
                                                    final boolean shared) {
        this.ipVersion = ipVersion;
        this.ipAddress = ipAddress;
        this.transportProtocol = transportProtocol;
        this.portNumber = portNumber;
        this.role = role;
        this.shared = shared;
    }

    @Override
    public boolean equals(Object o) {
        if (! (o instanceof IotdmRxDescriptorApplicationInterface)) {
            return false;
        }

        IotdmRxDescriptorApplicationInterface desc = (IotdmRxDescriptorApplicationInterface) o;

        if (! this.getIpVersion().equals(desc.getIpVersion())) {
            return false;
        }

        if (! this.getIpAddress().equals(desc.getIpAddress())) {
            return false;
        }

        if (! this.getTransportProtocol().equals(desc.getTransportProtocol())) {
            return false;
        }

        if (this.getPortNumber() != desc.getPortNumber()) {
            return false;
        }

        if (! this.getRole().equals(desc.getRole())) {
            return false;
        }

        if (this.isShared() != desc.isShared()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isInConflict(Object o) {
        if (! (o instanceof IotdmRxDescriptorApplicationInterface)) {
            return false;
        }

        IotdmRxDescriptorApplicationInterface desc = (IotdmRxDescriptorApplicationInterface) o;

        if (! this.getIpVersion().equals(desc.getIpVersion())) {
            return false;
        }

        if (! this.getTransportProtocol().equals(desc.getTransportProtocol())) {
            return false;
        }

        if (this.getIpAddress().equals(desc.getIpAddress()) ||
            this.getIpAddress().equals(IotdmAppProtoDefs.IPv4AllInterfaces) ||
            desc.getIpAddress().equals(IotdmAppProtoDefs.IPv4AllInterfaces)) {
            return true;
        }

        if (this.getPortNumber() == desc.getPortNumber()) {
            return true;
        }

        // IsShared doesn't matter here

        // TODO What about role ???

        return false;
    }

    @Override
    public boolean isShared() {
        return this.shared;
    }

    public String getIpVersion() {
        return ipVersion;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getTransportProtocol() {
        return transportProtocol;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public String getRole() {
        return role;
    }
}
