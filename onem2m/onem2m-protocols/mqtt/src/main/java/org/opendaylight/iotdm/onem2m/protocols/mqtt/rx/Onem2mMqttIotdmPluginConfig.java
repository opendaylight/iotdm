/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.mqtt.rx;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.mqtt.rev141210.ServerConfig;

import static org.opendaylight.iotdm.onem2m.protocols.common.utils.IotdmProtocolConfigGetter.getAttribute;

/**
 * Class extends ServerConfig class generated from yang model. New constructor is added which allows
 * to instantiate ServerConfig object from any other object implementing the same getter methods.
 * This is useful if other module imports the same yang model and reuses the ServerConfig items so
 * the same verification methods can be used.
 */
public class Onem2mMqttIotdmPluginConfig extends ServerConfig {

    public Onem2mMqttIotdmPluginConfig(Object config) {
        super();
        this.setMqttBrokerIp(getAttribute(config, "getMqttBrokerIp", String.class));
        this.setMqttBrokerPort(getAttribute(config, "getMqttBrokerPort", Integer.class));
        //this.setCseList(getAttribute(config, "getCseList", List.class));
        this.setServerSecurityLevel(getAttribute(config, "getServerSecurityLevel", SecurityLevel.class));
    }

}
