/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins;

import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by bjanosik on 9/21/16.
 */
public class Onem2mPluginsDbApi {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mPluginsDbApi.class);

    private final AtomicReference<ResourceTreeWriter> twc = new AtomicReference<>();
    private final AtomicReference<ResourceTreeReader> trc = new AtomicReference<>();
    private final AtomicBoolean apiReady = new AtomicBoolean(false);

    private static Onem2mPluginsDbApi api;
    private final List<PluginDbClientData> plugins = Collections.synchronizedList(new LinkedList<>());

    /**
     * States of the registered DB API client
     */
    enum IotdmPLuginDbClientState {
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
        private final IotdmPluginDbClient client;
        private IotdmPLuginDbClientState state;

        public PluginDbClientData(IotdmPluginDbClient client) {
            this.client = client;
        }

        public IotdmPluginDbClient getClient() {
            return client;
        }

        public IotdmPLuginDbClientState getState() {
            return state;
        }

        public void setState(IotdmPLuginDbClientState state) {
            this.state = state;
        }
    }

    public static Onem2mPluginsDbApi getInstance() {
        if (api == null) {
            api = new Onem2mPluginsDbApi();
        }
        return api;
    }

    private Onem2mPluginsDbApi() {
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

            for (PluginDbClientData dbClientData : plugins) {
                try {
                    dbClientData.getClient().dbClientStart(twc, trc);
                    dbClientData.setState(IotdmPLuginDbClientState.STARTED);
                } catch (Exception e) {
                    dbClientData.setState(IotdmPLuginDbClientState.ERROR);
                    LOG.error("Failed to start DB Client plugin: {}, error message: {}",
                              dbClientData.getClient().getPluginName(), e);
                }
            }

            LOG.info("New DB Reader and Writer registered");
        } else {
            for (PluginDbClientData dbClientData : plugins) {
                dbClientData.getClient().dbClientStop();
                dbClientData.setState(IotdmPLuginDbClientState.STOPPED);
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
        return Onem2mDb.getInstance().getCseList(this.trc.get());
    }

    public String findResourceIdUsingURI(String uri) {
        return Onem2mDb.getInstance().findResourceIdUsingURI(this.trc.get(), Onem2m.translateUriToOnem2m(uri));
    }

    public String getHierarchicalNameForResource(Onem2mResource onem2mResource) {
        return Onem2mDb.getInstance().getHierarchicalNameForResource(this.trc.get(), onem2mResource);
    }

    public List<String> getHierarchicalResourceList(String startResourceId, int limit) {
        return Onem2mDb.getInstance().getHierarchicalResourceList(this.trc.get(), startResourceId, limit);
    }

    public Onem2mResource getResource(String resourceId) {
        return Onem2mDb.getInstance().getResource(this.trc.get(), resourceId);
    }

    public Onem2mResource getResourceUsingURI(String targetURI) {
        return Onem2mDb.getInstance().getResourceUsingURI(this.trc.get(), targetURI);
    }

    public boolean isLatestCI(Onem2mResource onem2mResource) {
        return Onem2mDb.getInstance().isLatestCI(this.trc.get(), onem2mResource);
    }

    public boolean isResourceIdUnderTargetId(String targetResourceId, String onem2mResourceId) {
        return Onem2mDb.getInstance().isResourceIdUnderTargetId(this.trc.get(), targetResourceId, onem2mResourceId);
    }

    public String findCseForTarget(String targetResourceId) {
        return Onem2mDb.getInstance().findCseForTarget(this.trc.get(), targetResourceId);
    }

    private void handleRegistrationError(String format, String... args) throws IotdmPluginRegistrationException {
        Onem2mPluginManagerUtils.handleRegistrationError(LOG, format, args);
    }

    protected void registerDbClientPlugin(IotdmPluginDbClient plugin) throws IotdmPluginRegistrationException {
        for (PluginDbClientData data : this.plugins) {
            if (data.getClient().isPlugin(plugin)) {
                handleRegistrationError("Attempt to multiple registration of DB client plugin: {}",
                                        plugin.getPluginName());
            }
        }

        PluginDbClientData dbClientData = new PluginDbClientData(plugin);
        boolean ret = this.plugins.add(dbClientData);
        if (ret) {
            if (this.isApiReady()) {
                try {
                    plugin.dbClientStart(this.getWriter(), this.getReader());
                    dbClientData.setState(IotdmPLuginDbClientState.STARTED);
                } catch (Exception e) {
                    dbClientData.setState(IotdmPLuginDbClientState.ERROR);
                    handleRegistrationError("Failed to start DB Client plugin: {}, error message: {}",
                                            plugin.getPluginName(), e.toString());
                }
            }
        } else {
            handleRegistrationError("Failed to register DB client plugin: {}", plugin.getPluginName());
        }

        LOG.info("Registered DB client plugin: {}", plugin.getPluginName());
    }

    protected void unregisterDbClientPlugin(IotdmPluginDbClient plugin) {
        for (PluginDbClientData data : this.plugins) {
            if (data.getClient().isPlugin(plugin)) {
                plugin.dbClientStop();
                this.plugins.remove(data);
                LOG.info("Unregistered DB client plugin: {}", plugin.getPluginName());
            }
        }
    }

    protected List<PluginDbClientData> getPlugins() {
        return plugins;
    }
}
