/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.mqtt.rx;

import org.opendaylight.iotdm.onem2m.plugins.channels.common.IotdmPluginOnem2mBaseRequest;
import org.opendaylight.iotdm.onem2m.plugins.channels.common.IotdmPluginOnem2mBaseResponse;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mRxRequestAbstractFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;

/**
 * Implements the general RxRequest factory.
 */
public class Onem2mMqttRxRequestFactory implements Onem2mRxRequestAbstractFactory<Onem2mMqttRxRequest,IotdmPluginOnem2mBaseRequest,IotdmPluginOnem2mBaseResponse> {

    @Override
    public Onem2mMqttRxRequest createRxRequest(IotdmPluginOnem2mBaseRequest request,
                                               IotdmPluginOnem2mBaseResponse response,
                                                       Onem2mService onem2mService,
                                                       SecurityLevel securityLevel) {

        return new Onem2mMqttRxRequest(request, response, onem2mService, securityLevel);
    }
}
