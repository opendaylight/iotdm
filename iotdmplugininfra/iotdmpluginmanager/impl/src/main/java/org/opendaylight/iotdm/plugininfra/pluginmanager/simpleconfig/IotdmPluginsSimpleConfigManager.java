/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.simpleconfig;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;
import org.opendaylight.iotdm.plugininfra.pluginmanager.customservices.PluginManagerServicesUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginFilters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.data.definition.IotdmCommonPluginData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.simple.config.client.registrations.output.registered.simple.config.client.plugins.table.RegisteredSimpleConfigClientPluginInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.simple.config.client.registrations.output.registered.simple.config.client.plugins.table.RegisteredSimpleConfigClientPluginInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.simpleconfig.rev161122.IotdmSimpleConfigStartupConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.simpleconfig.rev161122.iotdm.simple.config.list.definition.IotdmSimpleConfigList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.simpleconfig.rev161122.iotdm.simple.config.list.definition.IotdmSimpleConfigListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.simpleconfig.rev161122.iotdm.simple.config.list.definition.IotdmSimpleConfigListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.simpleconfig.rev161122.iotdm.simple.config.list.definition.iotdm.simple.config.list.PluginInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.simpleconfig.rev161122.iotdm.simple.config.list.definition.iotdm.simple.config.list.PluginInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.simpleconfig.rev161122.iotdm.simple.config.list.definition.iotdm.simple.config.list.PluginInstancesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.simpleconfig.rev161122.plugin.simple.config.definition.PluginSimpleConfig;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class IotdmPluginsSimpleConfigManager {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmPluginsSimpleConfigManager.class);
    private final Map<String, SimpleConfigPluginRegistrations> registrations = new ConcurrentHashMap<>();
    private static final IotdmPluginsSimpleConfigManager instance = new IotdmPluginsSimpleConfigManager();

    // RW lock protecting from concurrent access to dataBroker
    private final ReadWriteLock brokerRwLock = new ReentrantReadWriteLock();
    private DataBroker dataBroker = null;

    private class SimpleConfigPluginRegistrations {
        private final Map<String, PluginRegCfg> instances = new ConcurrentHashMap<>();
        public Map<String, PluginRegCfg> getInstances() {
            return instances;
        }
    }

    private class PluginRegCfg {
        private IotdmPluginSimpleConfigClient plugin;

        // Protects single plugin instance from concurrent access to
        // it's configuration.
        // This way we don't have to restrict implementations of configure() method
        // of plugins to be thread safe.
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

        public PluginRegCfg(IotdmPluginSimpleConfigClient plugin) { //}, IotdmSimpleConfig config) {
            this.plugin = plugin;
        }

        private void setPlugin(IotdmPluginSimpleConfigClient plugin) {
            this.plugin = plugin;
        }

        public IotdmPluginSimpleConfigClient getPlugin() {
            return plugin;
        }

        private void rLock() {
            this.rwLock.readLock().lock();
        }

        private void wLock() {
            this.rwLock.writeLock().lock();
        }

        public void rUnlock() {
            this.rwLock.readLock().unlock();
        }

        public void wUnlock() {
            this.rwLock.writeLock().unlock();
        }
    }

    private IotdmPluginsSimpleConfigManager() {}

    /**
     * Sets DataBroker and walks StartupConfig and configures all
     * registered plugin instances.
     * @param dataBroker DataBroker
     */
    protected void setDataBroker(DataBroker dataBroker) {
        this.brokerRwLock.writeLock().lock();
        try {
            this.dataBroker = dataBroker;

            // Walk all registered plugins and configure current StartupConfig
            List<IotdmSimpleConfigList> startupCfg = null;
            try {
                startupCfg = getStartupConfig(null);
            } catch (IotdmPluginSimpleConfigException e) {
                LOG.error("Failed to get current StartupConfig: {}", e);
            }

            if (null != startupCfg && ! startupCfg.isEmpty()) {
                for (IotdmSimpleConfigList allInstancesCfg : startupCfg) {
                    for (PluginInstances instanceCfg : allInstancesCfg.getPluginInstances()) {
                        PluginRegCfg reg = findInstanceReg(allInstancesCfg.getPluginName(),
                                                           instanceCfg.getInstanceName());
                        if (null == reg || null == reg.getPlugin()) {
                            // Not registered, just continue
                            continue;
                        }

                        try {
                            this.configureInstance(allInstancesCfg.getPluginName(),
                                                   instanceCfg.getInstanceName(),
                                                   new IotdmSimpleConfig(instanceCfg.getPluginSimpleConfig()));
                        } catch (IotdmPluginSimpleConfigException e) {
                            LOG.error("Failed to update RunningConfig by StartupConfig of PluginName: {}, Instance: {}: {}",
                                      allInstancesCfg.getPluginName(), instanceCfg.getInstanceName(), e);
                        }

                        LOG.info("Updated RunningConfig by StartupConfig of PluginName: {}, Instance: {}",
                                 allInstancesCfg.getPluginName(), instanceCfg.getInstanceName());
                    }
                }
            }
        } finally {
            this.brokerRwLock.writeLock().unlock();
        }

        LOG.info("New data broker set");
    }

    protected void unsetDataBroker() {
        this.brokerRwLock.writeLock().lock();
        this.dataBroker = null;
        this.brokerRwLock.writeLock().unlock();
        LOG.info("Data broker unset");
    }

    /**
     * Returns singleton instance of the SimpleConfig manager.
     * @return The SimpleConfig manager singleton instance
     */
    public static IotdmPluginsSimpleConfigManager getInstance() {
        return instance;
    }

    private PluginRegCfg findInstanceReg(final String pluginName, final String instanceId) {
        if (this.registrations.containsKey(pluginName) &&
            this.registrations.get(pluginName).getInstances().containsKey(instanceId)) {
            return this.registrations.get(pluginName).getInstances().get(instanceId);
        }
        return null;
    }

    public boolean isRegistered(final String pluginName, final String instanceId) {
        return null != findInstanceReg(pluginName, instanceId);
    }

    // Reads configuration from plugin
    protected IotdmSimpleConfig getConfig(final String pluginName, final String instanceId)
            throws IotdmPluginSimpleConfigException {

        PluginRegCfg regCfg = findInstanceReg(pluginName, instanceId);

        if (null == regCfg) {
            LOG.error("Attempt to get config of non-existing plugin instance: pluginName: {}, instanceId: {}",
                      pluginName, instanceId);
            throw new IotdmPluginSimpleConfigException("No such plugin instance");
        }

        regCfg.rLock();
        try {
            return regCfg.getPlugin().getSimpleConfig();
        } finally {
            regCfg.rUnlock();
        }
    }

    // Expects obtained dataBroker W lock
    private void configureInstance(final String pluginName, final String instanceId, IotdmSimpleConfig cfg)
            throws IotdmPluginSimpleConfigException {
        PluginRegCfg regCfg = findInstanceReg(pluginName, instanceId);

        if (null == regCfg) {
            LOG.error("Attempt to configure non-existing plugin instance: pluginName: {}, instanceId: {}",
                      pluginName, instanceId);
            throw new IotdmPluginSimpleConfigException("No such plugin instance");
        }

        regCfg.wLock();
        try {
            regCfg.getPlugin().configure(cfg);
            return;
        } catch (IotdmPluginSimpleConfigException e) {
            LOG.error("Plugin configuration failed for pluginName: {}, instanceId: {}, msg: {}",
                      pluginName, instanceId, e);
            throw e;
        } catch (Exception e) {
            LOG.error("Plugin configuration failed for pluginName: {}, instanceId: {}, msg: {}",
                      pluginName, instanceId, e);
            throw new IotdmPluginSimpleConfigException("Unable to configure plugin instance");
        } finally {
            regCfg.wUnlock();
        }
    }

    private InstanceIdentifier<PluginSimpleConfig> getInstanceId(final String pluginName, final String instanceId) {
        InstanceIdentifier<PluginSimpleConfig> iid =
                InstanceIdentifier.builder(IotdmSimpleConfigStartupConfig.class)
                        .child(IotdmSimpleConfigList.class, new IotdmSimpleConfigListKey(pluginName))
                        .child(PluginInstances.class, new PluginInstancesKey(instanceId))
                        .child(PluginSimpleConfig.class).build();

        return iid;
    }

    /**
     * Configures the plugin instance identified by PluginName and InstanceId.
     * Re-throws configuration exception from the plugin instance configuration method.
     * @param pluginName The name of the plugin implementation
     * @param instanceId The unique instance ID
     * @param cfg SimpleConfig configuration for the instance
     * @throws IotdmPluginSimpleConfigException
     */
    protected void configurePluginInstance(final String pluginName, final String instanceId,
                                           IotdmSimpleConfig cfg)
            throws IotdmPluginSimpleConfigException {

        // Lock DataBroker, this avoids concurrent modification of StartupConfig and RunningConfig
        this.brokerRwLock.writeLock().lock();
        try {
            this.checkDataBroker();

            // Configure the plugin instance if registered
            PluginRegCfg regCfg = findInstanceReg(pluginName, instanceId);
            if (null != regCfg && null != regCfg.getPlugin()) {
                // Configure the new configuration to plugin
                try {
                    configureInstance(pluginName, instanceId, cfg);
                } catch (IotdmPluginSimpleConfigException e) {
                    // Try to rollback the StartupConfig
                    LOG.error("Failed to configure SimpleConfig: " +
                              "PluginName: {}, Instance: {}: {}", pluginName, instanceId, e);
                    throw e;
                }
            } else {
                throw new IotdmPluginSimpleConfigException(
                    "No such plugin instance: pluginName: "  + pluginName + " instanceId: " +  instanceId);
            }

            // Store the new configuration in startup config
            try {
                WriteTransaction wrTransaction = this.dataBroker.newWriteOnlyTransaction();

                InstanceIdentifier<PluginSimpleConfig> iid = getInstanceId(pluginName, instanceId);

                if (null != cfg) {
                    wrTransaction.put(LogicalDatastoreType.CONFIGURATION,
                                      iid, cfg.getConfiguration(), true);
                } else {
                    wrTransaction.delete(LogicalDatastoreType.CONFIGURATION, iid);
                }
                wrTransaction.submit().checkedGet();
            } catch (Exception e) {
                LOG.error("Failed to write new configuration into StartupConfig: " +
                          "PluginName: {}, Instance: {}: {}", pluginName, instanceId, e);
                if (null != regCfg && null != regCfg.getPlugin()) {
                    // This is very bad, we have inconsistent config !!!
                    throw new IotdmPluginSimpleConfigException(
                                      "Failed to write new startup config of plugin (" +
                                      pluginName +
                                      "), instance (" +
                                      instanceId +
                                      ") to data store. StartupConfig and RunningConfig are inconsistent now!");
                } else {
                    throw new IotdmPluginSimpleConfigException(
                                      "Failed to write new startup config of non-registered plugin (" +
                                      pluginName +
                                      "), instance (" +
                                      instanceId +
                                      ") to data store.");
                }
            }
        } finally {
            this.brokerRwLock.writeLock().unlock();
        }
    }

    private void handleRegistrationError(String format, String... args) throws IotdmPluginRegistrationException {
        PluginManagerServicesUtils.handleRegistrationError(LOG, format, args);
    }

    /**
     * Registers plugin instance, throws RegistrationException in case of failure.
     * @param plugin Plugin instance
     * @throws IotdmPluginRegistrationException
     */
    public void registerSimpleConfigPlugin(IotdmPluginSimpleConfigClient plugin)
            throws IotdmPluginRegistrationException {
        if (null != findInstanceReg(plugin.getPluginName(), plugin.getInstanceName())) {
            this.handleRegistrationError("SimpleConfigClient plugin already registered, PluginName: {}, Instance: {}",
                                         plugin.getPluginName(), plugin.getInstanceName());
        }

        SimpleConfigPluginRegistrations instancesRegs = null;
        boolean newPlugin = false;
        if (this.registrations.containsKey(plugin.getPluginName())) {
            instancesRegs = this.registrations.get(plugin.getPluginName());
        } else {
            instancesRegs = new SimpleConfigPluginRegistrations();
            newPlugin = true;
        }

        // Configure from startup config
        try {
            PluginSimpleConfig cfg = getInstanceStartupConfig(plugin.getPluginName(), plugin.getInstanceName());
            // We can configure the plugin directly since it's registration is not saved yet
            if (null != cfg) {
                plugin.configure(new IotdmSimpleConfig(cfg));

                LOG.info("Updated RunningConfig by StartupConfig of PluginName: {}, Instance: {}",
                         plugin.getPluginName(), plugin.getInstanceName());
            }
        } catch (Exception e) {
            LOG.error("Failed to update RunningConfig by StartupConfig of PluginName: {}, Instance: {}: {}",
                      plugin.getPluginName(), plugin.getInstanceName(), e);
        }

        // Make the new registration available at the end of processing so
        // it's not needed to lock the registration
        instancesRegs.getInstances().put(plugin.getInstanceName(), new PluginRegCfg(plugin));
        if (newPlugin) {
            this.registrations.put(plugin.getPluginName(), instancesRegs);
        }

        LOG.info("SimpleConfigManager: Registered plugin instance: pluginName {}, instanceId: {}",
                 plugin.getPluginName(), plugin.getInstanceName());
    }

    /**
     * Unregisters the plugin instance.
     * @param plugin Plugin instance
     */
    public void unregisterSimpleConfigPlugin(IotdmPluginSimpleConfigClient plugin) {
        PluginRegCfg reg = findInstanceReg(plugin.getPluginName(), plugin.getInstanceName());
        if (null != reg) {
            reg.wLock();
            try {
                this.registrations.get(plugin.getPluginName()).getInstances().remove(plugin.getInstanceName());
                if (this.registrations.get(plugin.getPluginName()).getInstances().isEmpty()) {
                    this.registrations.remove(plugin.getPluginName());
                }
                reg.setPlugin(null);
            } finally {
                reg.wUnlock();
            }

            LOG.info("SimpleConfigManager: Unregistered plugin instance: pluginName {}, instanceId: {}",
                     plugin.getPluginName(), plugin.getInstanceName());
        }
    }

    // expects obtained R/W lock
    private void checkDataBroker() throws IotdmPluginSimpleConfigException {
        if (null == this.dataBroker) {
            LOG.error("No dataBroker set");
            throw new IotdmPluginSimpleConfigException(
                                                 "Internal error, unable to access startup config data store");
        }
    }

    /**
     * Collects list of startup configurations of plugin instances.
     * Plugin instances of the specific plugin implementation are filtered if the
     * pluginName parameter is set.
     * Throws SimpleConfigException in case of failure.
     * @param pluginName The name of the plugin implementation
     * @return List of startup configurations of instances
     * @throws IotdmPluginSimpleConfigException
     */
    protected List<IotdmSimpleConfigList> getStartupConfig(String pluginName)
            throws IotdmPluginSimpleConfigException {

        if (null == pluginName) {
            // Get complete running config
            InstanceIdentifier<IotdmSimpleConfigStartupConfig> iid =
                    InstanceIdentifier.builder(IotdmSimpleConfigStartupConfig.class).build();

            Optional<IotdmSimpleConfigStartupConfig> startupConfig = null;
            this.brokerRwLock.readLock().lock();
            try {
                this.checkDataBroker();
                ReadTransaction readTransaction = this.dataBroker.newReadOnlyTransaction();
                startupConfig = readTransaction.read(LogicalDatastoreType.CONFIGURATION, iid).checkedGet();
            } catch (ReadFailedException e) {
                LOG.error("Failed to read startup config: {}", e.toString());
                throw new IotdmPluginSimpleConfigException(
                                                                   "Failed to read startup config from data store.");
            }
            finally {
                this.brokerRwLock.readLock().unlock();
            }

            if (!startupConfig.isPresent()) {
                LOG.info("There's not startup config stored");
                return null;
            }

            return startupConfig.get().getIotdmSimpleConfigList();
        } else {
            // Filter only instances of the specific plugin implementation
            InstanceIdentifier<IotdmSimpleConfigList> iid =
                    InstanceIdentifier.create(IotdmSimpleConfigStartupConfig.class)
                    .child(IotdmSimpleConfigList.class, new IotdmSimpleConfigListKey(pluginName));

            Optional<IotdmSimpleConfigList> startupConfig = null;
            this.brokerRwLock.readLock().lock();
            try {
                this.checkDataBroker();
                ReadTransaction readTransaction = this.dataBroker.newReadOnlyTransaction();
                startupConfig = readTransaction.read(LogicalDatastoreType.CONFIGURATION, iid).checkedGet();
            } catch (ReadFailedException e) {
                LOG.error("Failed to read startup config of plugin {}: {}", pluginName, e.toString());
                throw new IotdmPluginSimpleConfigException(
                                    "Failed to read startup config of plugin " + pluginName + " from data store.");
            }
            finally {
                this.brokerRwLock.readLock().unlock();
            }

            if (!startupConfig.isPresent()) {
                LOG.info("There's not startup config for plugin {} stored", pluginName);
                return null;
            }

            LinkedList<IotdmSimpleConfigList> list = new LinkedList<>();
            list.add(startupConfig.get());
            return list;
        }
    }

    /**
     * Returns startup configuration of specific plugin instance.
     * Throws SimpleConfig exception in case of failure.
     * @param pluginName Name of the plugin implementation
     * @param instanceId Unique ID of the plugin instance
     * @return Startup configuration of the plugin instance
     * @throws IotdmPluginSimpleConfigException
     */
    protected PluginSimpleConfig getInstanceStartupConfig(String pluginName, String instanceId)
                            throws IotdmPluginSimpleConfigException {
        InstanceIdentifier<PluginSimpleConfig> iid = this.getInstanceId(pluginName, instanceId);

        Optional<PluginSimpleConfig> startupConfig = null;
        this.brokerRwLock.readLock().lock();
        try {
            this.checkDataBroker();
            ReadTransaction readTransaction = this.dataBroker.newReadOnlyTransaction();
            startupConfig = readTransaction.read(LogicalDatastoreType.CONFIGURATION, iid).checkedGet();
        } catch (ReadFailedException e) {
            LOG.error("Failed to read startup config of plugin {}, instance {}: {}",
                      pluginName, instanceId, e.toString());
            throw new IotdmPluginSimpleConfigException("Failed to read startup config of plugin (" +
                                                       pluginName +
                                                       "), instance (" +
                                                       instanceId +
                                                       ") from data store.");
        }
        finally {
            this.brokerRwLock.readLock().unlock();
        }

        if (!startupConfig.isPresent()) {
            LOG.info("There's not startup config stored for plugin ({}), instance ({})", pluginName, instanceId);
            return null;
        }

        return startupConfig.get();
    }

    /**
     * Collects list of running configurations of plugin instances.
     * Plugin instances of the specific plugin implementation are filtered if the
     * pluginName parameter is set.
     * Throws SimpleConfigException in case of failure.
     * @param pluginName The name of the plugin implementation
     * @return List of running configurations of instances
     * @throws IotdmPluginSimpleConfigException
     */
    protected List<IotdmSimpleConfigList> getRunningConfig(String pluginName)
            throws IotdmPluginSimpleConfigException {
        List<IotdmSimpleConfigList> config = new LinkedList();
        for (Map.Entry<String, SimpleConfigPluginRegistrations> pluginReg : this.registrations.entrySet()) {
            if (null != pluginName && ! pluginReg.getKey().equals(pluginName)) {
                // filter out non matching plugin names
                continue;
            }

            List<PluginInstances> instancesList = new LinkedList<>();

            for (Map.Entry<String, PluginRegCfg> instanceReg : pluginReg.getValue().getInstances().entrySet()) {
                IotdmSimpleConfigBuilder cfgBuilder = null;

                instanceReg.getValue().rLock();
                try {
                    // Builder constructor will create a copy of the configuration
                    IotdmSimpleConfig cfg = instanceReg.getValue().getPlugin().getSimpleConfig();
                    if (null != cfg) {
                        cfgBuilder = new IotdmSimpleConfigBuilder(cfg.getConfiguration());
                    }
                } finally {
                    instanceReg.getValue().rUnlock();
                }

               PluginInstancesBuilder instance = new PluginInstancesBuilder()
                       .setInstanceName(instanceReg.getKey());

               if (null != cfgBuilder) {
                   instance.setPluginSimpleConfig(cfgBuilder.build().getConfiguration());
               }

               instancesList.add(instance.build());
            }

            IotdmSimpleConfigListBuilder builder = new IotdmSimpleConfigListBuilder()
                                                            .setPluginName(pluginReg.getKey())
                                                            .setPluginInstances(instancesList);
            config.add(builder.build());
        }

        if (config.isEmpty() && null != pluginName) {
            throw new IotdmPluginSimpleConfigException(
                              "GET running config failed for " + pluginName + " : No such plugin registered");
        }
        return config;
    }

    /**
     * Returns map of all registrations.
     * @return Map of all registrations
     */
    public Map<String, List<RegisteredSimpleConfigClientPluginInstances>> getRegistrationsMap(
                                                                                final IotdmPluginFilters filters) {

        // HashMap mapping PluginNames to List of data about plugin instance and it's registration
        Map<String, List<RegisteredSimpleConfigClientPluginInstances>> regs = new HashMap<>();

        // Walk all registrations
        for (Map.Entry<String, SimpleConfigPluginRegistrations> pluginReg : this.registrations.entrySet()) {
            // Create list of registration data about all instances of the same plugin implementation
            List<RegisteredSimpleConfigClientPluginInstances> instancesList = new LinkedList<>();

            // Walk all plugin instances
            for (Map.Entry<String, PluginRegCfg> instanceReg : pluginReg.getValue().getInstances().entrySet()) {
                IotdmSimpleConfigBuilder cfgBuilder = null;
                IotdmCommonPluginData commonData = null;

                instanceReg.getValue().rLock();
                try {
                    // Builder constructor will create a copy of the configuration
                    IotdmPluginSimpleConfigClient plugin = instanceReg.getValue().getPlugin();

                    // check if the instance matches filters
                    if (! PluginManagerServicesUtils.applyPluginFilters(filters, plugin)) {
                        // instances doesn't match filters, continue with next instance
                        continue;
                    }

                    IotdmSimpleConfig cfg = plugin.getSimpleConfig();
                    if (null != cfg) {
                        cfgBuilder = new IotdmSimpleConfigBuilder(cfg.getConfiguration());
                    }
                    // Get also common data about the plugin instance
                    commonData = PluginManagerServicesUtils.createIotdmPluginData(plugin);
                } finally {
                    instanceReg.getValue().rUnlock();
                }

                RegisteredSimpleConfigClientPluginInstancesBuilder instance =
                            new RegisteredSimpleConfigClientPluginInstancesBuilder()
                                    .setPluginInstanceName(instanceReg.getKey());
                if (null != cfgBuilder) {
                    instance.setPluginSimpleConfig(cfgBuilder.build().getConfiguration());
                }
                if (null != commonData) {
                    instance.setIotdmCommonPluginData(commonData);
                }

                instancesList.add(instance.build());
            }

            regs.put(pluginReg.getKey(), instancesList);
        }

        return regs;
    }

}
