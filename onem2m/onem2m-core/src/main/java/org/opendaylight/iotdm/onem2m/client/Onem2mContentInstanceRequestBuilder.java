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

public class Onem2mContentInstanceRequestBuilder extends Onem2mRequestPrimitiveClientBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mContentInstanceRequestBuilder.class);

    private ResourceContentInstanceBuilder b;

    public Onem2mContentInstanceRequestBuilder() {
        super();
        b = new ResourceContentInstanceBuilder();

        // set dome default parameters that all internal apps have no need to set but the core expects
        setFrom("onem2m://Onem2mContentInstanceRequest");
        setRequestIdentifier("Onem2mContentInstanceRequest-rqi");
        setProtocol(Onem2m.Protocol.NATIVEAPP);
        setContentFormat(Onem2m.ContentFormat.JSON);
        setNativeAppName("Onem2mContentInstanceRequest");

    }

    public Onem2mContentInstanceRequestBuilder setContent(String value) {
        b.setContent(value);
        return this;
    }
    public Onem2mContentInstanceRequestBuilder setContentInfo(String value) {
        b.setContentInfo(value);
        return this;
    }
    public Onem2mContentInstanceRequestBuilder setOntologyRef(String value) {
        b.setOntologyRef(value);
        return this;
    }
    public Onem2mContentInstanceRequestBuilder setPrimitiveContent(String value) {
        b.setPrimitiveContent(value);
        return this;
    }
    public Onem2mRequestPrimitiveClient build() {
        if (!isDelete) {
            String resourceString = b.build();
            super.setPrimitiveContent(resourceString);
        }
        if (isCreate) {
            setResourceType(Onem2m.ResourceType.CONTENT_INSTANCE);
        }
        return (super.build());
    }
}

