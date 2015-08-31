/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.rest;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.Onem2mCoreProvider;
import org.opendaylight.iotdm.onem2m.core.database.DbAttr;
import org.opendaylight.iotdm.onem2m.core.database.DbAttrSet;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceSubscription;
import org.opendaylight.iotdm.onem2m.core.rest.utils.NotificationPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.ResourceChanged;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.ResourceChangedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SubscriptionDeleted;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SubscriptionDeletedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.attr.set.Member;
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

        JSONObject content = new JSONObject();

        Onem2mResource onem2mResource = onem2mRequest.getOnem2mResource();

        // cache the resourceContent so resultContent options can be restricted
        onem2mNotification.setResourceContent(onem2mRequest.getResourceContent());

        Onem2mResource subscriptionResource = onem2mNotification.getSubscriptionResource();
        DbAttr subAttrList =  onem2mNotification.getDbAttrs();
        DbAttrSet subAttrSetList =  onem2mNotification.getDbAttrSets();

        String nct = subAttrList.getAttr(ResourceSubscription.NOTIFICATION_CONTENT_TYPE);
        if (nct == null) {
            nct = Onem2m.NotificationContentType.WHOLE_RESOURCE;
        }
        switch (nct) {
            case Onem2m.NotificationContentType.MODIFIED_ATTRIBUTES:
                produceJsonContentWholeResource(onem2mRequest, onem2mResource, onem2mNotification, content);
                break;
            case Onem2m.NotificationContentType.WHOLE_RESOURCE:
                produceJsonContentWholeResource(onem2mRequest, onem2mResource, onem2mNotification, content);
                break;
            case Onem2m.NotificationContentType.REFERENCE_ONLY:
                produceJsonContentResourceReference(onem2mRequest, onem2mResource, onem2mNotification, content);
                break;
        }


        return content;
    }

    private static void produceJsonContentResourceReference(RequestPrimitive onem2mRequest,
                                                            Onem2mResource onem2mResource,
                                                            NotificationPrimitive onem2mNotification,
                                                            JSONObject j) {

        String h = Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource.getResourceId());

        j.put(ResourceContent.RESOURCE_NAME, h);
    }

    private static void produceJsonContentWholeResource(RequestPrimitive onem2mRequest,
                                                        Onem2mResource onem2mResource,
                                                        NotificationPrimitive onem2mNotification,
                                                        JSONObject j) {

        String resourceType = Onem2mDb.getInstance().getResourceType(onem2mResource);
        String id;

        id = Onem2mDb.getInstance().getNonHierarchicalNameForResource(onem2mResource.getParentId());
        if (id != null) {
            j.put(ResourceContent.PARENT_ID, id);
        }

        id = Onem2mDb.getInstance().getNonHierarchicalNameForResource(onem2mResource.getResourceId());
        j.put(ResourceContent.RESOURCE_ID, id);

        String name = Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource.getResourceId());
        j.put(ResourceContent.RESOURCE_NAME, name);

        ResourceContent.produceJsonForResource(resourceType, onem2mResource, j);
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

            DbAttr subAttrList =  new DbAttr(subscriptionResource.getAttr());
            onem2mNotification.setDbAttrs(subAttrList);

            DbAttrSet subAttrSetList =  new DbAttrSet(subscriptionResource.getAttrSet());
            onem2mNotification.setDbAttrSets(subAttrSetList);

            List<Member> uriList = subAttrSetList.getAttrSet(ResourceSubscription.NOTIFICATION_URI);
            for (Member uri : uriList) {
                onem2mNotification.setPrimitiveMany(NotificationPrimitive.URI, uri.getMember());
            }

            representation = produceJsonContent(onem2mRequest, onem2mNotification);

            operationMonitor.put(ORIGINATOR, onem2mRequest.getPrimitive(RequestPrimitive.FROM));
            operationMonitor.put(OPERATION, Integer.valueOf(opCode));
            notificationEvent.put(OPERATION_MONITOR, operationMonitor);
            notificationEvent.put(REPRESENTATION, representation);
            notification.put(NOTIFICATION_EVENT, notificationEvent);

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

        DbAttr subAttrList =  new DbAttr(onem2mResource.getAttr());

        String uri = subAttrList.getAttr(ResourceSubscription.SUBSCRIBER_URI);
        if (uri == null) {
            return;
        }
        NotificationPrimitive onem2mNotification = new NotificationPrimitive();

        onem2mNotification.setPrimitiveMany(NotificationPrimitive.URI, subAttrList.getAttr(ResourceSubscription.SUBSCRIBER_URI));

        JSONObject notification = new JSONObject();

        String name = Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource.getResourceId());

        notification.put(SUBSCRIPTION_DELETION, true);
        notification.put(SUBSCRIPTION_REFERENCE, name);
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
        String resourceType = Onem2mDb.getInstance().getResourceType(onem2mResource);
        if (resourceType.contentEquals(Onem2m.ResourceType.SUBSCRIPTION)) {
            handleDeleteSubscription(onem2mRequest, onem2mResource);
        } else {
            handleOperation(onem2mRequest, Onem2m.Operation.DELETE);
        }
    }
}
