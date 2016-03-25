/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.rest;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.Onem2mCoreProvider;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceSubscription;
import org.opendaylight.iotdm.onem2m.core.rest.utils.NotificationPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.ResourceChanged;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.ResourceChangedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SubscriptionDeleted;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SubscriptionDeletedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The NotificationProcessor handles each of the operations that affect resources, ie C,U,D.  It finds the
 * appropriate subscription resources associated with the affected resource.  A message payload is formatted, and
 * for each subscription (each has policies), it send a message to the Notifier who in turns uses the set of
 * notificationURI's to send message to the endpoints.
 */
public class NotificationProcessor {

    public static final String NOTIFICATION_EVENT = "nev";
    public static final String REPRESENTATION = "rep";
    public static final String OPERATION_MONITOR = "om";
    public static final String OPERATION = "op";
    public static final String ORIGINATOR = "or";
    public static final String SUBSCRIPTION_DELETION = "sud";
    public static final String SUBSCRIPTION_REFERENCE = "sur";

    private static final Logger LOG = LoggerFactory.getLogger(NotificationProcessor.class);

    private NotificationProcessor() {}

    /**
     * This routine looks at the notification content type and build the json representation based on its setting.
     *
     * @param onem2mRequest the set of request primitives
     * @param onem2mNotification the set of notification primitives
     */
    private static JSONObject produceJsonContent(RequestPrimitive onem2mRequest, NotificationPrimitive onem2mNotification) {

        JSONObject content = null;

        Onem2mResource onem2mResource = onem2mRequest.getOnem2mResource();



        String nct = onem2mNotification.getJsonSubscriptionResourceContent()
                .optString(ResourceSubscription.NOTIFICATION_CONTENT_TYPE, null);
        if (nct == null) {
            nct = Onem2m.NotificationContentType.WHOLE_RESOURCE;
        }
        switch (nct) {
            case Onem2m.NotificationContentType.MODIFIED_ATTRIBUTES:
                // cache the resourceContent so input json keys are known for modified attrs
                // onem2mNotification.setResourceContent(onem2mRequest.getResourceContent());
                // TODO: support modified ... needs a little more effort
                content = produceJsonContentWholeResource(onem2mRequest, onem2mResource);
                break;
            case Onem2m.NotificationContentType.WHOLE_RESOURCE:
                content = produceJsonContentWholeResource(onem2mRequest, onem2mResource);
                break;
            case Onem2m.NotificationContentType.REFERENCE_ONLY:
                content = produceJsonContentResourceReference(onem2mResource);
                break;
        }

        return content;
    }

