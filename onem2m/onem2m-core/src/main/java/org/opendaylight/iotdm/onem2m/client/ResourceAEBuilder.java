/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.client;

import org.opendaylight.iotdm.onem2m.core.resource.ResourceAE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceAEBuilder extends ResourceContentBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceAEBuilder.class);
    /**
     * The onem2m-protocols use this to create a new RequestPrimitive class
     */
    public ResourceAEBuilder() {
        super();
    }
    public ResourceAEBuilder setAppName(String value) {
        jsonContent.put(ResourceAE.APP_NAME, value);
        return this;
    }
    public ResourceAEBuilder setResourceName(String value) {
        jsonContent.put(ResourceAE.APP_ID, value);
        return this;
    }
    public ResourceAEBuilder setParentId(String value) {
        jsonContent.put(ResourceAE.AE_ID, value);
        return this;
    }
    public ResourceAEBuilder setRequestIdentifier(String value) {
        jsonContent.put(ResourceAE.POINT_OF_ACCESS, value);
        return this;
    }
    public ResourceAEBuilder setExpirationTime(String value) {
        jsonContent.put(ResourceAE.ONTOLOGY_REF, value);
        return this;
    }
}
