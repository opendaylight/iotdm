/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.commchannels.utils.application;

public final class IotdmRxAppProtocolConfigurationNone implements IotdmRxAppProtocolConfigration {

    private static final IotdmRxAppProtocolConfigurationNone _instance = new IotdmRxAppProtocolConfigurationNone();

    protected IotdmRxAppProtocolConfigurationNone() {}

    public static IotdmRxAppProtocolConfigurationNone getInstance() { return _instance; }

    @Override
    public boolean equals(Object o) {
        if (! (o instanceof IotdmRxAppProtocolConfigurationNone)) {
            return false;
        }

        // Nothing to compare
        return true;
    }
}
