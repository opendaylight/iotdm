/*
 * Copyright © 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2moic.impl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.*;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.eclipse.californium.core.Utils.prettyPrint;

public class Onem2mOicClient {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mOicClient.class);

    public static final String WELL_KNOWN_QUERY = "/oic/res";
    public static final String WELL_KNOWN_DEVICE_QUERY = "/oic/d";
    public static final String WELL_KNOWN_PLATFORM_QUERY = "/oic/p";
    public static final int DEFAULT_PRESENCE_TTL = 60;
    public static final String PRESENCE_URI = "/oic/ad";
    public static final int OIC_MULTICAST_PORT = 5683;
    public static final String OIC_MULTICAST_ADDRESS = "224.0.1.187";

    public enum OicClientType {
        COAP, HTTP, XMPP
    }

    /**
     * Class to represent the attributes of OIC device/Server
     */
    public class OicDevice {
        String n;
        String di;
        String icv;
        String dmv;
        String intf[];
    }

    private OicClientType oicclienttype;
    private CoapClient coapClient;
    private Endpoint client;
    private int clientPort;

    /**
     * Creates a Oic Client given the Oic client type
     * @param type  Oic client type, currently only COAP is supported
     */
    public Onem2mOicClient(OicClientType type, String localIp) throws IOException {
        this.oicclienttype = type;
        switch (type) {
            case COAP:
                NetworkConfig config = new NetworkConfig()
                        .setInt(NetworkConfig.Keys.MAX_MESSAGE_SIZE, 32)
                        .setInt(NetworkConfig.Keys.PREFERRED_BLOCK_SIZE, 32)
                        .setInt(NetworkConfig.Keys.ACK_TIMEOUT, 200) // client retransmits after 200 ms
                        .setFloat(NetworkConfig.Keys.ACK_RANDOM_FACTOR, 1f)
                        .setFloat(NetworkConfig.Keys.ACK_TIMEOUT_SCALE, 1f);
                if (localIp == null || localIp.isEmpty()) {
                    client = new CoapEndpoint(new InetSocketAddress(0), config);
                } else {
                    client = new CoapEndpoint(new InetSocketAddress(localIp, 0), config);
                }

                client.start();
                clientPort = client.getAddress().getPort();

                coapClient = new CoapClient();
                coapClient.useCONs();
                coapClient.setEndpoint(client);
                LOG.info("Onem2mOicClient: Coap Client created listening on port " + clientPort);
                break;
            case HTTP:
                LOG.info("Onem2mOicClient: Http Clients are not yet supported");
                break;
            case XMPP:
                LOG.info("Onem2mOicClient: XMPP Clients are not yet supported");
                break;
            default:
                LOG.info("Onem2mOicClient: Wrong type");
                break;
        }
        return;
    }

    /**
     *
     * @param host  Host IP Address. If null or empty, Multicast is performed.
     * @param port  Host Port number. If Zero, 5683 is used
     * @param deviceUri Uri containing address to the virtual device. If null, /oic/res is used.
     * @param discoverHandler   Coaphandler for response
     */
    public void oicDeviceDiscovery (
            String host,
            int port,
            String deviceUri,
            CoapHandler discoverHandler) {
        String discoverUri = "coap://";

        if (host == null || host.isEmpty()) {
            host = OIC_MULTICAST_ADDRESS;
            port = OIC_MULTICAST_PORT;
        }

        if (port == 0) {
            port = OIC_MULTICAST_PORT;
        }

        if (deviceUri == null || deviceUri.isEmpty()) {
            deviceUri = WELL_KNOWN_DEVICE_QUERY;
        }

        discoverUri = discoverUri + host + ":" + port + deviceUri;
        coapClient.setURI(discoverUri);

        coapClient.get(discoverHandler);

    }

    /**
     * Parse the payload of GET request of /oic/if
     * @param payload   Payload of CoapResponse from GET /oic/if
     */
    public void oicParseInterfacePayload(byte payload[]) throws IOException {
        CBORFactory factory = new CBORFactory();
        ObjectMapper m = new ObjectMapper(factory);
        JsonNode rootNode = m.readTree(payload);

        JsonFactory jfactory = new JsonFactory();
        JsonParser jParser = jfactory.createParser(rootNode.toString());

        while (jParser.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jParser.getCurrentName();

            switch(fieldname==null ? "" : fieldname) {
                case "if":
                    jParser.nextToken();

                    while (jParser.nextToken() != JsonToken.END_ARRAY) {
                        if (jParser.getCurrentToken() != JsonToken.START_ARRAY) {
                            // TODO Need to populate to interface object which will be committed in next changes
                        }
                    }
                    continue;

                case "pi":
                case "mnmn":
                case "mnml":
                case "mnmo":
                case "mndt":
                case "mnpv":
                case "mnos":
                case "mnhw":
                case "mnfv":
                case "st":
                case "mnsl":
                case "rt":
                    // TODO Need to populate to interface object which will be committed in next changes
                    jParser.nextToken();
                    continue;
                default:
                    // TODO Need to discuss with OneM2M OIC Internetworking standard on how to handle this
                    jParser.nextToken();
                    continue;
            }
        }

        return;
    }

    /**
     * Parse the payload of GET request of /oic/p
     *
     * @param payload   Payload of CoapResponse from GET /oic/p
     */
    public void oicParsePlatformPayload(byte payload[]) throws IOException {
        CBORFactory factory = new CBORFactory();
        ObjectMapper m = new ObjectMapper(factory);
        JsonNode rootNode = m.readTree(payload);

        JsonFactory jfactory = new JsonFactory();
        JsonParser jParser = jfactory.createParser(rootNode.toString());

        OicDevice device = new OicDevice();

        while (jParser.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jParser.getCurrentName();

            switch (fieldname==null ? "" : fieldname) {
                case "if":
                    jParser.nextToken();

                    while (jParser.nextToken() != JsonToken.END_ARRAY) {
                        if (jParser.getCurrentToken() != JsonToken.START_ARRAY) {
                           // TODO Need to populate to platform object which will be committed in next changes
                        }
                        continue;
                    }

                case "pi":
                case "mnmn":
                case "mnml":
                case "mnmo":
                case "mndt":
                case "mnpv":
                case "mnos":
                case "mnhw":
                case "mnfv":
                case "mnsl":
                case "st":
                case "rt":
                    // TODO Need to populate to platform object which will be committed in next changes
                    jParser.nextToken();
                    continue;
                default:
                    // TODO Need to discuss with OneM2M OIC Internetworking standard on how to handle this
                    jParser.nextToken();
                    continue;
            }
        }

        return;
    }

    /**
     * Parse the payload of GET request of /oic/d
     *
     * @param payload   Payload of CoapResponse from GET /oic/d
     */
    public OicDevice oicParseDevicePayload(byte payload[]) throws IOException {
        CBORFactory factory = new CBORFactory();
        ObjectMapper m = new ObjectMapper(factory);
        JsonNode rootNode = m.readTree(payload);
        OicDevice oicDevice = new OicDevice();

        JsonFactory jfactory = new JsonFactory();
        JsonParser jParser = jfactory.createParser(rootNode.toString());

        while (jParser.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jParser.getCurrentName();

            switch (fieldname==null ? "" : fieldname) {
                case "if":
                    jParser.nextToken();

                    while (jParser.nextToken() != JsonToken.END_ARRAY) {
                        if (jParser.getCurrentToken() != JsonToken.START_ARRAY) {
                            // TODO Need to discuss with OneM2M OIC Internetworking standard on how to handle this
                        }
                    }
                    continue;

                case "di":
                    jParser.nextToken();
                    oicDevice.di = jParser.getValueAsString();
                    continue;

                case "n":
                    jParser.nextToken();
                    oicDevice.n = jParser.getValueAsString();
                    continue;

                case "icv":
                    jParser.nextToken();
                    oicDevice.icv = jParser.getValueAsString();
                    continue;

                case "dmv":
                    jParser.nextToken();
                    oicDevice.dmv = jParser.getValueAsString();
                    continue;

                default:
                    jParser.nextToken();
                    // Bail out
                    continue;
            }
        }

        return oicDevice;
    }

    /**
     * Parse the payload of GET request of /oic/res
     * TODO Need to populate to /oic/res object which will be committed in next changes, hence just parsing now
     *
     * @param payload   Payload of CoapResponse from GET /oic/res
     */
    public void oicParseDisDevices(byte payload[]) throws IOException {
        CBORFactory factory = new CBORFactory();
        ObjectMapper m = new ObjectMapper(factory);
        JsonNode rootNode = m.readTree(payload);

        JsonFactory jfactory = new JsonFactory();
        JsonParser jParser = jfactory.createParser(rootNode.toString());

        while (jParser.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jParser.getCurrentName();

            switch (fieldname==null ? "" : fieldname) {
                case "di":
                    jParser.nextToken();
                    continue;

                case "links":
                    jParser.nextToken();

                    //iterate through the array until token equal to "]"
                    while (jParser.nextToken() != JsonToken.END_ARRAY) {
                        fieldname = jParser.getCurrentName();

                        while (jParser.nextToken() != JsonToken.END_OBJECT) {
                            fieldname = jParser.getCurrentName();

                            switch (fieldname==null ? "" : fieldname) {
                                case "href":
                                    jParser.nextToken();
                                    continue;

                                case "rt":
                                case "if":
                                    jParser.nextToken();
                                    while (jParser.nextToken() != JsonToken.END_ARRAY) {
                                    }
                                    continue;

                                case "p":
                                    jParser.nextToken();

                                    while (jParser.nextToken() != JsonToken.END_OBJECT) {
                                        fieldname = jParser.getCurrentName();
                                        if ("bm".equals(fieldname)) {
                                            jParser.nextToken();
                                        }
                                    }
                                    continue;
                                default:
                                    jParser.nextToken();
                                    // Bail out
                                    continue;
                            }
                        }

                    }
                default:
                    jParser.nextToken();
                    // Bail out
                    continue;
            }
        }

        return;
    }

    /**
     * Print the coapResponse header and payload
     *
     * @param coapResponse  CoapResponse of the oic query
     */
    private void printCoapResponse(CoapResponse coapResponse) {
        LOG.info("Header:" + prettyPrint(coapResponse));
        LOG.info("Payload String:" + coapResponse.getResponseText().trim());
        printPayloadBytes(coapResponse); /* Enable only for debugging */
    }

    private void printPayloadBytes(CoapResponse coapResponse) {
        /* use http://cbor.me/ to decode data */
        LOG.info("Payload Bytes:");
        LOG.info(Arrays.toString(coapResponse.advanced().getPayload()));
    }
}