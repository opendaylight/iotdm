/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins;

import org.opendaylight.iotdm.onem2m.plugins.channels.Onem2mBaseCommunicationChannel;
import org.opendaylight.iotdm.onem2m.plugins.channels.Onem2mPluginChannelFactory;
import org.opendaylight.iotdm.onem2m.plugins.channels.coap.Onem2mCoapPluginServerFactory;
import org.opendaylight.iotdm.onem2m.plugins.channels.http.Onem2mHttpPluginServerFactory;
import org.opendaylight.iotdm.onem2m.plugins.channels.http.Onem2mHttpsPluginServerFactory;
import org.opendaylight.iotdm.onem2m.plugins.registry.Onem2mExclusiveRegistry;
import org.opendaylight.iotdm.onem2m.plugins.registry.Onem2mLocalEndpointRegistry;
import org.opendaylight.iotdm.onem2m.plugins.registry.Onem2mSharedExactMatchRegistry;
import org.opendaylight.iotdm.onem2m.plugins.registry.Onem2mSharedPrefixMatchRegistry;
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
public class Onem2mPluginManager implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mPluginManager.class);
    private static Onem2mPluginManager _instance;

    /* Specific IP address which means that plugin registers for all
     * local interfaces
     */
    private static final String AllInterfaces = "0.0.0.0";

    // Supported protocols
    public static final String ProtocolHTTP = "http";
    public static final String ProtocolHTTPS = "https";
    public static final String ProtocolCoAP = "coap";
    // TODO: uncomment when support added
