/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.client;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceSubscription;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceSubscriptionBuilder extends ResourceContentBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceSubscriptionBuilder.class);
    /**
     * The onem2m-protocols use this to create a new RequestPrimitive class
     */

    private JSONArray nUriArray;
    public ResourceSubscriptionBuilder() {
        super();
        nUriArray = new JSONArray();
    }
    public ResourceSubscriptionBuilder setNotificationUri(String value) {
        nUriArray.put(value);
        return this;
    }
    public ResourceSubscriptionBuilder setNotificationContentType(String value) {
        JsonUtils.put(jsonContent, ResourceSubscription.NOTIFICATION_CONTENT_TYPE, value);
        return this;
    }
    public ResourceSubscriptionBuilder setNotificationEventCat(String value) {
        JsonUtils.put(jsonContent, ResourceSubscription.NOTIFICATION_EVENT_CAT, value);
        return this;
    }
    public String build() {
        JsonUtils.put(jsonContent, ResourceSubscription.NOTIFICATION_URI, nUriArray);
        return JsonUtils.put(new JSONObject(), "m2m:" + Onem2m.ResourceTypeString.SUBSCRIPTION, jsonContent).toString();
    }
}
