/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.mqtt.tx.notification;

import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolTxRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Implements processing of payload to be sent as MQTT notification
 */
public class Onem2mMqttNotifierRequest extends Onem2mProtocolTxRequest {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mMqttNotifierRequest.class);

    protected final String topic;
    protected String payload;
    protected final String cseBaseId;
    protected final Onem2mMqttTxClient client;
    protected final Onem2mMqttTxClientConfiguration customClientCfg;
    protected final String toUrl;

    /**
     * @param url Destination URL. Supported formats are:
     *            mqtt:{topic}
     *            mqtt://{broker_address}{topic}
     *            mqtt://{broker_address}:{broker_port}{topic}
     * @param payload Payload of the notification
     * @param cseBaseId CSEBase as originator of the notification
     * @param client Default MQTT client which will be used to send the notification
     *               if the passed URL does not specify custom address or port of MQTT broker
     * @param defaultPort Default MQTT broker port is used if port number
     *                    is not specified in URL.
     */
    public Onem2mMqttNotifierRequest(@Nonnull final String url,
                                     @Nonnull final String payload,
                                     final String cseBaseId,
                                     Onem2mMqttTxClient client,
                                     int defaultPort) {

        // check the begin of URL
        if (! url.startsWith("mqtt:/")) {
            LOG.error("Invalid URL passed: {}", url);
            throw new IllegalArgumentException("Invalid URL: " + url);
        }

        this.toUrl = url;
        String uri = url.substring("mqtt:".length());

        if (uri.startsWith("//")) {
            // URI has specified MQTT broker IP address and can have specified port
            String urn = uri.replaceFirst("//", ""); // remove the leading //
            String broker = urn.substring(0, urn.indexOf("/")); // divide broker ip (and port) from topic

            // now we need to resolve the MQTT broker address and port
            String brokerAddress = null;
            int brokerPort = defaultPort; // use the default port value by default
            try {
                if (broker.contains(":")) {
                    // Port number is specified in the URI
                    // So split address from port and store them
                    String[] splitBroker = broker.split(":");
                    if (splitBroker.length != 2) {
                        LOG.error("Invalid broker address: {}, URL: {}", broker, url);
                        throw new IllegalArgumentException("Invalid broker address in URL: " + broker);
                    }

                    brokerAddress = splitBroker[0];
                    brokerPort = Integer.valueOf(splitBroker[1]);
                } else {
                    // There is not port number so only address is specified
                    brokerAddress = broker;
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid port number passed in URL: " + url);
            }

            // We can't use default client, we need create new one using custom configuration
            // including address and port from URL
            this.customClientCfg = new Onem2mMqttTxClientConfiguration(brokerAddress, brokerPort);
            this.topic = urn.substring(urn.indexOf("/"));
        } else {
            // URL contains only topic, default client will be used
            this.topic = uri;
            this.customClientCfg = null;
        }

        if (null != this.customClientCfg) {
            // We have custom configuration, create custom client
            this.client = new Onem2mMqttTxClient(this.customClientCfg);
        } else {
            // Use the default client
            this.client = client;
        }

        this.payload = payload;
        this.cseBaseId = cseBaseId;
    }

    @Override
    protected boolean preprocessRequest() {
        // nothing to do here
        return true;
    }

    /**
     * Method extends JSON payload with Onem2m attributes according to
     * Onem2m specification for MQTT binding.
     * @return True if successful False otherwise.
     */
    @Override
    protected boolean translateRequestFromOnem2m() {
        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(payload);
        } catch (JSONException e) {
            LOG.error("Invalid payload for MQTT notification request: {}", e);
            return false;
        }

        if (null == jsonObj) {
            LOG.error("Empty payload for MQTT notification passed.");
            return false;
        }

        // Set operation
        if (! jsonObj.has(RequestPrimitive.OPERATION)) {
            jsonObj.put(RequestPrimitive.OPERATION, Integer.valueOf(Onem2m.Operation.NOTIFY));
        }

        // Set to
        if (! jsonObj.has(RequestPrimitive.TO)) {
            jsonObj.put(RequestPrimitive.TO, this.toUrl);
        }

        // Set From
        if (! jsonObj.has(RequestPrimitive.FROM)) {
            jsonObj.put(RequestPrimitive.FROM, this.cseBaseId);
        }

        // TODO Set rquestId ??? - we're not waiting for responses

        this.payload = jsonObj.toString();
        return true;
    }

    /**
     * Method uses MQTT client to send the notification.
     * @return True if successful False otherwise.
     */
    @Override
    protected boolean sendRequest() {
        if (null != this.customClientCfg) {
            this.client.start();
        }

        try {
            return this.client.publishMqttNotifyRequest(this.topic, this.payload);
        } finally {
            if (null != this.customClientCfg) {
                this.client.close();
            }
        }
    }

    @Override
    protected boolean translateResponseToOnem2m() {
        // nothing to do here
        return true;
    }

    @Override
    protected void respondToOnem2mCore() {
        return;
    }
}
