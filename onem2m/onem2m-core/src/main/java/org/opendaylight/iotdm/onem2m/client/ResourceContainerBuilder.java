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
    public ResourceContainerBuilder setMaxNrInstances(String value) {
        jsonContent.put(ResourceContainer.MAX_NR_INSTANCES, value);
        return this;
    }
    public ResourceContainerBuilder setMaxByteSize(String value) {
        jsonContent.put(ResourceContainer.MAX_BYTE_SIZE, value);
        return this;
    }
    public ResourceContainerBuilder setMaxInstanceAge(String value) {
        jsonContent.put(ResourceContainer.MAX_INSTANCE_AGE, value);
        return this;
    }
    public ResourceContainerBuilder setCurrNrInstances(String value) {
        jsonContent.put(ResourceContainer.CURR_NR_INSTANCES, value);
        return this;
    }
    public ResourceContainerBuilder setCurrByteSize(String value) {
        jsonContent.put(ResourceContainer.CURR_BYTE_SIZE, value);
        return this;
    }
    public ResourceContainerBuilder setLocationId(String value) {
        jsonContent.put(ResourceContainer.LOCATION_ID, value);
        return this;
    }
    public ResourceContainerBuilder setOntologyRef(String value) {
        jsonContent.put(ResourceContainer.ONTOLOGY_REF, value);
        return this;
    }
}
