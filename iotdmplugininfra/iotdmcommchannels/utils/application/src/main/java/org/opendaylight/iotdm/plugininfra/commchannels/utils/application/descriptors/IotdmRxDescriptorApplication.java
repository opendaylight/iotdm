/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors;

import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.IotdmRxAppProtocolConfigration;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.descriptors.IotdmChannelDescriptor;

public abstract class IotdmRxDescriptorApplication<Tconfig extends IotdmRxAppProtocolConfigration>
                extends IotdmChannelDescriptor<IotdmRxDescriptorApplicationInterface,
                                               IotdmRxDescriptorApplicationProtocol<Tconfig>,
                                               IotdmRxDescriptorApplicationEndpoint> {

    protected IotdmRxDescriptorApplication(final IotdmRxDescriptorApplicationInterface intDesc,
                                           final IotdmRxDescriptorApplicationProtocol<Tconfig> protoDesc,
                                           final IotdmRxDescriptorApplicationEndpoint endpointDesc) {
        super(intDesc, protoDesc, endpointDesc);
    }

//    @Override
//    public IotdmRxDescriptorApplicationInterface getInterfaceDescriptor() {
//        return this.intDesc;
//    }
//
//    public Tprotod getProtocolDescriptor() {
//        return this.protoDesc;
//    }
//
//    public Tendpointd getEndpointDescriptor() {
//        return this.endpointDesc;
//    }
}
