/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.websocket;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmBaseRxCommunicationChannel;
import org.opendaylight.iotdm.onem2m.commchannels.common.IotdmPluginOnem2mBaseRequest;
import org.opendaylight.iotdm.onem2m.commchannels.common.IotdmPluginOnem2mBaseResponse;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPlugin;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxPluginsRegistryReadOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jkosmel
 */
class IotdmWebsocketPluginServerRx extends IotdmBaseRxCommunicationChannel {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmWebsocketPluginServerRx.class);
    private Server httpServer;
    public static final String ProtocolWebsocket = "websocket";

    IotdmWebsocketPluginServerRx(String ipAddress, int port,
                                 IotdmRxPluginsRegistryReadOnly registry) {
        super(ipAddress, port, registry, null, false);
    }

    @Override
    public String getProtocol() {
        return ProtocolWebsocket;
    }

    @Override
    public boolean init() {
        httpServer = new Server(port);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        httpServer.setHandler(context);

        try {
            Onem2mWebSocketBaseHandler handler = new Onem2mWebSocketBaseHandler();
            handler.setHandler(new DefaultHandler());
            httpServer.setHandler(handler);
            httpServer.start();
            LOG.info("startHttpWebsocketServer on port: {}", port);

        } catch (Exception e) {
            e.printStackTrace();
            LOG.info("Exception: {}", e.toString());
        }
        return true;
    }

    @Override
    public void close() throws Exception {
        try {
            httpServer.stop();
            LOG.info("stopHttpWebsocketServer on port: {}", port);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.info("Exception: {}", e.toString());
        }
    }

    private class Onem2mWebSocketBaseHandler extends WebSocketHandler {
        private final Set<WebSocket.Connection> connections = new CopyOnWriteArraySet<>();
        //private List<String> supportedSubProtocols = new ArrayList<>();

        @Override
        public WebSocket doWebSocketConnect(HttpServletRequest request,
                                            String protocol) {
//            List<String> swpHeaders =
//                    Collections.list(request.getHeaders("Sec-WebSocket-Protocol")) //TODO: uncomment when subprotocols are supported
//                            .stream()
//                            .flatMap(s -> Stream.of(s.split(",")))
//                            .map(String::trim)
//                            .collect(Collectors.toList());
//
//            supportedSubProtocols = swpHeaders.isEmpty() ? Collections.singletonList(Onem2m.SubProtocol.JSON) : swpHeaders;
//
//            if(supportedSubProtocols.size() == 1 && supportedSubProtocols.get(0).equalsIgnoreCase(Onem2m.SubProtocol.XML)) {
//                throw new IllegalArgumentException("Unsupported Sec-WebSocket-Protocol: "+Onem2m.SubProtocol.XML); //json only supported now
//            }

            return new WebSocketImpl();
        }

        private class WebSocketImpl implements WebSocket.OnTextMessage {
            private Connection connection;

            @Override
            public void onOpen(Connection connection) {
                this.connection = connection;
                connections.add(connection); //store connections for further notifications purposes
                LOG.info("Websocket connection successfully opened for: " + connection);
            }

            @Override
            public void onMessage(String wsMessage) {
                try {
                    LOG.info("Websocket message received from: "+ connection.toString());

                    if (StringUtils.isEmpty(wsMessage)) {
                        String msg = "Received websocket message is empty";
                        LOG.warn(msg);
                        this.connection.sendMessage(IotdmPluginOnem2mBaseResponse.buildErrorResponse(msg, Onem2m.ResponseStatusCode.BAD_REQUEST));
                        return;
                    }
                    IotdmPluginOnem2mBaseRequest request = new IotdmPluginOnem2mBaseRequest(wsMessage, Onem2m.SubProtocol.JSON);
                    IotdmPluginOnem2mBaseResponse response = new IotdmPluginOnem2mBaseResponse();

                    IotdmPlugin plg = pluginRegistry.getPlugin(request.getOnem2mUri());
                    if (plg != null) {
                        plg.handle(request, response);
                        this.connection.sendMessage(response.buildWebsocketResponse());
                    }
                    else {
                        String msg = "Websocket plugin not found";
                        LOG.warn(msg);
                        this.connection.sendMessage(IotdmPluginOnem2mBaseResponse.buildErrorResponse(msg, Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR));
                    }
                    LOG.info("Websocket message has been processed");
                } catch (Exception e) {
                    String msg = "Exception in websocket message processing";
                    LOG.warn(msg, e);
                    try {
                        this.connection.sendMessage(IotdmPluginOnem2mBaseResponse.buildErrorResponse(msg+": "+e.getMessage(), Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR));
                    } catch (IOException ioe) {
                        LOG.error("Error occured while sending websocket message", ioe);
                        //this.connection.close(); //TODO: should we close on error?
                    }
                }
            }

            @Override
            public void onClose(int closeCode, String message) {
                connections.remove(connection);
                LOG.info("Websocket connection has been closed: " + connection);
            }
        }
    }
}
