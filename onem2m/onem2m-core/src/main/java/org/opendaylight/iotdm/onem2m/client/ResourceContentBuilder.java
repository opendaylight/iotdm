/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.client;

import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the common builder base class for all the resource types.
 */
public class ResourceContentBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceContentBuilder.class);
    protected JSONObject jsonContent;

    public ResourceContentBuilder() {
        jsonContent = new JSONObject();
    }

    public ResourceContentBuilder setResourceType(String value) {
        JsonUtils.put(jsonContent, ResourceContent.RESOURCE_TYPE, value);
        return this;
    }
    public ResourceContentBuilder setResourceId(String value) {
        JsonUtils.put(jsonContent, ResourceContent.RESOURCE_ID, value);
        return this;
    }
    public ResourceContentBuilder setResourceName(String value) {
        JsonUtils.put(jsonContent, ResourceContent.RESOURCE_NAME, value);
        return this;
    }
    public ResourceContentBuilder setParentId(String value) {
        JsonUtils.put(jsonContent, ResourceContent.PARENT_ID, value);
        return this;
    }
    public ResourceContentBuilder setCreationTime(String value) {
        JsonUtils.put(jsonContent, ResourceContent.CREATION_TIME, value);
        return this;
    }
    public ResourceContentBuilder setExpirationTime(String value) {
        JsonUtils.put(jsonContent, ResourceContent.EXPIRATION_TIME, value);
        return this;
    }
    public ResourceContentBuilder setLastModifiedTime(String value) {
        JsonUtils.put(jsonContent, ResourceContent.LAST_MODIFIED_TIME, value);
        return this;
    }
    public ResourceContentBuilder setLabels(String[] value) {
        JsonUtils.put(jsonContent, ResourceContent.LABELS, value);
        return this;
    }
    public ResourceContentBuilder setStateTag(String value) {
        JsonUtils.put(jsonContent, ResourceContent.STATE_TAG, value);
        return this;
    }
    public ResourceContentBuilder setPrimitiveContent(String value) {
        JSONObject jsonPC = null;
        try {
            jsonPC = new JSONObject(value);
        } catch (JSONException e) {
            LOG.error("ResourceContentBuilder: {}", e.toString());
            jsonPC = null;
        }
        // take the key/value from the json pc and add it to the existing json content
        if (jsonPC != null) {
            Iterator<?> keys = jsonPC.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                Object o = jsonPC.opt(key);
                JsonUtils.put(jsonContent, key, o);
            }
        }
        return this;
    }
}