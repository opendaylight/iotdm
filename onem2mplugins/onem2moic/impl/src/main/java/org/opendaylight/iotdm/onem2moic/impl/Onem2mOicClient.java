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
import java.util.Arrays;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.californium.core.Utils.prettyPrint;
import static org.opendaylight.iotdm.onem2moic.impl.Onem2mOicClient.CoapReqType.GET;


public class Onem2mOicClient {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mOicClient.class);

    /******************************* OIC Client data *******************************/
    private OicClientType oicclienttype;

    public enum OicClientType {
        COAP, HTTP,
    }

    public enum OicMsgType {
        OIC_DISCOVERY,
        OIC_DEVICE,
        OIC_PLATFORM,
        OIC_INTERFACE,
    }

    /******************************* Coap Client data *******************************/
    public CoapClient coapClient;

    public enum CoapReqType {
        GET, POST, PUT, DELETE,
    }


    /*
     * https://github.com/mjung85/core/blob/master/schemas/oic.wk.d-schema.json
     */
    public class OicDevice {
        String n;
        String di;
        String icv;
        String dmv;
        String intf[];
    }

    /******************************* OIC Client APIs *******************************/

    /* APIs for Oic Client related functionality */
    public Onem2mOicClient(OicClientType type) {
        this.oicclienttype = type;

        switch (type) {
            case COAP:
                coapClient = new CoapClient();
                coapClient.useCONs();
                LOG.info("Onem2mOicClient: Coap Client created");
                break;
            case HTTP:
                LOG.info("Onem2mOicClient: Http Clients are not yet supported");
                break;
            default:
                LOG.info("Onem2mOicClient: Wrong type");
                break;
        }
        return;
    }

    /**** OIC Request ****/
    public void oicDiscoveryReq() {
        /* Write the response handler */
        CoapHandler discoverHandler = new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                oicResHandler(OicMsgType.OIC_DISCOVERY, coapResponse);
            }

            @Override
            public void onError() {
                oicErrResHandler(OicMsgType.OIC_DISCOVERY);
            }
        };

        /*
         * TODO Need to replace with multicast device discovery
         */
        coapReqHandler(GET, discoverHandler);
    }

    public void oicDeviceReq(CoapHandler discoverHandler) {
        /*
         * Discover the devices
         * Need to replace with multicast discovery
         */
        coapReqHandler(GET, discoverHandler);
    }

    public void oicPlatformReq() {
        /* Write the response handler */
        CoapHandler discoverHandler = new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                oicResHandler(OicMsgType.OIC_PLATFORM, coapResponse);
            }

            @Override
            public void onError() {
                oicErrResHandler(OicMsgType.OIC_PLATFORM);
            }
        };

        /*
         * Discover the devices
         * Need to replace with multicast discovery
         */
        coapReqHandler(GET, discoverHandler);
    }

    public void oicInterfaceReq() {
        /* Write the response handler */
        CoapHandler discoverHandler = new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                oicResHandler(OicMsgType.OIC_INTERFACE, coapResponse);
            }

            @Override
            public void onError() {
                oicErrResHandler(OicMsgType.OIC_PLATFORM);
            }
        };

        /*
         * Discover the devices
         * Need to replace with multicast discovery
         */
        coapReqHandler(GET, discoverHandler);
    }

    /**** OIC Response ****/
    public void oicResHandler(OicMsgType type, CoapResponse coapResponse) {
        switch (type) {
            case OIC_DISCOVERY:
                try {
                    oicParseDiscoveryPayload(coapResponse.advanced().getPayload());
                } catch (IOException ioe) {
                    //System.out.println(ioe);
                    ioe.printStackTrace();
                }
                break;
            case OIC_DEVICE:
                try {
                    OicDevice oicDevice = oicParseDevice(coapResponse.advanced().getPayload());
                    //if (oicDevice != null)
                        //onem2mOicIPE.createOicAe(oicDevice);
                } catch (IOException ioe) {
                    //System.out.println(ioe);
                    ioe.printStackTrace();
                }
                break;
            case OIC_PLATFORM:
                try {
                    oicParsePlatform(coapResponse.advanced().getPayload());
                } catch (IOException ioe) {
                    //System.out.println(ioe);
                    ioe.printStackTrace();
                }
                break;
            case OIC_INTERFACE:
                try {
                    printCoapResponse(coapResponse);
                    oicParseInterface(coapResponse.advanced().getPayload());
                } catch (IOException ioe) {
                    //System.out.println(ioe);
                    ioe.printStackTrace();
                }
                break;
            default:
                break;
        }
        return;
    }

    public void oicErrResHandler(OicMsgType type) {
        LOG.info("Onem2mOicClient: oicErrResHandler " + type);
        switch (type) {
            case OIC_DISCOVERY:
                break;
            case OIC_DEVICE:
                break;
            case OIC_PLATFORM:
                break;
            case OIC_INTERFACE:
                break;
            default:
                break;
        }
        return;
    }

    /**** OIC Response Parse APIs ****/
    public void oicParseDiscoveryPayload(byte[] payload) throws IOException {

        if(payload.length != 0)
            parseDisDevices(payload);
    }

    public OicDevice oicParseDevice(byte[] payload) throws IOException {
        OicDevice oicDevice = null;

        if(payload.length != 0)
            oicDevice = parseDevices(payload);

        return oicDevice;
    }

    public void oicParsePlatform(byte[] payload) throws IOException {
        if(payload.length != 0)
            parsePlatform(payload);
    }

    public void oicParseInterface(byte[] payload) throws IOException {
        if(payload.length != 0)
            parseInterface(payload);
    }

    public void parseInterface(byte payload[]) throws IOException {
        CBORFactory factory = new CBORFactory();
        ObjectMapper m = new ObjectMapper(factory);
        JsonNode rootNode = m.readTree(payload);

        JsonFactory jfactory = new JsonFactory();
        JsonParser jParser = jfactory.createParser(rootNode.toString());

        while (jParser.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jParser.getCurrentName();

            switch(fieldname==null?"":fieldname) {
                case "if":
                    jParser.nextToken();
                    //System.out.println("\tif:");
                    while (jParser.nextToken() != JsonToken.END_ARRAY) {
                        if (jParser.getCurrentToken() != JsonToken.START_ARRAY) {
                            //System.out.println("\t\t" + jParser.getText());
                        }
                    }
                    continue;

                case "pi":
                    jParser.nextToken();
                    //System.out.println("\tpi:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "mnmn":
                    jParser.nextToken();
                    //System.out.println("\tmnmn:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "mnml":
                    jParser.nextToken();
                    //System.out.println("\tmnml:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "mnmo":
                    jParser.nextToken();
                    //System.out.println("\tmnmo:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "mndt":
                    jParser.nextToken();
                    //System.out.println("\tmndt:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "mnpv":
                    jParser.nextToken();
                    //System.out.println("\tmnpv:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "mnos":
                    if ("mnos".equals(fieldname)) {
                        jParser.nextToken();
                        //System.out.println("\tmnos:");
                        //System.out.println("\t\t" + jParser.getValueAsString());
                        continue;
                    }

                case "mnhw":
                    jParser.nextToken();
                    //System.out.println("\tmnhw:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "mnfv":
                    jParser.nextToken();
                    //System.out.println("\tmnfv:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "st":
                    jParser.nextToken();
                    //System.out.println("\tst:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "mnsl":
                    jParser.nextToken();
                    //System.out.println("\tmnfv:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "rt":
                    jParser.nextToken();
                    //System.out.println("\trt:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;
            }
        }

        //System.out.println("CBOR:\n" + rootNode.toString());
        return;
    }

    public void parsePlatform(byte payload[]) throws IOException {
        CBORFactory factory = new CBORFactory();
        ObjectMapper m = new ObjectMapper(factory);
        JsonNode rootNode = m.readTree(payload);

        JsonFactory jfactory = new JsonFactory();
        JsonParser jParser = jfactory.createParser(rootNode.toString());

        OicDevice device = new OicDevice();

        while (jParser.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jParser.getCurrentName();

            switch (fieldname==null?"":fieldname) {
                case "if":
                    jParser.nextToken();
                    //System.out.println("\tif:");
                    while (jParser.nextToken() != JsonToken.END_ARRAY) {
                        if (jParser.getCurrentToken() != JsonToken.START_ARRAY) {
                            //System.out.println("\t\t" + jParser.getText());
                        }
                        continue;
                    }

                case "pi":
                    jParser.nextToken();
                    //System.out.println("\tpi:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "mnmn":
                    jParser.nextToken();
                    //System.out.println("\tmnmn:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "mnml":
                    jParser.nextToken();
                    //System.out.println("\tmnml:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "mnmo":
                    jParser.nextToken();
                    //System.out.println("\tmnmo:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "mndt":
                    jParser.nextToken();
                    //System.out.println("\tmndt:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "mnpv":
                    jParser.nextToken();
                    //System.out.println("\tmnpv:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "mnos":
                    jParser.nextToken();
                    //System.out.println("\tmnos:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "mnhw":
                    jParser.nextToken();
                    //System.out.println("\tmnhw:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "mnfv":
                    jParser.nextToken();
                    //System.out.println("\tmnfv:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "st":
                    jParser.nextToken();
                    //System.out.println("\tst:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "mnsl":
                    jParser.nextToken();
                    //System.out.println("\tmnfv:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "rt":
                    jParser.nextToken();
                    //System.out.println("\trt:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;
            }
        }
        //System.out.println("CBOR:\n" + rootNode.toString());
        return;
    }

    public OicDevice parseDevices(byte payload[]) throws IOException {
        CBORFactory factory = new CBORFactory();
        ObjectMapper m = new ObjectMapper(factory);
        JsonNode rootNode = m.readTree(payload);
        OicDevice oicDevice = new OicDevice();

        JsonFactory jfactory = new JsonFactory();
        JsonParser jParser = jfactory.createParser(rootNode.toString());

        while (jParser.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jParser.getCurrentName();

            switch (fieldname==null?"":fieldname) {
                case "if":
                    jParser.nextToken();
                    //System.out.println("\tif:");
                    while (jParser.nextToken() != JsonToken.END_ARRAY) {
                        if (jParser.getCurrentToken() != JsonToken.START_ARRAY) {
                            //System.out.println("\t\t" + jParser.getText());
                        }
                    }
                    continue;

                case "di":
                    jParser.nextToken();
                    //System.out.println("\tdi:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    oicDevice.di = jParser.getValueAsString();
                    continue;

                case "n":
                    jParser.nextToken();
                    //System.out.println("\tn:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    oicDevice.n = jParser.getValueAsString();
                    continue;

                case "icv":
                    jParser.nextToken();
                    //System.out.println("\ticv:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                case "dmv":
                    jParser.nextToken();
                    //System.out.println("\tdmv:");
                    //System.out.println("\t\t" + jParser.getValueAsString());
                    continue;

                default:
                    jParser.nextToken();
                    // Bail out
                    continue;
            }
        }

        //System.out.println("CBOR:\n" + rootNode.toString());
        return oicDevice;
    }

    public void parseDisDevices(byte payload[]) throws IOException {
        CBORFactory factory = new CBORFactory();
        ObjectMapper m = new ObjectMapper(factory);
        JsonNode rootNode = m.readTree(payload);

        JsonFactory jfactory = new JsonFactory();
        JsonParser jParser = jfactory.createParser(rootNode.toString());

        while (jParser.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jParser.getCurrentName();

            switch (fieldname==null?"":fieldname) {
                case "di":
                    jParser.nextToken();
                    //System.out.println("di:" + jParser.getValueAsString());
                    continue;

                case "links":
                    jParser.nextToken();
                    //System.out.println("\tlinks:");

                    // iterate through the array until token equal to "]"
                    while (jParser.nextToken() != JsonToken.END_ARRAY) {
                        fieldname = jParser.getCurrentName();

                        while (jParser.nextToken() != JsonToken.END_OBJECT) {
                            fieldname = jParser.getCurrentName();

                            switch (fieldname==null?"":fieldname) {
                                case "href":
                                    jParser.nextToken();
                                    //System.out.println("\t\thref: " + jParser.getValueAsString());
                                    continue;

                                case "rt":
                                    jParser.nextToken();
                                    //System.out.println("\t\trt:");
                                    while (jParser.nextToken() != JsonToken.END_ARRAY) {
                                        //System.out.println("\t\t\t" + jParser.getValueAsString());
                                    }
                                    continue;

                                case "if":
                                    jParser.nextToken();
                                    //System.out.println("\t\tif:");
                                    while (jParser.nextToken() != JsonToken.END_ARRAY) {
                                        //System.out.println("\t\t\t" + jParser.getValueAsString());
                                    }
                                    continue;

                                case "p":
                                    jParser.nextToken();
                                    //System.out.println("\t\tp:");
                                    while (jParser.nextToken() != JsonToken.END_OBJECT) {
                                        fieldname = jParser.getCurrentName();
                                        if ("bm".equals(fieldname)) {
                                            jParser.nextToken();
                                            //System.out.println("\t\t\t" + fieldname + " " + jParser.getValueAsInt());
                                        }
                                    }
                                    continue;
                            }
                        }

                    }
            }
        }

        //System.out.println("CBOR:\n" + rootNode.toString());
        return;
    }

    /******************************* CoAP CRUDN *******************************/
    public void coapReqHandler(CoapReqType type, CoapHandler coapHandler) {
        switch (type) {
            case GET:
                coapClient.get(coapHandler);
                break;
            default:
                break;
        }
        return;
    }

    public void coapResHandler(CoapReqType type, CoapResponse coapResponse) {
        switch (type) {
            case GET:
                printCoapResponse(coapResponse);
                break;
            default:
                break;
        }
        return;
    }

    /* Print routines & CBOR print routine */
    private void printCoapResponse(CoapResponse coapResponse) {
        System.out.println("Header:" + prettyPrint(coapResponse));
        System.out.println("Payload String:" + coapResponse.getResponseText().trim());
        //printPayloadBytes(coapResponse); /* Enable only for debugging */
    }

    private void printPayloadBytes(CoapResponse coapResponse) {
        /* use http://cbor.me/ to decode data */
        System.out.println("Payload Bytes:");
        System.out.println(Arrays.toString(coapResponse.advanced().getPayload()));
    }
}