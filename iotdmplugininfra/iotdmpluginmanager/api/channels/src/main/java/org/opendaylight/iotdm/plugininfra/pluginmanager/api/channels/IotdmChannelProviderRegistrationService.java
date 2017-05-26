/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels;

import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxChannelProvider;

public interface IotdmChannelProviderRegistrationService {

    void registerRxProvider(final IotdmRxChannelProvider channelProvider)
        throws IotdmChannelProviderRegistrationException;

    void unregisterRxProvider(final IotdmRxChannelProvider channelProvider)
        throws IotdmChannelProviderRegistrationException;

//    void registerTxProvider(IotdmTxChannelProvider txProvider,
//                            IotdmTxChannelInterfacesRegister txInterfacesRegister)
//        throws IotdmChannelProviderRegistrationException;
}
