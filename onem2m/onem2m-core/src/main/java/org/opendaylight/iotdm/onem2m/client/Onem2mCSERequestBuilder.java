/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.client;

import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mCSERequestBuilder extends Onem2mRequestPrimitiveClientBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCSERequestBuilder.class);

    private ResourceCSEBuilder b;

    public Onem2mCSERequestBuilder() {
        super();
        b = new ResourceCSEBuilder();

        // set some default parameters that all internal apps have no need to set but the core expects
        setFrom("onem2m://Onem2mCSERequest");
        setRequestIdentifier("Onem2mCSERequest-rqi");
        setProtocol(Onem2m.Protocol.NATIVEAPP);
        setContentFormat(Onem2m.ContentFormat.JSON);
        setNativeAppName("Onem2mCSERequest");
    }

    public Onem2mCSERequestBuilder setCseId(String value) {
        b.setCseId(value);
        return this;
    }

    public Onem2mCSERequestBuilder setCseType(String value) {
        b.setCseType(value);
        return this;
    }

    public Onem2mRequestPrimitiveClient build() {
        if (!isDelete) {
            String resourceString = b.build();
            setPrimitiveContent(resourceString);
        }
        if (isCreate) {
            setResourceType(Onem2m.ResourceType.CSE_BASE);
        }
        return (super.build());
    }
}

