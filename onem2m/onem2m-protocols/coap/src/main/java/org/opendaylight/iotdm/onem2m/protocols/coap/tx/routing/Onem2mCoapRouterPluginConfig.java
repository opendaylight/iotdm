/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.coap.tx.routing;

import org.opendaylight.iotdm.onem2m.protocols.common.utils.IoTdmProtocolConfigGetter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev141210.RouterPluginConfig;

/**
 * Class extends the RouterPluginConfig class generated from yang model.
 * New constructor is added which allows casting from any class implementing
 * the same getter methods.
 */
public class Onem2mCoapRouterPluginConfig extends RouterPluginConfig {

    public Onem2mCoapRouterPluginConfig(Object config) {
        super();
        this.setSecureConnection(IoTdmProtocolConfigGetter.getAttribute(config, "getSecureConnection", Boolean.class));
    }
}
