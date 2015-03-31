/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.client;

import org.opendaylight.iotdm.onem2m.core.resource.ResourceSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceSubscriptionBuilder extends ResourceContentBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceSubscriptionBuilder.class);
    /**
     * The onem2m-protocols use this to create a new RequestPrimitive class
     */
    public ResourceSubscriptionBuilder() {
        super();
    }
    public ResourceSubscriptionBuilder setExpirationCounter(String value) {
        jsonContent.put(ResourceSubscription.EXPIRATION_COUNTER, value);
        return this;
    }
    public ResourceSubscriptionBuilder setNotificationUri(String value) {
        jsonContent.put(ResourceSubscription.NOTIFICATION_URI, value);
        return this;
    }
    public ResourceSubscriptionBuilder setBatchNotify(String value) {
        jsonContent.put(ResourceSubscription.BATCH_NOTIFY, value);
        return this;
    }
    public ResourceSubscriptionBuilder setRateLimit(String value) {
        jsonContent.put(ResourceSubscription.RATE_LIMIT, value);
        return this;
    }
    public ResourceSubscriptionBuilder setPreSubscriptionNotify(String value) {
        jsonContent.put(ResourceSubscription.PRE_SUBSCRIPTION_NOTIFY, value);
        return this;
    }
    public ResourceSubscriptionBuilder setPendingNotification(String value) {
        jsonContent.put(ResourceSubscription.PENDING_NOTIFICATION, value);
        return this;
    }
    public ResourceSubscriptionBuilder setNotificationStoragePolicy(String value) {
        jsonContent.put(ResourceSubscription.NOTIFICATION_STORAGE_POLICY, value);
        return this;
    }
    public ResourceSubscriptionBuilder setLatestNotify(String value) {
        jsonContent.put(ResourceSubscription.LATEST_NOTIFY, value);
        return this;
    }
    public ResourceSubscriptionBuilder setNotificationContentType(String value) {
        jsonContent.put(ResourceSubscription.NOTIFICATION_CONTENT_TYPE, value);
        return this;
    }
    public ResourceSubscriptionBuilder setNotificationEventCat(String value) {
        jsonContent.put(ResourceSubscription.NOTIFICATION_EVENT_CAT, value);
        return this;
    }
    public ResourceSubscriptionBuilder setCreator(String value) {
        jsonContent.put(ResourceSubscription.CREATOR, value);
        return this;
    }
    public ResourceSubscriptionBuilder setSubscriberUri(String value) {
        jsonContent.put(ResourceSubscription.SUBSCRIBTER_URI, value);
        return this;
    }
}
