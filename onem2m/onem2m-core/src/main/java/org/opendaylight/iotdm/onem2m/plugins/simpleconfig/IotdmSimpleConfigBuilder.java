/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins.simpleconfig;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2msimpleconfig.rev161122.key.val.list.def.KeyValList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2msimpleconfig.rev161122.key.val.list.def.KeyValListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2msimpleconfig.rev161122.plugin.simple.config.definition.PluginSimpleConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2msimpleconfig.rev161122.plugin.simple.config.definition.PluginSimpleConfigBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class IotdmSimpleConfigBuilder {
    private final PluginSimpleConfigBuilder builder;

    public IotdmSimpleConfigBuilder() {
        // Internally uses builder generated from yang model
        this.builder = new PluginSimpleConfigBuilder();
    }

    /**
     * Creates copy of the configuration passed.
     * @param config Configuration
     */
    protected IotdmSimpleConfigBuilder(PluginSimpleConfig config) {
        this.builder = new PluginSimpleConfigBuilder();
        List<KeyValList> copy = new LinkedList<>();
        for (KeyValList kv : config.getKeyValList()) {
            KeyValListBuilder builder = new KeyValListBuilder();
            builder.setCfgKey(kv.getCfgKey());
            builder.setCfgVal(kv.getCfgVal());
            copy.add(builder.build());
        }

        this.builder.setKeyValList(copy);
    }

    private Optional<KeyValList> findKeyVal(String key) {
        if (null == this.builder.getKeyValList()) {
            return Optional.empty();
        }
        return this.builder.getKeyValList().stream().filter(item -> item.getCfgKey().equals(key)).findFirst();
    }

    /**
     * Set value for specific key.
     * @param key Key
     * @param val Value
     */
    public void setVal(String key, String val) {
        Optional<KeyValList> kv = findKeyVal(key);

        if (kv.isPresent()) {
            // Remove already existing value of the same key
            if (kv.get().getCfgVal().equals(val)) {
                // Key and Value are the same, nothing to do
                return;
            }

            // Remove the old value
            this.builder.getKeyValList().remove(kv.get());
        }

        if (null == this.builder.getKeyValList()) {
            // This is the first key-value pair, need to create list
            this.builder.setKeyValList(new LinkedList<KeyValList>());
        }

        // Build the new key-value pair and add to the list
        KeyValListBuilder kvBuilder = new KeyValListBuilder();
        kvBuilder.setCfgKey(key);
        kvBuilder.setCfgVal(val);
        this.builder.getKeyValList().add(kvBuilder.build());
    }

    /**
     * Returns value set for specific key.
     * @param key Key
     * @return Related value or null if the key doesn't exist
     */
    public String getVal(String key) {
        Optional<KeyValList> kv = findKeyVal(key);

        if (kv.isPresent()) {
            return kv.get().getCfgVal();
        }

        return null;
    }

    /**
     * Deletes specific key.
     * @param key Key
     */
    public void delVal(String key) {
        Optional<KeyValList> kv = findKeyVal(key);

        if (kv.isPresent()) {
            this.builder.getKeyValList().remove(kv.get());
            if (this.builder.getKeyValList().isEmpty()) {
                this.builder.setKeyValList(null);
            }
        }
    }

    /**
     * Builds the IotdmSimpleConfig instance.
     * @return SimpleConfig instance
     */
    public IotdmSimpleConfig build() {
        return new IotdmSimpleConfig(this.builder.build());
    }
}