    private static JSONObject produceJsonContentResourceReference(Onem2mResource onem2mResource) {
        return JsonUtils.put(new JSONObject(), ResourceContent.RESOURCE_NAME,
                Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource.getResourceId()));
    }

    private static JSONObject produceJsonContentWholeResource(RequestPrimitive onem2mRequest,
                                                        Onem2mResource onem2mResource) {

        JSONObject j = new JSONObject();

        String resourceType = onem2mResource.getResourceType();

        String name = Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource.getResourceId());
        JsonUtils.put(onem2mRequest.getJsonResourceContent(), ResourceContent.RESOURCE_NAME, name);

        String m2mPrefixString = Onem2m.USE_M2M_PREFIX ? "m2m:" : "";

        try {
            JSONObject wholeresource = new JSONObject(onem2mResource.getResourceContentJsonString());
            return JsonUtils.put(j, m2mPrefixString + Onem2m.resourceTypeToString.get(resourceType), wholeresource);
        } catch (JSONException e) {
            LOG.error("Invalid JSON {}", onem2mResource.getResourceContentJsonString(), e);
            throw new IllegalArgumentException("Invalid JSON", e);
        }
    }

    /**
     * Process the notification formatting based on the operation and content type desired and put it in the
     * proper JSON format.  Find all subscriptions for this resource, then based on each of their respective
     * polices, process the notification.  Then finally send the notification to the Notifier so that it can
     * in turn process each of the URI's and forward them to the appropriate NotifierPlugin.  Example:
     * if a URI has http://localhost, then the http plugin will be notified.
     *
     * @param onem2mRequest request
     * @param opCode CRUD operation
     */
    public static void handleOperation(RequestPrimitive onem2mRequest, String opCode) {

        List<String> subscriptionResourceIdList = Onem2mDb.getInstance().findSubscriptionResources(onem2mRequest);
        if (subscriptionResourceIdList.size() == 0) {
            return;
        }

        for (String subscriptionResourceId : subscriptionResourceIdList) {

            JSONObject notification = new JSONObject();
            JSONObject notificationEvent = new JSONObject();
            JSONObject representation;
            JSONObject operationMonitor = new JSONObject();

            NotificationPrimitive onem2mNotification = new NotificationPrimitive();

            Onem2mResource subscriptionResource = Onem2mDb.getInstance().getResource(subscriptionResourceId);
            onem2mNotification.setSubscriptionResource(subscriptionResource);
            onem2mNotification.setJsonSubscriptionResourceContent(subscriptionResource.getResourceContentJsonString());

            JSONArray uriArray = onem2mNotification.getJsonSubscriptionResourceContent()
                    .optJSONArray(ResourceSubscription.NOTIFICATION_URI);
            for (int i = 0; i < uriArray.length(); i++) {
                onem2mNotification.setPrimitiveMany(NotificationPrimitive.URI, uriArray.optString(i));
            }

            representation = produceJsonContent(onem2mRequest, onem2mNotification);

            JsonUtils.put(operationMonitor, ORIGINATOR, onem2mRequest.getPrimitive(RequestPrimitive.FROM));
            JsonUtils.put(operationMonitor, OPERATION, Integer.valueOf(opCode));
            JsonUtils.put(notificationEvent, OPERATION_MONITOR, operationMonitor);
            JsonUtils.put(notificationEvent, REPRESENTATION, representation);
            JsonUtils.put(notification, NOTIFICATION_EVENT, notificationEvent);

            onem2mNotification.setPrimitive(NotificationPrimitive.CONTENT, notification.toString());

            // copy the URI's to the notification,
            ResourceChanged rc = new ResourceChangedBuilder()
                    .setOnem2mPrimitive(onem2mNotification.getPrimitivesList())
                    .build();

            // now that we have a NotificationPrimitive, we need to send it to the Notifier
            Onem2mCoreProvider.getNotifier().publish(rc);
        }
    }

    public static void handleCreate(RequestPrimitive onem2mRequest) {
        handleOperation(onem2mRequest, Onem2m.Operation.CREATE);
    }

    public static void handleUpdate(RequestPrimitive onem2mRequest) {
        handleOperation(onem2mRequest, Onem2m.Operation.UPDATE);
    }

    /**
     * Notify of a subscription removal.
     *
     * @param onem2mRequest request
     * @param onem2mResource CRUD operation
     */
    public static void handleDeleteSubscription(RequestPrimitive onem2mRequest, Onem2mResource onem2mResource) {

        String uri = onem2mRequest.getJsonResourceContent().optString(ResourceSubscription.SUBSCRIBER_URI, null);
        if (uri == null) {
            return;
        }
        NotificationPrimitive onem2mNotification = new NotificationPrimitive();

        onem2mNotification.setPrimitiveMany(NotificationPrimitive.URI, uri);

        JSONObject notification = new JSONObject();

        String name = Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource.getResourceId());

        JsonUtils.put(notification, SUBSCRIPTION_DELETION, true);
        JsonUtils.put(notification, SUBSCRIPTION_REFERENCE, name);
        onem2mNotification.setPrimitive(NotificationPrimitive.CONTENT, notification.toString());

        SubscriptionDeleted sd = new SubscriptionDeletedBuilder()
                    .setOnem2mPrimitive(onem2mNotification.getPrimitivesList())
                    .build();

        // now that we have a NotificationPrimitive, we need to send it to the Notifier
        Onem2mCoreProvider.getNotifier().publish(sd);
    }

    /**
     * The results of the delete now must be put in a notification if there exists active subscriptions.
     *
     * @param onem2mRequest request
     */
    public static void handleDelete(RequestPrimitive onem2mRequest) {

        Onem2mResource onem2mResource = onem2mRequest.getOnem2mResource();
        String resourceType = onem2mResource.getResourceType();
        if (resourceType.contentEquals(Onem2m.ResourceType.SUBSCRIPTION)) {
            handleDeleteSubscription(onem2mRequest, onem2mResource);
        } else {
            handleOperation(onem2mRequest, Onem2m.Operation.DELETE);
        }
    }
}
