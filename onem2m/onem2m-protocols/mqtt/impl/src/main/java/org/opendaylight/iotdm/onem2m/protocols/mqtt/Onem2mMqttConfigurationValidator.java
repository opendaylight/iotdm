/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.mqtt;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.opendaylight.iotdm.onem2m.protocols.common.utils.Onem2mProtocolConfigException;
import org.opendaylight.iotdm.onem2m.protocols.common.utils.Onem2mProtocolConfigValidator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.mqtt.rev170118.mqtt.protocol.provider.config.MqttClientConfig;

/**
 * Implementation of validation logic for configuration of OneM2M MQTT protocol module
 */
public class Onem2mMqttConfigurationValidator implements Onem2mProtocolConfigValidator {
    protected final MqttClientConfig clientConfig;

    public Onem2mMqttConfigurationValidator(MqttClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    private boolean validateBrokerAddress(String address) {
        if (address == null) return false;
        //address in format tcp://hostOrIp
        //String addressParts[] = address.split("://");
        try {
            InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            return false;
        }
        return true; //addressParts[0].equalsIgnoreCase("TCP");
    }

    @Override
    public void validate() throws Onem2mProtocolConfigException {
        if (null == this.clientConfig) {
            // null is valid case
            return;
        }

        // validate port
        checkPortNumber(this.clientConfig.getMqttBrokerPort());

        // validate ip address
        checkCondition(validateBrokerAddress(this.clientConfig.getMqttBrokerIp()),
                       "Invalid MQTT broker address: " + this.clientConfig.getMqttBrokerIp());

        // validate security level
        checkCondition(clientConfig.getSecurityLevel() != SecurityLevel.L2,
                       "Security level L2 is not supported by this module");
    }
}
