/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.*;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;


interface AbstractOnem2mProtocolProvider {

    public int init(int port, Onem2mPluginManager.Mode operatingMode);

    public void cleanup();

    public int getPort();

    public String getProtocol();

    public Onem2mPluginManager.Mode getInstanceMode();
}

class Onem2mHttpBaseHandler extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpBaseHandler.class);
    private Onem2mHTTPProvider provider;

    public Onem2mHttpBaseHandler(Onem2mHTTPProvider provider) {
        this.provider = provider;
    }


    public static String getFullURL(HttpServletRequest request) {
        StringBuffer requestURL = request.getRequestURL();
        String queryString = request.getQueryString();

        if (queryString == null) {
            return requestURL.toString();
        } else {
            return requestURL.append('?').append(queryString).toString();
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //super.service(req, resp);
        IotDMPluginHttpRequest request = new IotDMPluginHttpRequest();
        IotDMPluginHttpResponse response = new IotDMPluginHttpResponse();
        request.setMethod(req.getMethod().toLowerCase());
        LOG.info("service called");

        //headers
        HashMap<String, String> headers = new HashMap<String, String>();
        Enumeration headerKeys = req.getHeaderNames();
        String header;
        while (headerKeys.hasMoreElements()) {
            header = (String) headerKeys.nextElement();
            request.addHeader(header, req.getHeader(header));
        }
        request.setHttpRequest(req);
        response.setHttpResponse(resp);


        String payload;
        // Read from request
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        String tmpUrl;

        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        request.setPayLoad(buffer.toString());

        Onem2mPluginManager mgr = Onem2mPluginManager.getInstance();

        try {
            URL urlSplit;
            urlSplit = new URL(getFullURL(req));
            tmpUrl = trim(urlSplit.getPath());
            request.setUrl(tmpUrl);
            LOG.info("Processed URL", tmpUrl);

            AbstractIotDMPlugin plg = (AbstractIotDMPlugin) mgr.getPlugin(this.provider.instanceKey, tmpUrl,"http");
            if (plg != null) {
                plg.handle(request, response);
                if ( response.getResponsePayload()!= null)
                {
                    resp.getWriter().println(response.getResponsePayload());
                }
                resp.setContentType("text/json;charset=utf-8");
                if (response.getReturnCode() != -1) {
                    resp.setStatus(response.getReturnCode());
                }
            }
            else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }

        } catch (Exception e) {
            e.printStackTrace();
            LOG.info("Exception: {}", e.toString());
        }
   }

    private String trim(String stringWithSlashes) {

        stringWithSlashes = stringWithSlashes.trim();
        stringWithSlashes = stringWithSlashes.startsWith("/") ?
                stringWithSlashes.substring("/".length()) : stringWithSlashes;
        stringWithSlashes = stringWithSlashes.endsWith("/") ?
                stringWithSlashes.substring(0, stringWithSlashes.length() - 1) : stringWithSlashes;
        return stringWithSlashes;
    }

}

class Onem2mBaseProvider implements AbstractOnem2mProtocolProvider {

    public String getInstanceKey() {
        return instanceKey;
    }
    String instanceKey;

    public Onem2mPluginManager.Mode getInstanceMode() {
        return instanceMode;
    }
    public void setInstanceMode(Onem2mPluginManager.Mode mode) {
        instanceMode = mode;
    }
    Onem2mPluginManager.Mode instanceMode;

    public Onem2mBaseProvider() {
        this.instanceKey = UUID.randomUUID().toString();
    }
    @Override
    public int init(int port, Onem2mPluginManager.Mode operatingMode) {
        return 0;
    }
    @Override
    public void cleanup() {
    }
    @Override
    public int getPort() {
        return 0;
    }
    @Override
    public String getProtocol() {
        return null;
    }
}

