/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager;

import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.IotdmChannelProviderRegistrationException;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.IotdmChannelProviderRegistrationService;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.IotdmChannelStartException;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmBaseRxCommunicationChannel;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxChannelFactory;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxChannelProvider;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxPluginsRegistry;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxPluginsRegistryFactory;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.descriptors.IotdmChannelDescriptor;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.descriptors.IotdmChannelInterfaceDescriptor;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.descriptors.IotdmChannelProtocolDescriptor;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPlugin;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginLoader;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationService;
import org.opendaylight.iotdm.plugininfra.pluginmanager.customservices.PluginManagerServicesUtils;
import org.opendaylight.iotdm.plugininfra.pluginmanager.simpleconfig.IotdmPluginsSimpleConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements the registration and un-registration methods for IotdmHandlerPlugin instances and
 * stores registrations in registry according to registration mode specified in registration.
 * PluginManager is implemented as singleton.
 */
public class IotdmPluginManager implements IotdmChannelProviderRegistrationService,
                                           IotdmPluginRegistrationService,
                                           AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmPluginManager.class);
    private static IotdmPluginManager _instance;
    private static IotdmPluginManagerProvider serviceProvider = null;
    private static IotdmPluginsSimpleConfigProvider simpleConfigProvider = null;

    private final Map<String, IotdmPluginLoader> pluginLoaders = new ConcurrentHashMap<>();
    private final Map<String, IotdmRxChannelProvider> rxChannelProviders = new ConcurrentHashMap<>();
    private final List<RxPluginRegistration> rxPluginsRegistry = new LinkedList<>();

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

    private class RxPluginRegistration {
        private final IotdmChannelDescriptor channelDescriptor;
        private final IotdmRxPluginsRegistry pluginRegistry;
        private final IotdmBaseRxCommunicationChannel commChannel;

        public RxPluginRegistration(final IotdmChannelDescriptor channelDescriptor,
                                    final IotdmRxPluginsRegistry pluginRegistry,
                                    final IotdmBaseRxCommunicationChannel commChannel) {
            this.channelDescriptor = channelDescriptor;
            this.pluginRegistry = pluginRegistry;
            this.commChannel = commChannel;
        }

        public IotdmChannelDescriptor getChannelDescriptor() {
            return channelDescriptor;
        }

        public IotdmRxPluginsRegistry getPluginRegistry() {
            return pluginRegistry;
        }

        public IotdmBaseRxCommunicationChannel getCommChannel() {
            return commChannel;
        }
    }

    private void verifyRxProvider(final IotdmRxChannelProvider channelProvider, boolean shouldBeRegistered)
                        throws IotdmChannelProviderRegistrationException {

        IotdmRxChannelFactory channelFactory = channelProvider.getRxChannelFactory();
        IotdmRxPluginsRegistryFactory registryFactory = channelProvider.getRxChannelRegistryFactory();
        List<String> channelIds = channelProvider.getProtocolChannelsIds();
        String channelProviderId = channelProvider.getProtocolChannelProviderId();

        if (null == channelFactory) {
            throw new IotdmChannelProviderRegistrationException("RxChannelProvider without ChannelFactory");
        }

        if (null == registryFactory) {
            throw new IotdmChannelProviderRegistrationException("RxChannelProvider without RegistryFactory");
        }

        /* Protocol Channel Provider IDs must equal */
        if (! channelFactory.getProtocolChannelProviderId().equals(channelProviderId)) {
            throw new IotdmChannelProviderRegistrationException(
                "RxChannelProvider and RxChannelFactory protocol channel provider IDs mismatch:: " +
                    "RxChannelProvider: " + channelProviderId +
                    " RxChannelFactory: " + channelFactory.getProtocolChannelProviderId());
        }

        if (! registryFactory.getProtocolChannelProviderId().equals(channelProviderId)) {
            throw new IotdmChannelProviderRegistrationException(
                "RxChannelProvider and RegistryFactory protocol channel provider IDs mismatch:: " +
                    "RxChannelProvider: " + channelProviderId +
                    " RegistryFactory: " + registryFactory.getProtocolChannelProviderId());
        }

        for (String chId : channelIds) {
            if (shouldBeRegistered) {
                if (! this.rxChannelProviders.containsKey(chId)) {
                    throw new IotdmChannelProviderRegistrationException(
                        "RxChannelProvider for protocol channel: " + chId + " not registered");
                }
            }
            else {
                if (this.rxChannelProviders.containsKey(chId)) {
                    throw new IotdmChannelProviderRegistrationException(
                        "RxChannelProvider for protocol channel: " + chId + " already registered");
                }
            }
        }
    }

    @Override
    synchronized public void registerRxProvider(final IotdmRxChannelProvider channelProvider)
        throws IotdmChannelProviderRegistrationException {

        verifyRxProvider(channelProvider, false);

        for (String chId : channelProvider.getProtocolChannelsIds()) {
            this.rxChannelProviders.put(chId, channelProvider);
            // TODO LOG
        }
    }

    @Override
    synchronized public void unregisterRxProvider(final IotdmRxChannelProvider channelProvider)
        throws IotdmChannelProviderRegistrationException {

        verifyRxProvider(channelProvider, true);

        for (String chId : channelProvider.getProtocolChannelsIds()) {
            this.rxChannelProviders.remove(chId);
        }
    }


    /*
     * Implementation of IotdmPluginRegistrationService
     */
    // TODO implement RW lock instead of synchronized ?

    @Override
    synchronized public void registerRxPlugin(IotdmChannelDescriptor descriptor, IotdmPlugin plugin)
        throws IotdmPluginRegistrationException {

        try {

            // Check if the Interface descriptor is in conflict with some already existing registration
            for (RxPluginRegistration registration : rxPluginsRegistry) {
                IotdmChannelInterfaceDescriptor newIntDesc = descriptor.getInterfaceDescriptor();
                IotdmChannelInterfaceDescriptor currIntDesc =
                    registration.getChannelDescriptor().getInterfaceDescriptor();

                if (newIntDesc.isInConflict(currIntDesc)) {

                    // conflicting descriptor found, check if equals
                    if (! newIntDesc.equals(currIntDesc)) {
                        handleRegistrationError(
                            "Conflicting and unequal channel interface registration found, plugin: {}",
                            plugin.getDebugString());
                    }

                    // conflicting equal interface descriptor registration found, check if shared
                    if (! newIntDesc.isShared()) {
                        handleRegistrationError("Conflicting, equal, non-shared channel interface registration " +
                                                    "found, plugin: {}",
                                                plugin.getDebugString());
                    }

                    // conflicting but equal and shared interface, check if protocol descriptor equals
                    IotdmChannelProtocolDescriptor newProtoDesc = descriptor.getProtocolDescriptor();
                    IotdmChannelProtocolDescriptor currProtoDesc =
                        registration.getChannelDescriptor().getProtocolDescriptor();
                    if (! newProtoDesc.equals(currProtoDesc)) {
                        handleRegistrationError(
                            "Conflicting, equal, shared channel interface registration using different protocol " +
                                "descriptor, plugin: {}",
                            plugin.getDebugString());
                    }

                    // conflicting but equal and shared interface used by the same channel and configuration
                    // new plugin can be registered together with others
                    registration.getPluginRegistry().registerPlugin(plugin, descriptor.getEndpointDescriptor());

                    // registration successful
                    return;
                }
            }

            // There's no such interface in use, create the new interface registration
            IotdmRxChannelProvider provider = rxChannelProviders.get(descriptor.getProtocolChannelId());
            if (null == provider) {
                handleRegistrationError("ProtocolChannelProvider for channel: {} not registered, plugin: {}",
                                        descriptor.getProtocolChannelId(), plugin.getDebugString());
                // previous method call always throws exception, this return; is just used
                // to avoid compilation warnings
                return;
            }

            IotdmRxChannelFactory providerChannelFactory = provider.getRxChannelFactory();
            IotdmRxPluginsRegistryFactory registryFactory = provider.getRxChannelRegistryFactory();

            // get the endpoint registry
            IotdmRxPluginsRegistry endpointReg = registryFactory.getEndpointRegistry(descriptor);
            if (null == endpointReg) {
                // failed to obtain endpoint registry
                handleRegistrationError("Unable to obtain endpoint registry of ProtocolChannelProvider: {} for " +
                                        "channel: {}, plugin: {}",
                                        provider.getProtocolChannelProviderId(),
                                        descriptor.getProtocolChannelId(),
                                        plugin.getDebugString());
                // previous method call always throws exception, this return; is just used
                // to avoid compilation warnings
                return;
            }

            // create new channel instance
            IotdmBaseRxCommunicationChannel channel =
                providerChannelFactory.getRxChannelInstance(descriptor, endpointReg);
            if (null == channel) {
                // failed to create communication channel
                handleRegistrationError("Unable to create communication channel for plugin: {}", plugin.getDebugString());
                // previous method call always throws exception, this return; is just used
                // to avoid compilation warnings
                return;
            }

            // register the plugin in the new registry
            endpointReg.registerPlugin(plugin, descriptor.getEndpointDescriptor());

            // initiate (start) the channel
            try {
                channel.init();
            } catch (IotdmChannelStartException e) {
                handleRegistrationError("Failed to start new communication channel of type: {} for plugin: {}, err: {}",
                                        descriptor.getProtocolChannelId(), plugin.getDebugString(), e.toString());
            }

            // channel is initiated successfully, now the registration can be stored
            RxPluginRegistration registration = new RxPluginRegistration(descriptor, endpointReg, channel);
            this.rxPluginsRegistry.add(registration);

        } catch(IotdmPluginRegistrationException e) {
            // just rethrow
            throw e;
        } catch(Exception e) {
            LOG.error("Unexpected exception during registration of plugin {}, error: {}",
                      plugin.getDebugString(), e);
            throw new IotdmPluginRegistrationException(e);
        }
    }

    @Override
    public void deregisterRxPlugin(IotdmChannelDescriptor descriptor, IotdmPlugin plugin) {
        // TODO
    }

    @Override
    public void deregisterRxPlugin(IotdmPlugin plugin) {
        // TODO
    }

    private void handleRegistrationError(String format, String... args)
        throws IotdmPluginRegistrationException {
        PluginManagerServicesUtils.handleRegistrationError(LOG, format, args);
    }



    /* TODO remove this when not needed to be used in the utils exposed to custom services */
    public Map<String, IotdmPluginLoader> getMgrPluginLoaders() {
        return this.pluginLoaders;
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

    public void close() {
        // TODO deregister all
    }

//
//    /***
//     * TODO: Old implementation, to be deleted
//     */
//
//
//    /* Specific IP address which means that plugin registers for all
//     * local interfaces
//     */
//    private static final String AllInterfaces = "0.0.0.0";
//
//    // Supported protocols
//    public static final String ProtocolHTTP = "http";
//    public static final String ProtocolHTTPS = "https";
//    public static final String ProtocolCoAP = "coap";
//    public static final String ProtocolCoAPS = "coaps";
//    public static final String ProtocolWebsocket = "websocket";
//    public static final String ProtocolMQTT = "mqtt";
//    // TODO: uncomment when support added
////    public static final String ProtocolMQTTS = "mqtts";
//
//    // The main registry of the PluginManager
//    private final PluginManagerRegister registry = new PluginManagerRegister();
//
//
//
//    /* Definition of factories for instantiating of channels for specific protocol. */
//    private static final Map<String, Onem2mPluginChannelFactory> pluginChannelFactoryMap = new ConcurrentHashMap<>();
//    static {
//        pluginChannelFactoryMap.put(ProtocolHTTP, new Onem2mHttpPluginServerFactory());
//        pluginChannelFactoryMap.put(ProtocolHTTPS, new Onem2mHttpsPluginServerFactory());
//        pluginChannelFactoryMap.put(ProtocolCoAP, new Onem2mCoapPluginServerFactory());
//        pluginChannelFactoryMap.put(ProtocolCoAPS, new Onem2mCoapsPluginServerFactory());
//        pluginChannelFactoryMap.put(ProtocolWebsocket, new Onem2mWebsocketPluginServerFactory());
//        pluginChannelFactoryMap.put(ProtocolMQTT, new Onem2mMqttPluginClientFactory());
//        // TODO add next supported protocols
//    }
//


//
//    protected PluginManagerRegister getMgrRegistry() {
//        return this.registry;
//    }
//
//    /**
//     * Registers plugin to receive HTTP requests. Port number of the
//     * HTTP server is specified and new server is started if needed.
//     * @param plugin Instance of IotdmHandlerPlugin to register.
//     * @param port Local TCP port number of the HTTP server.
//     * @param mode Registry sharing mode.
//     * @param uri Local URI for which the plugin is registering.
//     * @return This instance for chaining purpose.
//     * @throws IotdmPluginRegistrationException
//     */
//    public IotdmPluginManager registerPluginHttp(IotdmHandlerPlugin plugin, int port, IotdmPluginManager.Mode mode,
//                                                 String uri)
//            throws IotdmPluginRegistrationException {
//        return registerPlugin(plugin, ProtocolHTTP, AllInterfaces, port, mode, uri, null);
//    }
//
//    /**
//     * Registers plugin to receive HTTP requests. Port number of the
//     * HTTPS server is specified and new server is started if needed.
//     * Default KeyStore configuration is used.
//     * @param plugin Instance of IotdmHandlerPlugin to register.
//     * @param port Local TCP port number of the HTTPS server.
//     * @param mode Registry sharing mode.
//     * @param uri Local URI for which the plugin is registering.
//     * @return This instance for chaining purpose.
//     * @throws IotdmPluginRegistrationException
//     */
//    public IotdmPluginManager registerPluginHttps(IotdmHandlerPlugin plugin, int port, IotdmPluginManager.Mode mode,
//                                                  String uri)
//            throws IotdmPluginRegistrationException {
//        IotdmHttpsConfigBuilder builder = new IotdmHttpsConfigBuilder().setUseDefault(true);
//        return registerPlugin(plugin, ProtocolHTTPS, AllInterfaces, port, mode, uri, builder);
//    }
//
//    /**
//     * Registers plugin to receive HTTP requests. Port number of the
//     * HTTPS server is specified and new server is started if needed.
//     * @param plugin Instance of IotdmHandlerPlugin to register.
//     * @param port Local TCP port number of the HTTPS server.
//     * @param mode Registry sharing mode.
//     * @param uri Local URI for which the plugin is registering.
//     * @param configurationBuilder Configuration builder for HTTPS server.
//     * @return True in case of successful registration, False otherwise.
//     * @throws IotdmPluginRegistrationException
//     */
//    public IotdmPluginManager registerPluginHttps(IotdmHandlerPlugin plugin, int port, IotdmPluginManager.Mode mode,
//                                                  String uri, IotdmHttpsConfigBuilder configurationBuilder)
//            throws IotdmPluginRegistrationException {
//        return registerPlugin(plugin, ProtocolHTTPS, AllInterfaces, port, mode, uri, configurationBuilder);
//    }
//
//    /**
//     * Registers plugin to receive COAP requests. Port number of the
//     * COAP server is specified and new server is started if needed.
//     * @param plugin Instance of IotdmHandlerPlugin to register.
//     * @param port Local UDP port number of the COAP server.
//     * @param mode Registry sharing mode.
//     * @param uri Local URI for which the plugin is registering.
//     * @return This instance for chaining purpose.
//     * @throws IotdmPluginRegistrationException
//     */
//    public IotdmPluginManager registerPluginCoap(IotdmHandlerPlugin plugin, int port, IotdmPluginManager.Mode mode,
//                                                 String uri)
//            throws IotdmPluginRegistrationException {
//        return registerPlugin(plugin, ProtocolCoAP, AllInterfaces, port, mode, uri, null);
//    }
//
//    /**
//     * Registers plugin to receive COAPS requests. The new COAPS server is started af the specified port (if needed)
//     * and the configuration provided in the configurationBuilder is used to configure the new server instance.
//     * @param plugin Instance of IotdmHandlerPlugin to register.
//     * @param port Local UDP port number of the COAPS server.
//     * @param mode Registry sharing mode.
//     * @param uri Local URI for which the plugin is registering.
//     * @param configuratonBuilder Configuration builder for COAPS server.
//     * @return This instance for chaining purpose.
//     * @throws IotdmPluginRegistrationException
//     */
//    public IotdmPluginManager registerPluginCoaps(IotdmHandlerPlugin plugin, int port, IotdmPluginManager.Mode mode,
//                                                  String uri, IotdmCoapsConfigBuilder configuratonBuilder)
//            throws IotdmPluginRegistrationException {
//        return registerPlugin(plugin, ProtocolCoAPS, AllInterfaces, port, mode, uri, configuratonBuilder);
//    }
//
//    /**
//     * Registers plugin to receive COAPS requests. The new COAPS server is started af the specified port (if needed)
//     * and the configuration provided in the configurationBuilder is used to configure the new server instance.
//     * @param plugin Instance of IotdmHandlerPlugin to register.
//     * @param port Local UDP port number of the COAPS server.
//     * @param mode Registry sharing mode.
//     * @param uri Local URI for which the plugin is registering.
//     * @return This instance for chaining purpose.
//     * @throws IotdmPluginRegistrationException
//     */
//    public IotdmPluginManager registerPluginCoaps(IotdmHandlerPlugin plugin, int port, IotdmPluginManager.Mode mode,
//                                                  String uri)
//            throws IotdmPluginRegistrationException {
//        IotdmCoapsConfigBuilder builder = new IotdmCoapsConfigBuilder().setUseDefault(true);
//        return registerPlugin(plugin, ProtocolCoAPS, AllInterfaces, port, mode, uri, builder);
//    }
//
//     /**
//     * Registers plugin to receive Websocket requests. Port number of the
//     * Websocket server is specified and new server is started if needed.
//     * @param plugin Instance of IotdmHandlerPlugin to register.
//     * @param port Local TCP port number of the Websocket server.
//     * @param mode Registry sharing mode.
//     * @param uri Local URI for which the plugin is registering.
//     * @return True in case of successful registration, False otherwise.
//     * @throws IotdmPluginRegistrationException
//     */
//    public IotdmPluginManager registerPluginWebsocket(IotdmHandlerPlugin plugin, int port, IotdmPluginManager.Mode mode,
//                                                      String uri)
//            throws IotdmPluginRegistrationException {
//        return registerPlugin(plugin, ProtocolWebsocket, AllInterfaces, port, mode, uri, null);
//    }
//
//    /**
//     * Registers plugin to receive MQTT requests. Port number and ip address of a
//     * MQTT server is specified.
//     * @param plugin Instance of IotdmHandlerPlugin to register.
//     * @param port destination port of MQTT server.
//     * @param ipAddress destination ip address of MQTT server
//     * @param mode Registry sharing mode.
//     * @param uri Local URI for which the plugin is registering.
//     * @return This instance for chaining purpose.
//     * @throws IotdmPluginRegistrationException
//     */
//    public IotdmPluginManager registerPluginMQTT(IotdmHandlerPlugin plugin, int port, String ipAddress,
//                                                 IotdmPluginManager.Mode mode, String uri)
//            throws IotdmPluginRegistrationException {
//        return registerPlugin(plugin, ProtocolMQTT, ipAddress, port, mode, uri, null);
//    }
//
//
//    /*
//     * Registration of SimpleConfig client plugins
//     */
//
//    /**
//     * Registers plugin instance implementing SimpleConfig interface.
//     * @param plugin Plugin instance
//     * @return This instance of PluginManager for chaining purpose.
//     * @throws IotdmPluginRegistrationException
//     */
//    public IotdmPluginManager registerSimpleConfigPlugin(IotdmPluginSimpleConfigClient plugin)
//            throws IotdmPluginRegistrationException {
//        IotdmPluginsSimpleConfigManager.getInstance().registerSimpleConfigPlugin(plugin);
//        return this;
//    }
//
//    /**
//     * Unregisters plugin instance implementing SimpleConfig interface.
//     * @param plugin Plugin instance
//     * @return This instance of PluginManager for chaining purpose.
//     */
//    public IotdmPluginManager unregisterSimpleConfigPlugin(IotdmPluginSimpleConfigClient plugin) {
//        IotdmPluginsSimpleConfigManager.getInstance().unregisterSimpleConfigPlugin(plugin);
//        return this;
//    }
//
//    private boolean isIpAll(String ipAdddress) {
//        return ipAdddress.equals(AllInterfaces);
//
//    }
//
//    private boolean validateIpAddress(String ipAddress) {
//        if (isNull(ipAddress) || ipAddress.isEmpty()) {
//            return false;
//        }
//
//        String[] ip = ipAddress.split("\\.");
//        if (ip.length != 4) {
//            return false;
//        }
//
//        for (String octet : ip) {
//            try {
//                int val = Integer.valueOf(octet);
//                if (val < 0 || val > 255 ) {
//                    return false;
//                }
//            } catch (NumberFormatException e) {
//                return false;
//            }
//        }
//
//        return true;
//    }
//
//
//    /**
//     * Method implementing the registration logic.
//     * @param plugin Plugin to be registered.
//     * @param protocolName Name of the protocol.
//     * @param ipAddress IP address of the:
//     *              - local interface for CommunicationChannels of type SERVER.
//     *                  All interfaces can be used if 0.0.0.0 is passed.
//     *              - remote interface for CommunicationChannels of type CLIENT.
//     * @param port Local port number for servers and remote port number for clients.
//     * @param mode Registration mode describing the way of sharing of the CommunicationChannel by plugins.
//     * @param uri Local URI (for CLIENTS as well as for SERVERS) for which the plugin is registering.
//     *            Null can be used if plugin registers for all URIs.
//     * @param configurationBuilder Configuration builder for CommunicationChannel if needed.
//     *                             Null can be passed.
//     * @return This instance of PluginManager for chaining purpose.
//     * @throws IotdmPluginRegistrationException
//     */
//    private IotdmPluginManager registerPlugin(IotdmHandlerPlugin plugin, String protocolName,
//                                              String ipAddress, int port,
//                                              IotdmPluginManager.Mode mode, String uri,
//                                              IotdmPluginConfigurationBuilder configurationBuilder)
//            throws IotdmPluginRegistrationException {
//
//        // Get channel factory
//        if (! pluginChannelFactoryMap.containsKey(protocolName)) {
//            handleRegistrationError("Attempt to register for unsupported protocol: {}, plugin: {}",
//                                    protocolName, plugin.getDebugString());
//        }
//
//        Onem2mPluginChannelFactory channelFactory = pluginChannelFactoryMap.get(protocolName);
//
//        // Check the ipAddress
//        if (isNull(ipAddress) || ipAddress.isEmpty()) {
//            ipAddress = AllInterfaces;
//        }
//        if (! validateIpAddress(ipAddress)) {
//            handleRegistrationError("Invalid ipAddress passed: {}, plugin: {}",
//                                    ipAddress, plugin.getDebugString());
//        }
//
//        // Prepare channel identifier
//        ChannelIdentifier chId = new ChannelIdentifier(channelFactory.getChannelType(),
//                                                       channelFactory.getTransportProtocol(),
//                                                       ipAddress, port, protocolName, mode);
//
//        if (! isIpAll(ipAddress)) {
//            // Check if the specific IP address doesn't collide with registration for all interfaces
//            IotdmRxPluginsBaseRegistry registryAll = this.registry.getPluginRegistryAllInterfaces(chId);
//            if (nonNull(registryAll)) {
//                LOG.error("Failed to register plugin {} at channel: {}. Resources already used by channel: {}",
//                          plugin.getDebugString(), chId.getDebugString(), registryAll);
//            }
//        }
//
//        // Check if the specific IP address and port are not already in use for another protocol
//        IotdmRxPluginsBaseRegistry endpointReg = this.registry.getPluginRegistry(chId);
//        if (nonNull(endpointReg)) {
//            if (! endpointReg.getProtocol().equals(chId.getProtocolName())) {
//                handleRegistrationError("Failed to register plugin {} at channel: {}. " +
//                                        "Resources already used for protocol {}",
//                                        plugin.getDebugString(), chId.getDebugString(), endpointReg.getProtocol());
//            }
//
//            // Verify mode
//            if (endpointReg.getMode() != chId.mode) {
//                handleRegistrationError(
//                        "Failed to register plugin {} at channel: {}. Resources already used with mode: {}",
//                        plugin.getDebugString(), chId.getDebugString(), endpointReg.getMode().toString());
//            }
//
//            // Configuration must equal if exists
//            if (nonNull(configurationBuilder)) {
//                if (! endpointReg.getAssociatedChannel().compareConfig(configurationBuilder)) {
//                    handleRegistrationError(
//                                "Failed to register plugin {} at channel: {}. Different configuration passed",
//                                plugin.getDebugString(), chId.getDebugString());
//                }
//            }
//
//            // Verify whether the same URI is not already registered
//            IotdmHandlerPlugin regPlugin = endpointReg.getPlugin(uri);
//            if (nonNull(regPlugin)) {
//                // Maybe double registration ?
//                if (regPlugin.isPlugin(plugin)) {
//                    LOG.warn("Double registration of plugin: {} at channel: {} or URI: {}",
//                             plugin.getDebugString(), chId.getDebugString(), uri);
//                    return this;
//                }
//
//                // URI already registered by another plugin
//                handleRegistrationError(
//                        "Failed to register plugin: {} at channel: {}. URI: {} already in use by plugin: {}",
//                        plugin.getDebugString(), chId.getDebugString(), uri, regPlugin.getDebugString());
//            }
//
//            // register plugin in the registry
//            if (! endpointReg.regPlugin(plugin, uri)) {
//                handleRegistrationError("Failed to register plugin {} at channel: {}",
//                                        plugin.getDebugString(), chId.getDebugString());
//            }
//
//            // registration successful
//            LOG.info("Plugin: {} registered at shared channel: {} for URI: {}",
//                     plugin.getDebugString(), chId.getDebugString(), uri);
//            return this;
//        } else { // IpAddress and port are not in use
//            // Instantiate registry according to specified mode
//            IotdmRxPluginsBaseRegistry newRegistry = null;
//            switch (chId.getMode()) {
//                case Exclusive:
//                    newRegistry = new IotdmRxPluginsRegistryExclusive(chId);
//                    break;
//                case SharedPrefixMatch:
//                    newRegistry = new IotdmRxPluginsRegistrySharedPrefixMatch(chId);
//                    break;
//                case SharedExactMatch:
//                    newRegistry = new IotdmRxPluginsRegistrySharedExactMatch(chId);
//                    break;
//                default:
//                    handleRegistrationError("Registration for mode: {} not implemented", chId.getMode().toString());
//            }
//
//            // register plugin in the new registry
//            if (! newRegistry.regPlugin(plugin, uri)) {
//                handleRegistrationError("Failed to register plugin {} at channel: {}",
//                                        plugin.getDebugString(), chId.getDebugString());
//            }
//
//            // create new channel
//            IotdmBaseRxCommunicationChannel newChannel =
//                    channelFactory.createInstance(chId.getIpAddress(), chId.getPort(), configurationBuilder,
//                                                  newRegistry);
//            if (null == newChannel) {
//                handleRegistrationError("Failed to instantiate channel: {} for plugin: {}",
//                                        chId.getDebugString(), plugin.getDebugString());
//            }
//
//            // associate channel and registry also in reverse direction
//            newRegistry.setAssociatedChannel(newChannel);
//
//            // store the endpoint registry in the PluginManager registry
//            if (! this.registry.setPluginRegistry(chId, newRegistry)) {
//                // close running channel
//                try {
//                    newChannel.close();
//                } catch (Exception e) {
//                    LOG.error("Failed to close running channel: {}", chId.getDebugString());
//                }
//                handleRegistrationError("Failed to register plugin: {} at channel: {}. " +
//                                        "Failed to store channel registry in PluginManager registry",
//                                        plugin.getDebugString(), chId.getDebugString());
//            }
//
//            // registration successful
//            LOG.info("Plugin: {} registered at new channel: {} for URI: {}",
//                     plugin.getDebugString(), chId.getDebugString(), uri);
//            return this;
//        }
//    }
//
//    private void closeRegistryChannel(IotdmRxPluginsBaseRegistry endpointRegistry) {
//        try {
//            endpointRegistry.getAssociatedChannel().close();
//        } catch (Exception e) {
//            LOG.error("Failed to close non used channel: {}, error: {}",
//                      endpointRegistry.getChannelId().getDebugString(), e);
//        }
//    }
//
//    private void unregisterIotdmPluginProtocol(IotdmHandlerPlugin plugin, String protocol, Integer port) {
//        this.registry.registryStream().forEach( endpointRegistry -> {
//            if (endpointRegistry.hasPlugin(plugin)) {
//
//                if (null != protocol && ! endpointRegistry.getProtocol().equals(protocol)) {
//                    // Protocol doesn't match, don't remove this plugin registration
//                    return;
//                }
//
//                if (null != port && endpointRegistry.getPort() != port) {
//                    // Port number doesn't match, don't remove this plugin registration
//                    return;
//                }
//
//                // remove the plugin
//                if (! endpointRegistry.removePlugin(plugin)) {
//                    LOG.error("Failed to remove plugin from registry of channel: {}, plugin: {}",
//                              endpointRegistry.getChannelId().getDebugString(), plugin.getDebugString());
//                } else {
//                    // plugin has been removed, remove also registry if empty and close the channel
//                    if (endpointRegistry.isEmpty()) {
//                        if (nonNull(this.registry.removePluginRegistry(endpointRegistry.getChannelId()))) {
//                            // close also the channel
//                            closeRegistryChannel(endpointRegistry);
//                        } else {
//                            LOG.error("Failed to remove empty plugin registry of channel: {}",
//                                      endpointRegistry.getChannelId().getDebugString());
//                        }
//                    }
//                }
//            }
//        });
//    }
//
//    /**
//     * Unregisters already registered plugin. Channels which
//     * are not needed anymore are closed.
//     * @param plugin The instance of IotdmHandlerPlugin to unregister
//     * @return This instance of PluginManager for chaining purpose.
//     */
//    public IotdmPluginManager unregisterIotdmPlugin(IotdmHandlerPlugin plugin) {
//        this.unregisterIotdmPluginProtocol(plugin, null, null);
//        return this;
//    }
//
//    /**
//     * Unregisters already registered plugin for specific protocol name. Channels which
//     * are not needed anymore are closed.
//     * @param plugin The instance of IotdmHandlerPlugin to unregister
//     * @param protocol The protocol name
//     * @return This instance of PluginManager for chaining purpose.
//     */
//    public IotdmPluginManager unregisterIotdmPlugin(IotdmHandlerPlugin plugin, String protocol) {
//        this.unregisterIotdmPluginProtocol(plugin, protocol, null);
//        return this;
//    }
//
//    /**
//     * Unregisters already registered plugin for specific protocol name and specific
//     * port (TCP or UDP, it depends on the upper layer protocol).
//     * Channels which are not needed anymore are closed.
//     * @param plugin The instance of IotdmHandlerPlugin to unregister
//     * @param protocol The protocol name
//     * @param port The port number
//     * @return This instance of PluginManager for chaining purpose.
//     */
//    public IotdmPluginManager unregisterIotdmPlugin(IotdmHandlerPlugin plugin, String protocol, Integer port) {
//        this.unregisterIotdmPluginProtocol(plugin, protocol, port);
//        return this;
//    }
//
//    /**
//     * This method is called when default configuration has changed.
//     * Walks all running CommunicationChannels and re-initializes channels
//     * which uses default configuration.
//     */
//    public void handleDefaultConfigUpdate() {
//        try {
//            this.registry.registryStream().forEach(endpointRegistry -> {
//                if (endpointRegistry.getAssociatedChannel().getUsesDefaultConfiguration()) {
//                    try {
//                        endpointRegistry.getAssociatedChannel().close();
//                    } catch (Exception e) {
//                        LOG.error("Failed to close old channel: {}", endpointRegistry.getChannelId().getDebugString());
//                    }
//                    endpointRegistry.unsetAssociatedChannel();
//
//                    Onem2mPluginChannelFactory factory =
//                            pluginChannelFactoryMap.get(endpointRegistry.getProtocol());
//                    IotdmBaseRxCommunicationChannel newChannel =
//                            factory.createInstance(endpointRegistry.getIpAddress(), endpointRegistry.getPort(), null,
//                                                   endpointRegistry);
//                    if (null != newChannel) {
//                        endpointRegistry.setAssociatedChannel(newChannel);
//                    }
//                }
//            });
//        } catch (Exception e) {
//            LOG.error("Failed to handle update of default configuration: {}", e);
//        }
//    }
//
//    /**
//     * Closes all running channels.
//     */
//    @Override
//    public void close() {
//        this.registry.registryStream().forEach( endpointRegistry -> {
//            try {
//                closeRegistryChannel(endpointRegistry);
//                this.registry.removePluginRegistry(endpointRegistry.getChannelId());
//            } catch (Exception e) {
//                LOG.error("Failed to close communication channel: {}", e);
//            }
//        });
//    }
//
//    /**
//     * Stores set of parameters of communication channel
//     */
//    public class ChannelIdentifier {
//        private final IotdmBaseRxCommunicationChannel.CommunicationChannelType channelType;
//        private final IotdmBaseRxCommunicationChannel.TransportProtocol transportProtocol;
//        private final String ipAddress;
//        private final int port;
//        private final String protocolName;
//        private final Mode mode;
//
//        private final String addrAndPort;
//
//        public ChannelIdentifier(IotdmBaseRxCommunicationChannel.CommunicationChannelType channelType,
//                                 IotdmBaseRxCommunicationChannel.TransportProtocol transportProtocol,
//                                 String ipAddress,
//                                 int port,
//                                 String protocolName,
//                                 Mode mode) {
//
//            this.channelType = channelType;
//            this.transportProtocol = transportProtocol;
//            this.ipAddress = ipAddress;
//            this.port = port;
//            this.protocolName = protocolName;
//            this.mode = mode;
//
//            this.addrAndPort = ipAddress + ":" + port;
//        }
//
//        public IotdmBaseRxCommunicationChannel.CommunicationChannelType getChannelType() {
//            return channelType;
//        }
//
//        public IotdmBaseRxCommunicationChannel.TransportProtocol getTransportProtocol() {
//            return transportProtocol;
//        }
//
//        public String getIpAddress() {
//            return ipAddress;
//        }
//
//        public int getPort() {
//            return port;
//        }
//
//        public String getAddrAndPort() {
//            return addrAndPort;
//        }
//
//        public String getProtocolName() {
//            return protocolName;
//        }
//
//        public Mode getMode() {
//            return mode;
//        }
//
//        /**
//         * Returns string of format: 0.0.0.0:'port'
//         * @return String describing registration for specific
//         * port number on all local interfaces.
//         */
//        public String getAllInterfacesAddrAndPort() {
//            return AllInterfaces + ":" + this.port;
//        }
//
//        /**
//         * Returns debug string including all parameters.
//         * @return Debug string
//         */
//        public String getDebugString() {
//            return "Channel::" +
//                    " Type: " + channelType +
//                    " Address: " + addrAndPort +
//                    " Protocol: " + protocolName +
//                    " Transport: " + transportProtocol +
//                    " Mode: " + mode;
//        }
//    }

//
///**
// * Implementation of the main registry of PluginManager.
// * Stores registries for particular communication channel in four HashMaps:
// *  - TCP clients
// *  - TCP servers
// *  - UDP clients
// *  - UDP servers
// *
// *  Strings of format ipAddress:port are used as keys of these HashMaps and values are
// *  instances of the IotdmRxPluginsBaseRegistry where the running channel is stored
// *  together with registered plugins.
// */
//class PluginManagerRegister {
//    private static final Logger LOG = LoggerFactory.getLogger(PluginManagerRegister.class);
//
//    // TCP/UDP and Client/Server registries
//    private final Map<String, IotdmRxPluginsBaseRegistry> tcpClientMap = new ConcurrentHashMap<>();
//    private final Map<String, IotdmRxPluginsBaseRegistry> tcpServerMap = new ConcurrentHashMap<>();
//    private final Map<String, IotdmRxPluginsBaseRegistry> udpClientMap = new ConcurrentHashMap<>();
//    private final Map<String, IotdmRxPluginsBaseRegistry> udpServerMap = new ConcurrentHashMap<>();
//
//    /**
//     * Returns stream including all instances of the IotdmRxPluginsBaseRegistry
//     * stored in all HashMaps.
//     * @return Stream of IotdmRxPluginsBaseRegistry instances
//     */
//    public Stream<IotdmRxPluginsBaseRegistry> registryStream() {
//        return Stream.concat(Stream.concat(this.tcpClientMap.values().parallelStream(),
//                                           this.tcpServerMap.values().parallelStream()),
//                             Stream.concat(this.udpClientMap.values().parallelStream(),
//                                           this.udpServerMap.values().parallelStream()));
//    }
//
//    /**
//     * Removes registry of the communication channel identified by the channelId.
//     * @param channelId Identification of channel.
//     * @return Removed registry.
//     */
//    public IotdmRxPluginsBaseRegistry removePluginRegistry(IotdmPluginManager.ChannelIdentifier channelId) {
//        Map<String, IotdmRxPluginsBaseRegistry> map = getMap(channelId.getChannelType(),
//                                                             channelId.getTransportProtocol());
//        if (isNull(map)) {
//            LOG.error("Unsupported channelType ({}) or transportProtocol ({})",
//                      channelId.getChannelType(), channelId.getTransportProtocol());
//            return null;
//        }
//
//        return map.remove(channelId.getAddrAndPort());
//    }
//
//    /**
//     * Stores new registry in the HashMap according to parameters included
//     * in the channelId.
//     * @param channelId The channelId.
//     * @param newRegistry The new registry to be stored.
//     * @return True in case of success, False otherwise.
//     */
//    public boolean setPluginRegistry(IotdmPluginManager.ChannelIdentifier channelId,
//                                     IotdmRxPluginsBaseRegistry newRegistry) {
//        Map<String, IotdmRxPluginsBaseRegistry> map = getMap(channelId.getChannelType(),
//                                                             channelId.getTransportProtocol());
//        if (isNull(map)) {
//            LOG.error("Unsupported channelType ({}) or transportProtocol ({})",
//                      channelId.getChannelType(), channelId.getTransportProtocol());
//            return false;
//        }
//
//        map.put(channelId.getAddrAndPort(), newRegistry);
//        return true;
//    }
//
//    private Map<String, IotdmRxPluginsBaseRegistry> getMap(
//                                               IotdmBaseRxCommunicationChannel.CommunicationChannelType channelType,
//                                               IotdmBaseRxCommunicationChannel.TransportProtocol transportProtocol) {
//        switch (channelType) {
//            case SERVER:
//                switch (transportProtocol) {
//                    case UDP:
//                        return udpServerMap;
//                    case TCP:
//                        return tcpServerMap;
//                }
//            case CLIENT:
//                switch (transportProtocol) {
//                    case UDP:
//                        return udpClientMap;
//                    case TCP:
//                        return tcpClientMap;
//                }
//        }
//
//        return null;
//    }
//
//    private IotdmRxPluginsBaseRegistry getRegistry(IotdmBaseRxCommunicationChannel.CommunicationChannelType channelType,
//                                                   IotdmBaseRxCommunicationChannel.TransportProtocol transportProtocol,
//                                                   String addrAndPort) {
//        Map<String, IotdmRxPluginsBaseRegistry> map = getMap(channelType, transportProtocol);
//        if (isNull(map)) {
//            LOG.error("Unsupported channelType ({}) or transportProtocol ({})", channelType, transportProtocol);
//            return null;
//        }
//
//        return map.get(addrAndPort);
//    }
//
//    /**
//     * Returns the stored registry exactly specified by the channelId.
//     * @param channelId Channel parameters.
//     * @return The registry if exists, null otherwise.
//     */
//    public IotdmRxPluginsBaseRegistry getPluginRegistry(IotdmPluginManager.ChannelIdentifier channelId) {
//        return getRegistry(channelId.getChannelType(), channelId.getTransportProtocol(),
//                           channelId.getAddrAndPort());
//    }
//
//    /**
//     * Returns the stored registry specified by the channelId but used ipAddres
//     * identifies all interfaces i.e.: "0.0.0.0".
//     * @param channelId Channel parameters (ipAddress is not used).
//     * @return The registry if exists, null otherwise. Null is returned even if one or
//     * more registries are stored for the same port but just for one specific ipAddress.
//     */
//    public IotdmRxPluginsBaseRegistry getPluginRegistryAllInterfaces(IotdmPluginManager.ChannelIdentifier channelId) {
//        return getRegistry(channelId.getChannelType(), channelId.getTransportProtocol(),
//                           channelId.getAllInterfacesAddrAndPort());
//    }
}
