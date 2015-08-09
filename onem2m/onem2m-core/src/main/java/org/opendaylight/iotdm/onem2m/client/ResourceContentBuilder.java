/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.client;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the common builder base class for all the resource types.
 */
public class ResourceContentBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceContentBuilder.class);
    protected JSONObject jsonContent;
    protected boolean useAnySyntax;

    public ResourceContentBuilder() {
        jsonContent = new JSONObject();
        useAnySyntax = false;
    }

    public ResourceContentBuilder setResourceType(String value) {
        jsonContent.put(ResourceContent.RESOURCE_TYPE, value);
        return this;
    }
    public ResourceContentBuilder setResourceId(String value) {
        jsonContent.put(ResourceContent.RESOURCE_ID, value);
        return this;
    }
    public ResourceContentBuilder setResourceName(String value) {
        jsonContent.put(ResourceContent.RESOURCE_NAME, value);
        return this;
    }
    public ResourceContentBuilder setParentId(String value) {
        jsonContent.put(ResourceContent.PARENT_ID, value);
        return this;
    }
    public ResourceContentBuilder setCreationTime(String value) {
        jsonContent.put(ResourceContent.CREATION_TIME, value);
        return this;
    }
    public ResourceContentBuilder setLastModifiedTime(String value) {
        jsonContent.put(ResourceContent.LAST_MODIFIED_TIME, value);
        return this;
    }
    public ResourceContentBuilder setLabels(String[] value) {
        jsonContent.put(ResourceContent.LABELS, value);
        return this;
    }
    public ResourceContentBuilder setStateTag(String value) {
        jsonContent.put(ResourceContent.STATE_TAG, value);
        return this;
    }
    public void setUseAnySyntax(boolean useAny) {
        this.useAnySyntax = useAny;
    }
    public String build() {
        if (useAnySyntax) {
            JSONObject j = new JSONObject();
            JSONArray anyArray = new JSONArray();
            anyArray.put(jsonContent);
            j.put("any", anyArray);
            return (j.toString());
        } else {
            return (jsonContent.toString());
        }
    }
}