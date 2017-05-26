/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPlugin;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginCommonInterface;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginConfigurable;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.services.IotdmPluginsCustomService;
import org.opendaylight.iotdm.plugininfra.pluginmanager.customservices.PluginManagerCustomServicesService;
import org.opendaylight.iotdm.plugininfra.pluginmanager.customservices.PluginManagerServicesUtils;
import org.opendaylight.iotdm.plugininfra.pluginmanager.registry.IotdmRxPluginsRegistry;
import org.opendaylight.iotdm.plugininfra.pluginmanager.simpleconfig.IotdmPluginSimpleConfigClient;
import org.opendaylight.iotdm.plugininfra.pluginmanager.simpleconfig.IotdmPluginsSimpleConfigManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginFilters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginManagerCommunicationChannelsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginManagerCommunicationChannelsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginManagerCommunicationChannelsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginManagerCustomServicesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginManagerCustomServicesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginManagerCustomServicesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginManagerIotdmPluginRegistrationsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginManagerIotdmPluginRegistrationsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginManagerIotdmPluginRegistrationsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginManagerPluginDataInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginManagerPluginDataOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginManagerPluginDataOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginManagerSimpleConfigClientRegistrationsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginManagerSimpleConfigClientRegistrationsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginManagerSimpleConfigClientRegistrationsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.communication.channel.data.definition.ChannelDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.communication.channel.data.definition.IotdmPlugins;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.communication.channel.data.definition.IotdmPluginsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.communication.channel.data.definition.iotdm.plugins.IotdmPluginInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.communication.channel.data.definition.iotdm.plugins.IotdmPluginInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.instance.registration.data.definition.IotdmPluginRegistrationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.communication.channels.output.IotdmCommunicationChannelProtocols;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.communication.channels.output.IotdmCommunicationChannelProtocolsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.communication.channels.output.iotdm.communication.channel.protocols.IotdmCommunicationChannelAddresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.communication.channels.output.iotdm.communication.channel.protocols.IotdmCommunicationChannelAddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.communication.channels.output.iotdm.communication.channel.protocols.iotdm.communication.channel.addresses.IotdmCommunicationChannelPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.communication.channels.output.iotdm.communication.channel.protocols.iotdm.communication.channel.addresses.IotdmCommunicationChannelPortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.custom.services.output.IotdmPluginManagerCustomServicesList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.custom.services.output.IotdmPluginManagerCustomServicesListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.custom.services.output.iotdm.plugin.manager.custom.services.list.CustomServicePluginsTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.custom.services.output.iotdm.plugin.manager.custom.services.list.CustomServicePluginsTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.custom.services.output.iotdm.plugin.manager.custom.services.list.custom.service.plugins.table.CustomServicePluginInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.custom.services.output.iotdm.plugin.manager.custom.services.list.custom.service.plugins.table.CustomServicePluginInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.iotdm.plugin.registrations.output.RegisteredIotdmPluginsTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.iotdm.plugin.registrations.output.RegisteredIotdmPluginsTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.iotdm.plugin.registrations.output.registered.iotdm.plugins.table.RegisteredIotdmPluginInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.iotdm.plugin.registrations.output.registered.iotdm.plugins.table.RegisteredIotdmPluginInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.plugin.data.output.IotdmPluginManagerPluginsTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.plugin.data.output.IotdmPluginManagerPluginsTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.plugin.data.output.iotdm.plugin.manager.plugins.table.IotdmPluginManagerPluginInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.plugin.data.output.iotdm.plugin.manager.plugins.table.IotdmPluginManagerPluginInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.plugin.data.output.iotdm.plugin.manager.plugins.table.iotdm.plugin.manager.plugin.instances.CustomServicesList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.plugin.data.output.iotdm.plugin.manager.plugins.table.iotdm.plugin.manager.plugin.instances.CustomServicesListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.plugin.data.output.iotdm.plugin.manager.plugins.table.iotdm.plugin.manager.plugin.instances.ImplementedInterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.plugin.data.output.iotdm.plugin.manager.plugins.table.iotdm.plugin.manager.plugin.instances.PluginConfigurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.plugin.data.output.iotdm.plugin.manager.plugins.table.iotdm.plugin.manager.plugin.instances.plugin.configuration.plugin.specific.configuration.SimpleConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.simple.config.client.registrations.output.RegisteredSimpleConfigClientPluginsTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.simple.config.client.registrations.output.RegisteredSimpleConfigClientPluginsTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.simple.config.client.registrations.output.registered.simple.config.client.plugins.table.RegisteredSimpleConfigClientPluginInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.registration.data.definition.RegistrationDataBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class IotdmPluginManagerProvider implements IotdmPluginManagerService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmPluginManagerProvider.class);

    private static final IotdmPluginManagerProvider instance = new IotdmPluginManagerProvider();

    public static IotdmPluginManagerProvider getInstance() {
        return instance;
    }

    @Override
    public void close() {
        return;
    }

    @Override
    public Future<RpcResult<IotdmPluginManagerIotdmPluginRegistrationsOutput>>
                iotdmPluginManagerIotdmPluginRegistrations(IotdmPluginManagerIotdmPluginRegistrationsInput input) {
        return onem2mPluginManagerIotdmPluginRegistrationsImpl(input);
    }

    private Future<RpcResult<IotdmPluginManagerIotdmPluginRegistrationsOutput>>
                onem2mPluginManagerIotdmPluginRegistrationsImpl(IotdmPluginFilters input) {

        PluginManagerRegister registry = IotdmPluginManager.getInstance().getMgrRegistry();

        IotdmPluginManagerIotdmPluginRegistrationsOutputBuilder output =
                new IotdmPluginManagerIotdmPluginRegistrationsOutputBuilder();

        List<RegisteredIotdmPluginsTable> list = new LinkedList<>();
        Map<String, ConcurrentHashMap<String, RegisteredIotdmPluginInstancesBuilder>> regs = new ConcurrentHashMap<>();

        // Walk all registries and prepare data in the map
        registry.registryStream().forEach( endpointRegistry -> {
            // Get the stream of plugins from the registry
            endpointRegistry.getPluginStream()
                // Filter plugins
                .filter(urlPluginEntry -> PluginManagerServicesUtils.applyPluginFilters(input, urlPluginEntry.getValue()))
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
                        regs.put(plugin.getPluginName(), new ConcurrentHashMap<>());
                    }

                    if (null == instancesBuilder) {
                        // Instances builder doesn't exist, prepare common Plugin data and store the new
                        // instances builder in the map of registrations so it can be used in next iteration
                        instancesBuilder = new RegisteredIotdmPluginInstancesBuilder()
                            .setIotdmCommonPluginData(
                                PluginManagerServicesUtils.createIotdmPluginData(plugin, (null == input) ? null : input.getPluginLoaderName()))
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

        for (Map.Entry<String,
                       ConcurrentHashMap<String, RegisteredIotdmPluginInstancesBuilder>> pluginReg : regs.entrySet()) {
            // Create new row of the table of registered plugins
            RegisteredIotdmPluginsTableBuilder pluginTableBuilder = new RegisteredIotdmPluginsTableBuilder()
                .setPluginName(pluginReg.getKey());

            // Walk all instances create a list
            List<RegisteredIotdmPluginInstances> instancesList = new LinkedList<>();
            for (Map.Entry<String,
                           RegisteredIotdmPluginInstancesBuilder> instanceReg : pluginReg.getValue().entrySet()) {
                instancesList.add(instanceReg.getValue().build());
            }

            // Add list of instances into plugin registration row
            pluginTableBuilder.setRegisteredIotdmPluginInstances(instancesList);

            // Add plugin registration row into list of rows
            list.add(pluginTableBuilder.build());
        }

        return RpcResultBuilder.success(output.setRegisteredIotdmPluginsTable(list).build()).buildFuture();
    }

    private boolean filterCommunicationChannels(final IotdmRxPluginsRegistry registry,
                                                final IotdmPluginManagerCommunicationChannelsInput input) {
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
    public Future<RpcResult<IotdmPluginManagerCommunicationChannelsOutput>> iotdmPluginManagerCommunicationChannels(
        IotdmPluginManagerCommunicationChannelsInput input) {

        PluginManagerRegister registry = IotdmPluginManager.getInstance().getMgrRegistry();
        IotdmPluginManagerCommunicationChannelsOutputBuilder output =
                new IotdmPluginManagerCommunicationChannelsOutputBuilder();
        String protocolFilter = ((null == input) ? null : input.getProtocolName());

        Map<String, IotdmCommunicationChannelProtocols> protocolsMap = new ConcurrentHashMap<>();

        // Walk all registries and collect data of associated channels
        registry.registryStream()
            .filter(register -> filterCommunicationChannels(register, input))
            .forEach( endpointRegistry -> {
                IotdmCommunicationChannelProtocols channelProtocolList = null;
                List<IotdmCommunicationChannelAddresses> addressesList = null;
                IotdmCommunicationChannelAddresses addressesListItem = null;
                List<IotdmCommunicationChannelPorts> portsList = null;

                // set channelProtocolList - get existing one or create new
                if (protocolsMap.containsKey(endpointRegistry.getProtocol())) {
                    channelProtocolList = protocolsMap.get(endpointRegistry.getProtocol());
                } else {
                    channelProtocolList = new IotdmCommunicationChannelProtocolsBuilder()
                                                  .setCommunicationChannelProtocol(endpointRegistry.getProtocol())
                                                  .setIotdmCommunicationChannelAddresses(new LinkedList<>())
                                                  .build();
                    protocolsMap.put(endpointRegistry.getProtocol(), channelProtocolList);
                }

                // set addressListItem - get existing one or create new
                addressesList = channelProtocolList.getIotdmCommunicationChannelAddresses();
                try {
                    addressesListItem = addressesList.stream()
                                            .filter(item -> item.getCommunicationChannelAddress()
                                                                    .equals(endpointRegistry.getIpAddress()))
                                            .findFirst()
                                            .get();
                } catch (NoSuchElementException e) {
                    addressesListItem = new IotdmCommunicationChannelAddressesBuilder()
                                                .setCommunicationChannelAddress(endpointRegistry.getIpAddress())
                                                .setIotdmCommunicationChannelPorts(new LinkedList<>())
                                                .build();
                    addressesList.add(addressesListItem);
                }

                // there must be only one communication channel running on this port number
                // so just create the new item without checking whether already exists
                portsList = addressesListItem.getIotdmCommunicationChannelPorts();

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
                Map<String, List<String>> plugins = new ConcurrentHashMap<>();
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

                IotdmCommunicationChannelPortsBuilder portsListItemBuilder =
                        new IotdmCommunicationChannelPortsBuilder()
                            .setIotdmCommunicationChannelPort(endpointRegistry.getPort())
                            .setChannelData(channelData.build())
                            .setIotdmPlugins(pluginsList);

                portsList.add(portsListItemBuilder.build());
        });

        // Create a list from the map and set to the output
        output.setIotdmCommunicationChannelProtocols(protocolsMap.values().stream().collect(Collectors.toList()));

        // Build communication channel data
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

    @Override
    public Future<RpcResult<IotdmPluginManagerSimpleConfigClientRegistrationsOutput>>
                            iotdmPluginManagerSimpleConfigClientRegistrations(
                                IotdmPluginManagerSimpleConfigClientRegistrationsInput input) {
        return this.onem2mPluginManagerSimpleConfigClientRegistrationsImpl(input);
    }

    public Future<RpcResult<IotdmPluginManagerSimpleConfigClientRegistrationsOutput>>
                                    onem2mPluginManagerSimpleConfigClientRegistrationsImpl(IotdmPluginFilters input) {
        IotdmPluginManagerSimpleConfigClientRegistrationsOutputBuilder output =
                new IotdmPluginManagerSimpleConfigClientRegistrationsOutputBuilder();

        // PluginName : InstanceName : Registered
        Map<String, List<RegisteredSimpleConfigClientPluginInstances>>regs =
                        IotdmPluginsSimpleConfigManager.getInstance().getRegistrationsMap(input);

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
     * @param instancesMap Map of ConcurrentHashMaps of builders per plugin instance
     * @param pluginName Name of the plugin implementation
     * @param instanceName Name of the plugin instance
     * @return The builder
     */
    private IotdmPluginManagerPluginInstancesBuilder getInstanceBuilder(
                        Map<String, ConcurrentHashMap<String, IotdmPluginManagerPluginInstancesBuilder>> instancesMap,
                        String pluginName, String instanceName) {

        ConcurrentHashMap<String, IotdmPluginManagerPluginInstancesBuilder> instances = null;
        if (! instancesMap.containsKey(pluginName)) {
            instances = new ConcurrentHashMap<>();
            instancesMap.put(pluginName, instances);
        } else {
            instances = instancesMap.get(pluginName);
        }

        IotdmPluginManagerPluginInstancesBuilder instance = null;
        if (! instances.containsKey(instanceName)) {
            instance = new IotdmPluginManagerPluginInstancesBuilder()
                               .setPluginInstanceName(instanceName)
                               .setImplementedInterfaces(new LinkedList<>());
            instances.put(instanceName, instance);
        } else {
            instance = instances.get(instanceName);
        }

        return instance;
    }

    @Override
    public Future<RpcResult<IotdmPluginManagerPluginDataOutput>> iotdmPluginManagerPluginData(
                                                                    IotdmPluginManagerPluginDataInput input) {

        // This RPC call uses other RPC calls and collects all data per plugin instance

        IotdmPluginManagerPluginDataOutputBuilder output = new IotdmPluginManagerPluginDataOutputBuilder();
        Map<String, ConcurrentHashMap<String, IotdmPluginManagerPluginInstancesBuilder>> instancesMap =
                                                                                        new ConcurrentHashMap<>();

        // Get IotdmPlugin registrations data
        try {
            IotdmPluginManagerIotdmPluginRegistrationsOutput iotdmPluginRegistrations = null;
            RpcResult<IotdmPluginManagerIotdmPluginRegistrationsOutput> rpcResult =
                                                        onem2mPluginManagerIotdmPluginRegistrationsImpl(input).get();
            if (rpcResult.isSuccessful()) {
                iotdmPluginRegistrations = rpcResult.getResult();
            }

            if (null != rpcResult) {
                for (RegisteredIotdmPluginsTable plugin : iotdmPluginRegistrations.getRegisteredIotdmPluginsTable()) {
                    for (RegisteredIotdmPluginInstances instance : plugin.getRegisteredIotdmPluginInstances()) {
                        IotdmPluginManagerPluginInstancesBuilder builder =
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
                        IotdmPluginManager.getInstance().getMgrRegistry().registryStream().filter(registry -> {
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


        // Get Custom Services data
        try {
            Collection<IotdmPluginsCustomService> customServices =
                PluginManagerCustomServicesService.getInstance().getAllServices();
            for (IotdmPluginsCustomService service : customServices) {
                Collection<IotdmPluginCommonInterface> servicePlugins =
                    PluginManagerCustomServicesService.getInstance().getPluginsOfService(service);
                if (null == servicePlugins || servicePlugins.isEmpty()) {
                    continue;
                }

                for (IotdmPluginCommonInterface servicePlugin : servicePlugins) {
                    IotdmPluginManagerPluginInstancesBuilder builder =
                        getInstanceBuilder(instancesMap, servicePlugin.getPluginName(),
                                           servicePlugin.getInstanceName());

                    List<CustomServicesList> pluginCustomServices = builder.getCustomServicesList();
                    if (null == pluginCustomServices) {
                        pluginCustomServices = new LinkedList<>();
                        builder.setCustomServicesList(pluginCustomServices);
                    }

                    CustomServicesListBuilder builderPluginCustomServ = new CustomServicesListBuilder()
                        .setCustomServiceName(service.getCustomServiceName())
                        .setCustomServiceClass(service.getClass().getName())
                        .setCustomServicePluginData(service.getServiceSpecificPluginData(servicePlugin));
                    pluginCustomServices.add(builderPluginCustomServ.build());

                    // Set also common plugin data if not set
                    if (null == builder.getIotdmCommonPluginData()) {
                        builder.setIotdmCommonPluginData(
                            PluginManagerServicesUtils.createIotdmPluginData(
                                    servicePlugin,
                                    (null == input) ? null : input.getPluginLoaderName()));
                    }

                    // Set also configuration if exists and if not set
                    if (null == builder.getPluginConfiguration()) {
                        if (servicePlugin instanceof IotdmPluginConfigurable) {
                            builder.getImplementedInterfaces().add(
                                new ImplementedInterfacesBuilder()
                                    .setIotdmInterface(IotdmPluginConfigurable.class.getSimpleName())
                                    .build());
                            builder.setPluginConfiguration(
                                new PluginConfigurationBuilder().setPluginSpecificConfiguration(
                                    ((IotdmPluginConfigurable) servicePlugin).getRunningConfig()).build());
                        }
                    }
                }

            }
        } catch (Exception e) {
            LOG.error("Failed to get plugins custom service data: {}", e);
        }

        // Get SimpleConfig registration data
        try {
            IotdmPluginManagerSimpleConfigClientRegistrationsOutput simpleConfigRegData = null;
            RpcResult<IotdmPluginManagerSimpleConfigClientRegistrationsOutput> rpcResult =
                    onem2mPluginManagerSimpleConfigClientRegistrationsImpl(input).get();
            if (rpcResult.isSuccessful()) {
                simpleConfigRegData = rpcResult.getResult();
            }

            if (null != simpleConfigRegData) {
                for (RegisteredSimpleConfigClientPluginsTable plugin :
                        simpleConfigRegData.getRegisteredSimpleConfigClientPluginsTable()) {

                    for (RegisteredSimpleConfigClientPluginInstances instance :
                            plugin.getRegisteredSimpleConfigClientPluginInstances()) {

                        IotdmPluginManagerPluginInstancesBuilder builder =
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
        List<IotdmPluginManagerPluginsTable> pluginsTable = new LinkedList<>();
        for (Map.Entry<String, ConcurrentHashMap<String, IotdmPluginManagerPluginInstancesBuilder>> tableRow :
                instancesMap.entrySet()) {
            List<IotdmPluginManagerPluginInstances> instancesList = new LinkedList<>();
            for (IotdmPluginManagerPluginInstancesBuilder instancesBuilder : tableRow.getValue().values()) {
                instancesList.add(instancesBuilder.build());
            }

            IotdmPluginManagerPluginsTableBuilder tableBuilder = new IotdmPluginManagerPluginsTableBuilder()
                    .setPluginName(tableRow.getKey())
                    .setIotdmPluginManagerPluginInstances(instancesList);
            pluginsTable.add(tableBuilder.build());
        }

        output.setIotdmPluginManagerPluginsTable(pluginsTable);
        return RpcResultBuilder
                       .success(output.build())
                       .buildFuture();
    }

    @Override
    public Future<RpcResult<IotdmPluginManagerCustomServicesOutput>>
                                        iotdmPluginManagerCustomServices(
                                                IotdmPluginManagerCustomServicesInput input) {
        return iotdmPluginManagerCustomServicesImpl(input);
    }

    private Future<RpcResult<IotdmPluginManagerCustomServicesOutput>>
                                        iotdmPluginManagerCustomServicesImpl(
                                                IotdmPluginManagerCustomServicesInput input) {
        Collection<IotdmPluginsCustomService> customServices =
            PluginManagerCustomServicesService.getInstance().getAllServices();

        IotdmPluginManagerCustomServicesOutputBuilder output =
            new IotdmPluginManagerCustomServicesOutputBuilder();

        List<IotdmPluginManagerCustomServicesList> customServicesList = new LinkedList<>();

        for (IotdmPluginsCustomService service : customServices) {
            IotdmPluginManagerCustomServicesListBuilder customServiceBuilder =
                new IotdmPluginManagerCustomServicesListBuilder()
                    .setCustomServiceName(service.getCustomServiceName())
                    .setCustomServiceClass(service.getClass().getName())
                    .setCustomServiceState(service.getServiceStateData())
                    .setCustomServiceConfig(service.getServiceConfig());

            Collection<IotdmPluginCommonInterface> servicePlugins =
                PluginManagerCustomServicesService.getInstance().getPluginsOfService(service);

            if (null == servicePlugins || servicePlugins.isEmpty()) {
                continue;
            }

            Map<String, List<CustomServicePluginInstances>> pluginsTable = new ConcurrentHashMap<>();
            for (IotdmPluginCommonInterface plugin : servicePlugins) {

                CustomServicePluginInstancesBuilder instanceBuilder =
                    new CustomServicePluginInstancesBuilder()
                        .setPluginInstanceName(plugin.getInstanceName())
                        .setPluginInstanceCustomData(service.getPluginInstanceCustomData(plugin));

                List<CustomServicePluginInstances> instancesTable = null;
                if (pluginsTable.containsKey(plugin.getPluginName())) {
                    instancesTable = pluginsTable.get(plugin.getPluginName());
                }
                if (null == instancesTable) {
                    instancesTable = new LinkedList<>();
                    pluginsTable.put(plugin.getPluginName(), instancesTable);
                }

                instancesTable.add(instanceBuilder.build());
            }

            List<CustomServicePluginsTable> customServicePluginTable = new LinkedList<>();
            for (Map.Entry<String, List<CustomServicePluginInstances>> entry : pluginsTable.entrySet()) {
                CustomServicePluginsTableBuilder pluginTabBuilder =
                    new CustomServicePluginsTableBuilder()
                        .setPluginName(entry.getKey())
                        .setCustomServicePluginInstances(entry.getValue());

                customServicePluginTable.add(pluginTabBuilder.build());
            }

            customServiceBuilder.setCustomServicePluginsTable(customServicePluginTable);
            customServicesList.add(customServiceBuilder.build());
        }

        output.setIotdmPluginManagerCustomServicesList(customServicesList);
        return RpcResultBuilder
            .success(output.build())
            .buildFuture();
    }
}
