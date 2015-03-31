/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;


import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.DbAttr;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceSubscription {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceSubscription.class);
    private ResourceSubscription() {}

    // taken from CDT-contentInstance-v1_0_0.xsd / TS0004_v_1-0_1 Section 8.2.2 Short Names
    // TODO: ts0001 9.6.7-1

    public static final String EXPIRATION_COUNTER = "exc";
    public static final String NOTIFICATION_URI = "nu";
    public static final String BATCH_NOTIFY = "bn";
    public static final String RATE_LIMIT = "rl";
    public static final String PRE_SUBSCRIPTION_NOTIFY = "psn";
    public static final String PENDING_NOTIFICATION = "pn";
    public static final String NOTIFICATION_STORAGE_POLICY = "nsp";
    public static final String LATEST_NOTIFY = "ln";
    public static final String NOTIFICATION_CONTENT_TYPE = "nct";
    public static final String NOTIFICATION_EVENT_CAT = "nec";
    public static final String CREATOR = "cr";
    public static final String SUBSCRIBTER_URI = "su";

    // TODO: need to add a section or new file to handle event notification criteria

    // hard code set of acceptable create attributes, short and long name
    public static final Set<String> createAttributes = new HashSet<String>() {{
        // short; long
        add(ResourceContent.EXPIRATION_TIME); add("expirationTime");
        add(ResourceContent.CREATION_TIME); add("creationTime");
        add(ResourceContent.LABELS); add("labels");
        add(EXPIRATION_COUNTER); add("expirationCounter");
        add(NOTIFICATION_URI); add("notificationURI");
        add(BATCH_NOTIFY); add("batchNotify");
        add(RATE_LIMIT); add("rateLimit");
        add(PRE_SUBSCRIPTION_NOTIFY); add("preSubscriptionNotify");
        add(PENDING_NOTIFICATION); add("pendingNotification");
        add(NOTIFICATION_STORAGE_POLICY); add("notificationStoragePolicy");
        add(LATEST_NOTIFY); add("latestNotify");
        add(NOTIFICATION_CONTENT_TYPE); add("notificationContentType");
        add(NOTIFICATION_EVENT_CAT); add("notificationEventCat");
        add(CREATOR); add("creator");
        add(SUBSCRIBTER_URI); add("subscriberURI");
    }};

    // hard code set of acceptable retrieve attributes, short and long name
    public static final Set<String> retrieveAttributes = new HashSet<String>() {{
        // short; long
        add(ResourceContent.RESOURCE_TYPE); add("resourceType");
        add(ResourceContent.RESOURCE_ID); add("resourceID");
        add(ResourceContent.RESOURCE_NAME); add("resourceName");
        add(ResourceContent.PARENT_ID); add("parentID");
        add(ResourceContent.EXPIRATION_TIME); add("expirationTime");
        add(ResourceContent.CREATION_TIME); add("creationTime");
        add(ResourceContent.LAST_MODIFIED_TIME); add("lastModifiedTime");
        add(ResourceContent.LABELS); add("labels");
        add(EXPIRATION_COUNTER); add("expirationCounter");
        add(NOTIFICATION_URI); add("notificationURI");
        add(BATCH_NOTIFY); add("batchNotify");
        add(RATE_LIMIT); add("rateLimit");
        add(PRE_SUBSCRIPTION_NOTIFY); add("preSubscriptionNotify");
        add(PENDING_NOTIFICATION); add("pendingNotification");
        add(NOTIFICATION_STORAGE_POLICY); add("notificationStoragePolicy");
        add(LATEST_NOTIFY); add("latestNotify");
        add(NOTIFICATION_CONTENT_TYPE); add("notificationContentType");
        add(NOTIFICATION_EVENT_CAT); add("notificationEventCat");
        add(CREATOR); add("creator");
        add(SUBSCRIBTER_URI); add("subscriberURI");
    }
    };

    /**
     * The list<Attr> and List<AttrSet> must be filled in with the ContentPrimitive attributes
     * @param onem2mRequest
     * @param onem2mResponse
     */
    public static void handleCreate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        String tempStr;
        Integer tempInt;
        /**
         * When the parentURI was located in the tree, the attr list was read, this puts it into a class
         * so that each attribute can be easily accessed
         */
        DbAttr parentDbAttrs = onem2mRequest.getDbAttrs();

        /**
         * The only resource type that can be the parent according to TS0001 9.6.1.1-1 is a cseBase
         */
        String rt = parentDbAttrs.getAttr(ResourceContent.RESOURCE_TYPE);
        if (rt == null || !(rt.contentEquals(Onem2m.ResourceType.CSE_BASE) ||
                            rt.contentEquals(Onem2m.ResourceType.CONTAINER) ||
                            rt.contentEquals(Onem2m.ResourceType.AE))) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                    "Cannot create Subscription under this resource type: " + rt);
            return;
        }

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        // TODO: this is a list, so it should be in AttrSet
        tempStr = resourceContent.getDbAttr(NOTIFICATION_URI);
        if (tempStr == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "NOTIFICATION_URI missing parameter");
            return;
        }
    }
}