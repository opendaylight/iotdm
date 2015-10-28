/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.client;

import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContentInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceContentInstanceBuilder extends ResourceContentBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceContentInstanceBuilder.class);

    public ResourceContentInstanceBuilder() {
        super();
    }
    public ResourceContentInstanceBuilder setContent(String value) {
        jsonContent.put(ResourceContentInstance.CONTENT, value);
        return this;
    }
    public ResourceContentInstanceBuilder setContentInfo(String value) {
        jsonContent.put(ResourceContentInstance.CONTENT_INFO, value);
        return this;
    }
    public ResourceContentInstanceBuilder setOntologyRef(String value) {
        jsonContent.put(ResourceContentInstance.ONTOLOGY_REF, value);
        return this;
    }
    public String build() {
        JSONObject j = new JSONObject();
        j.put("m2m:" + Onem2m.ResourceTypeString.CONTENT_INSTANCE, jsonContent);
        return (j.toString());
    }
}
