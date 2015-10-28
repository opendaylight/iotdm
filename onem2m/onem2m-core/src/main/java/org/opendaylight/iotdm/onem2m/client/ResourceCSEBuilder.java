/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.client;

import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceCse;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceCSEBuilder extends ResourceContentBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceCSEBuilder.class);


    public ResourceCSEBuilder() {
        super();
    }
    public ResourceCSEBuilder setCseId(String value) {
        jsonContent.put(ResourceCse.CSE_ID, value);
        return this;
    }
    public ResourceCSEBuilder setCseType(String value) {
        jsonContent.put(ResourceCse.CSE_TYPE, value);
        return this;
    }
    public String build() {
        JSONObject j = new JSONObject();
        j.put("m2m:" + Onem2m.ResourceTypeString.CSE_BASE, jsonContent);
        return (j.toString());
    }
}
