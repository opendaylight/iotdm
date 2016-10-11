/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins.channels.coap;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPlugin;
import org.opendaylight.iotdm.onem2m.plugins.Onem2mPluginManager;
import org.opendaylight.iotdm.onem2m.plugins.channels.Onem2mBaseCommunicationChannel;
import org.opendaylight.iotdm.onem2m.plugins.registry.Onem2mLocalEndpointRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static java.util.Objects.nonNull;

/**
 * Implementation of server specific for COAP protocol. No configuration needed.
 */
class Onem2mCoapPluginServer extends Onem2mCoapBaseChannel {
    Onem2mCoapPluginServer(String ipAddress, int port,
                           Onem2mLocalEndpointRegistry registry) {
        super(ipAddress, port, registry, null);
    }
}

/**
 * Implementation of generic class for COAP and COAPS servers.
 * @param <Tconfig> Type of the configuration of server.
 */
class Onem2mCoapBaseChannel<Tconfig> extends Onem2mBaseCommunicationChannel<Tconfig> {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCoapPluginServer.class);
    private Onem2mCoapBaseHandler onem2mCoapBaseHandler;

    Onem2mCoapBaseChannel(String ipAddress, int port,
                          Onem2mLocalEndpointRegistry registry,
                          Tconfig config) {
        super(ipAddress, port, registry, config);
    }

    @Override
    public boolean init() {
        onem2mCoapBaseHandler = new Onem2mCoapBaseHandler(port);
        onem2mCoapBaseHandler.addEndpoints();
        try {
            onem2mCoapBaseHandler.start();
            LOG.info("startCoapServer: on port: {}", port);

        } catch (Exception e) {
            e.printStackTrace();
            LOG.info("Exception: {}", e.toString());
        }
        return true;
    }

    @Override
    public void close() {
        try {
            onem2mCoapBaseHandler.stop();
            LOG.info("stopCoapServer: on port: {}", port);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.info("Exception: {}", e.toString());
        }
    }

    @Override
    public String getProtocol() {
        return "coap";
    }

    /**
     * Implementation of COAP server for the base COAP channel
     */
    private class Onem2mCoapBaseHandler extends CoapServer {
        private final Logger LOG = LoggerFactory.getLogger(Onem2mCoapBaseHandler.class);
        int port;

        Onem2mCoapBaseHandler(int port){
            super(port);
            this.port = port;
        }

        @Override
        public Resource createRoot() {
            return new RootResource(this);
        }

        void addEndpoints() {
            for (InetAddress addr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
                // only binds to IPv4 addresses and localhost
                if (addr instanceof Inet4Address || addr.isLoopbackAddress()) {
                    System.out.println("addr: "+addr.toString());
                    InetSocketAddress bindToAddress = new InetSocketAddress(addr, port);
                    System.out.println("bindToAddress: "+bindToAddress.toString());
                    addEndpoint(new CoapEndpoint(bindToAddress));
                }
            }
        }

        private class RootResource extends CoapResource {
            Onem2mCoapBaseHandler coapServer;
            RootResource(Onem2mCoapBaseHandler cServer)
            {
                super("OpenDaylight OneM2M CoAP Server");
                this.coapServer = cServer;
            }

            @Override
            public Resource getChild(String name) {
                return this;
            }

            /**
             * The handler for the CoAP request
             *
             * @param exchange coap parameters
             */
            @Override
            public void handleRequest(final Exchange exchange) {
                CoapExchange coapExchange = new CoapExchange(exchange, this);
                OptionSet options = coapExchange.advanced().getRequest().getOptions();
                IotdmPluginCoapRequest request = new IotdmPluginCoapRequest(exchange);
                IotdmPluginCoapResponse response = new IotdmPluginCoapResponse();

                LOG.info("CoapServer - Handle Request: on port: {}", options.getUriPort());

                // take the entire payload text and put it in the CONTENT field; it is the representation of the resource
                String cn = coapExchange.getRequestText().trim();
                if (!cn.contentEquals("")) {
                    switch (options.getContentFormat()) {
                        case MediaTypeRegistry.APPLICATION_JSON:
                        case MediaTypeRegistry.APPLICATION_XML:
                        case Onem2m.CoapContentFormat.APP_VND_RES_JSON:
                        case Onem2m.CoapContentFormat.APP_VND_RES_XML:
                            break;
                        default:
                            response.prepareErrorResponse(CoAP.ResponseCode.NOT_ACCEPTABLE, "Unknown media type: " +
                                    options.getContentFormat());
                            coapExchange.respond(response.buildCoapResponse());
                            return;
                    }
                    request.setPayLoad(cn);
                }

                Onem2mPluginManager mgr = Onem2mPluginManager.getInstance();

                IotdmPlugin plg = pluginRegistry.getPlugin(options.getUriPathString());
                if (nonNull(plg)) {
                    plg.handle(request, response);
                    coapExchange.respond(response.buildCoapResponse());
                }
                else {
                    coapExchange.respond(CoAP.ResponseCode.NOT_FOUND);
                }
            }
        }
    }
}
