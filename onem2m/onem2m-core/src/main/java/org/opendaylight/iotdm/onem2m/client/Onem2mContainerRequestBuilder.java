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

public class Onem2mContainerRequestBuilder extends Onem2mRequestPrimitiveClientBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mContainerRequestBuilder.class);

    private ResourceContainerBuilder b;

    public Onem2mContainerRequestBuilder() {
        super();
        b = new ResourceContainerBuilder();

        // set dome default parameters that all internal apps have no need to set but the core expects
        setFrom("onem2m://Onem2mContainerRequest");
        setRequestIdentifier("Onem2mContainerRequest-rqi");
        setProtocol(Onem2m.Protocol.NATIVEAPP);
        setContentFormat(Onem2m.ContentFormat.JSON);
        setNativeAppName("Onem2mContainerRequest");

    }
    public Onem2mContainerRequestBuilder setCreator(String value) {
        b.setCreator(value);
        return this;
    }
    public Onem2mContainerRequestBuilder setMaxNrInstances(Integer value) {
        b.setMaxNrInstances(value);
        return this;
    }
    public Onem2mContainerRequestBuilder setMaxByteSize(Integer value) {
        b.setMaxByteSize(value);
        return this;
    }
    public Onem2mContainerRequestBuilder setOntologyRef(String value) {
        b.setOntologyRef(value);
        return this;
    }
    public Onem2mContainerRequestBuilder setPrimitiveContent(String value) {
        b.setPrimitiveContent(value);
        return this;
    }
    public Onem2mRequestPrimitiveClient build() {
        if (!isDelete) {
            String resourceString = b.build();
            super.setPrimitiveContent(resourceString);
        }
        if (isCreate) {
            setResourceType(Onem2m.ResourceType.CONTAINER);
        }
        return (super.build());
    }
}

