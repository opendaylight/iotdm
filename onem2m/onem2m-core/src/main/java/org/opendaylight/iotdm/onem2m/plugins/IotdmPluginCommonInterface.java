/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins;

public interface IotdmPluginCommonInterface {
    /**
     * Returns the name of plugin (common for all instances).
     * @return Plugin name.
     */
    String getPluginName();
}
