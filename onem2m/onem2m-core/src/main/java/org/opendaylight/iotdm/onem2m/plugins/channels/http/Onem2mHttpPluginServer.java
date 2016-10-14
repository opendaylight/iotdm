/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins.channels.http;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.opendaylight.iotdm.onem2m.plugins.*;
import org.opendaylight.iotdm.onem2m.plugins.channels.Onem2mBaseCommunicationChannel;
import org.opendaylight.iotdm.onem2m.plugins.registry.Onem2mLocalEndpointRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;

/**
 * Implementation of server specific for HTTP protocol. No configuration needed.
 */
public class Onem2mHttpPluginServer extends Onem2mHttpBaseChannel {
    public Onem2mHttpPluginServer(String ipAddress, int port,
                                 Onem2mLocalEndpointRegistry registry) {
        super(ipAddress, port, registry, null, false);
    }
}

/**
 * Implementation of generic class for HTTP and HTTPS servers.
 * @param <Tconfig> Type of the configuration of server.
 */
class Onem2mHttpBaseChannel<Tconfig> extends Onem2mBaseCommunicationChannel<Tconfig> {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpPluginServer.class);
    protected Server httpServer;
    private FilterHolder cors;
    private ServletContextHandler context;
    private Onem2mHttpBaseHandler onem2mHttpBaseHandler;

    public Onem2mHttpBaseChannel(String ipAddress, int port,
                                 Onem2mLocalEndpointRegistry registry,
                                 Tconfig config, boolean usesDefaultCfg) {
        super(ipAddress, port, registry, config, usesDefaultCfg);
    }

    protected void prepareServer() {
        context = new ServletContextHandler();
        context.setContextPath("/");
        httpServer.setHandler(context);

        cors = context.addFilter(CrossOriginFilter.class, "*", EnumSet.of(DispatcherType.REQUEST));
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM, "false");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,DELETE,PUT,HEAD");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM,
                              "X-Requested-With,Content-Type,Accept,Origin,X-M2M-Origin,X-M2M-RI,X-M2M-NM," +
                              "X-M2M-GID,X-M2M-RTU,X-M2M-OT,X-M2M-RST,X-M2M-RET,X-M2M-OET,X-M2M-EC,X-M2M-RSC");
        cors.setInitParameter(CrossOriginFilter.EXPOSED_HEADERS_PARAM,
                              "X-Requested-With,Content-Type,Accept,Origin,X-M2M-Origin,X-M2M-RI,X-M2M-NM," +
                              "X-M2M-GID,X-M2M-RTU,X-M2M-OT,X-M2M-RST,X-M2M-RET,X-M2M-OET,X-M2M-EC,X-M2M-RSC");
        onem2mHttpBaseHandler = new Onem2mHttpBaseHandler();
        context.addServlet(new ServletHolder(onem2mHttpBaseHandler), "/*");
    }

    protected boolean startServer() {
        try {
            httpServer.start();
            LOG.info("startHttpServer: on port: {}", port);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.info("Exception: {}", e.toString());
            return false;
        }
    }

    @Override
    public boolean init() {
        httpServer = new Server(port);

        // Prepare the httpServer instance
        this.prepareServer();

        // Start the prepared server
        if (this.startServer()) {
            this.setState(ChannelState.RUNNING);
        } else {
            this.setState(ChannelState.INITFAILED);
        }
        return true;
    }

    @Override
    public void close() {
        if (null == httpServer) {
            return;
        }

        try {
            httpServer.stop();
            LOG.info("stopHttpServer: on port: {}", port);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.info("Exception: {}", e.toString());
        }
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    /**
     * Implementation of HTTP servlet for the base HTTP channel
     */
    class Onem2mHttpBaseHandler extends HttpServlet {
        private final Logger LOG = LoggerFactory.getLogger(Onem2mHttpBaseHandler.class);

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            IotdmPluginHttpRequest request = new IotdmPluginHttpRequest(req);
            IotdmPluginHttpResponse response = new IotdmPluginHttpResponse(resp);

            try {
                IotdmPlugin plg = pluginRegistry.getPlugin(request.getOnem2mUri());
                if (plg != null) {
                    plg.handle(request, response);
                }
                else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }

            } catch (Exception e) {
                e.printStackTrace();
                LOG.trace("Exception: {}", e.toString());
            }
       }
    }
}
