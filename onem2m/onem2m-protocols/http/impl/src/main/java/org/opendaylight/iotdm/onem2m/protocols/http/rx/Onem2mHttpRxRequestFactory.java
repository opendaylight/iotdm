/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http.rx;

import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mRxRequestAbstractFactory;
import org.opendaylight.iotdm.onem2m.commchannels.http.IotdmPluginHttpRequest;
import org.opendaylight.iotdm.onem2m.commchannels.http.IotdmPluginHttpResponse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;

/**
 * Implements the general RxRequest factory.
 */
public class Onem2mHttpRxRequestFactory implements Onem2mRxRequestAbstractFactory<Onem2mHttpRxRequest,IotdmPluginHttpRequest,IotdmPluginHttpResponse> {

    @Override
    public Onem2mHttpRxRequest createRxRequest(IotdmPluginHttpRequest request,
                                               IotdmPluginHttpResponse response,
                                               Onem2mService onem2mService,
                                               SecurityLevel securityLevel) {

        return new Onem2mHttpRxRequest(request, response, onem2mService, securityLevel);
    }
}