//    public static final String ProtocolCoAPS = "coaps";
//    public static final String ProtocolMQTT = "mqtt";
//    public static final String ProtocolMQTTS = "mqtts";

    // The main registry of the PluginManager
    private final PluginManagerRegistry registry = new PluginManagerRegistry();

    // Variable used for generating instance keys unique for every IotdmPlugin instance
    private int pluginInstanceKeyGen = 0;

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
        // TODO add next supported protocols
    }

    /**
     * Returns the only instance of the PluginManager.
     * @return Singleton instance.
     */
    static public Onem2mPluginManager getInstance() {
        if (_instance == null) {
            _instance = new Onem2mPluginManager();
        }
        return _instance;
    }

    /**
     * Generates unique instance key for instances of IotdmPlugin.
     * @return Generated instance key.
     */
    protected int getNewPluginInstanceKey() {
        return ++this.pluginInstanceKeyGen;
    }

    /**
     * Registers plugin to receive HTTP requests. Port number of the
     * HTTP server is specified and new server is started if needed.
     * @param plugin Instance of IotdmPlugin to register.
     * @param port Local TCP port number of the HTTP server.
     * @param mode Registry sharing mode.
     * @param uri Local URI for which the plugin is registering.
     * @return True in case of successful registration, False otherwise.
     */
    public boolean registerPluginHttp(IotdmPlugin plugin, int port, Onem2mPluginManager.Mode mode, String uri) {
        return registerPlugin(plugin, ProtocolHTTP, AllInterfaces, port, mode, uri, null);
    }

    /**
     * Registers plugin to receive HTTP requests. Port number of the
     * HTTPS server is specified and new server is started if needed.
     * @param plugin Instance of IotdmPlugin to register.
     * @param port Local TCP port number of the HTTPS server.
     * @param mode Registry sharing mode.
     * @param uri Local URI for which the plugin is registering.
     * @param configuration Configuration of HTTPS server.
     * @return True in case of successful registration, False otherwise.
     */
    public boolean registerPluginHttps(IotdmPlugin plugin, int port, Onem2mPluginManager.Mode mode,
                                       String uri, Object configuration) {
        return registerPlugin(plugin, ProtocolHTTPS, AllInterfaces, port, mode, uri, configuration);
    }

    /**
     * Registers plugin to receive COAP requests. Port number of the
     * COAP server is specified and new server is started if needed.
     * @param plugin Instance of IotdmPlugin to register.
     * @param port Local UDP port number of the COAP server.
     * @param mode Registry sharing mode.
     * @param uri Local URI for which the plugin is registering.
     * @return True in case of successful registration, False otherwise.
     */
    public boolean registerPluginCoap(IotdmPlugin plugin, int port, Onem2mPluginManager.Mode mode, String uri) {
        return registerPlugin(plugin, ProtocolCoAP, AllInterfaces, port, mode, uri, null);
    }

    // TODO add registration methods for other supported protocols


    // TODO this is here for backward compatibility purposes
    public int registerPluginAtPort(String protocol, IotdmPlugin instance, int port, Onem2mPluginManager.Mode mode) {
        return this.registerPluginAtPort(protocol, instance, port, mode, null);
    }

    public int registerPluginAtPort(String protocol, IotdmPlugin instance, int port, Onem2mPluginManager.Mode mode,
                                    Object configuration) {
        this.registerPlugin(instance, protocol, AllInterfaces, port, mode, null, configuration);
        return 0;
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
     * @param configuration Configuration for CommunicationChannel if needed. Null can be passed.
     * @return True if the registration has been successful, False otherwise.
     */
    private boolean registerPlugin(IotdmPlugin plugin, String protocolName,
                                   String ipAddress, int port,
                                   Onem2mPluginManager.Mode mode, String uri,
                                   Object configuration) {

        // Get channel factory
        if (! pluginChannelFactoryMap.containsKey(protocolName)) {
            LOG.error("Attempt to register for unsupported protocol: {}", protocolName);
            return false;
        }

        Onem2mPluginChannelFactory channelFactory = pluginChannelFactoryMap.get(protocolName);

        // Check the ipAddress
        if (isNull(ipAddress) || ipAddress.isEmpty()) {
            ipAddress = AllInterfaces;
        }
        if (! validateIpAddress(ipAddress)) {
            LOG.error("Invalid ipAddress passed: {}, plugin: {}", ipAddress, plugin.getDebugString());
            return false;
        }

        // Prepare channel identifier
        ChannelIdentifier chId = new ChannelIdentifier(channelFactory.getChannelType(),
                                                       channelFactory.getTransportProtocol(),
                                                       ipAddress, port, protocolName, mode);

        if (! isIpAll(ipAddress)) {
            // Check if the specific IP address doesn't collide with registration for all interfaces
            Onem2mLocalEndpointRegistry registryAll = this.registry.getPluginRegistryAllInterfaces(chId);
            if (nonNull(registryAll)) {
                LOG.error("Failed to register plugin {} at channel: {}. Resources already used by channel: {}",
                          plugin.getDebugString(), chId.getDebugString(), registryAll);
                return false;
            }
        }

        // Check if the specific IP address and port are not already in use for another protocol
        Onem2mLocalEndpointRegistry endpointReg = this.registry.getPluginRegistry(chId);
        if (nonNull(endpointReg)) {
            if (! endpointReg.getProtocol().equals(chId.getProtocolName())) {
                LOG.error("Failed to register plugin {} at channel: {}. Resources already used for protocol {}",
                          plugin.getDebugString(), chId.getDebugString(), endpointReg.getProtocol());
                return false;
            }

            // Verify mode
            if (endpointReg.getMode() != chId.mode) {
                LOG.error("Failed to register plugin {} at channel: {}. Resources already used with mode: {}",
                          plugin.getDebugString(), chId.getDebugString(), endpointReg.getMode());
                return false;
            }

            // Configuration must equal if exists
            if (nonNull(configuration)) {
                if (! endpointReg.getAssociatedChannel().validateConfig(configuration)) {
                    LOG.error("Failed to register plugin {} at channel: {}. Invalid configuration passed",
                              plugin.getDebugString(), chId.getDebugString());
                    return false;
                }

                if (! endpointReg.getAssociatedChannel().compareConfig(configuration)) {
                    LOG.error("Failed to register plugin {} at channel: {}. Different configuration passed",
                              plugin.getDebugString(), chId.getDebugString());
                    return false;
                }
            }

            // Verify whether the same URI is not already registered
            IotdmPlugin regPlugin = endpointReg.getPlugin(uri);
            if (nonNull(regPlugin)) {
                // Maybe double registration ?
                if (regPlugin.isPlugin(plugin)) {
                    LOG.warn("Double registration of plugin: {} at channel: {} or URI: {}",
                             plugin.getDebugString(), chId.getDebugString(), uri);
                    return true;
                }

                // URI already registered by another plugin
                LOG.error("Failed to register plugin: {} at channel: {}. URI: {} already in use by plugin: {}",
                          plugin.getDebugString(), chId.getDebugString(), uri, regPlugin.getDebugString());
                return false;
            }

            // register plugin in the registry
            if (! endpointReg.regPlugin(plugin, uri)) {
                LOG.error("Failed to register plugin {} at channel: {}",
                          plugin.getDebugString(), chId.getDebugString());
                return false;
            }

            // registration successful
            LOG.info("Plugin: {} registered at shared channel: {} for URI: {}",
                     plugin.getDebugString(), chId.getDebugString(), uri);
            return true;
        } else { // IpAddress and port are not in use
            // Instantiate registry according to specified mode
            Onem2mLocalEndpointRegistry newRegistry;
            switch (chId.getMode()) {
                case Exclusive:
                    newRegistry = new Onem2mExclusiveRegistry(chId);
                    break;
                case SharedPrefixMatch:
                    newRegistry = new Onem2mSharedPrefixMatchRegistry(chId);
                    break;
                case SharedExactMatch:
                    newRegistry = new Onem2mSharedExactMatchRegistry(chId);
                    break;
                default:
                    LOG.error("Registry for mode: {} not implemented", chId.getMode());
                    return false;
            }

            // register plugin in the new registry
            if (! newRegistry.regPlugin(plugin, uri)) {
                LOG.error("Failed to register plugin {} at channel: {}",
                          plugin.getDebugString(), chId.getDebugString());
                return false;
            }

            // create new channel
            Onem2mBaseCommunicationChannel newChannel =
                    channelFactory.createInstance(chId.getIpAddress(), chId.getPort(), configuration,
                                                  newRegistry);
            if (null == newChannel) {
                LOG.error("Failed to instantiate channel: {} for plugin: {}",
                          chId.getDebugString(), plugin.getDebugString());
                return false;
            }

            // associate channel and registry also in reverse direction
            newRegistry.setAssociatedChannel(newChannel);

            // store the endpoint registry in the PluginManager registry
            if (! this.registry.setPluginRegistry(chId, newRegistry)) {
                LOG.error("Failed to register plugin: {} at channel: {}. " +
                          "Failed to store channel registry in PluginManager registry",
                          plugin.getDebugString(), chId.getDebugString());
                // close running channel
                try {
                    newChannel.close();
                } catch (Exception e) {
                    LOG.error("Failed to close running channel: {}", chId.getDebugString());
                }
                return false;
            }

            // registration successful
            LOG.info("Plugin: {} registered at new channel: {} for URI: {}",
                     plugin.getDebugString(), chId.getDebugString(), uri);
            return true;
        }
    }

    private void closeRegistryChannel(Onem2mLocalEndpointRegistry endpointRegistry) {
        try {
            endpointRegistry.getAssociatedChannel().close();
        } catch (Exception e) {
            LOG.error("Failed to close non used channel: {}, error: {}",
                      endpointRegistry.getChannelId().getDebugString(), e);
        }
    }

    /**
     * Unregisters already registered plugin. Channels which
     * are not needed anymore are closed.
     * @param plugin The instance of IotdmPlugin to unregister.
     */
    public void unregisterPlugin(IotdmPlugin plugin) {
        this.registry.registryStream().forEach( endpointRegistry -> {
            if (endpointRegistry.hasPlugin(plugin)) {
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
     * Closes all running channels.
     * @throws Exception closing error
     */
    @Override
    public void close() throws Exception {
        this.registry.registryStream().forEach( endpointRegistry -> {
            closeRegistryChannel(endpointRegistry);
            this.registry.removePluginRegistry(endpointRegistry.getChannelId());
        });
    }

    /**
     * Stores set of parameters of communication channel
     */
    public class ChannelIdentifier {
        private final Onem2mBaseCommunicationChannel.CommunicationChannelType channelType;
        private final Onem2mBaseCommunicationChannel.TransportProtocol transportProtocol;
        private final String ipAddress;
        private final int port;
        private final String protocolName;
        private final Mode mode;

        private final String addrAndPort;

        public ChannelIdentifier(Onem2mBaseCommunicationChannel.CommunicationChannelType channelType,
                                 Onem2mBaseCommunicationChannel.TransportProtocol transportProtocol,
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

        public Onem2mBaseCommunicationChannel.CommunicationChannelType getChannelType() {
            return channelType;
        }

        public Onem2mBaseCommunicationChannel.TransportProtocol getTransportProtocol() {
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
 *  instances of the Onem2mLocalEndpointRegistry where the running channel is stored
 *  together with registered plugins.
 */
class PluginManagerRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(PluginManagerRegistry.class);

    // TCP/UDP and Client/Server registries
    private final Map<String, Onem2mLocalEndpointRegistry> tcpClientMap = new ConcurrentHashMap<>();
    private final Map<String, Onem2mLocalEndpointRegistry> tcpServerMap = new ConcurrentHashMap<>();
    private final Map<String, Onem2mLocalEndpointRegistry> udpClientMap = new ConcurrentHashMap<>();
    private final Map<String, Onem2mLocalEndpointRegistry> udpServerMap = new ConcurrentHashMap<>();

    /**
     * Returns stream including all instances of the Onem2mLocalEndpointRegistry
     * stored in all HashMaps.
     * @return Stream of Onem2mLocalEndpointRegistry instances
     */
    public Stream<Onem2mLocalEndpointRegistry> registryStream() {
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
    public Onem2mLocalEndpointRegistry removePluginRegistry(Onem2mPluginManager.ChannelIdentifier channelId) {
        Map<String, Onem2mLocalEndpointRegistry> map = getMap(channelId.getChannelType(),
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
    public boolean setPluginRegistry(Onem2mPluginManager.ChannelIdentifier channelId,
                                     Onem2mLocalEndpointRegistry newRegistry) {
        Map<String, Onem2mLocalEndpointRegistry> map = getMap(channelId.getChannelType(),
                                                              channelId.getTransportProtocol());
        if (isNull(map)) {
            LOG.error("Unsupported channelType ({}) or transportProtocol ({})",
                      channelId.getChannelType(), channelId.getTransportProtocol());
            return false;
        }

        map.put(channelId.getAddrAndPort(), newRegistry);
        return true;
    }

    private Map<String, Onem2mLocalEndpointRegistry> getMap(
                                               Onem2mBaseCommunicationChannel.CommunicationChannelType channelType,
                                               Onem2mBaseCommunicationChannel.TransportProtocol transportProtocol) {
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

    private Onem2mLocalEndpointRegistry getRegistry(Onem2mBaseCommunicationChannel.CommunicationChannelType channelType,
                                                    Onem2mBaseCommunicationChannel.TransportProtocol transportProtocol,
                                                    String addrAndPort) {
        Map<String, Onem2mLocalEndpointRegistry> map = getMap(channelType, transportProtocol);
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
    public Onem2mLocalEndpointRegistry getPluginRegistry(Onem2mPluginManager.ChannelIdentifier channelId) {
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
    public Onem2mLocalEndpointRegistry getPluginRegistryAllInterfaces(Onem2mPluginManager.ChannelIdentifier channelId) {
        return getRegistry(channelId.getChannelType(), channelId.getTransportProtocol(),
                           channelId.getAllInterfacesAddrAndPort());
    }
}
