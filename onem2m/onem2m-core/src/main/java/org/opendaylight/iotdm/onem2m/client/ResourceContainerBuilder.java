/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.client;

import org.opendaylight.iotdm.onem2m.core.resource.ResourceContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceContainerBuilder extends ResourceContentBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceContainerBuilder.class);
    /**
     * The onem2m-protocols use this to create a new RequestPrimitive class
     */
    public ResourceContainerBuilder() {
        super();
    }

    public ResourceContainerBuilder setCreator(String value) {
        jsonContent.put(ResourceContainer.CREATOR, value);
        return this;
    }
    public ResourceContainerBuilder setMaxNrInstances(Integer value) {
        jsonContent.put(ResourceContainer.MAX_NR_INSTANCES, value);
        return this;
    }
    public ResourceContainerBuilder setMaxByteSize(Integer value) {
        jsonContent.put(ResourceContainer.MAX_BYTE_SIZE, value);
        return this;
    }
    public ResourceContainerBuilder setOntologyRef(String value) {
        jsonContent.put(ResourceContainer.ONTOLOGY_REF, value);
        return this;
    }
}
