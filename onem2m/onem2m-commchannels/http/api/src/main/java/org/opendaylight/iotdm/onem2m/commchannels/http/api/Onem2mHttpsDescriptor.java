/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.http.api;

import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors.IotdmRxDescriptorApplication;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors.IotdmRxDescriptorApplicationEndpoint;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors.IotdmRxDescriptorApplicationInterface;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors.IotdmRxDescriptorApplicationProtocol;

public class Onem2mHttpsDescriptor extends
                                   IotdmRxDescriptorApplication<HttpsServerConfiguration> {

    protected Onem2mHttpsDescriptor(final IotdmRxDescriptorApplicationInterface intDesc,
                                    final IotdmRxDescriptorApplicationProtocol<HttpsServerConfiguration> protoDesc,
                                    final IotdmRxDescriptorApplicationEndpoint endpointDesc) {
        super(intDesc, protoDesc, endpointDesc);
    }
}
