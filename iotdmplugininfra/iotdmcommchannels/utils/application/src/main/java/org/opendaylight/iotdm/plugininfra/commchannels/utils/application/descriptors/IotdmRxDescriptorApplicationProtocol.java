/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors;

import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.IotdmRxAppProtocolConfigration;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.descriptors.IotdmChannelProtocolDescriptor;

public class IotdmRxDescriptorApplicationProtocol<Tconfig extends IotdmRxAppProtocolConfigration>
                            implements IotdmChannelProtocolDescriptor {

    private final String protocolChannelId;
    private final String schema;
    private final boolean isSecure;
    private final Tconfig config;

    public IotdmRxDescriptorApplicationProtocol(final String protocolChannelId,
                                                final String schema,
                                                final boolean isSecure,
                                                final Tconfig config) {
        this.protocolChannelId = protocolChannelId;
        this.schema = schema;
        this.isSecure = isSecure;
        this.config = config;
    }

    @Override
    public String getProtocolChannelId() {
        return this.protocolChannelId;
    }

    @Override
    public boolean equals(Object o) {
        if (! (o instanceof IotdmRxDescriptorApplicationProtocol)) {
            return false;
        }

        IotdmRxDescriptorApplicationProtocol desc = (IotdmRxDescriptorApplicationProtocol) o;

        if (! this.getProtocolChannelId().equals(desc.getProtocolChannelId())) {
            return false;
        }

        if (! this.getSchema().equals(desc.getSchema())) {
            return false;
        }

        if (this.isSecure() != desc.isSecure) {
            return false;
        }

        if (! this.getConfig().equals(desc.getConfig())) {
            return false;
        }

        return true;
    }

    public String getSchema() {
        return schema;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public Tconfig getConfig() {
        return config;
    }
}
