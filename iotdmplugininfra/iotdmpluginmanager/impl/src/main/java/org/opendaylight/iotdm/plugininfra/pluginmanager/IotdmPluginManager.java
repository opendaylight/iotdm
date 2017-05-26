/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager;

import org.opendaylight.iotdm.onem2m.commchannels.coap.IotdmCoapsConfigBuilder;
import org.opendaylight.iotdm.onem2m.commchannels.coap.Onem2mCoapPluginServerFactory;
import org.opendaylight.iotdm.onem2m.commchannels.coap.Onem2mCoapsPluginServerFactory;
import org.opendaylight.iotdm.onem2m.commchannels.common.Onem2mPluginChannelFactory;
import org.opendaylight.iotdm.onem2m.commchannels.mqtt.Onem2mMqttPluginClientFactory;
import org.opendaylight.iotdm.onem2m.commchannels.websocket.Onem2mWebsocketPluginServerFactory;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmBaseRxCommunicationChannel;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPlugin;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginLoader;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;
import org.opendaylight.iotdm.plugininfra.pluginmanager.customservices.PluginManagerServicesUtils;
import org.opendaylight.iotdm.plugininfra.pluginmanager.registry.IotdmRxPluginsRegistry;
import org.opendaylight.iotdm.plugininfra.pluginmanager.registry.IotdmRxPluginsRegistryExclusive;
import org.opendaylight.iotdm.plugininfra.pluginmanager.registry.IotdmRxPluginsRegistrySharedExactMatch;
import org.opendaylight.iotdm.plugininfra.pluginmanager.registry.IotdmRxPluginsRegistrySharedPrefixMatch;
import org.opendaylight.iotdm.plugininfra.pluginmanager.simpleconfig.IotdmPluginSimpleConfigClient;
import org.opendaylight.iotdm.plugininfra.pluginmanager.simpleconfig.IotdmPluginsSimpleConfigManager;
import org.opendaylight.iotdm.plugininfra.pluginmanager.simpleconfig.IotdmPluginsSimpleConfigProvider;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginConfigurationBuilder;
import org.opendaylight.iotdm.onem2m.commchannels.http.IotdmHttpsConfigBuilder;
import org.opendaylight.iotdm.onem2m.commchannels.http.Onem2mHttpPluginServerFactory;
import org.opendaylight.iotdm.onem2m.commchannels.http.Onem2mHttpsPluginServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Implements the registration and un-registration methods for IotdmPlugin instances and
 * stores registrations in registry according to registration mode specified in registration.
 * PluginManager is implemented as singleton.
 */
public class IotdmPluginManager implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmPluginManager.class);
    private static IotdmPluginManager _instance;
    private static IotdmPluginManagerProvider serviceProvider = null;
    private static IotdmPluginsSimpleConfigProvider simpleConfigProvider = null;

    private final Map<String, IotdmPluginLoader> pluginLoaders = new ConcurrentHashMap<>();

    /* Specific IP address which means that plugin registers for all
     * local interfaces
     */
    private static final String AllInterfaces = "0.0.0.0";

    // Supported protocols
    public static final String ProtocolHTTP = "http";
    public static final String ProtocolHTTPS = "https";
    public static final String ProtocolCoAP = "coap";
    public static final String ProtocolCoAPS = "coaps";
    public static final String ProtocolWebsocket = "websocket";
    public static final String ProtocolMQTT = "mqtt";
    // TODO: uncomment when support added
