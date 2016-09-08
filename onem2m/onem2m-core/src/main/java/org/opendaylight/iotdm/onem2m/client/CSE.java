/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.client;

import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceCse;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSE extends Onem2mRequestPrimitiveClientBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(CSE.class);

    public CSE() {
        super();

        // set some default parameters that all internal apps have no need to set but the core expects
        setFrom("onem2m://Onem2mCSERequest");
        setRequestIdentifier("Onem2mCSERequest-rqi");
        setProtocol(Onem2m.Protocol.NATIVEAPP);
    }

    public CSE setCseId(String value) {
        setPrimitiveNameValue("CSE_ID", value);
        return this;
    }

    public CSE setCseType(String value) {
        setPrimitiveNameValue("CSE_TYPE", value);
        return this;
    }

    public Onem2mRequestPrimitiveClient build() {
        if (isCreate) {
            setNativeAppName("CSEProvisioning");
        }
        return (super.build());
    }
}

