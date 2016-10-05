/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins.channels.coap;

public class Onem2mCoapPluginServer {//} extends Onem2mBaseCommunicationChannel { //Onem2mBasePluginServer { //Onem2mBasePluginServer {
//    private int __port;
//    private Onem2mCoapBaseHandler server;
//
//    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCoapPluginServer.class);
//
//    public Onem2mCoapPluginServer(String ipAddress, int port,
//                                  Onem2mLocalEndpointRegistry registry) {
//        super(ipAddress, port, registry, null);
//        this.__port = port;
//    }
//
//    @Override
//    public boolean init() {
//        Onem2mCoapBaseHandler server = new Onem2mCoapBaseHandler(__port);
//        server.addEndpoints();
//        try {
//            server.start();
//            LOG.info("startCoapServer: on port: {}", __port);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            LOG.info("Exception: {}", e.toString());
//        }
//        return true;
//    }
//
//    @Override
//    public void close() {
//        try {
//            server.stop();
//            LOG.info("stopCoapServer: on port: {}", __port);
//        } catch (Exception e) {
//            e.printStackTrace();
//            LOG.info("Exception: {}", e.toString());
//        }
//    }
//
//    @Override
//    public String getProtocol() {
//        return "coap";
//    }
//
//    class Onem2mCoapBaseHandler extends CoapServer {
//        private final Logger LOG = LoggerFactory.getLogger(Onem2mCoapBaseHandler.class);
//        int port;
//
//        Onem2mCoapBaseHandler(int port){
//            super(port);
//            this.port = port;
//        }
//
//        @Override
//        public Resource createRoot() {
//            return new RootResource(this);
//        }
//
//        public void addEndpoints() {
//            for (InetAddress addr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
//                // only binds to IPv4 addresses and localhost
//                if (addr instanceof Inet4Address || addr.isLoopbackAddress()) {
//                    System.out.println("addr: "+addr.toString());
//                    InetSocketAddress bindToAddress = new InetSocketAddress(addr, port);
//                    System.out.println("bindToAddress: "+bindToAddress.toString());
//                    addEndpoint(new CoapEndpoint(bindToAddress));
//                }
//            }
//        }
//
//        private class RootResource extends CoapResource {
//            Onem2mCoapBaseHandler coapServer;
//            public RootResource(Onem2mCoapBaseHandler cServer)
//            {
//                super("OpenDaylight OneM2M CoAP Server");
//                this.coapServer = cServer;
//            }
//
//            @Override
//            public Resource getChild(String name) {
//                return this;
//            }
//
//            /**
//             * The handler for the CoAP request
//             *
//             * @param exchange coap parameters
//             */
//            @Override
//            public void handleRequest(final Exchange exchange) {
//                CoAP.Code code = exchange.getRequest().getCode();
//                CoapExchange coapExchange = new CoapExchange(exchange, this);
//                OptionSet options = coapExchange.advanced().getRequest().getOptions();
//                IotdmPluginRequest request = new IotdmPluginRequest();
//                IotdmPluginResponse response = new IotdmPluginResponse();
//
//                LOG.info("CoapServer - Handle Request: on port: {}", options.getUriPort());
//
//                // according to the spec, the uri query string can contain in short form, the
//                // resourceType, responseType, result persistence,  Delivery Aggregation, Result Content,
//                /*Boolean resourceTypePresent = options.getURIQueryString();
//
//                if (resourceTypePresent && code != CoAP.Code.POST) {
//                    coapExchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Specifying resource type not permitted.");
//                    return;
//                }*/
//
//                // take the entire payload text and put it in the CONTENT field; it is the representation of the resource
//                String cn = coapExchange.getRequestText().trim();
//                if (cn != null && !cn.contentEquals("")) {
//
//                    switch (options.getContentFormat()) {
//                        case MediaTypeRegistry.APPLICATION_JSON:
//                        case MediaTypeRegistry.APPLICATION_XML:
//                        case Onem2m.CoapContentFormat.APP_VND_RES_JSON:
//                        case Onem2m.CoapContentFormat.APP_VND_RES_XML:
//                            break;
//                        default:
//                            coapExchange.respond(CoAP.ResponseCode.NOT_ACCEPTABLE, "Unknown media type: " +
//                                                                                   options.getContentFormat());
//                            return;
//                    }
//
//                    request.setPayLoad(cn);
//                }
//
//
//                Onem2mPluginManager mgr = Onem2mPluginManager.getInstance();
//
//                String tmpUrl = options.getUriPathString();
//
//                LOG.info("Processed URL: {}", tmpUrl);
//                request.setUrl(tmpUrl);
//                switch (code) {
//                    case GET:
//                        request.setMethod("GET");
//                        break;
//
//                    case POST:
//                        request.setMethod("POST");
//                        break;
//
//                    case PUT:
//                        request.setMethod("PUT");
//                        break;
//
//                    case DELETE:
//                        request.setMethod("DELETE");
//                        break;
//                }
//
//
////                IotdmPlugin plg = (IotdmPlugin) mgr.getPlugin(this.coapServer.getCoapProvider().instanceKey, tmpUrl, "coap");
//                IotdmPlugin plg = pluginRegistry.getPlugin(tmpUrl);
//                if (plg != null) {
//                    plg.handle(request, response);
//                    coapExchange.respond(CoAP.ResponseCode.CONTENT,response.getResponsePayload());
//                }
//                else {
//                    coapExchange.respond(CoAP.ResponseCode.NOT_FOUND);
//                }
//            }
//        }
//    }
}
