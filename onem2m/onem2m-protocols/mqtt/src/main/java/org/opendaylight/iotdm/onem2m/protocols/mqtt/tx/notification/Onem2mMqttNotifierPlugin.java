/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.mqtt.tx.notification;


import org.opendaylight.iotdm.onem2m.notifier.Onem2mNotifierPlugin;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolTxHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implemetation of notifier plugin for MQTT
 */
public class Onem2mMqttNotifierPlugin implements Onem2mNotifierPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mMqttNotifierPlugin.class);

    protected final Onem2mProtocolTxHandler onem2mHandler;
    protected final Onem2mMqttNotifierRequestAbstractFactory requestFactory;

    public Onem2mMqttNotifierPlugin(Onem2mProtocolTxHandler onem2mHandler,
                                    Onem2mMqttNotifierRequestAbstractFactory requestFactory) {
        this.onem2mHandler = onem2mHandler;
        this.requestFactory = requestFactory;
    }

    @Override
    public String getNotifierPluginName() { return "mqtt"; }

    @Override
    public void sendNotification(String url, String payload, String cseBaseId) {
        try {
            this.onem2mHandler.handle(requestFactory.createMqttNotifierRequest(url, payload, cseBaseId));
        } catch (IllegalArgumentException e) {
            LOG.error("Failed to create notification: {}", e);
        }
    }
}
