/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.common;

import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginResponse;

public abstract class Onem2mProtocolPluginResponse implements IotdmPluginResponse {

    /**
     * Sets response parameter from the generic Onem2m response primitive.
     * @param onem2mResponse The Onem2m response primitive.
     * @return True is returned in case of success, False otherwise.
     */
    public abstract boolean setFromResponsePrimitive(ResponsePrimitive onem2mResponse);
}
