/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.mqtt.tx.notification;

/**
 * Configuration of the MQTT client.
 * Stores these parameters:
 *  ipAddress: IP address of MQTT broker.
 *  port: Port number of MQTT broker.
 */
public class Onem2mMqttTxClientConfiguration {
    private final String ipAddress;
    private final int port;

    public Onem2mMqttTxClientConfiguration (String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }
}
