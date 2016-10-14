/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http.rx;

import org.opendaylight.iotdm.onem2m.protocols.common.utils.IotdmProtocolConfigGetter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev141210.ServerConfig;

/**
 * Class extends ServerConfig class generated from yang model. New constructor is added which allows
 * to instantiate ServerConfig object from any other object implementing the same getter methods.
 * This is useful if other module imports the same yang model and reuses the ServerConfig items so
 * the same verification methods can be used.
 */
public class Onem2mHttpBaseIotdmPluginConfig extends ServerConfig {

    public Onem2mHttpBaseIotdmPluginConfig(Object config) {
        super();
        this.setSecureConnection(IotdmProtocolConfigGetter.getAttribute(config, "getSecureConnection", Boolean.class));
        this.setServerPort(IotdmProtocolConfigGetter.getAttribute(config, "getServerPort", Integer.class));
        this.setServerSecurityLevel(IotdmProtocolConfigGetter.getAttribute(config, "getServerSecurityLevel",
                                                                           SecurityLevel.class));
    }

}
