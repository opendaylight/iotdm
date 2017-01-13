/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.plugin.data.output.onem2m.plugin.manager.plugins.table.onem2m.plugin.manager.plugin.instances.plugin.configuration.PluginSpecificConfiguration;

/**
 * This interface describes implementation of plugins which are configurable
 * by specific configuration class which extends PluginConfiguration.
 */
public interface IotdmPluginConfigurable extends IotdmPluginCommonInterface {

    /**
     * Method returns current running configuration of the plugin instance.
     * @return Running config.
     */
    PluginSpecificConfiguration getRunningConfig();
}
