/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.registry;

import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPlugin;
import org.opendaylight.iotdm.plugininfra.pluginmanager.IotdmPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * This registry allows sharing of associated CommunicationChannel. Received requests are processed
 * by plugin which is registered for URI with the longest URI prefix matching the target URI of
 * the received request.
 * The same plugin instance can register for multiple URIs.
 */
public class IotdmRxPluginsRegistrySharedPrefixMatch
    extends IotdmRxPluginsRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmRxPluginsRegistrySharedPrefixMatch.class);
    private final RegistryNode registryRoot = new RegistryNode();

    public IotdmRxPluginsRegistrySharedPrefixMatch(IotdmPluginManager.ChannelIdentifier channelIdentifier) {
        super(channelIdentifier);
    }

    private static boolean isUriValid(@Nonnull final String onem2mUri) {
        /*
         * Allows only (see TS-004 6.2.3):
         *  - digits
         *  - lover / upper letters
         *  - slash '/', dash '-', dot '.', underscore '_', tilde '~'
         */
        String regex = "(([0-9]|[a-z]|[A-Z]|/|-|\\.|_|~|)+)";
        return onem2mUri.matches(regex);
    }

    @Override
    public boolean regPlugin(IotdmPlugin plugin, String onem2mUri) {
        if (! isUriValid(onem2mUri)) {
            LOG.error("Invalid URI passed to plugin registration: {}", onem2mUri);
            return false;
        }

        return this.registryRoot.registerPlugin(onem2mUri, plugin);
    }

    @Override
    public IotdmPlugin getPlugin(String onem2mUri) {
        return this.registryRoot.getPlugin(onem2mUri);
    }

    @Override
    public Stream<Map.Entry<String, IotdmPlugin>> getPluginStream() {
        return this.registryRoot.getPluginMap().entrySet().stream();
    }

    @Override
    public boolean hasPlugin(IotdmPlugin plugin) {
        return this.registryRoot.hasPluginRegistered(plugin);
    }

    @Override
    public boolean hasPlugin(IotdmPlugin plugin, String onem2mUri) {
        return this.registryRoot.hasPluginRegistered(plugin, onem2mUri);
    }

    @Override
    public boolean removePlugin(IotdmPlugin plugin) {
        return this.registryRoot.unregisterPlugin(plugin);
    }

    @Override
    public boolean removePlugin(IotdmPlugin plugin, String onem2mUri) {
        return this.registryRoot.unregisterPlugin(onem2mUri, plugin);
    }

    @Override
    public boolean isEmpty() { return this.registryRoot.isEmpty(); }
}

class RegistryNode {
    private static final Logger LOG = LoggerFactory.getLogger(RegistryNode.class);
    private final Map<String, RegistryNode> registry = new ConcurrentHashMap<>();
    private final AtomicReference<IotdmPlugin> plugin = new AtomicReference<>(null);

    private boolean regPath(@Nonnull final String[] uri,
                            int index,
                            @Nonnull final IotdmPlugin plugin) {
        String path = uri[index];
        index++;

        if (index == uri.length) {
            // We are at the end of URI, register the plugin here
            IotdmPlugin regPlugin = this.plugin.get();
            if (regPlugin != null) {
                if (regPlugin.isPlugin(plugin)) {
                    LOG.warn("Plugin already registered, nothing to do, URI: {} {}",
                             String.join("/", uri), regPlugin.getDebugString());
                    return true;
                } else {
                    LOG.error("Plugin already registered at URI: {}, {} " +
                              "(Conflicts with new plugin: {}",
                              String.join("/", uri), regPlugin.getDebugString(), plugin.getDebugString());
                    return false;
                }
            }

            // There is not any plugin registered, let's register the current one
            this.plugin.set(plugin);
            LOG.info("Registered plugin at shared endpoint: {}, {}",
                     String.join("/", uri), plugin.getDebugString());
            return true;
        }

        if (this.registry.containsKey(path)) {
            // Call this method recursively on the next already existing node on the path
            return this.registry.get(path).regPath(uri, index, plugin);
        } else {
            // Next node on the path doesn't exist, create it and all this method recursively with it
            RegistryNode newNextNode = new RegistryNode();
            this.registry.put(path, newNextNode);
            return newNextNode.regPath(uri, index, plugin);
        }
    }

    public boolean registerPlugin(final String uri, @Nonnull final IotdmPlugin plugin) {
        if (null == uri || uri.isEmpty() || uri.equals("/")) {
            this.plugin.set(plugin);
            LOG.info("Plugin registered at: \'root\', plugin: {}",
                     plugin.getDebugString());
            return true;
        }

        return this.regPath(uri.split("/"), 0, plugin);
    }

    private boolean delPlugin(String uri, IotdmPlugin plugin) {
        IotdmPlugin regPlugin = this.plugin.get();
        if (null == regPlugin) {
            LOG.warn("There's not any plugin registered at: {} " +
                     "Plugin to unregister: {}",
                     uri, plugin.getDebugString());
            // Nothing to do, return true
            return true;
        }

        if (regPlugin.isPlugin(plugin)) {
            this.plugin.set(null);
            LOG.info("Plugin unregistered at: {} {}",
                     uri, plugin.getDebugString());
            return true;
        }

        LOG.error("Attempt to unregister non-registered plugin at: {} " +
                  "Plugin registered: {} " +
                  "Plugin to be unregistered: {}",
                  uri,
                  regPlugin.getDebugString(), plugin.getDebugString());
        return false;
    }

