/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.http;

import java.io.IOException;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpDescriptor;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpPlugin;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpRegistry;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpRequest;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpResponse;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors.IotdmRxDescriptorApplicationEndpoint;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors.IotdmRxDescriptorApplicationInterface;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors.IotdmRxDescriptorApplicationProtocol;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmBaseRxCommunicationChannel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.communication.channel.data.definition.channel.data.ChannelConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of server specific for HTTP protocol. No configuration needed.
 */
public class Onem2mHttpRxChannel extends Onem2mHttpBaseRxChannel {
    public static final String ProtocolHTTP = "http";
    public Onem2mHttpRxChannel(Onem2mHttpDescriptor appDescriptor,
                               Onem2mHttpRegistry pluginRegistry) {
        super(appDescriptor.getInterfaceDescriptor().getIpAddress(),
              appDescriptor.getInterfaceDescriptor().getPortNumber(),
              pluginRegistry);

        IotdmRxDescriptorApplicationInterface interfaceDescriptor = appDescriptor.getInterfaceDescriptor();
        IotdmRxDescriptorApplicationProtocol protocolDescriptor = appDescriptor.getProtocolDescriptor();
        IotdmRxDescriptorApplicationEndpoint appEndpointDesc = appDescriptor.getEndpointDescriptor();
    }

    @Override
    public ChannelConfiguration getChannelConfig() {
        // TODO need to augment yang model an build the augmented data here
        return null;
    }
}

/**
 * Implementation of generic class for HTTP and HTTPS servers.
 */
abstract class Onem2mHttpBaseRxChannel extends IotdmBaseRxCommunicationChannel {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpRxChannel.class);

    protected final String ipAddress;
    protected final int port;
    protected final Onem2mHttpRegistry pluginRegistry;

    protected Server httpServer;
    private FilterHolder cors;
    private ServletContextHandler context;
    private Onem2mHttpBaseHandler onem2mHttpBaseHandler;

    public Onem2mHttpBaseRxChannel(String ipAddress, int port,
                                   Onem2mHttpRegistry pluginRegistry) {



        this.ipAddress = ipAddress; //interfaceDescriptor.getIpAddress();
        this.port = port; //interfaceDescriptor.getPortNumber();

        this.pluginRegistry = pluginRegistry;
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
    public void init() {
        httpServer = new Server(port);

        // Prepare the httpServer instance
        this.prepareServer();

        // Start the prepared server
        if (this.startServer()) {
            this.setState(ChannelState.RUNNING);
        } else {
            this.setState(ChannelState.INITFAILED);
        }
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


//    @Override
//    public ChannelConfiguration getChannelConfig() {
//        // TODO need to augment yang model an build the augmented data here
//        return null;
//    }

    /**
     * Implementation of HTTP servlet for the base HTTP channel
     */
    class Onem2mHttpBaseHandler extends HttpServlet {
        private final Logger LOG = LoggerFactory.getLogger(Onem2mHttpBaseHandler.class);

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            Onem2mHttpRequest request = new Onem2mHttpRequest(req);
            Onem2mHttpResponse response = new Onem2mHttpResponse(resp);

            try {
                Onem2mHttpPlugin plg = pluginRegistry.getPlugin(request.getOnem2mUri());
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