class CoapServerProvider extends CoapServer {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCoapProvider.class);
    Onem2mBaseProvider provider;
    int port;

    CoapServerProvider(int port,Onem2mBaseProvider provider){
        super(port);
        this.port = port;
        this.provider = provider;

    }

    Onem2mBaseProvider getCoapProvider()
    {
        return provider;
    }

    @Override
    public Resource createRoot() {
        return new RootResource(this);
    }

    public void addEndpoints() {
        for (InetAddress addr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
            // only binds to IPv4 addresses and localhost
            if (addr instanceof Inet4Address || addr.isLoopbackAddress()) {
                LOG.info("addr: "+addr.toString());
                InetSocketAddress bindToAddress = new InetSocketAddress(addr, port);
                LOG.info("bindToAddress: "+bindToAddress.toString());
                addEndpoint(new CoapEndpoint(bindToAddress));
            }
        }
    }

    private class RootResource extends CoapResource {
        CoapServerProvider coapServer;
        Onem2mBaseProvider provider;
        public RootResource(CoapServerProvider cServer)
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
            LOG.info("CoapServer - Handle Request: on port: {}");
            CoAP.Code code = exchange.getRequest().getCode();
            CoapExchange coapExchange = new CoapExchange(exchange, this);
            OptionSet options = coapExchange.advanced().getRequest().getOptions();
            IotDMPluginRequest request = new IotDMPluginRequest();
            IotDMPluginResponse response = new IotDMPluginResponse();


            if ((options.getContentFormat() != MediaTypeRegistry.APPLICATION_JSON)||
                    (options.getContentFormat() == MediaTypeRegistry.APPLICATION_XML))
            {
                    coapExchange.respond(CoAP.ResponseCode.NOT_ACCEPTABLE, "Unknown media type: " +
                        options.getContentFormat());
                return;
            }


            // according to the spec, the uri query string can contain in short form, the
            // resourceType, responseType, result persistence,  Delivery Aggregation, Result Content,
            /*Boolean resourceTypePresent = options.getURIQueryString();

            if (resourceTypePresent && code != CoAP.Code.POST) {
                coapExchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Specifying resource type not permitted.");
                return;
            }*/

            // take the entire payload text and put it in the CONTENT field; it is the representation of the resource
            String cn = coapExchange.getRequestText().trim();
            if (cn != null && !cn.contentEquals("")) {
                request.setPayLoad(cn);
            }


            Onem2mPluginManager mgr = Onem2mPluginManager.getInstance();

            String tmpUrl = options.getUriPathString();
            LOG.info("Processed URL", tmpUrl);
            request.setUrl(tmpUrl);
            switch (code) {
                case GET:
                    request.setMethod("GET");
                    break;

                case POST:
                    request.setMethod("POST");
                    break;

                case PUT:
                    request.setMethod("PUT");
                    break;

                case DELETE:
                    request.setMethod("DELETE");
                    break;
            }


            AbstractIotDMPlugin plg = (AbstractIotDMPlugin) mgr.getPlugin(this.coapServer.getCoapProvider().instanceKey, tmpUrl,"coap");
            if (plg != null) {
                plg.handle(request, response);
                coapExchange.respond(CoAP.ResponseCode.CONTENT,response.getResponsePayload());
            }
            else {
                coapExchange.respond(CoAP.ResponseCode.NOT_FOUND);
            }
        }
    }
}
class Onem2mCoapProvider extends Onem2mBaseProvider {
    int __port;
    private CoapServerProvider server;

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCoapProvider.class);

    @Override
    public int init(int port, Onem2mPluginManager.Mode mode) {
        this.__port = port;
        this.instanceMode = mode;
        CoapServerProvider server = new CoapServerProvider(__port,this);
        server.addEndpoints();
        try {
            server.start();
            LOG.info("startCoapServer: on port: {}", __port);

        } catch (Exception e) {
            e.printStackTrace();
            LOG.info("Exception: {}", e.toString());
        }
        return 0;
    }

    @Override
    public void cleanup() {
        try {
            server.stop();
            LOG.info("stopCoapServer: on port: {}", __port);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.info("Exception: {}", e.toString());
        }
    }

    @Override
    public int getPort() {
        return this.__port;
    }

    @Override
    public String getProtocol() {
        return "coap";
    }
}
class Onem2mHTTPProvider extends Onem2mBaseProvider {
    static int __port;
    private Server httpServer;
    private FilterHolder cors;
    private ServletContextHandler context;
    private Onem2mHttpBaseHandler onem2mHttpBaseHandler;
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHTTPProvider.class);

    @Override
    public int init(int port, Onem2mPluginManager.Mode mode) {
        this.__port = port;
        this.instanceMode = mode;
        httpServer = new Server(__port);
        context = new ServletContextHandler();
        context.setContextPath("/");
        httpServer.setHandler(context);


        cors = context.addFilter(CrossOriginFilter.class, "*", EnumSet.of(DispatcherType.REQUEST));
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,HEAD");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin");
        onem2mHttpBaseHandler = new Onem2mHttpBaseHandler(this);
        context.addServlet(new ServletHolder(onem2mHttpBaseHandler), "/*");

        try {
            httpServer.start();
            LOG.info("startHttpServer: on port: {}", __port);

        } catch (Exception e) {
            e.printStackTrace();
            LOG.info("Exception: {}", e.toString());
        }
        return 0;
    }

    @Override
    public void cleanup() {
        try {
            httpServer.stop();
            LOG.info("stopHttpServer: on port: {}", __port);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.info("Exception: {}", e.toString());
        }
    }

    @Override
    public int getPort() {
        return this.__port;
    }

    @Override
    public String getProtocol() {
        return "http";
    }
}