//    public static final String ProtocolMQTTS = "mqtts";

    // The main registry of the PluginManager
    private final PluginManagerRegister registry = new PluginManagerRegister();

    // Registration modes (specific registry classes are implemented for particular mode)
    public enum Mode {

        /* Registered channel is shared by plugins,
         * request is passed to plugin in case of exact match of target URI
         */
        SharedExactMatch,

        /* Registered channel is shared by plugins,
         * request is passed to plugin which is registered for URI which matches the
         * most sub-paths from the begin of the target URI of the request.
         */
        SharedPrefixMatch,

        /* Registered channel is dedicated for one plugin only.
         * All requests are passed to the plugin regardless to the target URI of the request.
         */
        Exclusive
    }

    /* Definition of factories for instantiating of channels for specific protocol. */
    private static final Map<String, Onem2mPluginChannelFactory> pluginChannelFactoryMap = new ConcurrentHashMap<>();
    static {
        pluginChannelFactoryMap.put(ProtocolHTTP, new Onem2mHttpPluginServerFactory());
        pluginChannelFactoryMap.put(ProtocolHTTPS, new Onem2mHttpsPluginServerFactory());
        pluginChannelFactoryMap.put(ProtocolCoAP, new Onem2mCoapPluginServerFactory());
        pluginChannelFactoryMap.put(ProtocolCoAPS, new Onem2mCoapsPluginServerFactory());
        pluginChannelFactoryMap.put(ProtocolWebsocket, new Onem2mWebsocketPluginServerFactory());
        pluginChannelFactoryMap.put(ProtocolMQTT, new Onem2mMqttPluginClientFactory());
        // TODO add next supported protocols
    }

    /**
     * Returns the only instance of the PluginManager.
     * @return Singleton instance.
     */
    static public IotdmPluginManager getInstance() {
        if (_instance == null) {
            _instance = new IotdmPluginManager();
        }
        return _instance;
    }

    /* TODO remove this when not needed to be used in the utils exposed to custom services */
    public Map<String, IotdmPluginLoader> getMgrPluginLoaders() {
        return this.pluginLoaders;
    }

    protected PluginManagerRegister getMgrRegistry() {
        return this.registry;
    }

    /**
     * Registers plugin to receive HTTP requests. Port number of the
     * HTTP server is specified and new server is started if needed.
     * @param plugin Instance of IotdmPlugin to register.
     * @param port Local TCP port number of the HTTP server.
     * @param mode Registry sharing mode.
     * @param uri Local URI for which the plugin is registering.
     * @return This instance for chaining purpose.
     * @throws IotdmPluginRegistrationException
     */
    public IotdmPluginManager registerPluginHttp(IotdmPlugin plugin, int port, IotdmPluginManager.Mode mode,
                                                 String uri)
            throws IotdmPluginRegistrationException {
        return registerPlugin(plugin, ProtocolHTTP, AllInterfaces, port, mode, uri, null);
    }

    /**
     * Registers plugin to receive HTTP requests. Port number of the
     * HTTPS server is specified and new server is started if needed.
     * Default KeyStore configuration is used.
     * @param plugin Instance of IotdmPlugin to register.
     * @param port Local TCP port number of the HTTPS server.
     * @param mode Registry sharing mode.
     * @param uri Local URI for which the plugin is registering.
     * @return This instance for chaining purpose.
     * @throws IotdmPluginRegistrationException
     */
    public IotdmPluginManager registerPluginHttps(IotdmPlugin plugin, int port, IotdmPluginManager.Mode mode,
                                                  String uri)
            throws IotdmPluginRegistrationException {
        IotdmHttpsConfigBuilder builder = new IotdmHttpsConfigBuilder().setUseDefault(true);
        return registerPlugin(plugin, ProtocolHTTPS, AllInterfaces, port, mode, uri, builder);
    }

    /**
     * Registers plugin to receive HTTP requests. Port number of the
     * HTTPS server is specified and new server is started if needed.
     * @param plugin Instance of IotdmPlugin to register.
     * @param port Local TCP port number of the HTTPS server.
     * @param mode Registry sharing mode.
     * @param uri Local URI for which the plugin is registering.
     * @param configurationBuilder Configuration builder for HTTPS server.
     * @return True in case of successful registration, False otherwise.
     * @throws IotdmPluginRegistrationException
     */
    public IotdmPluginManager registerPluginHttps(IotdmPlugin plugin, int port, IotdmPluginManager.Mode mode,
                                                  String uri, IotdmHttpsConfigBuilder configurationBuilder)
            throws IotdmPluginRegistrationException {
        return registerPlugin(plugin, ProtocolHTTPS, AllInterfaces, port, mode, uri, configurationBuilder);
    }

    /**
     * Registers plugin to receive COAP requests. Port number of the
     * COAP server is specified and new server is started if needed.
     * @param plugin Instance of IotdmPlugin to register.
     * @param port Local UDP port number of the COAP server.
     * @param mode Registry sharing mode.
     * @param uri Local URI for which the plugin is registering.
     * @return This instance for chaining purpose.
     * @throws IotdmPluginRegistrationException
     */
    public IotdmPluginManager registerPluginCoap(IotdmPlugin plugin, int port, IotdmPluginManager.Mode mode,
                                                 String uri)
            throws IotdmPluginRegistrationException {
        return registerPlugin(plugin, ProtocolCoAP, AllInterfaces, port, mode, uri, null);
    }

    /**
     * Registers plugin to receive COAPS requests. The new COAPS server is started af the specified port (if needed)
     * and the configuration provided in the configurationBuilder is used to configure the new server instance.
     * @param plugin Instance of IotdmPlugin to register.
     * @param port Local UDP port number of the COAPS server.
     * @param mode Registry sharing mode.
     * @param uri Local URI for which the plugin is registering.
     * @param configuratonBuilder Configuration builder for COAPS server.
     * @return This instance for chaining purpose.
     * @throws IotdmPluginRegistrationException
     */
    public IotdmPluginManager registerPluginCoaps(IotdmPlugin plugin, int port, IotdmPluginManager.Mode mode,
                                                  String uri, IotdmCoapsConfigBuilder configuratonBuilder)
            throws IotdmPluginRegistrationException {
        return registerPlugin(plugin, ProtocolCoAPS, AllInterfaces, port, mode, uri, configuratonBuilder);
    }

    /**
     * Registers plugin to receive COAPS requests. The new COAPS server is started af the specified port (if needed)
     * and the configuration provided in the configurationBuilder is used to configure the new server instance.
     * @param plugin Instance of IotdmPlugin to register.
     * @param port Local UDP port number of the COAPS server.
     * @param mode Registry sharing mode.
     * @param uri Local URI for which the plugin is registering.
     * @return This instance for chaining purpose.
     * @throws IotdmPluginRegistrationException
     */
    public IotdmPluginManager registerPluginCoaps(IotdmPlugin plugin, int port, IotdmPluginManager.Mode mode,
                                                  String uri)
            throws IotdmPluginRegistrationException {
        IotdmCoapsConfigBuilder builder = new IotdmCoapsConfigBuilder().setUseDefault(true);
        return registerPlugin(plugin, ProtocolCoAPS, AllInterfaces, port, mode, uri, builder);
    }

     /**
     * Registers plugin to receive Websocket requests. Port number of the
     * Websocket server is specified and new server is started if needed.
     * @param plugin Instance of IotdmPlugin to register.
     * @param port Local TCP port number of the Websocket server.
     * @param mode Registry sharing mode.
     * @param uri Local URI for which the plugin is registering.
     * @return True in case of successful registration, False otherwise.
     * @throws IotdmPluginRegistrationException
     */
    public IotdmPluginManager registerPluginWebsocket(IotdmPlugin plugin, int port, IotdmPluginManager.Mode mode,
                                                      String uri)
            throws IotdmPluginRegistrationException {
        return registerPlugin(plugin, ProtocolWebsocket, AllInterfaces, port, mode, uri, null);
    }

    /**
     * Registers plugin to receive MQTT requests. Port number and ip address of a
     * MQTT server is specified.
     * @param plugin Instance of IotdmPlugin to register.
     * @param port destination port of MQTT server.
     * @param ipAddress destination ip address of MQTT server
     * @param mode Registry sharing mode.
     * @param uri Local URI for which the plugin is registering.
     * @return This instance for chaining purpose.
     * @throws IotdmPluginRegistrationException
     */
    public IotdmPluginManager registerPluginMQTT(IotdmPlugin plugin, int port, String ipAddress,
                                                 IotdmPluginManager.Mode mode, String uri)
            throws IotdmPluginRegistrationException {
        return registerPlugin(plugin, ProtocolMQTT, ipAddress, port, mode, uri, null);
    }


    /*
     * Registration of SimpleConfig client plugins
     */

    /**
     * Registers plugin instance implementing SimpleConfig interface.
     * @param plugin Plugin instance
     * @return This instance of PluginManager for chaining purpose.
     * @throws IotdmPluginRegistrationException
     */
    public IotdmPluginManager registerSimpleConfigPlugin(IotdmPluginSimpleConfigClient plugin)
            throws IotdmPluginRegistrationException {
        IotdmPluginsSimpleConfigManager.getInstance().registerSimpleConfigPlugin(plugin);
        return this;
    }

    /**
     * Unregisters plugin instance implementing SimpleConfig interface.
     * @param plugin Plugin instance
     * @return This instance of PluginManager for chaining purpose.
     */
    public IotdmPluginManager unregisterSimpleConfigPlugin(IotdmPluginSimpleConfigClient plugin) {
        IotdmPluginsSimpleConfigManager.getInstance().unregisterSimpleConfigPlugin(plugin);
        return this;
    }

    private boolean isIpAll(String ipAdddress) {
        return ipAdddress.equals(AllInterfaces);

    }

    private boolean validateIpAddress(String ipAddress) {
        if (isNull(ipAddress) || ipAddress.isEmpty()) {
            return false;
        }

        String[] ip = ipAddress.split("\\.");
        if (ip.length != 4) {
            return false;
        }

        for (String octet : ip) {
            try {
                int val = Integer.valueOf(octet);
                if (val < 0 || val > 255 ) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    private void handleRegistrationError(String format, String... args)
            throws IotdmPluginRegistrationException {
        PluginManagerServicesUtils.handleRegistrationError(LOG, format, args);
    }

    /**
     * Method implementing the registration logic.
     * @param plugin Plugin to be registered.
     * @param protocolName Name of the protocol.
     * @param ipAddress IP address of the:
     *              - local interface for CommunicationChannels of type SERVER.
     *                  All interfaces can be used if 0.0.0.0 is passed.
     *              - remote interface for CommunicationChannels of type CLIENT.
     * @param port Local port number for servers and remote port number for clients.
     * @param mode Registration mode describing the way of sharing of the CommunicationChannel by plugins.
     * @param uri Local URI (for CLIENTS as well as for SERVERS) for which the plugin is registering.
     *            Null can be used if plugin registers for all URIs.
     * @param configurationBuilder Configuration builder for CommunicationChannel if needed.
     *                             Null can be passed.
     * @return This instance of PluginManager for chaining purpose.
     * @throws IotdmPluginRegistrationException
     */
    private IotdmPluginManager registerPlugin(IotdmPlugin plugin, String protocolName,
                                              String ipAddress, int port,
                                              IotdmPluginManager.Mode mode, String uri,
                                              IotdmPluginConfigurationBuilder configurationBuilder)
            throws IotdmPluginRegistrationException {

        // Get channel factory
        if (! pluginChannelFactoryMap.containsKey(protocolName)) {
            handleRegistrationError("Attempt to register for unsupported protocol: {}, plugin: {}",
                                    protocolName, plugin.getDebugString());
        }

        Onem2mPluginChannelFactory channelFactory = pluginChannelFactoryMap.get(protocolName);

        // Check the ipAddress
        if (isNull(ipAddress) || ipAddress.isEmpty()) {
            ipAddress = AllInterfaces;
        }
        if (! validateIpAddress(ipAddress)) {
            handleRegistrationError("Invalid ipAddress passed: {}, plugin: {}",
                                    ipAddress, plugin.getDebugString());
        }

        // Prepare channel identifier
        ChannelIdentifier chId = new ChannelIdentifier(channelFactory.getChannelType(),
                                                       channelFactory.getTransportProtocol(),
                                                       ipAddress, port, protocolName, mode);

        if (! isIpAll(ipAddress)) {
            // Check if the specific IP address doesn't collide with registration for all interfaces
            IotdmRxPluginsRegistry registryAll = this.registry.getPluginRegistryAllInterfaces(chId);
            if (nonNull(registryAll)) {
                LOG.error("Failed to register plugin {} at channel: {}. Resources already used by channel: {}",
                          plugin.getDebugString(), chId.getDebugString(), registryAll);
            }
        }

        // Check if the specific IP address and port are not already in use for another protocol
        IotdmRxPluginsRegistry endpointReg = this.registry.getPluginRegistry(chId);
        if (nonNull(endpointReg)) {
            if (! endpointReg.getProtocol().equals(chId.getProtocolName())) {
                handleRegistrationError("Failed to register plugin {} at channel: {}. " +
                                        "Resources already used for protocol {}",
                                        plugin.getDebugString(), chId.getDebugString(), endpointReg.getProtocol());
            }

            // Verify mode
            if (endpointReg.getMode() != chId.mode) {
                handleRegistrationError(
                        "Failed to register plugin {} at channel: {}. Resources already used with mode: {}",
                        plugin.getDebugString(), chId.getDebugString(), endpointReg.getMode().toString());
            }

            // Configuration must equal if exists
            if (nonNull(configurationBuilder)) {
                if (! endpointReg.getAssociatedChannel().compareConfig(configurationBuilder)) {
                    handleRegistrationError(
                                "Failed to register plugin {} at channel: {}. Different configuration passed",
                                plugin.getDebugString(), chId.getDebugString());
                }
            }

            // Verify whether the same URI is not already registered
            IotdmPlugin regPlugin = endpointReg.getPlugin(uri);
            if (nonNull(regPlugin)) {
                // Maybe double registration ?
                if (regPlugin.isPlugin(plugin)) {
                    LOG.warn("Double registration of plugin: {} at channel: {} or URI: {}",
                             plugin.getDebugString(), chId.getDebugString(), uri);
                    return this;
                }

                // URI already registered by another plugin
                handleRegistrationError(
                        "Failed to register plugin: {} at channel: {}. URI: {} already in use by plugin: {}",
                        plugin.getDebugString(), chId.getDebugString(), uri, regPlugin.getDebugString());
            }

            // register plugin in the registry
            if (! endpointReg.regPlugin(plugin, uri)) {
                handleRegistrationError("Failed to register plugin {} at channel: {}",
                                        plugin.getDebugString(), chId.getDebugString());
            }

            // registration successful
            LOG.info("Plugin: {} registered at shared channel: {} for URI: {}",
                     plugin.getDebugString(), chId.getDebugString(), uri);
            return this;
        } else { // IpAddress and port are not in use
            // Instantiate registry according to specified mode
            IotdmRxPluginsRegistry newRegistry = null;
            switch (chId.getMode()) {
                case Exclusive:
                    newRegistry = new IotdmRxPluginsRegistryExclusive(chId);
                    break;
                case SharedPrefixMatch:
                    newRegistry = new IotdmRxPluginsRegistrySharedPrefixMatch(chId);
                    break;
                case SharedExactMatch:
                    newRegistry = new IotdmRxPluginsRegistrySharedExactMatch(chId);
                    break;
                default:
                    handleRegistrationError("Registration for mode: {} not implemented", chId.getMode().toString());
            }

            // register plugin in the new registry
            if (! newRegistry.regPlugin(plugin, uri)) {
                handleRegistrationError("Failed to register plugin {} at channel: {}",
                                        plugin.getDebugString(), chId.getDebugString());
            }

            // create new channel
            IotdmBaseRxCommunicationChannel newChannel =
                    channelFactory.createInstance(chId.getIpAddress(), chId.getPort(), configurationBuilder,
                                                  newRegistry);
            if (null == newChannel) {
                handleRegistrationError("Failed to instantiate channel: {} for plugin: {}",
                                        chId.getDebugString(), plugin.getDebugString());
            }

            // associate channel and registry also in reverse direction
            newRegistry.setAssociatedChannel(newChannel);

            // store the endpoint registry in the PluginManager registry
            if (! this.registry.setPluginRegistry(chId, newRegistry)) {
                // close running channel
                try {
                    newChannel.close();
                } catch (Exception e) {
                    LOG.error("Failed to close running channel: {}", chId.getDebugString());
                }
                handleRegistrationError("Failed to register plugin: {} at channel: {}. " +
                                        "Failed to store channel registry in PluginManager registry",
                                        plugin.getDebugString(), chId.getDebugString());
            }

            // registration successful
            LOG.info("Plugin: {} registered at new channel: {} for URI: {}",
                     plugin.getDebugString(), chId.getDebugString(), uri);
            return this;
        }
    }

    private void closeRegistryChannel(IotdmRxPluginsRegistry endpointRegistry) {
        try {
            endpointRegistry.getAssociatedChannel().close();
        } catch (Exception e) {
            LOG.error("Failed to close non used channel: {}, error: {}",
                      endpointRegistry.getChannelId().getDebugString(), e);
        }
    }

    private void unregisterIotdmPluginProtocol(IotdmPlugin plugin, String protocol, Integer port) {
        this.registry.registryStream().forEach( endpointRegistry -> {
            if (endpointRegistry.hasPlugin(plugin)) {

                if (null != protocol && ! endpointRegistry.getProtocol().equals(protocol)) {
                    // Protocol doesn't match, don't remove this plugin registration
                    return;
                }

                if (null != port && endpointRegistry.getPort() != port) {
                    // Port number doesn't match, don't remove this plugin registration
                    return;
                }

                // remove the plugin
                if (! endpointRegistry.removePlugin(plugin)) {
                    LOG.error("Failed to remove plugin from registry of channel: {}, plugin: {}",
                              endpointRegistry.getChannelId().getDebugString(), plugin.getDebugString());
                } else {
                    // plugin has been removed, remove also registry if empty and close the channel
                    if (endpointRegistry.isEmpty()) {
                        if (nonNull(this.registry.removePluginRegistry(endpointRegistry.getChannelId()))) {
                            // close also the channel
                            closeRegistryChannel(endpointRegistry);
                        } else {
                            LOG.error("Failed to remove empty plugin registry of channel: {}",
                                      endpointRegistry.getChannelId().getDebugString());
                        }
                    }
                }
            }
        });
    }

    /**
     * Unregisters already registered plugin. Channels which
     * are not needed anymore are closed.
     * @param plugin The instance of IotdmPlugin to unregister
     * @return This instance of PluginManager for chaining purpose.
     */
    public IotdmPluginManager unregisterIotdmPlugin(IotdmPlugin plugin) {
        this.unregisterIotdmPluginProtocol(plugin, null, null);
        return this;
    }

    /**
     * Unregisters already registered plugin for specific protocol name. Channels which
     * are not needed anymore are closed.
     * @param plugin The instance of IotdmPlugin to unregister
     * @param protocol The protocol name
     * @return This instance of PluginManager for chaining purpose.
     */
    public IotdmPluginManager unregisterIotdmPlugin(IotdmPlugin plugin, String protocol) {
        this.unregisterIotdmPluginProtocol(plugin, protocol, null);
        return this;
    }

    /**
     * Unregisters already registered plugin for specific protocol name and specific
     * port (TCP or UDP, it depends on the upper layer protocol).
     * Channels which are not needed anymore are closed.
     * @param plugin The instance of IotdmPlugin to unregister
     * @param protocol The protocol name
     * @param port The port number
     * @return This instance of PluginManager for chaining purpose.
     */
    public IotdmPluginManager unregisterIotdmPlugin(IotdmPlugin plugin, String protocol, Integer port) {
        this.unregisterIotdmPluginProtocol(plugin, protocol, port);
        return this;
    }

    /**
     * This method is called when default configuration has changed.
     * Walks all running CommunicationChannels and re-initializes channels
     * which uses default configuration.
     */
    public void handleDefaultConfigUpdate() {
        try {
            this.registry.registryStream().forEach(endpointRegistry -> {
                if (endpointRegistry.getAssociatedChannel().getUsesDefaultConfiguration()) {
                    try {
                        endpointRegistry.getAssociatedChannel().close();
                    } catch (Exception e) {
                        LOG.error("Failed to close old channel: {}", endpointRegistry.getChannelId().getDebugString());
                    }
                    endpointRegistry.unsetAssociatedChannel();

                    Onem2mPluginChannelFactory factory =
                            pluginChannelFactoryMap.get(endpointRegistry.getProtocol());
                    IotdmBaseRxCommunicationChannel newChannel =
                            factory.createInstance(endpointRegistry.getIpAddress(), endpointRegistry.getPort(), null,
                                                   endpointRegistry);
                    if (null != newChannel) {
                        endpointRegistry.setAssociatedChannel(newChannel);
                    }
                }
            });
        } catch (Exception e) {
            LOG.error("Failed to handle update of default configuration: {}", e);
        }
    }

    /**
     * Closes all running channels.
     */
    @Override
    public void close() {
        this.registry.registryStream().forEach( endpointRegistry -> {
            try {
                closeRegistryChannel(endpointRegistry);
                this.registry.removePluginRegistry(endpointRegistry.getChannelId());
            } catch (Exception e) {
                LOG.error("Failed to close communication channel: {}", e);
            }
        });
    }

    /**
     * Stores set of parameters of communication channel
     */
    public class ChannelIdentifier {
        private final IotdmBaseRxCommunicationChannel.CommunicationChannelType channelType;
        private final IotdmBaseRxCommunicationChannel.TransportProtocol transportProtocol;
        private final String ipAddress;
        private final int port;
        private final String protocolName;
        private final Mode mode;

        private final String addrAndPort;

        public ChannelIdentifier(IotdmBaseRxCommunicationChannel.CommunicationChannelType channelType,
                                 IotdmBaseRxCommunicationChannel.TransportProtocol transportProtocol,
                                 String ipAddress,
                                 int port,
                                 String protocolName,
                                 Mode mode) {

            this.channelType = channelType;
            this.transportProtocol = transportProtocol;
            this.ipAddress = ipAddress;
            this.port = port;
            this.protocolName = protocolName;
            this.mode = mode;

            this.addrAndPort = ipAddress + ":" + port;
        }

        public IotdmBaseRxCommunicationChannel.CommunicationChannelType getChannelType() {
            return channelType;
        }

        public IotdmBaseRxCommunicationChannel.TransportProtocol getTransportProtocol() {
            return transportProtocol;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public int getPort() {
            return port;
        }

        public String getAddrAndPort() {
            return addrAndPort;
        }

        public String getProtocolName() {
            return protocolName;
        }

        public Mode getMode() {
            return mode;
        }

        /**
         * Returns string of format: 0.0.0.0:'port'
         * @return String describing registration for specific
         * port number on all local interfaces.
         */
        public String getAllInterfacesAddrAndPort() {
            return AllInterfaces + ":" + this.port;
        }

        /**
         * Returns debug string including all parameters.
         * @return Debug string
         */
        public String getDebugString() {
            return "Channel::" +
                    " Type: " + channelType +
                    " Address: " + addrAndPort +
                    " Protocol: " + protocolName +
                    " Transport: " + transportProtocol +
                    " Mode: " + mode;
        }
    }

    /**
     * Registers instance of PluginLoader.
     * @param pluginLoader The PluginLoader instance
     * @return True if successful, False otherwise
     */
    public boolean registerPluginLoader(IotdmPluginLoader pluginLoader) {
        if (this.pluginLoaders.containsKey(pluginLoader.getLoaderName())) {
            LOG.error("PluginLoader {} already registered", pluginLoader.getLoaderName());
            return false;
        }

        this.pluginLoaders.put(pluginLoader.getLoaderName(), pluginLoader);
        LOG.info("Registered PluginLoader: {}", pluginLoader.getLoaderName());
        return true;
    }

    /**
     * Unregisters instance of PluginLoader.
     * @param pluginLoader The PluginLoader instance
     * @return True if successful, False otherwise
     */
    public boolean unregisterPluginLoader(IotdmPluginLoader pluginLoader) {
        if (! this.pluginLoaders.containsKey(pluginLoader.getLoaderName())) {
            LOG.debug("PluginLoader {} not registered", pluginLoader.getLoaderName());
            return true;
        }

        if (this.pluginLoaders.get(pluginLoader.getLoaderName()) != pluginLoader) {
            LOG.error("Different plugin loader registered with the same name {}", pluginLoader.getLoaderName());
            return false;
        }

        this.pluginLoaders.remove(pluginLoader.getLoaderName());
        LOG.info("PluginLoader {} unregistered", pluginLoader.getLoaderName());
        return true;
    }
}

/**
 * Implementation of the main registry of PluginManager.
 * Stores registries for particular communication channel in four HashMaps:
 *  - TCP clients
 *  - TCP servers
 *  - UDP clients
 *  - UDP servers
 *
 *  Strings of format ipAddress:port are used as keys of these HashMaps and values are
 *  instances of the IotdmRxPluginsRegistry where the running channel is stored
 *  together with registered plugins.
 */
class PluginManagerRegister {
    private static final Logger LOG = LoggerFactory.getLogger(PluginManagerRegister.class);

    // TCP/UDP and Client/Server registries
    private final Map<String, IotdmRxPluginsRegistry> tcpClientMap = new ConcurrentHashMap<>();
    private final Map<String, IotdmRxPluginsRegistry> tcpServerMap = new ConcurrentHashMap<>();
    private final Map<String, IotdmRxPluginsRegistry> udpClientMap = new ConcurrentHashMap<>();
    private final Map<String, IotdmRxPluginsRegistry> udpServerMap = new ConcurrentHashMap<>();

    /**
     * Returns stream including all instances of the IotdmRxPluginsRegistry
     * stored in all HashMaps.
     * @return Stream of IotdmRxPluginsRegistry instances
     */
    public Stream<IotdmRxPluginsRegistry> registryStream() {
        return Stream.concat(Stream.concat(this.tcpClientMap.values().parallelStream(),
                                           this.tcpServerMap.values().parallelStream()),
                             Stream.concat(this.udpClientMap.values().parallelStream(),
                                           this.udpServerMap.values().parallelStream()));
    }

    /**
     * Removes registry of the communication channel identified by the channelId.
     * @param channelId Identification of channel.
     * @return Removed registry.
     */
    public IotdmRxPluginsRegistry removePluginRegistry(IotdmPluginManager.ChannelIdentifier channelId) {
        Map<String, IotdmRxPluginsRegistry> map = getMap(channelId.getChannelType(),
                                                         channelId.getTransportProtocol());
        if (isNull(map)) {
            LOG.error("Unsupported channelType ({}) or transportProtocol ({})",
                      channelId.getChannelType(), channelId.getTransportProtocol());
            return null;
        }

        return map.remove(channelId.getAddrAndPort());
    }

    /**
     * Stores new registry in the HashMap according to parameters included
     * in the channelId.
     * @param channelId The channelId.
     * @param newRegistry The new registry to be stored.
     * @return True in case of success, False otherwise.
     */
    public boolean setPluginRegistry(IotdmPluginManager.ChannelIdentifier channelId,
                                     IotdmRxPluginsRegistry newRegistry) {
        Map<String, IotdmRxPluginsRegistry> map = getMap(channelId.getChannelType(),
                                                         channelId.getTransportProtocol());
        if (isNull(map)) {
            LOG.error("Unsupported channelType ({}) or transportProtocol ({})",
                      channelId.getChannelType(), channelId.getTransportProtocol());
            return false;
        }

        map.put(channelId.getAddrAndPort(), newRegistry);
        return true;
    }

    private Map<String, IotdmRxPluginsRegistry> getMap(
                                               IotdmBaseRxCommunicationChannel.CommunicationChannelType channelType,
                                               IotdmBaseRxCommunicationChannel.TransportProtocol transportProtocol) {
        switch (channelType) {
            case SERVER:
                switch (transportProtocol) {
                    case UDP:
                        return udpServerMap;
                    case TCP:
                        return tcpServerMap;
                }
            case CLIENT:
                switch (transportProtocol) {
                    case UDP:
                        return udpClientMap;
                    case TCP:
                        return tcpClientMap;
                }
        }

        return null;
    }

    private IotdmRxPluginsRegistry getRegistry(IotdmBaseRxCommunicationChannel.CommunicationChannelType channelType,
                                               IotdmBaseRxCommunicationChannel.TransportProtocol transportProtocol,
                                               String addrAndPort) {
        Map<String, IotdmRxPluginsRegistry> map = getMap(channelType, transportProtocol);
        if (isNull(map)) {
            LOG.error("Unsupported channelType ({}) or transportProtocol ({})", channelType, transportProtocol);
            return null;
        }

        return map.get(addrAndPort);
    }

    /**
     * Returns the stored registry exactly specified by the channelId.
     * @param channelId Channel parameters.
     * @return The registry if exists, null otherwise.
     */
    public IotdmRxPluginsRegistry getPluginRegistry(IotdmPluginManager.ChannelIdentifier channelId) {
        return getRegistry(channelId.getChannelType(), channelId.getTransportProtocol(),
                           channelId.getAddrAndPort());
    }

    /**
     * Returns the stored registry specified by the channelId but used ipAddres
     * identifies all interfaces i.e.: "0.0.0.0".
     * @param channelId Channel parameters (ipAddress is not used).
     * @return The registry if exists, null otherwise. Null is returned even if one or
     * more registries are stored for the same port but just for one specific ipAddress.
     */
    public IotdmRxPluginsRegistry getPluginRegistryAllInterfaces(IotdmPluginManager.ChannelIdentifier channelId) {
        return getRegistry(channelId.getChannelType(), channelId.getTransportProtocol(),
                           channelId.getAllInterfacesAddrAndPort());
    }
}
