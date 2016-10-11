/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.coap.rx;

import org.opendaylight.iotdm.onem2m.plugins.channels.coap.IotdmPluginCoapRequest;
import org.opendaylight.iotdm.onem2m.plugins.channels.coap.IotdmPluginCoapResponse;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mRxRequestAbstractFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;

/**
 * Implements the general RxRequest factory.
 */
public class Onem2mCoapRxRequestFactory implements Onem2mRxRequestAbstractFactory<Onem2mCoapRxRequest,IotdmPluginCoapRequest,IotdmPluginCoapResponse> {

    @Override
    public Onem2mCoapRxRequest createRxRequest(IotdmPluginCoapRequest request,
                                               IotdmPluginCoapResponse response,
                                                       Onem2mService onem2mService,
                                                       SecurityLevel securityLevel) {

        return new Onem2mCoapRxRequest(request, response, onem2mService, securityLevel);
    }
}