interface AbstractOnem2mPluginManager {
    public AbstractIotDMPlugin getPlugin(String instanceKey, String url, String protocol);

    public ArrayList<String> getLoadedPlugins();

    public int registerPlugin(String protocol, AbstractIotDMPlugin instance, Onem2mPluginManager.Mode operatingMode);

    public int registerPluginAtPort(String protocol, AbstractIotDMPlugin intstnce, int port, Onem2mPluginManager.Mode mode);

    public int deRegisterPlugin(String pluginName);

    public int registerProtocol(String protocol, AbstractOnem2mProtocolProvider instance);

    public int deRegisterProtocol(String protocol);
}

class IotDMSamplePlugin implements AbstractIotDMPlugin, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(IotDMSamplePlugin.class);

    @Override
    public void init() {
        //Onem2mPluginManager.getInstance().registerPlugin("http", this, Onem2mPluginManager.Mode.Shared);
        Onem2mPluginManager.getInstance().registerPluginAtPort("http", this, 8284,Onem2mPluginManager.Mode.Shared);
        Onem2mPluginManager.getInstance().registerPluginAtPort("http", this, 8284,Onem2mPluginManager.Mode.Exclusive);
        //Onem2mPluginManager.getInstance().registerPluginAtPort("coap", this, 8285,Onem2mPluginManager.Mode.Exclusive);
    }

    @Override
    public void cleanup() {
        Onem2mPluginManager.getInstance().deRegisterPlugin(this.pluginName());
    }


    @Override
    public String pluginName() {
        return "example";
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public void handle(IotDMPluginRequest request, IotDMPluginResponse response) {
        LOG.info("Handle() method called");
    }
}

public class Onem2mPluginManager implements AbstractOnem2mPluginManager, AutoCloseable {

    static Onem2mPluginManager _instance;

    public enum Mode {
        NotInUse, Shared, Exclusive
    }


    private static final Logger LOG = LoggerFactory.getLogger(Onem2mPluginManager.class);
    //TODU callback model support for multiple plugin for same path
    static HashMap<String, AbstractIotDMPlugin> registeredPlugin;
    static HashMap<String, AbstractOnem2mProtocolProvider> registeredProtocolList;
    static ArrayList<Onem2mBaseProvider> protocolProviders;
    static HashMap<String, ArrayList<AbstractIotDMPlugin>> pluginProviderBinding;


    public Onem2mBaseProvider getProvider(String protocolName, Onem2mPluginManager.Mode operatingMode) {
        for (Onem2mBaseProvider provider : protocolProviders) {
            if (provider.getProtocol().equals(protocolName) && provider.getInstanceMode() == operatingMode) {
                return provider;
            }
        }
        return null;
    }

    public AbstractOnem2mProtocolProvider getProviderAtPort(int port) {
        for (AbstractOnem2mProtocolProvider provider : protocolProviders) {
            if  (provider.getPort() == port ) {
                return provider;
            }
        }
        return null;
    }

    static public Onem2mPluginManager getInstance() {
        if (_instance == null) {
            _instance = new Onem2mPluginManager();
            // TODO: Do we need this?
            registeredPlugin = new HashMap<String, AbstractIotDMPlugin>();
            // TODO: Do we need this?
            registeredProtocolList = new HashMap<String, AbstractOnem2mProtocolProvider>();
            // TODO: Do we need this?
            protocolProviders = new ArrayList<Onem2mBaseProvider>();

            pluginProviderBinding = new HashMap<String, ArrayList<AbstractIotDMPlugin>>();

            // TODO to be removed later
            IotDMSamplePlugin plg = new IotDMSamplePlugin();
            plg.init();


        }
        return _instance;
    }

    @Override
    public AbstractIotDMPlugin getPlugin(String instanceKey, String url, String protocol) {
        LOG.info("getplugin called");

        ArrayList<AbstractIotDMPlugin> pluginList = pluginProviderBinding.get(instanceKey);
        if (pluginList != null) {
            if (pluginList.size() > 1) {
                for (AbstractIotDMPlugin plugin : pluginList) {
                    String path[] = url.split("/");
                    // here process the url to resolve the plugin
                    if (plugin.pluginName().equals(path[0]))
                            return plugin;

                }
            }
            else
            {
                // there is only one plugin return the same
                return pluginList.get(0);
            }
        }
        return null;
    }

    public AbstractIotDMPlugin getPluginForPath(String instanceKey, String path) {
        LOG.info("getplugin called");
        ArrayList<AbstractIotDMPlugin> pluginList = pluginProviderBinding.get(instanceKey);
        if (pluginList != null) {
            if (pluginList.size() > 1) {
                return pluginList.get(0);
            }
            for (AbstractIotDMPlugin plugin : pluginList) {
                if (plugin.pluginName().equals(path)) {
                    return plugin;
                }
            }
        }
        return null;
    }

