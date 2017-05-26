/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.common;

import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRequest;

public abstract class Onem2mProtocolPluginRequest<TOriginalRequest> implements IotdmPluginRequest<TOriginalRequest> {

    /**
     * Returns string representation of the Onem2m operation type
     * of the request.
     * @return Onem2m operation type.
     */
    public abstract Integer getOnem2mOperation();

    /**
     * Returns received target URI translated to Onem2m format.
     * @return The URI in Onem2m format.
     */
    public abstract String getOnem2mUri();

    // TODO do we really need this ???
    public abstract void setPayLoad(String s);

    public abstract void setContentType(String contentType);
}
