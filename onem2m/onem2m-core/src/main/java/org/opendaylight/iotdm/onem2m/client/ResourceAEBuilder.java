/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.client;

import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceAE;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceAEBuilder extends ResourceContentBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceAEBuilder.class);


    public ResourceAEBuilder() {
        super();
    }
    public ResourceAEBuilder setAppName(String value) {
        JsonUtils.put(jsonContent, ResourceAE.APP_NAME, value);
        return this;
    }
    public ResourceAEBuilder setRequestReachability(Boolean value) {
        JsonUtils.put(jsonContent, ResourceAE.REQUEST_REACHABILITY, value);
        return this;
    }
    public ResourceAEBuilder setAppId(String value) {
        JsonUtils.put(jsonContent, ResourceAE.APP_ID, value);
        return this;
    }
    public ResourceAEBuilder setAEId(String value) {
        JsonUtils.put(jsonContent, ResourceAE.AE_ID, value);
        return this;
    }
    public ResourceAEBuilder setOntologyRef(String value) {
        JsonUtils.put(jsonContent, ResourceAE.ONTOLOGY_REF, value);
        return this;
    }
    public String build() {
        return JsonUtils.put(new JSONObject(), "m2m:" + Onem2m.ResourceTypeString.AE, jsonContent).toString();
    }
}
