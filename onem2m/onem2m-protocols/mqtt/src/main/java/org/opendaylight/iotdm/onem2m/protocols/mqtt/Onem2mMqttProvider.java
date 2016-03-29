/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.mqtt;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClient;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClientBuilder;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.Onem2mStats;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.mqtt.rev150105.Onem2mMqttClientService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.mqtt.rev150105.Onem2mMqttConfigInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.mqtt.rev150105.Onem2mMqttConfigOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.mqtt.rev150105.Onem2mMqttConfigOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.mqtt.rev150105.onem2m.mqtt.config.input.CseList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Monitor;

public class Onem2mMqttProvider implements Onem2mMqttClientService, BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mMqttProvider.class);
    private BindingAwareBroker.RpcRegistration<Onem2mMqttClientService> rpcReg;
    private final ExecutorService executor;
    protected Onem2mService onem2mService;
    protected Onem2mMqttAsyncClient onem2mMqttClient;
    private DataBroker dataBroker;
    private Onem2mStats stats;
    private Onem2mDb db;
    private static NotificationProviderService notifierService;
    private Monitor crudMonitor;
    Onem2mMqttConfigOutputBuilder statusBuilder = new Onem2mMqttConfigOutputBuilder();
    private Boolean validated = false;
    private  String mqttBroker = null;
    private String mqttAddress = null;
    private String status = null;
    protected static HashSet<String> cseList = null;
    protected static Boolean connectedToBroker = false;

    public Onem2mMqttProvider() {
        executor = Executors.newFixedThreadPool(1);
        cseList = new HashSet();
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        this.rpcReg = session.addRpcImplementation(Onem2mMqttClientService.class, this);
        this.dataBroker = session.getSALService(DataBroker.class);
        this.notifierService = session.getSALService(NotificationProviderService.class);
        crudMonitor = new Monitor();
        stats = Onem2mStats.getInstance();
        db = Onem2mDb.getInstance();
        db.initializeDatastore(dataBroker);
        onem2mService = session.getRpcService(Onem2mService.class);
        onem2mMqttClient = new Onem2mMqttAsyncClient();
        LOG.info("Onem2mMqttProvider Session Initiated ...");

    }

    @Override
    public void close() throws Exception {
        LOG.info("Onem2mMqttProvider Closed");
        executor.shutdown();
    }

    /**
     * Provision the broker address and a list of cse's to use as mqtt subscribers.
     * First we need to provision the cse, then configure the mqtt parameters (broker address and CSE names).
     * We support MANY cse's in oneM2M, not all maye be managed by a MQTT broker so an individual message is
     * required to indicate which cse's required mqtt.
     * @param input
     * @return
     * How to provision external mqtt broker : reference
     * https://wiki.opendaylight.org/view/Iotdm:MQTT-HowTo
     */
    @Override
    public Future<RpcResult<Onem2mMqttConfigOutput>> onem2mMqttConfig(Onem2mMqttConfigInput input) {
        ResponsePrimitive onem2mResponse;
        Onem2mMqttConfigOutput output = null;
        List<CseList> tempCseList = input.getCseList();
        validated = validateBrokerAddress(input.getMqttBroker());
        /**
         * If broker is not connected, then store these values and call connectToMqttServer.
        */
        if (!connectedToBroker && validated) {
            mqttBroker = input.getMqttBroker();
            mqttAddress = mqttBroker;
            addCSEListandConnect(tempCseList, mqttBroker);
        }
        else {
             /**
             * If broker is not changed, then ensure we only subscribe to the list of cse's in this new list.
             */
            if (connectedToBroker && mqttAddress.equals(input.getMqttBroker()) && validated){
                cseList.clear();
                    for (CseList c : tempCseList) {
                        String cseId = c.getCseId();
                        cseList.add(cseId);
                    }
                    for (String cseId : cseList) {
                    boolean status=onem2mMqttClient.registerCseAsMqttSubscriber(cseId);
                    }
            }
            /**
             * If the broker is changed, disconnect from old broker, then store these values and call connectToMqttServer.
             */
            else if (connectedToBroker && !mqttAddress.equals(input.getMqttBroker()) && validated){
                onem2mMqttClient.disconnectFromMqttServer();
                mqttAddress = input.getMqttBroker();
                addCSEListandConnect(tempCseList, mqttAddress);
            }
        }
        status = statusBuilder.getStatus();
        output = new Onem2mMqttConfigOutputBuilder().setStatus(status).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    private boolean validateBrokerAddress(String mqttBroker) {
        // TODO Auto-generated method stub

        if(mqttBroker == null )
        {
        LOG.error("Mqtt Broker Adress string : Incorrect format :: Address is empty or null ");
        statusBuilder.setStatus("Mqtt Broker Adress string : Incorrect format :: Address is empty or null ");

        return false;
        }
        String mqttBrokerData[]=mqttBroker.split(":");
        String mqttBrokerIP[]=mqttBrokerData[1].split("//");
        try {
            InetAddress.getByName(mqttBrokerIP[1]);
            }catch (UnknownHostException e) {

            statusBuilder.setStatus("Mqtt Broker Adress string : Incorrect format :: Inorrect IP Address ");
            LOG.error("Mqtt Broker Adress string : Incorrect format :: Inorrect IP Address  ");
            return false;
            }
            if(!mqttBrokerData[0].equalsIgnoreCase("TCP"))
                {
                statusBuilder.setStatus("Mqtt Broker Adress string : Incorrect format :: Wrong Protocol in Address ");
                LOG.error("Mqtt Broker Adress string : Incorrect format :: Wrong Protocol in Address");
                return false;
                }
            if(!mqttBrokerData[2].equalsIgnoreCase("1883"))
                {
                statusBuilder.setStatus("Mqtt Broker Adress string : Incorrect format :: Wrong Port in Address");
                LOG.error("Mqtt Broker Adress string : Incorrect format :: Wrong Port in Address");
                return false;
                }
        return true;
    }

    private void addCSEListandConnect(List<CseList> tempCseList, String brokerAddress) {
        // TODO Auto-generated method stub

        cseList.clear();
        for (CseList c : tempCseList) {
            String cseId = c.getCseId();
            cseList.add(cseId);
        }
        try {
            onem2mMqttClient.connectToMqttServer(brokerAddress);
        } catch (MqttException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public class Onem2mMqttAsyncClient {
        MqttAsyncClient client;
        Onem2mMqttAsyncClient() {
        }

        public void disconnectFromMqttServer() {
            try {
                client.disconnect();
            } catch (MqttException e) {
                LOG.error("DisconnectFromMqttServer: trouble disconnecing {}", e.toString());
            }
            finally {
                connectedToBroker = false;
            }
        }

        public boolean registerCseAsMqttSubscriber(String cseId) {

            Boolean status = true;
            String topic = "/oneM2M/" + Onem2m.MqttMessageType.REQUEST + "/+/" + cseId + "/+" ;
            try {
                client.subscribe(topic, 1);
            } catch (MqttException e) {
                LOG.error("RegisterCseAsMqttSubscriber: cannot register {}", cseId);
                status = false;
            }
            return status;
        }

        public void connectToMqttServer(final String mqttBroker) throws MqttException {

            if (mqttBroker == null) {
                LOG.info("Broker not configured, returning without connecting ...");
                statusBuilder.setStatus("Broker address is missing.");
                return;
            }

            try {
                client = new MqttAsyncClient(
                         mqttBroker,//Broker Address
                         MqttClient.generateClientId(), //ClientId
                         new MemoryPersistence()); //Persistence
                MqttConnectOptions options = new MqttConnectOptions();
                options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
                options.setUserName("mqtt");
                options.setPassword("mqtt".toCharArray());
                options.setCleanSession(false);
                // register a callback for messages that we subscribe to ...
                client.setCallback(new MqttCallback() {

                    @Override
                    public void connectionLost(Throwable cause) {
                        LOG.error("Onem2mMqttClient: lost connection to server");
                        try {
                            Onem2mMqttAsyncClient.this.connectToMqttServer(mqttBroker);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        //check if Qos is not 0
                        if ((message.isRetained()==false)&&(message.getQos() ==1)){
                            handleMqttMessage(topic, message.toString());
                        }
                        if (message.getQos() !=1){
                            sendResponse(topic, "QoS must be 1");
                        }
                        if (message.isRetained()==true){
                            sendResponse(topic, "Message retained should be false");
                        }
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {//Called when a outgoing publish is complete.
                    }
                });
                IMqttActionListener conListener=new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken arg0) {
                    // TODO Auto-generated method stub
                    LOG.info("Connection successfull with broker : " + mqttBroker);
                    statusBuilder.setStatus("Mqtt broker provisioned.");
                    connectedToBroker = true;

                }
                @Override
                public void onFailure(IMqttToken arg0, Throwable arg1) {
                    // TODO Auto-generated method stub
                    LOG.error("Connection failed ");
                }
                };
                IMqttToken conToken=client.connect(options,"Connect async client to server",conListener);
                conToken.waitForCompletion();
                //connecting client to server
                if (!client.isConnected()) {
                    statusBuilder.setStatus("Onem2mMqttClient: trouble connecting to server");
                    LOG.error("Onem2mMqttClient: trouble connecting to server");
                }

                for (String cseId : cseList) {
                    registerCseAsMqttSubscriber(cseId);
                }

                connectedToBroker = true;

            } catch (MqttException e) {
                statusBuilder.setStatus(e.toString());
            }

        }

        // Handler for a request and a response
        void handleMqttMessage(String topic, String message) {

            String mqttMessageType = null;
            String mqttMessageFormat=null;
            String to=null;
            String from=null;
            String hierarchyTopic[] = parseTopicString(topic);
            mqttMessageFormat=hierarchyTopic[4];
                if (hierarchyTopic[1].contains("req")) {
                    mqttMessageType= Onem2m.MqttMessageType.REQUEST;
                } else if (hierarchyTopic[1].contains("resp")) {
                    mqttMessageType= Onem2m.MqttMessageType.RESPONSE;
                }
                if (hierarchyTopic[4].contains("json")) {
                    mqttMessageFormat= Onem2m.ContentFormat.JSON;
                } else if (hierarchyTopic[4].contains("xml")) {
                    mqttMessageFormat= Onem2m.ContentFormat.XML;
                }

            to=hierarchyTopic[2].replace(":", "/");
            from=hierarchyTopic[3].replace(":", "/");

            switch (mqttMessageType) {
                case Onem2m.MqttMessageType.REQUEST:
                    handleRequest(topic, message, to, from);
                    break;
                case Onem2m.MqttMessageType.RESPONSE:
                    break;
            }
        }

        /**
         * The topic string is of the format /onem2m/(message-type)/originator/receiver ... verify and pull out the
         * relevant components in the topic hierarchy string.
         */
        private String[] parseTopicString(String topic) {

            topic = trimTopic(topic);
            // split the topic into its hierarchy of path component strings
            String hierarchy[] = topic.split("/");

            if (hierarchy.length != 5) {
            LOG.error("Length of topics is less than expected");
            }

            if (!hierarchy[0].toLowerCase().contentEquals("onem2m")){
                LOG.error("Topic must contain onem2m");
            }
            if (!hierarchy[1].contains("req")){
                LOG.error("Topic must contain req or resp");
            }
            if(!(hierarchy[4].equalsIgnoreCase("json")||(hierarchy[4].equalsIgnoreCase("xml"))))
            {
                LOG.error("Topic must include type as either json or xml only");
            }
            return hierarchy;

        }

        private String trimTopic(String topic) {

            topic = topic.trim();
            topic = topic.startsWith("/") ? topic.substring("/".length()) : topic;
            topic = topic.endsWith("/") ? topic.substring(0,topic.length()-1) : topic;
            return topic;
        }

        private void handleRequest(String topic, String message, String from, String to) {

            Onem2mRequestPrimitiveClientBuilder clientBuilder = new Onem2mRequestPrimitiveClientBuilder();
            clientBuilder.setProtocol(Onem2m.Protocol.MQTT);
            Onem2mStats.getInstance().inc(Onem2mStats.MQTT_REQUESTS);
            String hierarchy[]=parseTopicString(topic);
            String mqttMessageFormat=hierarchy[4];
            String operation=null;
            if(mqttMessageFormat.contains(Onem2m.ContentFormat.JSON))
            {
                operation = processJsonRequestPrimitive(message, clientBuilder);
            }
            if(mqttMessageFormat.contains(Onem2m.ContentFormat.XML))
            {
                operation = processXMLRequestPrimitive(message, clientBuilder);
                sendResponse(topic, "XML format is not supported yet");
            }

            switch (operation) {
                case Onem2m.Operation.RETRIEVE:
                    Onem2mStats.getInstance().inc(Onem2mStats.MQTT_REQUESTS_RETRIEVE);
                    break;

                case Onem2m.Operation.CREATE:
                    Onem2mStats.getInstance().inc(Onem2mStats.MQTT_REQUESTS_CREATE);

                    break;

                case Onem2m.Operation.NOTIFY:
                    Onem2mStats.getInstance().inc(Onem2mStats.MQTT_REQUESTS_NOTIFY);
                    break;

                case Onem2m.Operation.UPDATE:
                    Onem2mStats.getInstance().inc(Onem2mStats.MQTT_REQUESTS_UPDATE);
                    break;

                case Onem2m.Operation.DELETE:
                    Onem2mStats.getInstance().inc(Onem2mStats.MQTT_REQUESTS_DELETE);
                    break;

                default:
                    Onem2mStats.getInstance().inc(Onem2mStats.MQTT_REQUESTS_ERROR);
                    return;
            }

            Onem2mRequestPrimitiveClient onem2mRequest = clientBuilder.build();
            ResponsePrimitive onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);
            // Now place the fields from the onem2m result response back in the mqtt fields, and send
            if(mqttMessageFormat.contains(Onem2m.ContentFormat.JSON))
            {
                sendMqttJsonResponseFromOnem2mResponse(topic, onem2mResponse);
            }
        }

          private String processXMLRequestPrimitive(String message,
                  Onem2mRequestPrimitiveClientBuilder clientBuilder) {
                  // TODO Auto-generated method stub
                  return null;
          }

         /**
         * The payload is a json string containing the request primitive attributes.
         * All the error checking is done in one place in the core.
         */
        private String processJsonRequestPrimitive(String message,
                Onem2mRequestPrimitiveClientBuilder clientBuilder) {
            JSONObject jsonContent = null;
            String operation = null;
            if (message == null) {
                LOG.info("Content not specified");
                return null;
            }
            try {
                jsonContent = new JSONObject(message);
            } catch (JSONException e) {
                LOG.info("Content Unacceptable");
                return null;
            }

            Iterator<?> keys = jsonContent.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                Object o = jsonContent.opt(key);
                if (o != null) {
                    clientBuilder.setPrimitiveNameValue(key, o.toString());
                    if (key.contentEquals(RequestPrimitive.OPERATION)) {
                        operation = o.toString();
                    }
                }
            }

            clientBuilder.setContentFormat("json");
            return operation ;
        }

        private void sendMqttJsonResponseFromOnem2mResponse(String topic,ResponsePrimitive onem2mResponse){

            JSONObject responseJsonObject = new JSONObject();
            String content = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
            JsonUtils.put(responseJsonObject, ResponsePrimitive.CONTENT, content);
            String rscString = onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE);
            JsonUtils.put(responseJsonObject, ResponsePrimitive.RESPONSE_STATUS_CODE, rscString);
            String rqi = onem2mResponse.getPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER);
            JsonUtils.put(responseJsonObject, ResponsePrimitive.REQUEST_IDENTIFIER, rqi);
            sendResponse(topic, responseJsonObject.toString());

            if (rscString.charAt(0) =='2') {
                Onem2mStats.getInstance().inc(Onem2mStats.MQTT_REQUESTS_OK);
            } else {
                Onem2mStats.getInstance().inc(Onem2mStats.MQTT_REQUESTS_ERROR);
            }

        }

        void sendResponse(String requestTopic, String message) {

            String hierarchyTopic[] = trimTopic(requestTopic).split("/");
            String format_type=hierarchyTopic[4];
            String cse_name = hierarchyTopic[3].replace("/", ":");
            String resource_name = hierarchyTopic[2].replace("/", ":");
            String responseTopic ="/oneM2M/" + Onem2m.MqttMessageType.RESPONSE + "/" + resource_name + "/" + cse_name + "/" +format_type;

            IMqttActionListener pubListener=new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken arg0) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onFailure(IMqttToken arg0, Throwable arg1) {
                    // TODO Auto-generated method stub
                }
            };
            try {
                client.publish(responseTopic, message.getBytes(), Onem2m.MqttOptions.QOS1,
                               Onem2m.MqttOptions.RETAINED,"Pub Sample Context",pubListener);
            } catch (MqttPersistenceException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (MqttException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
