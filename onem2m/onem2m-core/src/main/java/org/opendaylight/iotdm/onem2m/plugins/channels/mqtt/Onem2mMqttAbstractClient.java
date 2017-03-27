/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins.channels.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Implements some basic functionality for MQTT clients including
 * connection to MQTT Broker and re-connecting in case of connection lost.
 * Class doesn't implement subscription to topics.
 */
public abstract class Onem2mMqttAbstractClient implements AutoCloseable {
    private final Logger LOG;
    protected final String mqttBroker;
    protected MqttAsyncClient client;

    protected static final int RECONNECTSECONDS = 5; // TODO this could be configurable
    protected Timer reconnectTimer = new Timer();
    protected final Lock reconnectingLock = new ReentrantLock();
    protected boolean reconnecting = false;

    protected Onem2mMqttAbstractClient(final String mqttBroker, Logger LOG) {
        this.mqttBroker = mqttBroker;
        this.LOG = LOG;
    }

    /**
     * Method schedules reconnection task in RECONNECTSECONDS if the
     * reconnection is not already in progress.
     */
    protected void reconnect() {
        reconnectingLock.lock();
        try {
            if (reconnecting || null == reconnectTimer) {
                return;
            }

            this.reconnectTimer.schedule(new ReconnectTaskDefault(),
                                         RECONNECTSECONDS * 1000);
            reconnecting = true;
        } finally {
            reconnectingLock.unlock();
        }
    }

    /**
     * Implementation of the reconnection procedure.
     */
    protected class ReconnectTaskDefault extends TimerTask {
        public void run() {
            reconnectingLock.lock();
            try {
                reconnecting = false;

                try {
                    if (!connectToMqttServer()) {
                        LOG.debug("Failed to re-connect to MQTT broker");
                        reconnect();
                        return;
                    }
                } catch (MqttException e) {
                    LOG.debug("Failed to re-connect to MQTT broker: {}", e);
                    reconnect();
                    return;
                }

                reconnectionSuccessful();
            } finally {
                reconnectingLock.unlock();
            }
        }
    }

    /**
     * Method called when the lost connection has been
     * re-established.
     */
    protected void reconnectionSuccessful() {
        LOG.info("Reconnection successful");
    }

    /**
     * Method in case of connection lost.
     * Provides template implementation of connection lost handling
     * calling connectionFailureCallback() and reconnect() methods.
     * @param cause
     */
    protected void connectionLost(Throwable cause) {
        LOG.error("Onem2mMqttClient: lost connection to broker");
        this.connectionFailureCallback();

        LOG.debug("Reconnecting to broker: {}", mqttBroker);
        reconnect();
    }

    /**
     * Method called when connection fails.
     */
    protected abstract void connectionFailureCallback();

    /**
     * Method called when a MQTT message is received on specific topic.
     * @param topic
     * @param message
     * @throws Exception
     */
    protected abstract void messageArrived(String topic, MqttMessage message) throws Exception;

    /**
     * Default set of callbacks for used implementation of MQTT client.
     * Callbacks just calls provided abstract implementations.
     */
    protected class MqttCallbackSetDefault implements MqttCallback {
        @Override
        public void connectionLost(Throwable cause) {
            Onem2mMqttAbstractClient.this.connectionLost(cause);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Onem2mMqttAbstractClient.this.messageArrived(topic, message);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            //Called when a outgoing publish is complete.
        }
    }

    /**
     * Method implements connection to MQTT broker.
     * @return
     * @throws MqttException
     */
    public boolean connectToMqttServer() throws MqttException {
        try {
            client = new MqttAsyncClient(mqttBroker,//Broker Address
                                         MqttClient.generateClientId(), //ClientId
                                         new MemoryPersistence()); //Persistence

            // register a callback for messages that we subscribe to ...
            client.setCallback(new MqttCallbackSetDefault());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setMaxInflight(65000);
            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
            options.setUserName("mqtt");
            options.setPassword("mqtt".toCharArray());
            options.setCleanSession(false);

            IMqttActionListener connectionListener = new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken arg0) {
                    LOG.info("Connection successfull with broker : " + mqttBroker);
                }

                @Override
                public void onFailure(IMqttToken arg0, Throwable arg1) {
                    connectionFailureCallback();

                    LOG.debug("Reconnecting to broker: {}", mqttBroker);
                    reconnect();
                }
            };

            IMqttToken conToken = client.connect(options, "Connect async client to server", connectionListener);
            conToken.waitForCompletion();
            //connecting client to server
            if (!client.isConnected()) {
                LOG.error("Onem2mMqttClient: trouble connecting to server");
                return false;
            }

            return true;
        } catch (MqttException e) {
            LOG.debug("Onem2mMqttClient: error occurred when connecting to server", e);
            return false;
        }
    }

    /**
     * Method closes connection to MQTT broker
     */
    protected void disconnectFromMqttBroker() {
        if (client.isConnected()) {
            try {
                client.disconnect();
            }
            catch (MqttException e) {
                LOG.error("Onem2mMqttClient: Disconnection from MQTT broker failed: {}", e);
            }
        }
    }

    /**
     * Method cancels possible reconnection process before
     * the connection to MQTT broker is closed.
     */
    @Override
    public void close() {
        this.reconnectingLock.lock();
        try {
            this.reconnectTimer.cancel();
            this.reconnectTimer = null;
            this.reconnecting = false;
        } finally {
            this.reconnectingLock.unlock();
        }

        this.disconnectFromMqttBroker();
    }
}
