/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.dbapi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeWriter;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginCommonInterface;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.services.IotdmPluginManagerCustomServicesService;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.services.IotdmPluginsCustomService;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.services.IotdmPluginsCustomServicePluginContextRegistry;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.services.IotdmPluginsCustomServiceRegistrationException;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.services.registers.CustomServicePluginsCtxRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginFilters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.custom.services.output.iotdm.plugin.manager.custom.services.list.CustomServiceConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.custom.services.output.iotdm.plugin.manager.custom.services.list.CustomServiceState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.custom.services.output.iotdm.plugin.manager.custom.services.list.custom.service.plugins.table.custom.service.plugin.instances.PluginInstanceCustomData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.plugin.data.output.iotdm.plugin.manager.plugins.table.iotdm.plugin.manager.plugin.instances.custom.services.list.CustomServicePluginData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.dbapi.rev180305.Onem2mDbApiClientRegistrationsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.dbapi.rev180305.Onem2mDbApiClientRegistrationsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.dbapi.rev180305.Onem2mDbApiClientRegistrationsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.dbapi.rev180305.iotdm.plugin.manager.custom.services.output.iotdm.plugin.manager.custom.services.list.custom.service.plugins.table.custom.service.plugin.instances.plugin.instance.custom.data.Onem2mDbApiDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.dbapi.rev180305.iotdm.plugin.manager.plugin.data.output.iotdm.plugin.manager.plugins.table.iotdm.plugin.manager.plugin.instances.custom.services.list.custom.service.plugin.data.Onem2mDbApiPluginDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.dbapi.rev180305.onem2m.db.api.client.registration.definition.DbApiClientPluginDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.dbapi.rev180305.onem2m.db.api.client.registrations.output.RegisteredDbApiClientPluginsTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.dbapi.rev180305.onem2m.db.api.client.registrations.output.RegisteredDbApiClientPluginsTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.dbapi.rev180305.onem2m.db.api.client.registrations.output.registered.db.api.client.plugins.table.RegisteredDbApiClientPluginInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.dbapi.rev180305.onem2m.db.api.client.registrations.output.registered.db.api.client.plugins.table.RegisteredDbApiClientPluginInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.dbapi.rev180305.Onem2mDbApiService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by bjanosik on 9/21/16.
 */
