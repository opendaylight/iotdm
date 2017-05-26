/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins;

import org.opendaylight.iotdm.plugininfra.pluginmanager.api.descriptors.IotdmChannelDescriptor;

public interface IotdmPluginRegistrationService {
    void registerRxPlugin(IotdmChannelDescriptor descriptor, IotdmPlugin plugin)
        throws IotdmPluginRegistrationException;

    void deregisterRxPlugin(IotdmChannelDescriptor descriptor, IotdmPlugin plugin);
    void deregisterRxPlugin(IotdmPlugin plugin);
}
