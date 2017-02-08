/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.iotdm.onem2m.plugins.registry.Onem2mLocalEndpointRegistry;
import org.opendaylight.iotdm.onem2m.plugins.simpleconfig.IotdmPluginSimpleConfigClient;
import org.opendaylight.iotdm.onem2m.plugins.simpleconfig.Onem2mPluginsSimpleConfigManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.iotdm.plugin.data.definition.IotdmCommonPluginData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.iotdm.plugin.data.definition.IotdmCommonPluginDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.iotdm.plugin.instance.registration.data.definition.IotdmPluginRegistrationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.iotdm.plugin.registration.data.definition.RegistrationDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.communication.channel.data.definition.ChannelDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.communication.channel.data.definition.IotdmPlugins;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.communication.channel.data.definition.IotdmPluginsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.communication.channel.data.definition.iotdm.plugins.IotdmPluginInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.communication.channel.data.definition.iotdm.plugins.IotdmPluginInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.db.api.client.registration.definition.DbApiClientPluginDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.communication.channels.output.Onem2mCommunicationChannelProtocols;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.communication.channels.output.Onem2mCommunicationChannelProtocolsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.communication.channels.output.onem2m.communication.channel.protocols.Onem2mCommunicationChannelAddresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.communication.channels.output.onem2m.communication.channel.protocols.Onem2mCommunicationChannelAddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.communication.channels.output.onem2m.communication.channel.protocols.onem2m.communication.channel.addresses.Onem2mCommunicationChannelPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.communication.channels.output.onem2m.communication.channel.protocols.onem2m.communication.channel.addresses.Onem2mCommunicationChannelPortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.db.api.client.registrations.output.RegisteredDbApiClientPluginsTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.db.api.client.registrations.output.RegisteredDbApiClientPluginsTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.db.api.client.registrations.output.registered.db.api.client.plugins.table.RegisteredDbApiClientPluginInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.db.api.client.registrations.output.registered.db.api.client.plugins.table.RegisteredDbApiClientPluginInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.iotdm.plugin.registrations.output.RegisteredIotdmPluginsTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.iotdm.plugin.registrations.output.RegisteredIotdmPluginsTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.iotdm.plugin.registrations.output.registered.iotdm.plugins.table.RegisteredIotdmPluginInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.iotdm.plugin.registrations.output.registered.iotdm.plugins.table.RegisteredIotdmPluginInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.plugin.data.output.Onem2mPluginManagerPluginsTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.plugin.data.output.Onem2mPluginManagerPluginsTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.plugin.data.output.onem2m.plugin.manager.plugins.table.Onem2mPluginManagerPluginInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.plugin.data.output.onem2m.plugin.manager.plugins.table.Onem2mPluginManagerPluginInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.plugin.data.output.onem2m.plugin.manager.plugins.table.onem2m.plugin.manager.plugin.instances.DbApiClientDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.plugin.data.output.onem2m.plugin.manager.plugins.table.onem2m.plugin.manager.plugin.instances.ImplementedInterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.plugin.data.output.onem2m.plugin.manager.plugins.table.onem2m.plugin.manager.plugin.instances.PluginConfigurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.plugin.data.output.onem2m.plugin.manager.plugins.table.onem2m.plugin.manager.plugin.instances.plugin.configuration.plugin.specific.configuration.SimpleConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.simple.config.client.registrations.output.RegisteredSimpleConfigClientPluginsTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.simple.config.client.registrations.output.RegisteredSimpleConfigClientPluginsTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.simple.config.client.registrations.output.registered.simple.config.client.plugins.table.RegisteredSimpleConfigClientPluginInstances;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Class implements PluginManager RPC services.
 */
