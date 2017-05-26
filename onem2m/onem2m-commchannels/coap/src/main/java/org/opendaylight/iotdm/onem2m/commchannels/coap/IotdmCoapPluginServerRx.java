/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.coap;

import static java.util.Objects.nonNull;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmBaseRxCommunicationChannel;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxPluginsRegistryReadOnly;
import org.opendaylight.iotdm.plugininfra.commchannels.common.IotdmHandlerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of server specific for COAP protocol. No configuration needed.
 */
public class IotdmCoapPluginServerRx extends IotdmCoapBaseRxChannel {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmCoapPluginServerRx.class);

    public static final String ProtocolCoAP = "coap";

    IotdmCoapPluginServerRx(String ipAddress, int port,
                            IotdmRxPluginsRegistryReadOnly registry) {
        super(ipAddress, port, registry, null, false);
    }

    @Override
    public boolean init() {
        onem2mCoapBaseHandler = new Onem2mCoapBaseHandler(port);
        onem2mCoapBaseHandler.addEndpoints();

        try {
            onem2mCoapBaseHandler.start();
            this.setState(ChannelState.RUNNING);
            LOG.info("Started CoAP Server: on port: {}", port);

        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Failed to start CoAP server: {}", e.toString());
            this.setState(ChannelState.INITFAILED);
        }
        return true;
    }

    @Override
    public void close() {
        if (null == onem2mCoapBaseHandler) {
            return;
        }
        try {
            onem2mCoapBaseHandler.stop();
            onem2mCoapBaseHandler.destroy();
            LOG.info("Stopped CoAP Server: on port: {}", port);
        } catch (Exception e) {
            LOG.error("Failed to stop CoAP server: {}", e.toString());
        }
    }
}

/**
 * Implementation of generic class for COAP and COAPS servers.
 * @param <Tconfig> Type of the configuration of server.
 */
abstract class IotdmCoapBaseRxChannel<Tconfig> extends IotdmBaseRxCommunicationChannel<Tconfig> {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmCoapBaseRxChannel.class);
    protected Onem2mCoapBaseHandler onem2mCoapBaseHandler = null;

    IotdmCoapBaseRxChannel(String ipAddress, int port,
                           IotdmRxPluginsRegistryReadOnly registry,
                           Tconfig config, boolean usesDefaultCfg) {
        super(ipAddress, port, registry, config, usesDefaultCfg);
    }

    @Override
    public String getProtocol() {
        return IotdmCoapPluginServerRx.ProtocolCoAP; //Onem2mPluginManager.ProtocolCoAP;
    }

    /**
     * Implementation of COAP server for the base COAP channel
     */
    protected class Onem2mCoapBaseHandler extends CoapServer {

        protected Onem2mCoapBaseHandler() {
            // Do not call constructor of superclass here !!!
            // Otherwise DTLS will not work in subclasses implementing CoAPS !!!
        }

        public Onem2mCoapBaseHandler(int port){
            super(port);
        }

        @Override
        public Resource createRoot() {
            return new RootResource();// (this);
        }

        protected void addEndpoints() {
            addEndpoint(new CoapEndpoint(NetworkConfig.getStandard()));
        }

//        void addEndpoints() {
//            for (InetAddress addr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
//                // only binds to IPv4 addresses and localhost
//                if (addr instanceof Inet4Address || addr.isLoopbackAddress()) {
//                    LOG.info("CoAP server: addr: "+addr.toString());
//                    InetSocketAddress bindToAddress = new InetSocketAddress(addr, port);
//                    LOG.info("CoAP server: bindToAddress: "+bindToAddress.toString());
//                    addEndpoint(new CoapEndpoint(bindToAddress));
//                }
//            }
//        }

        protected class RootResource extends CoapResource {
            protected RootResource(String serverMessage) {
                super(serverMessage);
            }

            public RootResource()
            {
                super("OpenDaylight OneM2M CoAP Server");
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

                LOG.trace("CoapServer - Handle Request: on port: {}", options.getUriPort());

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
                            response.prepareErrorResponse(Onem2m.ResponseStatusCode.NOT_ACCEPTABLE,
                                                          "Unknown media type: " + options.getContentFormat());
                            coapExchange.respond(response.buildCoapResponse());
                            return;
                    }
                    request.setPayLoad(cn);
                }

                IotdmHandlerPlugin plg = pluginRegistry.getPlugin(request.getOnem2mUri());
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
