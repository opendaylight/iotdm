/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.http.api;

import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors.IotdmRxDescriptorApplication;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;

public interface Onem2mHttpChannelProviderService {
    String PROTOCHANNELHTTPS = "OneM2M-HTTPS";
    String PROTOCHANNELPRODVIDERID = "OneM2M-HTTP(S)";
    String PROTOCHANNELHTTP = "OneM2M-HTTP";
    String SCHEMAHTTP = "http";
    String SCHEMAHTTPS = "https";

    Onem2mHttpDescriptorBuilder getHttpDescriptorBuilder();
    Onem2mHttpsDescriptorBuilder getHttpsDescriptorBuilder();
    Onem2mHttpsConfigBuilder getHttpsConfigBuilder();

    void registerPluginInManager(final Onem2mHttpPlugin plugin, IotdmRxDescriptorApplication descriptor)
        throws IotdmPluginRegistrationException;

    void deregisterPluginInManager(final Onem2mHttpPlugin plugin, IotdmRxDescriptorApplication descriptor);
}