    @Override
    public ArrayList<String> getLoadedPlugins() {
        return (ArrayList<String>) registeredPlugin.keySet();
    }

    public ArrayList<String> getLoadedProtocols() {
        return (ArrayList<String>) registeredProtocolList.keySet();
    }

    public boolean isProtocolLoaded(String protocol) {
        return registeredProtocolList.keySet().contains(protocol);
    }

    @Override
    public int registerPlugin(String protocol, AbstractIotDMPlugin instance, Onem2mPluginManager.Mode operatingMode) {
        // TODO Exception handling
        int rc = 1;
        do {
            if (instance.pluginName() == null) {
                break;
            }
            Onem2mBaseProvider provider = getProvider(protocol, operatingMode);
            ArrayList<AbstractIotDMPlugin> tmp = pluginProviderBinding.get(provider.getInstanceKey());
            if (tmp == null) {
                tmp = new ArrayList<AbstractIotDMPlugin>();
                pluginProviderBinding.put(provider.getInstanceKey(), tmp);
            } else {
                for (AbstractIotDMPlugin p : tmp) {
                    if (p.pluginName().equals(instance.pluginName())) {
                        rc = 2;
                        break;
                    }
                }
            }
            tmp.add(instance);
            rc = 0;
        } while (false);
        return rc;
    }

    public AbstractOnem2mProtocolProvider createProvider(String protocol, AbstractIotDMPlugin instance, int port, Onem2mPluginManager.Mode mode) {
        Onem2mBaseProvider provider;
        if ( protocol.equals("http")) {
            provider = new Onem2mHTTPProvider();
            LOG.info("Onem2mHTTPProvider {}", provider.getInstanceKey());
        }
        else if ( protocol.equals("coap"))
        {
            provider = new Onem2mCoapProvider();
            LOG.info("Onem2mCoapProvider {}", provider.getInstanceKey());
        }
        else
        {
            return null;
        }
        provider.init(port, mode);
        protocolProviders.add(provider);
        this.registerProtocol(protocol, provider);
        ArrayList<AbstractIotDMPlugin> tmp = new ArrayList<AbstractIotDMPlugin>();
        pluginProviderBinding.put(provider.getInstanceKey(), tmp);
        tmp.add(instance);
        return provider;
    }

    @Override
    public int registerPluginAtPort(String protocol, AbstractIotDMPlugin instance, int port, Onem2mPluginManager.Mode mode) {
        int rc = 1;
        Onem2mBaseProvider provider = (Onem2mBaseProvider) this.getProviderAtPort(port);
        // port is not in use, no provider at the port, hence create one
        if (provider == null) {
            createProvider(protocol, instance, port, mode);
            return 0;
        }
        if (!(provider.getProtocol().equals(protocol)))
        {

            // provider already in use for a different protocol return error
            rc = 3;
            return rc;
        }
        //TODO: Check for port duplication

        // if the port is in use and the req mode is exclusive
        //      reject
        //if the port is in use and the req mode is shared
        //      check if the plugin name is same
        //           reject

        ArrayList<AbstractIotDMPlugin> tmp = pluginProviderBinding.get(provider.getInstanceKey());
        if (tmp == null) {
            // there is a provider at the port, but no one is using
            // reuse
            // Set the right mode
            provider.setInstanceMode(mode);
            tmp = new ArrayList<AbstractIotDMPlugin>();
            pluginProviderBinding.put(provider.getInstanceKey(), tmp);

        } else {
            // plugin cannot be added if the provider is already in exclusive mode
            // plugin cannot be added if the provider is already shared.
            if ((mode == Mode.Exclusive) || (provider.getInstanceMode()== Mode.Exclusive)) {
                //port is already in use, cannot use exclusively
                // reject
                rc = 2;
                return 2;
            }
            for (AbstractIotDMPlugin p : tmp) {
                if (p.pluginName().equals(instance.pluginName())) {
                    rc = 2;
                    return 2; //duplicate error
                }
            }
        }
        tmp.add(instance);
        return 0;
    }


    @Override
    public int deRegisterPlugin(String pluginName) {
        if (registeredPlugin.containsKey(pluginName)) {
            registeredPlugin.remove(registeredPlugin.get(pluginName));
        }
        return 0;
    }

    @Override
    public int registerProtocol(String protocol, AbstractOnem2mProtocolProvider instance) {
        if (registeredProtocolList.get(protocol) == null) {
            registeredProtocolList.put(protocol, instance);
        } else {
            return 1;
        }
        return 0;
    }

    @Override
    public int deRegisterProtocol(String protocol) {
        registeredProtocolList.remove(protocol);
        return 0;
    }

    @Override
    public void close() throws Exception {

    }
}