public class Onem2mDbApiProvider implements Onem2mDbApiService, Onem2mPluginsDbApi,
                                            IotdmPluginsCustomService {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mDbApiProvider.class);

    private final AtomicReference<ResourceTreeWriter> twc = new AtomicReference<>();
    private final AtomicReference<ResourceTreeReader> trc = new AtomicReference<>();
    private final AtomicBoolean apiReady = new AtomicBoolean(false);

    private IotdmPluginManagerCustomServicesService customServicesService = null;
    private final CustomServicePluginsCtxRegistry<Onem2mDbApiClientPlugin, PluginDbClientData> pluginsRegister =
                                                                                new CustomServicePluginsCtxRegistry<>();
    private static final Onem2mDbApiProvider _instance = new Onem2mDbApiProvider();

    /**
     * States of the registered DB API client
     */
    protected enum IotdmPLuginDbClientState {
        /**
         * Initial state
         */
        INIT,

        /**
         * DB API client has been started successfully
         */
        STARTED,

        /**
         * DB API client has been stopped
         */
        STOPPED,

        /**
         * DB API client is in error state
         */
        ERROR
    }

    protected class PluginDbClientData {
        private final Onem2mDbApiClientPlugin client;
        private IotdmPLuginDbClientState state = IotdmPLuginDbClientState.INIT;

        public PluginDbClientData(Onem2mDbApiClientPlugin client) {
            this.client = client;
        }

        public Onem2mDbApiClientPlugin getClient() {
            return client;
        }

        public IotdmPLuginDbClientState getState() {
            return state;
        }

        public void setState(IotdmPLuginDbClientState state) {
            this.state = state;
        }
    }

    private Onem2mDbApiProvider() {}

    public void setCustomServicesService(IotdmPluginManagerCustomServicesService customServicesService) {
        this.customServicesService = customServicesService;
    }

    public void init() throws IotdmPluginsCustomServiceRegistrationException {
        this.customServicesService.registerCustomService(this, pluginsRegister);
    }

    public void close() {
        if (null != this.customServicesService) {
            this.customServicesService.unregisterCustomService(this);
        }
    }

    public static Onem2mDbApiProvider getInstance() {
        return _instance;
    }

    public boolean isApiReady() {
        return this.apiReady.get();
    }

    // This method is synchronized so all modifications of Reader, Writer and apiReady are synchronized
    private synchronized void changeReaderWriter(boolean register, ResourceTreeWriter twc, ResourceTreeReader trc) {
        if (register) {
            this.trc.set(trc);
            this.twc.set(twc);
            this.apiReady.set(true);

            for (IotdmPluginsCustomServicePluginContextRegistry
                    .PluginContextRegistryItem<Onem2mDbApiClientPlugin, PluginDbClientData> item :
                                                                pluginsRegister.getAllRegistryItems()) {
                try {
                    item.getPlugin().dbClientStart();
                    item.getContext().setState(IotdmPLuginDbClientState.STARTED);
                } catch (Exception e) {
                    item.getContext().setState(IotdmPLuginDbClientState.ERROR);
                    LOG.error("Failed to start DB Client plugin: {}, error message: {}",
                              item.getPlugin().getDebugString(), e);
                }
            }

            LOG.info("New DB Reader and Writer registered");
        } else {
            for (IotdmPluginsCustomServicePluginContextRegistry
                    .PluginContextRegistryItem<Onem2mDbApiClientPlugin, PluginDbClientData> item :
                                                                    pluginsRegister.getAllRegistryItems()) {
                item.getPlugin().dbClientStop();
                item.getContext().setState(IotdmPLuginDbClientState.STOPPED);
            }

            this.apiReady.set(false);
            this.trc.set(null);
            this.twc.set(null);

            LOG.info("DB Reader and Writer unregistered");
        }
    }

    public void registerDbReaderAndWriter(ResourceTreeWriter twc, ResourceTreeReader trc) {
        this.changeReaderWriter(true, twc, trc);
    }

    public void unregisterDbReaderAndWriter() {
        this.changeReaderWriter(false, null, null);
    }

    // TODO throw IotdmDaoReadException if the reader is not there
    public ResourceTreeReader getReader() { return this.trc.get(); }

    // TODO throw IotdmDaoWriteException if the writer is not there
    public ResourceTreeWriter getWriter() { return this.twc.get(); }

    public List<String> getCseList() {
        return Onem2mDb.getInstance().getCseList();
    }

    public String findResourceIdUsingURI(String uri) {
        return Onem2mDb.getInstance().findResourceIdUsingURI(Onem2m.translateUriToOnem2m(uri));
    }

    public String getHierarchicalNameForResource(Onem2mResource onem2mResource) {
        return Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource);
    }

    public List<String> getHierarchicalResourceList(String startResourceId, int limit) {
        return Onem2mDb.getInstance().getHierarchicalResourceList(startResourceId, limit);
    }

    public Onem2mResource getResource(String resourceId) {
        return Onem2mDb.getInstance().getResource(resourceId);
    }

    public Onem2mResource getResourceUsingURI(String targetURI) {
        return Onem2mDb.getInstance().findResourceUsingURI(targetURI);
    }

    public boolean isLatestCI(Onem2mResource onem2mResource) {
        return Onem2mDb.getInstance().isLatestCI(onem2mResource);
    }

    public boolean isResourceIdUnderTargetId(String targetResourceId, String onem2mResourceId) {
        return Onem2mDb.getInstance().isResourceIdUnderTargetId(targetResourceId, onem2mResourceId);
    }

    public String findCseForTarget(String targetResourceId) {
        return Onem2mDb.getInstance().findCseForTarget(targetResourceId);
    }

    @Override
    public Future<RpcResult<Onem2mDbApiClientRegistrationsOutput>>
            onem2mDbApiClientRegistrations(Onem2mDbApiClientRegistrationsInput input) {
        return this.onem2mPluginManagerDbApiClientRegistrationsImpl(input);
    }

    private Future<RpcResult<Onem2mDbApiClientRegistrationsOutput>>
    onem2mPluginManagerDbApiClientRegistrationsImpl(IotdmPluginFilters input) {

        Onem2mDbApiClientRegistrationsOutputBuilder output =
            new Onem2mDbApiClientRegistrationsOutputBuilder();

        // PluginName : InstanceName : Registered
        Map<String, List<RegisteredDbApiClientPluginInstances>> regs = new ConcurrentHashMap<>();

        for (IotdmPluginsCustomServicePluginContextRegistry
                    .PluginContextRegistryItem<Onem2mDbApiClientPlugin, PluginDbClientData> item :
                                                                    pluginsRegister.getAllRegistryItems()) {
            // filter only pluginsRegister matching the filters
            if (! this.customServicesService.applyPluginFilters(input, item.getPlugin())) {
                continue;
            }

            // Registered Instances of the plugin
            RegisteredDbApiClientPluginInstancesBuilder instancesBuilder = null;

            if (! regs.containsKey(item.getPlugin().getPluginName())) {
                // This is the first plugin instance of such name
                regs.put(item.getPlugin().getPluginName(), new LinkedList<>());
            }

            // Prepare DbApiClientPlugin data first
            DbApiClientPluginDataBuilder builder = new DbApiClientPluginDataBuilder()
                .setIotdmCommonPluginData(this.customServicesService.createIotdmPluginData(item.getPlugin()))
                .setDbApiClientState(item.getContext().getState().toString());

            instancesBuilder = new RegisteredDbApiClientPluginInstancesBuilder()
                .setPluginInstanceName(item.getPlugin().getInstanceName())
                .setDbApiClientPluginData(builder.build());

            // Put the instances item into the list
            regs.get(item.getPlugin().getPluginName()).add(instancesBuilder.build());
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

    private void handleRegistrationError(String format, String... args) throws IotdmPluginRegistrationException {
        this.customServicesService.handlePluginRegistrationError(LOG, format, args);
    }

    @Override
    public void registerDbClientPlugin(Onem2mDbApiClientPlugin plugin) throws IotdmPluginRegistrationException {
        for (IotdmPluginsCustomServicePluginContextRegistry
                .PluginContextRegistryItem<Onem2mDbApiClientPlugin, PluginDbClientData> item :
                                                            pluginsRegister.getAllRegistryItems()) {
            if (item.getPlugin().isPlugin(plugin)) {
                handleRegistrationError("Attempt to multiple registration of DB client plugin: {}",
                                        plugin.getPluginName());
            }
        }

        PluginDbClientData dbClientData = new PluginDbClientData(plugin);
        this.pluginsRegister.registerPlugin(plugin, dbClientData);
        if (this.isApiReady()) {
            try {
                plugin.dbClientStart();
                dbClientData.setState(IotdmPLuginDbClientState.STARTED);
            } catch (Exception e) {
                dbClientData.setState(IotdmPLuginDbClientState.ERROR);
                handleRegistrationError("Failed to start DB Client plugin: {}, error message: {}",
                                        plugin.getPluginName(), e.toString());
            }
        }

        LOG.info("Registered DB client plugin: {}", plugin.getPluginName());
    }

    @Override
    public void unregisterDbClientPlugin(Onem2mDbApiClientPlugin plugin) {
        for (IotdmPluginsCustomServicePluginContextRegistry
                .PluginContextRegistryItem<Onem2mDbApiClientPlugin, PluginDbClientData> item :
                                                            pluginsRegister.getAllRegistryItems()) {
            if (item.getPlugin().isPlugin(plugin)) {
                plugin.dbClientStop();
                this.pluginsRegister.unregisterPlugin(plugin);
                LOG.info("Unregistered DB client plugin: {}", plugin.getPluginName());
            }
        }
    }

    /*
     * IotdmPluginCustomService implementation
     */
    @Override
    public String getCustomServiceName() {
        return "Onem2mPluginsDbApi";
    }


    @Override
    public CustomServicePluginData getServiceSpecificPluginData(IotdmPluginCommonInterface plugin) {
        Onem2mDbApiPluginDataBuilder builder = new Onem2mDbApiPluginDataBuilder();
        /* TODO need to simplify search for pluginsRegister */
        for (IotdmPluginsCustomServicePluginContextRegistry
            .PluginContextRegistryItem<Onem2mDbApiClientPlugin, PluginDbClientData> item :
            pluginsRegister.getAllRegistryItems()) {
            if (item.getPlugin().isPlugin(plugin)) {
                builder.setDbApiClientState(item.getContext().getState().toString());
            }
        }
        return builder.build();
    }

    @Override
    public PluginInstanceCustomData getPluginInstanceCustomData(IotdmPluginCommonInterface plugin) {
        Onem2mDbApiDataBuilder builder = new Onem2mDbApiDataBuilder();
        /* TODO need to simplify search for pluginsRegister */
        for (IotdmPluginsCustomServicePluginContextRegistry
            .PluginContextRegistryItem<Onem2mDbApiClientPlugin, PluginDbClientData> item :
            pluginsRegister.getAllRegistryItems()) {
            if (item.getPlugin().isPlugin(plugin)) {
                builder.setDbApiClientState(item.getContext().getState().toString());
            }
        }
        return builder.build();
    }

    @Override
    public CustomServiceState getServiceStateData() {
        // Not used
        return null;
    }

    @Override
    public CustomServiceConfig getServiceConfig() {
        // Not used
        return null;
    }
}
