/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.simpleadapter.impl;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2msimpleadapter.rev160210.onem2m.simple.adapter.config.SimpleAdapterDesc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mSimpleAdapterHttpServer {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mSimpleAdapterHttpServer.class);
    private HashMap<Integer,HttpServerParms> httpMap;

    private String response;
    private int httpRSC;
    private Onem2mSimpleAdapterManager onem2mSimpleAdapterManager = null;

    public Onem2mSimpleAdapterHttpServer(Onem2mSimpleAdapterManager onem2mSimpleAdapterManager) {
        httpMap = new HashMap<Integer,HttpServerParms>();
        this.onem2mSimpleAdapterManager = onem2mSimpleAdapterManager;
    }



    public void startHttpServer(SimpleAdapterDesc simpleAdapterDesc) {

        Integer port = simpleAdapterDesc.getHttpServerPort().intValue();
        HttpServerParms hp = httpMap.get(port);
        if (hp == null) {
            Server server = new Server(port);
            server.setHandler(new Onem2mSimpleAdapterHttpHandler());
            hp = new HttpServerParms(port, server);
            httpMap.put(port, hp);
            try {
                server.start();
            } catch (Exception e) {
                LOG.info("Exception: {}", e.toString());
            }
        } else {
            hp.count++;
        }
    }

    public void stopHttpServer(SimpleAdapterDesc simpleAdapterDesc) {

        Integer port = simpleAdapterDesc.getHttpServerPort().intValue();
        HttpServerParms hp = httpMap.get(port);
        if (hp != null) {
            if (--hp.count == 0) {
                try {
                    hp.server.stop();
                } catch (Exception e) {
                    LOG.info("Exception: {}", e.toString());
                }
                httpMap.remove(port);
            }
        }
    }

    private class HttpServerParms {
        private long port;
        private Server server;
        private long count;
        protected HttpServerParms(long port, Server server) {
            this.port = port;
            this.server = server;
            this.count = 1;
        }
    }

    public class Onem2mSimpleAdapterHttpHandler extends AbstractHandler {

        @Override
        public void handle(String target, Request baseRequest,
                           HttpServletRequest httpRequest,
                           HttpServletResponse httpResponse) throws IOException, ServletException {

            response = null;
            httpRSC = HttpServletResponse.SC_OK;
            String method = baseRequest.getMethod().toLowerCase();
            String uri = baseRequest.getRequestURI();
            String payload = IOUtils.toString(baseRequest.getInputStream()).trim();

            LOG.info("handle: received http message: start");
            LOG.info("Method {}", method);
            LOG.info("URI {}", uri);
            LOG.info("RemoteAddr {}", baseRequest.getRemoteAddr());
            for (Enumeration<String> e = baseRequest.getHeaderNames(); e.hasMoreElements(); ) {
                String header = e.nextElement();
                LOG.info("Header: {}, Value: {}", header, baseRequest.getHeader(header));
            }
            LOG.info("payload: {}", payload);

            if (method.compareToIgnoreCase("POST") == 0 || method.compareToIgnoreCase("PUT") == 0) {
                processHttpMessage(baseRequest, uri, payload);
            }

            sendHttpResponse(httpResponse);
            baseRequest.setHandled(true);

            LOG.info("handle: received http message: end");
        }
    }

    private boolean processHttpMessage(Request baseRequest, String uri, String payload) {

        uri = trim(uri);

        // verify uri is configured as an entry in the simple adapter and it is in the onem2m datastore
        SimpleAdapterDesc simpleAdapterDesc = onem2mSimpleAdapterManager.findDescriptorUsingUri(uri);
        if (simpleAdapterDesc == null) {
            setErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "onem2m target uri not configured: " + uri);
            return false;
        }

        String onem2mContainerHeaderValue = null;
        String onem2mContainerHeaderName = simpleAdapterDesc.getOnem2mContainerHttpHeaderName();
        if (onem2mContainerHeaderName != null) {
            onem2mContainerHeaderValue = baseRequest.getHeader(onem2mContainerHeaderName);
            if (onem2mContainerHeaderValue == null) {
                setErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "missing expected header: " + onem2mContainerHeaderName);
                return false;
            }
        }

        String error = onem2mSimpleAdapterManager.processUriAndPayload(simpleAdapterDesc, uri, payload, onem2mContainerHeaderValue);
        if (error != null) {
            setErrorResponse(HttpServletResponse.SC_BAD_REQUEST, error);
            return false;
        }

        return true;
    }

    private String trim(String stringWithSlashes) {

        stringWithSlashes = stringWithSlashes.trim();
        stringWithSlashes = stringWithSlashes.startsWith("/") ?
                stringWithSlashes.substring("/".length()) : stringWithSlashes;
        stringWithSlashes = stringWithSlashes.endsWith("/") ?
                stringWithSlashes.substring(0,stringWithSlashes.length()-1) : stringWithSlashes;
        return stringWithSlashes;
    }

    private void setErrorResponse(int rsc, String content) {
        httpRSC = rsc;
        response = "{\"error\":\"" + content + "\"}";
    }

    private void sendHttpResponse(HttpServletResponse httpResponse) throws IOException {

        if (response != null) {
            httpResponse.setStatus(httpRSC);
            httpResponse.getWriter().println(response);
        } else {
            httpResponse.setStatus(httpRSC);
        }
        httpResponse.setContentType("text/json;charset=utf-8");
    }
}