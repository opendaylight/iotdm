/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins.channels.mqtt;

import com.google.common.collect.Lists;
import org.eclipse.paho.client.mqttv3.*;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPlugin;
import org.opendaylight.iotdm.onem2m.plugins.channels.common.IotdmPluginOnem2mBaseRequest;
import org.opendaylight.iotdm.onem2m.plugins.Onem2mPluginManager;
import org.opendaylight.iotdm.onem2m.plugins.channels.Onem2mBaseCommunicationChannel;
import org.opendaylight.iotdm.onem2m.plugins.channels.common.IotdmPluginOnem2mBaseResponse;
import org.opendaylight.iotdm.onem2m.plugins.registry.Onem2mLocalEndpointRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jkosmel
 */
public class Onem2mMqttPluginClient extends Onem2mBaseCommunicationChannel {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mMqttPluginClient.class);
    private Onem2mMqttAsyncClient onem2mMqttClient;
    private String mqttBrokerAddress = null;
    private List<LinkedBlockingQueue<Onem2mMqttAsyncClient.QEntry>> queueList = Lists.newArrayList();
    private static final Integer NUM_SUBSCRIBER_PROCESSORS = 128;

    Onem2mMqttPluginClient(String ipAddress, int port,
                           Onem2mLocalEndpointRegistry registry) {
        super(ipAddress, port, registry, null, false);
        mqttBrokerAddress = "tcp://" + ipAddress + ":" + port;
    }

    @Override
    public String getProtocol() {
        return Onem2mPluginManager.ProtocolMQTT;
    }

    @Override
    public boolean init() {
        onem2mMqttClient = new Onem2mMqttAsyncClient();
        try {
            if (! onem2mMqttClient.connectToMqttServer()) {
                LOG.error("Failed to initiate connection to MQTT broker");
                this.setState(ChannelState.INITFAILED);
            } else {
                this.setState(ChannelState.RUNNING);
                LOG.info("Connected to MQTT broker on: {}", mqttBrokerAddress);
            }
        } catch (Exception e) {
            LOG.error("Failed to connect to MQTT broker: {}", e);
            this.setState(ChannelState.INITFAILED);
        }
        //configure your MQTT https://wiki.opendaylight.org/view/Iotdm:MQTT-HowTo
        return true;
    }

    @Override
    public void close() throws Exception {
        try {
            onem2mMqttClient.close();
        } catch (Exception e) {
            LOG.error("Exception: {}", e);
        }
    }

    /**
     * Onem2m mqtt client - will subscribe to given mqtt server
     */
    private class Onem2mMqttAsyncClient extends Onem2mMqttAbstractClient {

        protected Onem2mMqttAsyncClient() {
            super(mqttBrokerAddress, LOG);
            initThreadsAndQueuesForResourceProcessing();
        }

        @Override
        protected void connectionFailureCallback() {
            LOG.debug("Connection failed in state: {}", getState());
            switch (getState()) {
                case INIT:
                    setState(ChannelState.INITFAILED);
                    break;

                default:
                    setState(ChannelState.FAILED);
                    break;
            }
        }

        @Override
        protected void reconnectionSuccessful() {
            super.reconnectionSuccessful();
            if (getUsesDefaultConfiguration()) {
                setState(ChannelState.RUNNINGDEFAULT);
            } else {
                setState(ChannelState.RUNNING);
            }
        }

        public void initThreadsAndQueuesForResourceProcessing() {
            ExecutorService executorService = Executors.newFixedThreadPool(NUM_SUBSCRIBER_PROCESSORS);
            AtomicInteger qNum = new AtomicInteger(-1);
            for (int i = 0; i < NUM_SUBSCRIBER_PROCESSORS; i++) {

                LinkedBlockingQueue<QEntry> q = new LinkedBlockingQueue<>();
                queueList.add(i, q);
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        runProcessSubscriberQ(qNum.incrementAndGet());
                    }
                });
            }
        }

        private void runProcessSubscriberQ(Integer qNum) {

            Thread.currentThread().setName("subsc-proc-" + qNum);
            QEntry qEntry;
            while (true) {
                try {
                    qEntry = queueList.get(qNum).take();
                } catch (Exception e) {
                    LOG.error("{}", e.toString());
                    qEntry = null;
                }
                if (qEntry != null) handleMqttMessage(qEntry.topic, qEntry.message);
            }
        }

        private void enqueueNotifierOperation(String topic, String message) {
            Integer qNum = Math.abs(message.hashCode()) % NUM_SUBSCRIBER_PROCESSORS;
            try {
                queueList.get(qNum).put(new QEntry(topic, message));
            } catch (InterruptedException e) {
                LOG.error("Couldn't enqueue mqtt message: topic:{}, message: {}", topic, message);
            }
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            //check if Qos is not 0
            if ((!message.isRetained()) && (message.getQos() == 1)) {
                enqueueNotifierOperation(topic, message.toString());
            }
            if (message.getQos() != 1) {
                publishMqttResponse(topic, "QoS must be 1");
            }
            if (message.isRetained()) {
                publishMqttResponse(topic, "Message retained should be false");
            }
        }

        @Override
        public boolean connectToMqttServer() throws MqttException {
            if (! super.connectToMqttServer()) {
                return false;
            }

            return registerMqttSubscriber();
        }

        //subscribe for all onem2m requests
        private boolean registerMqttSubscriber() {
            Boolean status = true;
            String topic = "/" + Onem2m.Mqtt.OM2M_TOPIC_LITERAL + "/" + Onem2m.Mqtt.MessageType.REQUEST + "/#";
            try {
                client.subscribe(topic, 1);
            } catch (MqttException e) {
                LOG.error("registerMqttSubscriber: cannot subscribe {}", topic);
                status = false;
            }
            return status;
        }

        // Handler for a request and a response
        private void handleMqttMessage(String topic, String message) {
            String mqttMessageType = null;
            String hierarchyTopic[] = parseTopicString(topic);
            if (hierarchyTopic[1].contains("req")) {
                mqttMessageType = Onem2m.Mqtt.MessageType.REQUEST;
            } else if (hierarchyTopic[1].contains("resp")) {
                mqttMessageType = Onem2m.Mqtt.MessageType.RESPONSE;
            }
            String mqttMessageFormat = null;
            if (hierarchyTopic[4].contains("json")) {
                mqttMessageFormat = Onem2m.ContentFormat.JSON;
            } else if (hierarchyTopic[4].contains("xml")) {
                mqttMessageFormat = Onem2m.ContentFormat.XML;
            }

            switch (mqttMessageType) {
                case Onem2m.Mqtt.MessageType.REQUEST:
                    IotdmPluginOnem2mBaseRequest request = new IotdmPluginOnem2mBaseRequest(message, mqttMessageFormat);
                    IotdmPluginOnem2mBaseResponse response = new IotdmPluginOnem2mBaseResponse();
                    IotdmPlugin plg = pluginRegistry.getPlugin(request.getOnem2mUri());
                    if (plg != null) {
                        plg.handle(request, response);
                        publishMqttResponse(topic, response.buildWebsocketResponse());
                    }
                    else {
                        String msg = "Mqtt plugin not found";
                        LOG.warn(msg);
                        publishMqttResponse(topic, IotdmPluginOnem2mBaseResponse.buildErrorResponse(msg, Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR));
                    }
                    break;
                case Onem2m.Mqtt.MessageType.RESPONSE:
                    break;
            }
        }

        private void publishMqttResponse(String requestTopic, String message) {

            String topicParts[] = trimTopic(requestTopic).split("/");
            String format_type = topicParts[4];
            String cse_name = topicParts[3].replace("/", ":");
            String resource_name = topicParts[2].replace("/", ":");
            String responseTopic = "/" + Onem2m.Mqtt.OM2M_TOPIC_LITERAL + "/" + Onem2m.Mqtt.MessageType.RESPONSE + "/" + resource_name + "/" + cse_name + "/" + format_type;

            IMqttActionListener defaultActionListener = new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken arg0) {}
                @Override
                public void onFailure(IMqttToken arg0, Throwable arg1) {}
            };

            try {
                client.publish(responseTopic, message.getBytes(), Onem2m.Mqtt.Options.QOS1,
                        Onem2m.Mqtt.Options.RETAINED, "Pub Sample Context", defaultActionListener);
            } catch (MqttException e) {
                LOG.error("Error occured when sending mqtt response", e);
            }
        }

        /**
         * The topic string is of the format /onem2m/(message-type)/originator/receiver ... verify and pull out the
         * relevant components in the topic hierarchy string.
         */
        private String[] parseTopicString(String topic) {

            topic = trimTopic(topic);
            // split the topic into its hierarchy of path component strings
            String topicParts[] = topic.split("/");

            if (topicParts.length != 5) {
                LOG.error("Length of topics is less than expected");
            }
            else if (!(topicParts[4].equalsIgnoreCase("json") || (topicParts[4].equalsIgnoreCase("xml")))) {
                LOG.error("Topic must include type as either json or xml only");
            }

            if (!topicParts[0].equalsIgnoreCase(Onem2m.Mqtt.OM2M_TOPIC_LITERAL)) {
                LOG.error("Topic must contain " + Onem2m.Mqtt.OM2M_TOPIC_LITERAL);
                throw new IllegalArgumentException("Incorrect topic format");
            }
            if (!topicParts[1].equalsIgnoreCase("req")) {
                LOG.error("Topic must include req or resp");
                throw new IllegalArgumentException("Incorrect topic format");
            }
            return topicParts;
        }

        private String trimTopic(String topic) {
            topic = topic.trim();
            topic = topic.startsWith("/") ? topic.substring("/".length()) : topic;
            topic = topic.endsWith("/") ? topic.substring(0, topic.length() - 1) : topic;
            return topic;
        }

        private class QEntry {
            protected String topic;
            protected String message;
            QEntry(String topic, String message) {
                this.topic = topic;
                this.message = message;
            }
        }
    }
}
