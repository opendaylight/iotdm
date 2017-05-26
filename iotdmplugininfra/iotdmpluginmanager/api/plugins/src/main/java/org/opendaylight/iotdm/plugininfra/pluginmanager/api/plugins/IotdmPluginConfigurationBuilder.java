/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins;

/**
 * Generic interface for PluginConfiguration builders.
 * Every PluginConfiguration builder must implement verify() and build()
 * methods.
 * @param <Tconfig>
 */
public interface IotdmPluginConfigurationBuilder<Tconfig> {

    /**
     * Verify method checks whether the current state of the builder
     * instance would result in successful configuration build. Exception is
     * thrown if the current state of the builder instance is invalid.
     * @return Returns this to enable method call chaining.
     * @throws IllegalArgumentException If the validation failed.
     */
    IotdmPluginConfigurationBuilder<Tconfig> verify() throws IllegalArgumentException;

    /**
     * Calls verify() method at the begin and returns built configuration
     * if the verification passed. Exception is thrown otherwise.
     * @return Resulting configuration.
     * @throws IllegalArgumentException If the verification or build failed.
     */
    Tconfig build() throws IllegalArgumentException;
}
