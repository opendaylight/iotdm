/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.websocket.rx;

import org.opendaylight.iotdm.onem2m.plugins.channels.websocket.IotdmPluginWebsocketRequest;
import org.opendaylight.iotdm.onem2m.plugins.channels.websocket.IotdmPluginWebsocketResponse;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mRxRequestAbstractFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;

/**
 * Implements the Websocket RxRequest factory.
 */
public class Onem2mWebsocketRxRequestFactory implements Onem2mRxRequestAbstractFactory<Onem2mWebsocketRxRequest,IotdmPluginWebsocketRequest,IotdmPluginWebsocketResponse> {

    @Override
    public Onem2mWebsocketRxRequest createRxRequest(IotdmPluginWebsocketRequest request,
                                                    IotdmPluginWebsocketResponse response,
                                                   Onem2mService onem2mService,
                                                   SecurityLevel securityLevel) {

        return new Onem2mWebsocketRxRequest(request, response, onem2mService, securityLevel);
    }
}