public class Onem2mPluginManagerProvider implements Onem2mPluginManagerService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mPluginManagerProvider.class);
    private BindingAwareBroker.RpcRegistration<Onem2mPluginManagerService> rpcReg;

    public Onem2mPluginManagerProvider(BindingAwareBroker.ProviderContext session) {
        this.rpcReg = session.addRpcImplementation(Onem2mPluginManagerService.class, this);
    }

    @Override
    public void close() {
        this.rpcReg.close();
    }

    private static IotdmCommonPluginData createIotdmPluginData(final IotdmPluginCommonInterface plugin,
                                                               String loaderName) {
        Map<String, IotdmPluginLoader> pluginLoaders = Onem2mPluginManager.getInstance().getMgrPluginLoaders();

        if (null == loaderName || loaderName.isEmpty()) {
            // Must walk all loaders and try to find it
            Optional<IotdmPluginLoader> currentLoader =
                    pluginLoaders.values().stream()
                            .filter(l -> l.hasLoadedPlugin(plugin)).findFirst();
            if (currentLoader.isPresent()) {
                loaderName = currentLoader.get().getLoaderName();
            }
        }

        // Build pluginData which are common for the instance
        IotdmCommonPluginDataBuilder pluginData = new IotdmCommonPluginDataBuilder()
                                                    .setPluginName(plugin.getPluginName())
                                                    .setPluginInstanceName(plugin.getInstanceName())
                                                    .setPluginClass(plugin.getClass().getName())
                                                    .setPluginLoader(loaderName);
        return pluginData.build();
    }

    /**
     * Creates instances of IotdmCommonPluginData for specific plugin instance.
     * @param plugin Plugin instance
     * @return Created IotdmCommonPluginData instance
     */
    public static IotdmCommonPluginData createIotdmPluginData(IotdmPluginCommonInterface plugin) {
        return createIotdmPluginData(plugin, null);
    }

    @Override
    public Future<RpcResult<Onem2mPluginManagerIotdmPluginRegistrationsOutput>>
                onem2mPluginManagerIotdmPluginRegistrations(Onem2mPluginManagerIotdmPluginRegistrationsInput input) {
        return onem2mPluginManagerIotdmPluginRegistrationsImpl(input);
    }

    private Future<RpcResult<Onem2mPluginManagerIotdmPluginRegistrationsOutput>>
                onem2mPluginManagerIotdmPluginRegistrationsImpl(IotdmPluginFilters input) {

        PluginManagerRegistry registry = Onem2mPluginManager.getInstance().getMgrRegistry();

        Onem2mPluginManagerIotdmPluginRegistrationsOutputBuilder output =
                new Onem2mPluginManagerIotdmPluginRegistrationsOutputBuilder();

        List<RegisteredIotdmPluginsTable> list = new LinkedList<>();
        Map<String, HashMap<String, RegisteredIotdmPluginInstancesBuilder>> regs = new HashMap<>();

        // Walk all registries and prepare data in the map
        registry.registryStream().forEach( endpointRegistry -> {
            // Get the stream of plugins from the registry
            endpointRegistry.getPluginStream()
                // Filter plugins
                .filter(urlPluginEntry -> Onem2mPluginManagerUtils.applyPluginFilters(input, urlPluginEntry.getValue()))
                // Collect information about the plugin instance
                .forEach(urlPluginEntry -> {
                    IotdmPlugin plugin = urlPluginEntry.getValue();

                    // Registered Instances of the plugin
                    RegisteredIotdmPluginInstancesBuilder instancesBuilder = null;

                    if (regs.containsKey(plugin.getPluginName())) {
                        // We have already prepared such instancesBuilder
                        if (regs.get(plugin.getPluginName()).containsKey(plugin.getInstanceName())) {
                            instancesBuilder = regs.get(plugin.getPluginName()).get(plugin.getInstanceName());
                        }
                    } else {
                        // This is the first plugin instance of such name
                        regs.put(plugin.getPluginName(), new HashMap<>());
                    }

                    if (null == instancesBuilder) {
                        // Instances builder doesn't exist, prepare common Plugin data and store the new
                        // instances builder in the map of registrations so it can be used in next iteration
                        instancesBuilder = new RegisteredIotdmPluginInstancesBuilder()
                            .setIotdmCommonPluginData(
                                createIotdmPluginData(plugin, (null == input) ? null : input.getPluginLoaderName()))
                            .setPluginInstanceName(plugin.getInstanceName())
                            .setIotdmPluginRegistrations(new LinkedList<>());

                        // Put the instances builder into registrations map
                        regs.get(plugin.getPluginName()).put(plugin.getInstanceName(), instancesBuilder);
                    }

                    // Build registration data
                    RegistrationDataBuilder regData = new RegistrationDataBuilder()
                        .setProtocol(endpointRegistry.getProtocol())
                        .setAddress(endpointRegistry.getIpAddress())
                        .setPort(endpointRegistry.getPort())
                        .setTransportProtocol(endpointRegistry.getChannelId().getTransportProtocol().toString())
                        .setRegistrationMode(endpointRegistry.getMode().toString())
                        .setLocalUrl(urlPluginEntry.getKey());

                    // Add registration data into list of registration for this instance
                    instancesBuilder.getIotdmPluginRegistrations().add(
                            new IotdmPluginRegistrationsBuilder().setRegistrationData(regData.build()).build());
                });
        });

        // Registered Plugins

        for (Map.Entry<String, HashMap<String, RegisteredIotdmPluginInstancesBuilder>> pluginReg : regs.entrySet()) {
            // Create new row of the table of registered plugins
            RegisteredIotdmPluginsTableBuilder pluginTableBuilder = new RegisteredIotdmPluginsTableBuilder()
                .setPluginName(pluginReg.getKey());

            // Walk all instances create a list
            List<RegisteredIotdmPluginInstances> instancesList = new LinkedList<>();
            for (Map.Entry<String, RegisteredIotdmPluginInstancesBuilder> instanceReg : pluginReg.getValue().entrySet()) {
                instancesList.add(instanceReg.getValue().build());
            }

            // Add list of instances into plugin registration row
            pluginTableBuilder.setRegisteredIotdmPluginInstances(instancesList);

            // Add plugin registration row into list of rows
            list.add(pluginTableBuilder.build());
        }

        return RpcResultBuilder.success(output.setRegisteredIotdmPluginsTable(list).build()).buildFuture();
    }

    private boolean filterCommunicationChannels(final Onem2mLocalEndpointRegistry registry,
                                                final Onem2mPluginManagerCommunicationChannelsInput input) {
        if (null == input) {
            return true;
        }

        String protocolFilter = input.getProtocolName();
        String pluginName = input.getPluginName();
        String pluginInstanceId = input.getPluginInstanceName();

        if (null != protocolFilter && ! protocolFilter.equals(registry.getProtocol())) {
            return false;
        }

        if (null != pluginName) {
            Optional<Map.Entry<String, IotdmPlugin>> entry =
                registry.getPluginStream()
                        .filter(plugin -> plugin.getValue().getPluginName().equals(pluginName)).findFirst();
            if (null == entry || ! entry.isPresent()) {
                return false;
            }
        }

        if (null != pluginInstanceId) {
            Optional<Map.Entry<String, IotdmPlugin>> entry =
                registry.getPluginStream()
                        .filter(plugin -> plugin.getValue().getInstanceName().equals(pluginInstanceId)).findFirst();
            if (null == entry || ! entry.isPresent()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Future<RpcResult<Onem2mPluginManagerCommunicationChannelsOutput>> onem2mPluginManagerCommunicationChannels(
                                                            Onem2mPluginManagerCommunicationChannelsInput input) {

        PluginManagerRegistry registry = Onem2mPluginManager.getInstance().getMgrRegistry();
        Onem2mPluginManagerCommunicationChannelsOutputBuilder output =
                new Onem2mPluginManagerCommunicationChannelsOutputBuilder();
        String protocolFilter = ((null == input) ? null : input.getProtocolName());

        Map<String, Onem2mCommunicationChannelProtocols> protocolsMap = new HashMap<>();

        // Walk all registries and collect data of associated channels
        registry.registryStream()
            .filter(register -> filterCommunicationChannels(register, input))
            .forEach( endpointRegistry -> {
                Onem2mCommunicationChannelProtocols channelProtocolList = null;
                List<Onem2mCommunicationChannelAddresses> addressesList = null;
                Onem2mCommunicationChannelAddresses addressesListItem = null;
                List<Onem2mCommunicationChannelPorts> portsList = null;

                // set channelProtocolList - get existing one or create new
                if (protocolsMap.containsKey(endpointRegistry.getProtocol())) {
                    channelProtocolList = protocolsMap.get(endpointRegistry.getProtocol());
                } else {
                    channelProtocolList = new Onem2mCommunicationChannelProtocolsBuilder()
                                                  .setCommunicationChannelProtocol(endpointRegistry.getProtocol())
                                                  .setOnem2mCommunicationChannelAddresses(new LinkedList<>())
                                                  .build();
                    protocolsMap.put(endpointRegistry.getProtocol(), channelProtocolList);
                }

                // set addressListItem - get existing one or create new
                addressesList = channelProtocolList.getOnem2mCommunicationChannelAddresses();
                try {
                    addressesListItem = addressesList.stream()
                                            .filter(item -> item.getCommunicationChannelAddress()
                                                                    .equals(endpointRegistry.getIpAddress()))
                                            .findFirst()
                                            .get();
                } catch (NoSuchElementException e) {
                    addressesListItem = new Onem2mCommunicationChannelAddressesBuilder()
                                                .setCommunicationChannelAddress(endpointRegistry.getIpAddress())
                                                .setOnem2mCommunicationChannelPorts(new LinkedList<>())
                                                .build();
                    addressesList.add(addressesListItem);
                }

                // there must be only one communication channel running on this port number
                // so just create the new item without checking whether already exists
                portsList = addressesListItem.getOnem2mCommunicationChannelPorts();

                // Collect channel data
                ChannelDataBuilder channelData = new ChannelDataBuilder()
                     .setProtocol(endpointRegistry.getAssociatedChannel().getProtocol())
                     .setChannelType(endpointRegistry.getChannelId().getChannelType().toString())
                     .setAddress(endpointRegistry.getIpAddress())
                     .setPort(endpointRegistry.getPort())
                     .setTransportProtocol(endpointRegistry.getChannelId().getTransportProtocol().toString())
                     .setChannelConfiguration(endpointRegistry.getAssociatedChannel().getChannelConfig())
                     .setChannelState(endpointRegistry.getAssociatedChannel().getState().toString());


                // Collect plugin names and instances in map
                Map<String, List<String>> plugins = new HashMap<>();
                endpointRegistry.getPluginStream().forEach(urlPlugin -> {
                    String pluginName = urlPlugin.getValue().getPluginName();
                    String instanceId = urlPlugin.getValue().getInstanceName();
                    List<String> instanceList = null;
                    if (! plugins.containsKey(pluginName)) {
                        instanceList = new LinkedList<String>();
                        instanceList.add(instanceId);
                        plugins.put(pluginName, instanceList);
                    } else {
                        instanceList = plugins.get(pluginName);
                        if (! instanceList.contains(instanceId)) {
                            instanceList.add(instanceId);
                        }
                    }
                });

                // Walk the mapping of plugins to list of plugin instances and fill the output
                List<IotdmPlugins> pluginsList = new LinkedList<>();
                for (Map.Entry<String, List<String>> entry : plugins.entrySet()) {
                    IotdmPluginsBuilder pluginsBuilder = new IotdmPluginsBuilder();
                    pluginsBuilder.setIotdmPluginName(entry.getKey());

                    List<IotdmPluginInstances> instancesList = new LinkedList();

                    for (String instanceId : entry.getValue()) {
                        instancesList.add(new IotdmPluginInstancesBuilder()
                                                  .setIotdmPluginInstanceId(instanceId)
                                                  .build());
                    }

                    pluginsBuilder.setIotdmPluginInstances(instancesList);
                    pluginsList.add(pluginsBuilder.build());
                }

                Onem2mCommunicationChannelPortsBuilder portsListItemBuilder =
                        new Onem2mCommunicationChannelPortsBuilder()
                            .setOnem2mCommunicationChannelPort(endpointRegistry.getPort())
                            .setChannelData(channelData.build())
                            .setIotdmPlugins(pluginsList);

                portsList.add(portsListItemBuilder.build());
        });

        // Create a list from the map and set to the output
        output.setOnem2mCommunicationChannelProtocols(protocolsMap.values().stream().collect(Collectors.toList()));

        // Build communication channel data
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

    @Override
    public Future<RpcResult<Onem2mPluginManagerDbApiClientRegistrationsOutput>>
                onem2mPluginManagerDbApiClientRegistrations(Onem2mPluginManagerDbApiClientRegistrationsInput input) {
        return this.onem2mPluginManagerDbApiClientRegistrationsImpl(input);
    }

    private Future<RpcResult<Onem2mPluginManagerDbApiClientRegistrationsOutput>>
                onem2mPluginManagerDbApiClientRegistrationsImpl(IotdmPluginFilters input) {

        Onem2mPluginManagerDbApiClientRegistrationsOutputBuilder output =
                new Onem2mPluginManagerDbApiClientRegistrationsOutputBuilder();

        // PluginName : InstanceName : Registered
        Map<String, List<RegisteredDbApiClientPluginInstances>> regs = new HashMap<>();

        for (Onem2mPluginsDbApi.PluginDbClientData plugin : Onem2mPluginsDbApi.getInstance().getPlugins()) {
            // filter only plugins matching the filters
            if (! Onem2mPluginManagerUtils.applyPluginFilters(input, plugin.getClient())) {
                continue;
            }

            // Registered Instances of the plugin
            RegisteredDbApiClientPluginInstancesBuilder instancesBuilder = null;

            if (! regs.containsKey(plugin.getClient().getPluginName())) {
                // This is the first plugin instance of such name
                regs.put(plugin.getClient().getPluginName(), new LinkedList<>());
            }

            // Prepare DbApiClientPlugin data first
            DbApiClientPluginDataBuilder builder = new DbApiClientPluginDataBuilder()
                .setIotdmCommonPluginData(createIotdmPluginData(plugin.getClient()))
                .setDbApiClientState(plugin.getState().toString());

            instancesBuilder = new RegisteredDbApiClientPluginInstancesBuilder()
                                       .setPluginInstanceName(plugin.getClient().getInstanceName())
                                       .setDbApiClientPluginData(builder.build());

            // Put the instances item into the list
            regs.get(plugin.getClient().getPluginName()).add(instancesBuilder.build());
        }

        List<RegisteredDbApiClientPluginsTable> pluginsTable = new LinkedList<>();
        for (Map.Entry<String, List<RegisteredDbApiClientPluginInstances>> entry : regs.entrySet()) {
            RegisteredDbApiClientPluginsTableBuilder builder = new RegisteredDbApiClientPluginsTableBuilder()
                .setPluginName(entry.getKey())
                .setRegisteredDbApiClientPluginInstances(entry.getValue());
            pluginsTable.add(builder.build());
        }

        return RpcResultBuilder
                       .success(output.setRegisteredDbApiClientPluginsTable(pluginsTable).build())
                       .buildFuture();
    }

    @Override
    public Future<RpcResult<Onem2mPluginManagerSimpleConfigClientRegistrationsOutput>>
                            onem2mPluginManagerSimpleConfigClientRegistrations(
                                                Onem2mPluginManagerSimpleConfigClientRegistrationsInput input) {
        return this.onem2mPluginManagerSimpleConfigClientRegistrationsImpl(input);
    }

    public Future<RpcResult<Onem2mPluginManagerSimpleConfigClientRegistrationsOutput>>
                                    onem2mPluginManagerSimpleConfigClientRegistrationsImpl(IotdmPluginFilters input) {
        Onem2mPluginManagerSimpleConfigClientRegistrationsOutputBuilder output =
                new Onem2mPluginManagerSimpleConfigClientRegistrationsOutputBuilder();

        // PluginName : InstanceName : Registered
        Map<String, List<RegisteredSimpleConfigClientPluginInstances>>regs =
                        Onem2mPluginsSimpleConfigManager.getInstance().getRegistrationsMap(input);

        List<RegisteredSimpleConfigClientPluginsTable> pluginsTable = new LinkedList<>();
        for (Map.Entry<String, List<RegisteredSimpleConfigClientPluginInstances>> entry : regs.entrySet()) {
            RegisteredSimpleConfigClientPluginsTableBuilder builder =
                    new RegisteredSimpleConfigClientPluginsTableBuilder()
                                                   .setPluginName(entry.getKey())
                                                   .setRegisteredSimpleConfigClientPluginInstances(entry.getValue());
            pluginsTable.add(builder.build());
        }

        return RpcResultBuilder
                       .success(output.setRegisteredSimpleConfigClientPluginsTable(pluginsTable).build())
                       .buildFuture();
    }

    /**
     * Returns already existing builder from passed map or creates new one.
     * @param instancesMap Map of HashMaps of builders per plugin instance
     * @param pluginName Name of the plugin implementation
     * @param instanceName Name of the plugin instance
     * @return The builder
     */
    private Onem2mPluginManagerPluginInstancesBuilder getInstanceBuilder(
                                Map<String, HashMap<String, Onem2mPluginManagerPluginInstancesBuilder>> instancesMap,
                                String pluginName, String instanceName) {

        HashMap<String, Onem2mPluginManagerPluginInstancesBuilder> instances = null;
        if (! instancesMap.containsKey(pluginName)) {
            instances = new HashMap<>();
            instancesMap.put(pluginName, instances);
        } else {
            instances = instancesMap.get(pluginName);
        }

        Onem2mPluginManagerPluginInstancesBuilder instance = null;
        if (! instances.containsKey(instanceName)) {
            instance = new Onem2mPluginManagerPluginInstancesBuilder()
                               .setPluginInstanceName(instanceName)
                               .setImplementedInterfaces(new LinkedList<>());
            instances.put(instanceName, instance);
        } else {
            instance = instances.get(instanceName);
        }

        return instance;
    }

    @Override
    public Future<RpcResult<Onem2mPluginManagerPluginDataOutput>> onem2mPluginManagerPluginData(
                                                                        Onem2mPluginManagerPluginDataInput input) {

        // This RPC call uses other RPC calls and collects all data per plugin instance

        Onem2mPluginManagerPluginDataOutputBuilder output = new Onem2mPluginManagerPluginDataOutputBuilder();
        Map<String, HashMap<String, Onem2mPluginManagerPluginInstancesBuilder>> instancesMap = new HashMap<>();

        // Get IotdmPlugin registrations data
        try {
            Onem2mPluginManagerIotdmPluginRegistrationsOutput iotdmPluginRegistrations = null;
            RpcResult<Onem2mPluginManagerIotdmPluginRegistrationsOutput> rpcResult =
                                                        onem2mPluginManagerIotdmPluginRegistrationsImpl(input).get();
            if (rpcResult.isSuccessful()) {
                iotdmPluginRegistrations = rpcResult.getResult();
            }

            if (null != rpcResult) {
                for (RegisteredIotdmPluginsTable plugin : iotdmPluginRegistrations.getRegisteredIotdmPluginsTable()) {
                    for (RegisteredIotdmPluginInstances instance : plugin.getRegisteredIotdmPluginInstances()) {
                        Onem2mPluginManagerPluginInstancesBuilder builder =
                                                    getInstanceBuilder(instancesMap, plugin.getPluginName(),
                                                                       instance.getPluginInstanceName());
                        builder.getImplementedInterfaces().add(
                                new ImplementedInterfacesBuilder()
                                        .setIotdmInterface(IotdmPlugin.class.getSimpleName())
                                        .build());
                        builder.setIotdmCommonPluginData(instance.getIotdmCommonPluginData());
                        builder.setIotdmPluginRegistrations(instance.getIotdmPluginRegistrations());

                        // Set configuration if exists
                        AtomicReference<IotdmPlugin> pluginInstance = new AtomicReference<>();
                        Onem2mPluginManager.getInstance().getMgrRegistry().registryStream().filter(registry -> {
                            Optional<Map.Entry<String, IotdmPlugin>> p =
                                    registry.getPluginStream()
                                            .filter(entry ->
                                               (entry.getValue().getPluginName().equals(plugin.getPluginName()) &&
                                                entry.getValue().getInstanceName().equals(
                                                    instance.getPluginInstanceName()))).findFirst();
                            if (p.isPresent()) {
                                pluginInstance.set(p.get().getValue());
                            }
                            return p.isPresent();
                        }).findFirst();

                        if (pluginInstance.get() instanceof IotdmPluginConfigurable) {
                            builder.getImplementedInterfaces().add(
                                    new ImplementedInterfacesBuilder()
                                            .setIotdmInterface(IotdmPluginConfigurable.class.getSimpleName())
                                            .build());
                            builder.setPluginConfiguration(
                                    new PluginConfigurationBuilder().setPluginSpecificConfiguration(
                                        ((IotdmPluginConfigurable) pluginInstance.get()).getRunningConfig()).build());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to get IotdmPlugin registrations data: {}", e);
        }

        // Get DB API client registration data
        try {
            Onem2mPluginManagerDbApiClientRegistrationsOutput dbApiClientRegistrations = null;
            RpcResult<Onem2mPluginManagerDbApiClientRegistrationsOutput> rpcResult =
                    onem2mPluginManagerDbApiClientRegistrationsImpl(input).get();
            if (rpcResult.isSuccessful()) {
                dbApiClientRegistrations = rpcResult.getResult();
            }

            if (null != dbApiClientRegistrations) {
                for (RegisteredDbApiClientPluginsTable plugin :
                        dbApiClientRegistrations.getRegisteredDbApiClientPluginsTable()) {

                    for (RegisteredDbApiClientPluginInstances instance : plugin.getRegisteredDbApiClientPluginInstances()) {
                        if (null == instance.getDbApiClientPluginData()) {
                            LOG.debug("DB API plugin data not set for pluginName: {}, instanceName: {}",
                                      plugin.getPluginName(), instance.getPluginInstanceName());
                            continue;
                        }

                        Onem2mPluginManagerPluginInstancesBuilder builder =
                                                getInstanceBuilder(instancesMap, plugin.getPluginName(),
                                                                   instance.getPluginInstanceName());
                        builder.getImplementedInterfaces().add(
                                new ImplementedInterfacesBuilder()
                                        .setIotdmInterface(IotdmPluginDbClient.class.getSimpleName())
                                        .build());
                        builder.setDbApiClientData(
                                new DbApiClientDataBuilder().setDbApiClientState(
                                        instance.getDbApiClientPluginData().getDbApiClientState()).build());

                        // Set also common plugin data if not set
                        if (null == builder.getIotdmCommonPluginData()) {
                            builder.setIotdmCommonPluginData(
                                    instance.getDbApiClientPluginData().getIotdmCommonPluginData());
                        }

                        // Set also configuration if exists and if not set
                        if (null == builder.getPluginConfiguration()) {
                            for (Onem2mPluginsDbApi.PluginDbClientData pluginInstance :
                                    Onem2mPluginsDbApi.getInstance().getPlugins()) {
                                if (pluginInstance.getClient().getPluginName().equals(
                                                                                plugin.getPluginName()) &&
                                    pluginInstance.getClient().getInstanceName().equals(
                                                                                instance.getPluginInstanceName())) {

                                    if (pluginInstance.getClient() instanceof IotdmPluginConfigurable) {
                                        builder.getImplementedInterfaces().add(
                                            new ImplementedInterfacesBuilder()
                                                    .setIotdmInterface(IotdmPluginConfigurable.class.getSimpleName())
                                                    .build());
                                        builder.setPluginConfiguration(
                                            new PluginConfigurationBuilder().setPluginSpecificConfiguration(
                                                ((IotdmPluginConfigurable) pluginInstance.getClient())
                                                    .getRunningConfig()).build());
                                    }

                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to get DB API client registrations data: {}", e);
        }

        // Get SimpleConfig registration data
        try {
            Onem2mPluginManagerSimpleConfigClientRegistrationsOutput simpleConfigRegData = null;
            RpcResult<Onem2mPluginManagerSimpleConfigClientRegistrationsOutput> rpcResult =
                    onem2mPluginManagerSimpleConfigClientRegistrationsImpl(input).get();
            if (rpcResult.isSuccessful()) {
                simpleConfigRegData = rpcResult.getResult();
            }

            if (null != simpleConfigRegData) {
                for (RegisteredSimpleConfigClientPluginsTable plugin :
                        simpleConfigRegData.getRegisteredSimpleConfigClientPluginsTable()) {

                    for (RegisteredSimpleConfigClientPluginInstances instance :
                            plugin.getRegisteredSimpleConfigClientPluginInstances()) {

                        Onem2mPluginManagerPluginInstancesBuilder builder =
                                                getInstanceBuilder(instancesMap, plugin.getPluginName(),
                                                                   instance.getPluginInstanceName());
                        builder.getImplementedInterfaces().add(
                                new ImplementedInterfacesBuilder()
                                        .setIotdmInterface(IotdmPluginSimpleConfigClient.class.getSimpleName())
                                        .build());

                        // Set also common plugin data if not set
                        if (null == builder.getIotdmCommonPluginData()) {
                            builder.setIotdmCommonPluginData(instance.getIotdmCommonPluginData());
                        }

                        if (null == instance.getPluginSimpleConfig()) {
                            LOG.debug("SimpleConfig configuration not set for pluginName: {}, instanceName: {}",
                                      plugin.getPluginName(), instance.getPluginInstanceName());
                            continue;
                        }

                        // Set also configuration if exists and if not set
                        if (null == builder.getPluginConfiguration()) {
                            builder.setPluginConfiguration(
                                new PluginConfigurationBuilder().setPluginSpecificConfiguration(
                                        new SimpleConfigBuilder().setPluginSimpleConfig(
                                            instance.getPluginSimpleConfig()).build())
                                    .build());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to get DB API client registrations data: {}", e);
        }

        // Walk all instance builders and collect data in table
        List<Onem2mPluginManagerPluginsTable> pluginsTable = new LinkedList<>();
        for (Map.Entry<String, HashMap<String, Onem2mPluginManagerPluginInstancesBuilder>> tableRow :
                instancesMap.entrySet()) {
            List<Onem2mPluginManagerPluginInstances> instancesList = new LinkedList<>();
            for (Onem2mPluginManagerPluginInstancesBuilder instancesBuilder : tableRow.getValue().values()) {
                instancesList.add(instancesBuilder.build());
            }

            Onem2mPluginManagerPluginsTableBuilder tableBuilder = new Onem2mPluginManagerPluginsTableBuilder()
                    .setPluginName(tableRow.getKey())
                    .setOnem2mPluginManagerPluginInstances(instancesList);
            pluginsTable.add(tableBuilder.build());
        }

        output.setOnem2mPluginManagerPluginsTable(pluginsTable);
        return RpcResultBuilder
                       .success(output.build())
                       .buildFuture();
    }
}
