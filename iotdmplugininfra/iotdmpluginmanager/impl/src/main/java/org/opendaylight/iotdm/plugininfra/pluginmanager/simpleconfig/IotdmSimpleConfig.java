/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.simpleconfig;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.simpleconfig.rev161122.key.val.list.def.KeyValList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.simpleconfig.rev161122.plugin.simple.config.definition.PluginSimpleConfig;

/**
 * Class wraps generic data model for SimpleConfig and
 * implements accessing data from the configuration.
 */
public class IotdmSimpleConfig {

    private final PluginSimpleConfig config;
    private final Map<String, String> kvMap;

    protected IotdmSimpleConfig(PluginSimpleConfig pluginConfig) {
        this.config = pluginConfig;
        this.kvMap = new HashMap<>();

        // Fills key-value map with data from the configuration passed
        if (null != pluginConfig && null != pluginConfig.getKeyValList()) {
            for (KeyValList kv : pluginConfig.getKeyValList()) {
                this.kvMap.put(kv.getCfgKey(), kv.getCfgVal());
            }
        }
    }

    protected PluginSimpleConfig getConfiguration() {
        return this.config;
    }

    /**
     * Gets value identified by unique key.
     * @param key Unique key identifying value
     * @return Related value or null if the key doesn't exist
     */
    public String getVal(String key) {
        if (null == key) {
            return null;
        }
        return this.kvMap.get(key);
    }

    /**
     * Returns Map of key-value pairs of the configuration.
     * @return Configuration as map
     */
    public Map<String, String> getKeyValMap() {
        return this.kvMap;
    }

    /**
     * Returns configuration as string for logging/debugging purposes.
     * @return String representation of the configuration
     */
    public String getDebugString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SimpleConfig:\n");
        boolean first = true;
        for (Map.Entry<String, String> entry : this.kvMap.entrySet()) {
            if (first) {
                first = false;
            } else {
                builder.append(",\n");
            }

            builder.append(entry.getKey()).append(":").append(entry.getValue());
        }

        return builder.toString();
    }
}
