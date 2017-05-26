/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.common;

import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRequest;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginResponse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;

/**
 * Defines abstract factory for RxRequests.
 */
public interface Onem2mRxRequestAbstractFactory<TRxRequest extends Onem2mProtocolRxRequest,
                                                TIotdmPluginRequest extends IotdmPluginRequest,
                                                TIotdmPluginResponse extends IotdmPluginResponse> {

    /**
     * Creates Onem2mRxRequest for further processing.
     * @param request The plugin manager request.
     * @param response The plugin manager response.
     * @param onem2mService Onem2m service which is used for the
     *                      processing of request.
     * @param securityLevel The security level configured at the
     *                      time when the request was received.
     * @return The generic RxRequest which can be handled by RxHandler.
     */
    TRxRequest createRxRequest(TIotdmPluginRequest request,
                                            TIotdmPluginResponse response,
                                            Onem2mService onem2mService,
                                            SecurityLevel securityLevel);
}
