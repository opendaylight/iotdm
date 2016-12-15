/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.mqtt.tx.notification;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.plugins.channels.mqtt.Onem2mMqttAbstractClient;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolTxChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of TX channel for MQTT notifications.
 */
public class Onem2mMqttTxClient implements Onem2mProtocolTxChannel<Onem2mMqttTxClientConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mMqttTxClient.class);
    protected Onem2mMqttTxAsyncClient onem2mMqttClient;
    protected String mqttBrokerAddress = null;

    @Override
    public void start(Onem2mMqttTxClientConfiguration configuration) {
        mqttBrokerAddress = "tcp://" + configuration.getIpAddress() + ":" + configuration.getPort();
        onem2mMqttClient = new Onem2mMqttTxAsyncClient();
        try {
            if (! onem2mMqttClient.connectToMqttServer()) {
                LOG.error("Failed to initiate connection to MQTT broker");
            } else {
                LOG.info("Connected to MQTT broker on: {}", mqttBrokerAddress);
            }
        } catch (Exception e) {
            LOG.error("Failed to connect to MQTT broker: {}", e);
        }
    }

    @Override
    public void close() {
        try {
            onem2mMqttClient.close();
        } catch (Exception e) {
            LOG.error("Exception: {}", e);
        }
    }

    /**
     * Uses the MQTT client to publish notification
     * @param topic Destination topic
     * @param payload Notification payload
     * @return True if the notification has been successfully published
     *         False otherwise
     */
    public boolean publishMqttNotifyRequest(String topic, String payload) {
        return this.onem2mMqttClient.publishMqttNotifyRequest(topic, payload);
    }

    /**
     * Onem2m mqtt client - doesn't subscribe to MQTT broker, just connects
     * and sends notifications
     */
    private class Onem2mMqttTxAsyncClient extends Onem2mMqttAbstractClient {

        protected Onem2mMqttTxAsyncClient() {
            super(mqttBrokerAddress, LOG);
        }

        @Override
        protected void connectionFailureCallback() {
            LOG.debug("Connection failed");
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            // nothing to do
            return;
        }

        public boolean publishMqttNotifyRequest(String topic, String payload) {
            IMqttActionListener defaultActionListener = new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken arg0) {}
                @Override
                public void onFailure(IMqttToken arg0, Throwable arg1) {}
            };

            try {
                client.publish(topic, payload.getBytes(), Onem2m.Mqtt.Options.QOS1,
                               Onem2m.Mqtt.Options.RETAINED, "Pub Sample Context", defaultActionListener);
            } catch (MqttException e) {
                LOG.error("Error occured when sending mqtt response", e);
                return false;
            }

            return true;
        }
    }
}