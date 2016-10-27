/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.mqtt;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxHandler;
import org.opendaylight.iotdm.onem2m.protocols.mqtt.rx.Onem2mMqttIotdmPlugin;
import org.opendaylight.iotdm.onem2m.protocols.mqtt.rx.Onem2mMqttIotdmPluginConfig;
import org.opendaylight.iotdm.onem2m.protocols.mqtt.rx.Onem2mMqttRxRequestFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mMqttProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mMqttProvider.class);
    private Onem2mMqttIotdmPlugin onem2MMqttIotdmPlugin = null;
    private final Onem2mMqttIotdmPluginConfig serverConfig;

    public Onem2mMqttProvider(Onem2mMqttIotdmPluginConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        Onem2mService onem2mService = session.getRpcService(Onem2mService.class);
        try {
            onem2MMqttIotdmPlugin = new Onem2mMqttIotdmPlugin(new Onem2mProtocolRxHandler(),
                    new Onem2mMqttRxRequestFactory(),
                    onem2mService);
            onem2MMqttIotdmPlugin.start(this.serverConfig);
        } catch (Exception e) {
            LOG.error("Failed to start mqtt server: {}", e);
        }
        LOG.info("Onem2mMqttProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        try {
            onem2MMqttIotdmPlugin.close();
        } catch (Exception e) {
            LOG.error("Failed to close mqtt plugin: {}", e);
        }
    }

    /**
     * Provision the broker address and a list of cse's to use as mqtt subscribers.
     * First we need to provision the cse, then configure the mqtt parameters (broker address and CSE names).
     * We support MANY cse's in oneM2M, not all maye be managed by a MQTT broker so an individual message is
     * required to indicate which cse's required mqtt.
     * @param input Onem2mMqttConfigInput
     * @return How to provision external mqtt broker : reference
     * https://wiki.opendaylight.org/view/Iotdm:MQTT-HowTo
     */
//    @Override
//    public Future<RpcResult<Onem2mMqttConfigOutput>> onem2mMqttConfig(Onem2mMqttConfigInput input) {
//        Onem2mMqttConfigOutput output;
//        List<CseList> tempCseList = input.getCseList();
//        Boolean validated = validateBrokerAddress(input.getMqttBroker());
//        /*
//         * If broker is not connected, then store these values and call connectToMqttServer.
//         */
//        if (!connectedToBroker && validated) {
//            String mqttBroker = input.getMqttBroker();
//            mqttAddress = mqttBroker;
//            addCSEListandConnect(tempCseList, mqttBroker);
//        } else {
//            /*
//             * If broker is not changed, then ensure we only subscribe to the list of cse's in this new list.
//             */
//            if (connectedToBroker && mqttAddress.equals(input.getMqttBroker()) && validated) {
//                cseList.clear();
//                for (CseList c : tempCseList) {
//                    String cseId = c.getCseId();
//                    cseList.add(cseId);
//                }
//                for (String cseId : cseList) {
//                    onem2mMqttClient.registerCseAsMqttSubscriber(cseId);
//                }
//            }
//            /*
//              If the broker is changed, disconnect from old broker, then store these values and call connectToMqttServer.
//             */
//            else if (connectedToBroker && !mqttAddress.equals(input.getMqttBroker()) && validated) {
//                onem2mMqttClient.disconnectFromMqttServer();
//                mqttAddress = input.getMqttBroker();
//                addCSEListandConnect(tempCseList, mqttAddress);
//            }
//        }
//        String status = statusBuilder.getStatus();
//        output = new Onem2mMqttConfigOutputBuilder().setStatus(status).build();
//        return RpcResultBuilder.success(output).buildFuture();
//    }
//
//    private boolean validateBrokerAddress(String mqttBroker) {
//        // TODO Auto-generated method stub
//
//        if (mqttBroker == null) {
//            LOG.error("Mqtt Broker Adress string : Incorrect format :: Address is empty or null ");
//            statusBuilder.setStatus("Mqtt Broker Adress string : Incorrect format :: Address is empty or null ");
//
//            return false;
//        }
//        String mqttBrokerData[] = mqttBroker.split(":");
//        String mqttBrokerIP[] = mqttBrokerData[1].split("//");
//        try {
//            InetAddress.getByName(mqttBrokerIP[1]);
//        } catch (UnknownHostException e) {
//
//            statusBuilder.setStatus("Mqtt Broker Adress string : Incorrect format :: Inorrect IP Address ");
//            LOG.error("Mqtt Broker Adress string : Incorrect format :: Inorrect IP Address  ");
//            return false;
//        }
//        if (!mqttBrokerData[0].equalsIgnoreCase("TCP")) {
//            statusBuilder.setStatus("Mqtt Broker Adress string : Incorrect format :: Wrong Protocol in Address ");
//            LOG.error("Mqtt Broker Adress string : Incorrect format :: Wrong Protocol in Address");
//            return false;
//        }
//        if (!mqttBrokerData[2].equalsIgnoreCase("1883")) {
//            statusBuilder.setStatus("Mqtt Broker Adress string : Incorrect format :: Wrong Port in Address");
//            LOG.error("Mqtt Broker Adress string : Incorrect format :: Wrong Port in Address");
//            return false;
//        }
//        return true;
//    }
//
//    private void addCSEListandConnect(List<CseList> tempCseList, String brokerAddress) {
//        // TODO Auto-generated method stub
//
//        cseList.clear();
//        for (CseList c : tempCseList) {
//            String cseId = c.getCseId();
//            cseList.add(cseId);
//        }
//        try {
//            onem2mMqttClient.connectToMqttServer(brokerAddress);
//        } catch (MqttException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }

