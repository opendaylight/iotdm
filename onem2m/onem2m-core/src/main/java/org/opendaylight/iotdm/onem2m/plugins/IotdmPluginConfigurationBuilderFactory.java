/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins;

import org.opendaylight.iotdm.onem2m.plugins.channels.coap.IotdmCoapsConfigBuilder;
import org.opendaylight.iotdm.onem2m.plugins.channels.http.IotdmHttpsConfigBuilder;

/**
 * Class implementing static methods which creates instances of PluginConfiguration builders
 * for specific protocol.
 * All CommunicationChannels using configuration should add method in this class.
 */
public final class IotdmPluginConfigurationBuilderFactory {

    /**
     * Creates HTTPS configuration builder instance.
     * @return HTTPS configuration builder.
     */
    public static IotdmHttpsConfigBuilder getNewHttpsConfigBuilder() {
        return new IotdmHttpsConfigBuilder();
    }

    /**
     * Creates CoAPS configuration builder instance.
     * @return CoAPS configuration builder.
     */
    public static IotdmCoapsConfigBuilder getNewCoapsConfigBuilder() {
        return new IotdmCoapsConfigBuilder();
    }
}
