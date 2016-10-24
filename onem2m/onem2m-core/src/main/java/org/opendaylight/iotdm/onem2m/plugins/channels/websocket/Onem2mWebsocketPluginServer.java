/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins.channels.websocket;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPlugin;
import org.opendaylight.iotdm.onem2m.plugins.Onem2mPluginManager;
import org.opendaylight.iotdm.onem2m.plugins.channels.Onem2mBaseCommunicationChannel;
import org.opendaylight.iotdm.onem2m.plugins.registry.Onem2mLocalEndpointRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author jkosmel
 */
public class Onem2mWebsocketPluginServer extends Onem2mBaseCommunicationChannel {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mWebsocketPluginServer.class);
    private Server httpServer;
    private ServletContextHandler context;

    Onem2mWebsocketPluginServer(String ipAddress, int port,
                           Onem2mLocalEndpointRegistry registry) {
        super(ipAddress, port, registry, null, false);
    }

    @Override
    public String getProtocol() {
        return Onem2mPluginManager.ProtocolWebsocket;
    }

    @Override
    public boolean init() {
        httpServer = new Server(port);
        context = new ServletContextHandler();
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
        private String hostUrl;

        @Override
        public WebSocket doWebSocketConnect(HttpServletRequest request,
                                            String protocol) {
            List<String> swpHeaders =
                    Collections.list(request.getHeaders("Sec-WebSocket-Protocol"))
                            .stream()
                            .flatMap(s -> Stream.of(s.split(",")))
                            .map(String::trim)
                            .collect(Collectors.toList());

//            supportedSubProtocols = swpHeaders.isEmpty() ? Collections.singletonList(Onem2m.SubProtocol.JSON) : swpHeaders; //TODO: uncomment when subprotocols are supported
//
//            if(supportedSubProtocols.size() == 1 && supportedSubProtocols.get(0).equalsIgnoreCase(Onem2m.SubProtocol.XML)) {
//                throw new IllegalArgumentException("Unsupported Sec-WebSocket-Protocol: "+Onem2m.SubProtocol.XML); //json only supported now
//            }

            hostUrl = "ws://"+request.getHeader("Host");
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
                        this.connection.sendMessage(IotdmPluginWebsocketResponse.buildErrorResponse(msg, Onem2m.ResponseStatusCode.BAD_REQUEST));
                        return;
                    }
                    IotdmPluginWebsocketRequest request = new IotdmPluginWebsocketRequest(wsMessage, Onem2m.SubProtocol.JSON);
                    IotdmPluginWebsocketResponse response = new IotdmPluginWebsocketResponse();

                    IotdmPlugin plg = pluginRegistry.getPlugin(hostUrl);
                    if (plg != null) {
                        plg.handle(request, response);
                        this.connection.sendMessage(response.buildWebsocketResponse());
                    }
                    else {
                        String msg = "Websocket plugin not found";
                        LOG.warn(msg);
                        this.connection.sendMessage(IotdmPluginWebsocketResponse.buildErrorResponse(msg, Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR));
                    }
                    LOG.info("Websocket message has been processed");
                } catch (Exception e) {
                    String msg = "Exception in websocket message processing";
                    LOG.warn(msg, e);
                    try {
                        this.connection.sendMessage(IotdmPluginWebsocketResponse.buildErrorResponse(msg+": "+e.getMessage(), Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR));
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