//    public class Onem2mMqttAsyncClient {
//        MqttAsyncClient client;
//        Onem2mMqttAsyncClient() {
//        }
//
//        public void disconnectFromMqttServer() {
//            try {
//                client.disconnect();
//            } catch (MqttException e) {
//                LOG.error("DisconnectFromMqttServer: trouble disconnecing {}", e.toString());
//            } finally {
//                connectedToBroker = false;
//            }
//        }
//
//        public boolean registerCseAsMqttSubscriber(String cseId) {
//
//            Boolean status = true;
//            String topic = "/oneM2M/" + Onem2m.MqttMessageType.REQUEST + "/+/" + cseId + "/+";
//            try {
//                client.subscribe(topic, 1);
//            } catch (MqttException e) {
//                LOG.error("RegisterCseAsMqttSubscriber: cannot register {}", cseId);
//                status = false;
//            }
//            return status;
//        }
//
//        public void connectToMqttServer(final String mqttBroker) throws MqttException {
//
//            if (mqttBroker == null) {
//                LOG.info("Broker not configured, returning without connecting ...");
//                statusBuilder.setStatus("Broker address is missing.");
//                return;
//            }
//
//            try {
//                client = new MqttAsyncClient(
//                        mqttBroker,//Broker Address
//                        MqttClient.generateClientId(), //ClientId
//                        new MemoryPersistence()); //Persistence
//                MqttConnectOptions options = new MqttConnectOptions();
//                options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
//                options.setUserName("mqtt");
//                options.setPassword("mqtt".toCharArray());
//                options.setCleanSession(false);
//                // register a callback for messages that we subscribe to ...
//                client.setCallback(new MqttCallback() {
//
//                    @Override
//                    public void connectionLost(Throwable cause) {
//                        LOG.error("Onem2mMqttClient: lost connection to server");
//                        try {
//                            Onem2mMqttAsyncClient.this.connectToMqttServer(mqttBroker);
//                        } catch (MqttException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    @Override
//                    public void messageArrived(String topic, MqttMessage message) throws Exception {
//                        //check if Qos is not 0
//                        if ((message.isRetained() == false) && (message.getQos() == 1)) {
//                            handleMqttMessage(topic, message.toString());
//                        }
//                        if (message.getQos() != 1) {
//                            sendResponse(topic, "QoS must be 1");
//                        }
//                        if (message.isRetained() == true) {
//                            sendResponse(topic, "Message retained should be false");
//                        }
//                    }
//
//                    @Override
//                    public void deliveryComplete(IMqttDeliveryToken token) {//Called when a outgoing publish is complete.
//                    }
//                });
//                IMqttActionListener conListener = new IMqttActionListener() {
//                    @Override
//                    public void onSuccess(IMqttToken arg0) {
//                        // TODO Auto-generated method stub
//                        LOG.info("Connection successfull with broker : " + mqttBroker);
//                        statusBuilder.setStatus("Mqtt broker provisioned.");
//                        connectedToBroker = true;
//
//                    }
//
//                    @Override
//                    public void onFailure(IMqttToken arg0, Throwable arg1) {
//                        // TODO Auto-generated method stub
//                        LOG.error("Connection failed ");
//                    }
//                };
//                IMqttToken conToken = client.connect(options, "Connect async client to server", conListener);
//                conToken.waitForCompletion();
//                //connecting client to server
//                if (!client.isConnected()) {
//                    statusBuilder.setStatus("Onem2mMqttClient: trouble connecting to server");
//                    LOG.error("Onem2mMqttClient: trouble connecting to server");
//                }
//
//                for (String cseId : cseList) {
//                    registerCseAsMqttSubscriber(cseId);
//                }
//
//                connectedToBroker = true;
//
//            } catch (MqttException e) {
//                statusBuilder.setStatus(e.toString());
//            }
//
//        }
//
//        // Handler for a request and a response
//        void handleMqttMessage(String topic, String message) {
//
//            String mqttMessageType = null;
//            String mqttMessageFormat = null;
//            String to = null;
//            String from = null;
//            String hierarchyTopic[] = parseTopicString(topic);
//            mqttMessageFormat = hierarchyTopic[4];
//            if (hierarchyTopic[1].contains("req")) {
//                mqttMessageType = Onem2m.MqttMessageType.REQUEST;
//            } else if (hierarchyTopic[1].contains("resp")) {
//                mqttMessageType = Onem2m.MqttMessageType.RESPONSE;
//            }
//            if (hierarchyTopic[4].contains("json")) {
//                mqttMessageFormat = Onem2m.ContentFormat.JSON;
//            } else if (hierarchyTopic[4].contains("xml")) {
//                mqttMessageFormat = Onem2m.ContentFormat.XML;
//            }
//
//            to = hierarchyTopic[2].replace(":", "/");
//            from = hierarchyTopic[3].replace(":", "/");
//
//            switch (mqttMessageType) {
//                case Onem2m.MqttMessageType.REQUEST:
//                    handleRequest(topic, message, to, from);
//                    break;
//                case Onem2m.MqttMessageType.RESPONSE:
//                    break;
//            }
//        }
//
//        /**
//         * The topic string is of the format /onem2m/(message-type)/originator/receiver ... verify and pull out the
//         * relevant components in the topic hierarchy string.
//         */
//        private String[] parseTopicString(String topic) {
//
//            topic = trimTopic(topic);
//            // split the topic into its hierarchy of path component strings
//            String hierarchy[] = topic.split("/");
//
//            if (hierarchy.length != 5) {
//                LOG.error("Length of topics is less than expected");
//            }
//
//            if (!hierarchy[0].toLowerCase().contentEquals("onem2m")) {
//                LOG.error("Topic must contain onem2m");
//            }
//            if (!hierarchy[1].contains("req")) {
//                LOG.error("Topic must contain req or resp");
//            }
//            if (!(hierarchy[4].equalsIgnoreCase("json") || (hierarchy[4].equalsIgnoreCase("xml")))) {
//                LOG.error("Topic must include type as either json or xml only");
//            }
//            return hierarchy;
//
//        }
//
//        private String trimTopic(String topic) {
//            topic = topic.trim();
//            topic = topic.startsWith("/") ? topic.substring("/".length()) : topic;
//            topic = topic.endsWith("/") ? topic.substring(0, topic.length() - 1) : topic;
//            return topic;
//        }
//
//        private void handleRequest(String topic, String message, String from, String to) {
//            Onem2mRequestPrimitiveClientBuilder clientBuilder = new Onem2mRequestPrimitiveClientBuilder();
//            clientBuilder.setProtocol(Onem2m.Protocol.MQTT);
//            Onem2mStats.getInstance().inc(Onem2mStats.MQTT_REQUESTS);
//            String hierarchy[]=parseTopicString(topic);
//            String mqttMessageFormat=hierarchy[4];
//            String operation=null;
//            if(mqttMessageFormat.contains(Onem2m.ContentFormat.JSON))
//            {
//                operation = Onem2mProtocolUtils.processRequestPrimitiveFromJson(message, clientBuilder);
//            }
//            if (mqttMessageFormat.contains(Onem2m.ContentFormat.XML)) {
//                operation = processXMLRequestPrimitive(message, clientBuilder);
//                sendResponse(topic, "XML format is not supported yet");
//            }
//
//            switch (operation) {
//                case Onem2m.Operation.RETRIEVE:
//                    Onem2mStats.getInstance().inc(Onem2mStats.MQTT_REQUESTS_RETRIEVE);
//                    break;
//
//                case Onem2m.Operation.CREATE:
//                    Onem2mStats.getInstance().inc(Onem2mStats.MQTT_REQUESTS_CREATE);
//
//                    break;
//
//                case Onem2m.Operation.NOTIFY:
//                    Onem2mStats.getInstance().inc(Onem2mStats.MQTT_REQUESTS_NOTIFY);
//                    break;
//
//                case Onem2m.Operation.UPDATE:
//                    Onem2mStats.getInstance().inc(Onem2mStats.MQTT_REQUESTS_UPDATE);
//                    break;
//
//                case Onem2m.Operation.DELETE:
//                    Onem2mStats.getInstance().inc(Onem2mStats.MQTT_REQUESTS_DELETE);
//                    break;
//
//                default:
//                    Onem2mStats.getInstance().inc(Onem2mStats.MQTT_REQUESTS_ERROR);
//                    return;
//            }
//
//            Onem2mRequestPrimitiveClient onem2mRequest = clientBuilder.build();
//            ResponsePrimitive onem2mResponse = Onem2m.serviceOnem2mRequest(onem2mRequest, onem2mService);
//            // Now place the fields from the onem2m result response back in the mqtt fields, and send
//            if (mqttMessageFormat.contains(Onem2m.ContentFormat.JSON)) {
//                sendMqttJsonResponseFromOnem2mResponse(topic, onem2mResponse);
//            }
//        }
//
//        private String processXMLRequestPrimitive(String message,
//                                                  Onem2mRequestPrimitiveClientBuilder clientBuilder) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        private void sendMqttJsonResponseFromOnem2mResponse(String topic,ResponsePrimitive onem2mResponse){
//            JSONObject jsonResponse = onem2mResponse.toJson();
//            sendResponse(topic, jsonResponse.toString());
//
//            if (jsonResponse.getString(ResponsePrimitive.RESPONSE_STATUS_CODE).charAt(0) =='2') {
//                Onem2mStats.getInstance().inc(Onem2mStats.MQTT_REQUESTS_OK);
//            }
//            else {
//                Onem2mStats.getInstance().inc(Onem2mStats.MQTT_REQUESTS_ERROR);
//            }
//
//        }
//
//        void sendResponse(String requestTopic, String message) {
//
//            String hierarchyTopic[] = trimTopic(requestTopic).split("/");
//            String format_type = hierarchyTopic[4];
//            String cse_name = hierarchyTopic[3].replace("/", ":");
//            String resource_name = hierarchyTopic[2].replace("/", ":");
//            String responseTopic = "/oneM2M/" + Onem2m.MqttMessageType.RESPONSE + "/" + resource_name + "/" + cse_name + "/" + format_type;
//
//            IMqttActionListener pubListener = new IMqttActionListener() {
//
//                @Override
//                public void onSuccess(IMqttToken arg0) {
//                    // TODO Auto-generated method stub
//                }
//
//                @Override
//                public void onFailure(IMqttToken arg0, Throwable arg1) {
//                    // TODO Auto-generated method stub
//                }
//            };
//            try {
//                client.publish(responseTopic, message.getBytes(), Onem2m.MqttOptions.QOS1,
//                        Onem2m.MqttOptions.RETAINED, "Pub Sample Context", pubListener);
//            } catch (MqttPersistenceException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } catch (MqttException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//    }

}
