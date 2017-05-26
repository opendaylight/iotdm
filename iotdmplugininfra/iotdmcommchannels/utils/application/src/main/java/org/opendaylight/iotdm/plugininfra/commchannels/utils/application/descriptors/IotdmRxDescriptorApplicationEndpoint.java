/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors;

import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.IotdmAppProtoDefs;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.descriptors.IotdmChannelEndpointDescriptor;

public class IotdmRxDescriptorApplicationEndpoint implements IotdmChannelEndpointDescriptor {

    private final String endpoint;
    private final IotdmAppProtoDefs.EndpointRegistryMode mode;

    public IotdmRxDescriptorApplicationEndpoint(final String endpoint,
                                                final IotdmAppProtoDefs.EndpointRegistryMode mode) {
        this.endpoint = endpoint;
        this.mode = mode;
    }

    @Override
    public boolean equals(Object o) {
        if (! (o instanceof IotdmRxDescriptorApplicationEndpoint)) {
            return false;
        }

        IotdmRxDescriptorApplicationEndpoint desc = (IotdmRxDescriptorApplicationEndpoint) o;

        if (! this.getEndpoint().equals(desc.getEndpoint())) {
            return false;
        }

        if (! this.getMode().equals(desc.getMode())) {
            return false;
        }

        return true;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public IotdmAppProtoDefs.EndpointRegistryMode getMode() {
        return this.mode;
    }
}
