/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.mqtt.tx.notification;

/**
 * Definition of abstract factory which instantiates
 * MQTT notification requests.
 */
public interface Onem2mMqttNotifierRequestAbstractFactory {

    /**
     * Instantiates MQTT notification requests.
     * @param url Target URL. Supported formats are:
     *            mqtt:{topic}
     *            mqtt://{broker_address}{topic}
     *            mqtt://{broker_address}:{broker_port}{topic}
     * @param payload Notification payload
     * @param cseBaseId CSE-ID of the CSEBase originating the notification.
     * @return New instance of the notification request.
     */
    Onem2mMqttNotifierRequest createMqttNotifierRequest(String url, String payload, String cseBaseId);
}
