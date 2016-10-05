/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements some methods used by PluginManager in order
 * to make and maintain protocol plugin registrations.
 * Also abstract methods are defined which have to be implemented by
 * every concrete plugin implementation.
 * @param <Treq> Type of the supported request.
 * @param <Trsp> Type of the supported response.
 */
public abstract class IotdmPlugin<Treq extends IotdmPluginRequest,
                                  Trsp extends IotdmPluginResponse> implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmPlugin.class);

    private final int instanceKey; // Value which is unique for this plugin instance
    private final Onem2mPluginManager pluginManager;

    /**
     * Constructor requires instance of plugin manager which is used for registration.
     * Plugin manager generates unique instanceKey value needed for registration.
     * @param pluginManager The PluginManager instance.
     */
    public IotdmPlugin(Onem2mPluginManager pluginManager) {
        this.pluginManager = pluginManager;
        this.instanceKey = pluginManager.getNewPluginInstanceKey();
        LOG.info("New instance of IotdmPlugin with pluginName: {}, instanceKey: {}",
                 this.pluginName(), this.instanceKey);
    }

    /**
     * Returns the unique instanceKey value.
     * @return Instance key value unique in the PluginManager.
     */
    public final int getInstanceKey() {
        return this.instanceKey;
    }

    /**
     * Compares this instances and given plugin and returns true in case
     * of match and false otherwise.
     * @param plugin The second plugin instance.
     * @return True if the instances are the same, False otherwise.
     */
    public final boolean isPlugin(IotdmPlugin plugin) {
        if (this.pluginName().equals(plugin.pluginName()) && this.getInstanceKey() == plugin.getInstanceKey()) {
            return true;
        }
        return false;
    }

    /**
     * Returns debugging string describing the plugin instance.
     * @return String with plugin parameters.
     */
    public final String getDebugString() {
        return "pluginName: " + this.pluginName() + " instanceKey: " + this.getInstanceKey();
    }

    /**
     * Returns the name of plugin (common for all instances).
     * @return Plugin name.
     */
    public abstract String pluginName();

    /**
     * Implementation of the handler method.
     * @param request Received request to be handled.
     * @param response Response to be filled by result of the handling.
     */
    public abstract void handle(Treq request, Trsp response);
}