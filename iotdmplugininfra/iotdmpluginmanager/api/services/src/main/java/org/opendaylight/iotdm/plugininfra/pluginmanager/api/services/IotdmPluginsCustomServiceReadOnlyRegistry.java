/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.api.services;

import java.util.Collection;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginCommonInterface;

/**
 * Read only registry of plugins registered by custom service. This interface
 * is used by PluginManager which can retrieve plugins registered to the custom service
 * and provide data about them in outputs of RPC calls provided by PluginManager.
 */
public interface IotdmPluginsCustomServiceReadOnlyRegistry {
    boolean isPluginRegistered(IotdmPluginCommonInterface plugin);
    Collection<IotdmPluginCommonInterface> getRegisteredPlugins();
}
