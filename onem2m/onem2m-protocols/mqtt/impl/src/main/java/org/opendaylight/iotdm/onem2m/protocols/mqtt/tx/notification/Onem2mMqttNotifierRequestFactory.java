/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.mqtt.tx.notification;

/**
 * Default implementation of the abstract factory for
 * notification requests.
 */
public class Onem2mMqttNotifierRequestFactory implements Onem2mMqttNotifierRequestAbstractFactory {

    protected final Onem2mMqttTxClient client;
    protected final int defaultPort;

    /**
     * Sets default MQTT client and default MQTT broker destination port number.
     * @param client Default MQTT client which is used if the destination URL
     *               does not specify custom destination address or port.
     * @param defaultPort Default port number to be used if URL specifies custom
     *                    address but does not specify port.
     */
    public Onem2mMqttNotifierRequestFactory(Onem2mMqttTxClient client, int defaultPort) {
        this.client = client;
        this.defaultPort = defaultPort;
    }

    @Override
    public Onem2mMqttNotifierRequest createMqttNotifierRequest(String url, String payload, String cseBaseId) {
         return new Onem2mMqttNotifierRequest(url, payload, cseBaseId, client, defaultPort);
    }
}
