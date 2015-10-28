/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.client;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceAE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceAEBuilder extends ResourceContentBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceAEBuilder.class);


    public ResourceAEBuilder() {
        super();
    }
    public ResourceAEBuilder setAppName(String value) {
        jsonContent.put(ResourceAE.APP_NAME, value);
        return this;
    }
    public ResourceAEBuilder setRequestReachability(Boolean value) {
        jsonContent.put(ResourceAE.REQUEST_REACHABILITY, value);
        return this;
    }
    public ResourceAEBuilder setAppId(String value) {
        jsonContent.put(ResourceAE.APP_ID, value);
        return this;
    }
    public ResourceAEBuilder setAEId(String value) {
        jsonContent.put(ResourceAE.AE_ID, value);
        return this;
    }
    public ResourceAEBuilder setOntologyRef(String value) {
        jsonContent.put(ResourceAE.ONTOLOGY_REF, value);
        return this;
    }
    public String build() {
        JSONObject j = new JSONObject();
        j.put("m2m:" + Onem2m.ResourceTypeString.AE, jsonContent);
        return (j.toString());
    }
}