    private boolean unregPath(@Nonnull final String[] uri, int index, @Nonnull final IotdmPlugin plugin) {
        if (index >= uri.length) {
            // we are at the end of URI, the plugin should be registered here
            return delPlugin(String.join("/", uri), plugin);
        }

        String path = uri[index];

        if (! this.registry.containsKey(path)) {
            LOG.error("Path element ({}, index: {}) not found for URI: {}, plugin: {}",
                      path, index, String.join("/", uri), plugin.getDebugString());
            return false;
        }

        RegistryNode nextNode = this.registry.get(path);
        if (null == nextNode) {
            LOG.error("No such URI registered: {}, failed at path: ({}, index: {}), {}",
                      String.join("/", uri), path, index, plugin.getDebugString());
            return false;
        }

        boolean ret = nextNode.unregPath(uri, index + 1, plugin);
        if (! ret) {
            return false;
        }

        // Delete also empty nodes
        if (nextNode.plugin.get() == null && nextNode.registry.isEmpty()) {
            this.registry.remove(path);
        }
        return true;
    }

    private int unregAll(@Nonnull final IotdmPlugin plugin) {
        int occurrences = 0;

        for (Map.Entry<String, RegistryNode> entry : registry.entrySet()) {
            occurrences += entry.getValue().unregAll(plugin);

            if (entry.getValue().plugin.get() == null && entry.getValue().registry.isEmpty()) {
                // remove also next node from local map because it's empty
                this.registry.remove(entry.getKey());
            }

            if (this.plugin.get() != null && this.plugin.get().isPlugin(plugin)) {
                // unregister the plugin locally
                this.plugin.set(null);
                occurrences += 1;
            }
        }

        return occurrences;
    }

    public boolean unregisterPlugin(final String uri, @Nonnull final IotdmPlugin plugin) {
        if (null == uri || uri.isEmpty() || uri.equals("/")) {
            return delPlugin("\'root\'", plugin);
        }

        return this.unregPath(uri.split("/"), 0, plugin);
    }

    public boolean unregisterPlugin(@Nonnull final IotdmPlugin plugin) {
        int occurrences = 0;

        occurrences = this.unregAll(plugin);

        LOG.info("Unregistered all URIs registered for plugin: {}, occurrences: {}",
                 plugin.getDebugString(), occurrences);
        return true;
    }

    private IotdmPlugin getPluginAtSubPath(String [] uri, int index) {
        IotdmPlugin plugin = null;

        if (uri.length <= index) {
            return this.plugin.get();
        }

        if (this.registry.containsKey(uri[index])) {
            plugin = this.registry.get(uri[index]).getPluginAtSubPath(uri, index + 1);
            if (null != plugin) {
                return plugin;
            }
        }

        return this.plugin.get();
    }

    public IotdmPlugin getPlugin(@Nonnull final String uri) {
        IotdmPlugin plugin = null;

        if (uri.isEmpty() || uri.equals("/")) {
            return this.plugin.get(); // Root by default
        }

        // Find the longest URI that match begin of the passed URI
        plugin = this.getPluginAtSubPath(uri.split("/"), 0);
        if (null == plugin) {
            this.plugin.get(); // Root by default
        }

        return plugin;
    }

    public boolean hasPluginRegistered(@Nonnull final IotdmPlugin plugin) {
        if (this.plugin.get() != null && this.plugin.get().isPlugin(plugin)) {
            return true;
        }

        for(Map.Entry<String, RegistryNode> entry : this.registry.entrySet()) {
            if (entry.getValue().hasPluginRegistered(plugin)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasPluginRegisteredAtSubPath(String [] uri, int index, @Nonnull final IotdmPlugin plugin) {
        if (index >= uri.length) {
            // we are at the end of URI, the plugin should be registered here
            if (null != this.plugin.get() && this.plugin.get().isPlugin(plugin)) {
                return true;
            }

            return false;
        }

        String path = uri[index];

        if (! this.registry.containsKey(path)) {
            return false;
        }

        RegistryNode nextNode = this.registry.get(path);
        if (null == nextNode) {
            return false;
        }

        return nextNode.hasPluginRegisteredAtSubPath(uri, index + 1, plugin);
    }

    public boolean hasPluginRegistered(@Nonnull final IotdmPlugin plugin, @Nonnull final String uri) {
        if (uri.isEmpty() || uri.equals("/")) {
            return this.hasPluginRegisteredAtSubPath(new String[0], 0, plugin);
        }

        return this.hasPluginRegisteredAtSubPath(uri.split("/"), 0, plugin);
    }

    private void getPluginMap(Map<String, IotdmPlugin> map, String currentUrl) {
        IotdmPlugin plugin = this.plugin.get();
        if (null != plugin) {
            map.put(currentUrl, plugin);
        }

        for (Map.Entry<String, RegistryNode> entry: this.registry.entrySet()) {
            entry.getValue().getPluginMap(map, currentUrl + entry.getKey() + "/");
        }
    }

    public Map<String, IotdmPlugin> getPluginMap() {
        Map<String, IotdmPlugin> map = new HashMap<>();
        this.getPluginMap(map, "/");
        return map;
    }

    public boolean isEmpty() {
        return (null == this.plugin.get() && this.registry.isEmpty());
    }
}