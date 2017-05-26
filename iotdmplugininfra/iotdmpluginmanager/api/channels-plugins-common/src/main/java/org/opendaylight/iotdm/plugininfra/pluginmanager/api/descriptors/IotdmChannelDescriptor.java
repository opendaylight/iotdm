/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.api.descriptors;

public abstract class IotdmChannelDescriptor<Tintd extends IotdmChannelInterfaceDescriptor,
                                             Tprotod extends IotdmChannelProtocolDescriptor,
                                             Tendpointd extends IotdmChannelEndpointDescriptor> {

    protected final Tintd intDesc;
    protected final Tprotod protoDesc;
    protected final Tendpointd endpointDesc;

    public IotdmChannelDescriptor(final Tintd intDesc,
                                  final Tprotod protoDesc,
                                  final Tendpointd endpointDesc) {
        this.intDesc = intDesc;
        this.protoDesc = protoDesc;
        this.endpointDesc = endpointDesc;
    }

    public String getProtocolChannelId() {
        return this.getProtocolDescriptor().getProtocolChannelId();
    }

    public Tintd getInterfaceDescriptor() {
        return this.intDesc;
    }

    public Tprotod getProtocolDescriptor() {
        return this.protoDesc;
    }

    public Tendpointd getEndpointDescriptor() {
        return this.endpointDesc;
    }

    public boolean equals(Object o) {

        if (! (o instanceof IotdmChannelDescriptor)) {
            return false;
        }

        IotdmChannelDescriptor descriptor = (IotdmChannelDescriptor) o;

        if (! this.getProtocolChannelId().equals(descriptor.getProtocolChannelId())) {
            return false;
        }

        if (! this.getInterfaceDescriptor().equals(descriptor.getInterfaceDescriptor())) {
            return false;
        }

        if (! this.getProtocolDescriptor().equals(descriptor.getProtocolDescriptor())) {
            return false;
        }

        if (! this.getEndpointDescriptor().equals(descriptor.getEndpointDescriptor())) {
            return false;
        }

        return true;
    }
}
