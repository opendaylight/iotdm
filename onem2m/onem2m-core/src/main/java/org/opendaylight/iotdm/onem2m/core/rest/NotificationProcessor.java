/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.ResourceChanged;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.ResourceChangedBuilder;
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

    private static final Logger LOG = LoggerFactory.getLogger(NotificationProcessor.class);

    private NotificationProcessor() {}

    /**
     * This routine
     *
     * @param onem2mRequest the set of request primitives
     * @param onem2mNotification the set of notification primitives
     */
    private static void produceJsonNotification(RequestPrimitive onem2mRequest, NotificationPrimitive onem2mNotification) {

        JSONObject jContent = new JSONObject();

        Onem2mResource onem2mResource = onem2mRequest.getOnem2mResource();

        // cache the resourceContent so resultContent options can be restricted
        onem2mNotification.setResourceContent(onem2mRequest.getResourceContent());

        Onem2mResource subscriptionResource = onem2mNotification.getSubscriptionResource();
        DbAttr subAttrList =  onem2mNotification.getDbAttrs();
        DbAttrSet subAttrSetList =  onem2mNotification.getDbAttrSets();

        String nct = subAttrList.getAttr(ResourceSubscription.NOTIFICATION_CONTENT_TYPE);
        if (nct == null) {
        } else {
            switch (nct) {
                case Onem2m.NotificationContentType.MODIFIED_ATTRIBUTES:
                    break;
                case Onem2m.NotificationContentType.WHOLE_RESOURCE:
                    break;
                case Onem2m.NotificationContentType.REFERENCE_ONLY:
                    break;
            }
        }

        onem2mNotification.setPrimitive(NotificationPrimitive.RESOURCE_REPRESENTATION, jContent.toString());
    }


    /**
     * The results of the create now must be put in the response.  The result content is used to decide how the
     * results should be formatted.
     *
     * @param onem2mRequest request
     */
    public static void handleCreate(RequestPrimitive onem2mRequest) {
        List<String> subscriptionResourceIdList = Onem2mDb.getInstance().findSubscriptionResources(onem2mRequest);
        if (subscriptionResourceIdList.size() == 0) {
            return;
        }

        for (String subscriptionResourceId : subscriptionResourceIdList) {

            NotificationPrimitive onem2mNotification = new NotificationPrimitive();
            onem2mNotification.setPrimitive(NotificationPrimitive.OPERATION, Onem2m.Operation.CREATE);
            onem2mNotification.setPrimitive(NotificationPrimitive.ORIGINATOR,
                    onem2mRequest.getPrimitive(RequestPrimitive.FROM));
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

            produceJsonNotification(onem2mRequest, onem2mNotification);

            // copy the URI's to the notification,
            ResourceChanged rc = new ResourceChangedBuilder()
                    .setOnem2mPrimitive(onem2mNotification.getPrimitivesList())
                    .build();

            // now that we have a NotificationPrimitive, we need to send it to the Notifier
            Onem2mCoreProvider.getNotifier().publish(rc);
        }
    }

    /**
     * The results of the create now must be put in the response.  The result content is used to decide how the
     * results should be formatted.
     *
     * @param onem2mRequest request
     */
    public static void handleUpdate(RequestPrimitive onem2mRequest) {
        List<String> subscriptionResourceIdList = Onem2mDb.getInstance().findSubscriptionResources(onem2mRequest);
        if (subscriptionResourceIdList.size() == 0) {
            return;
        }

        for (String subscriptionResourceId : subscriptionResourceIdList) {

            NotificationPrimitive onem2mNotification = new NotificationPrimitive();
            onem2mNotification.setPrimitive(NotificationPrimitive.OPERATION, Onem2m.Operation.UPDATE);
            onem2mNotification.setPrimitive(NotificationPrimitive.ORIGINATOR,
                    onem2mRequest.getPrimitive(RequestPrimitive.FROM));
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

            produceJsonNotification(onem2mRequest, onem2mNotification);

            // copy the URI's to the notification,
            ResourceChanged rc = new ResourceChangedBuilder()
                    .setOnem2mPrimitive(onem2mNotification.getPrimitivesList())
                    .build();

            // now that we have a NotificationPrimitive, we need to send it to the Notifier
            Onem2mCoreProvider.getNotifier().publish(rc);
        }
    }

    /**
     * The results of the delete now must be put in a notification if there exists active subscriptions.
     *
     * @param onem2mRequest request
     */
    public static void handleDelete(RequestPrimitive onem2mRequest) {

        List<String> subscriptionResourceIdList = Onem2mDb.getInstance().findSubscriptionResources(onem2mRequest);
        if (subscriptionResourceIdList.size() == 0) {
            return;
        }

        for (String subscriptionResourceId : subscriptionResourceIdList) {

            NotificationPrimitive onem2mNotification = new NotificationPrimitive();
            onem2mNotification.setPrimitive(NotificationPrimitive.OPERATION, Onem2m.Operation.DELETE);
            onem2mNotification.setPrimitive(NotificationPrimitive.ORIGINATOR,
                    onem2mRequest.getPrimitive(RequestPrimitive.FROM));
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

            produceJsonNotification(onem2mRequest, onem2mNotification);

            // copy the URI's to the notification,
            ResourceChanged rc = new ResourceChangedBuilder()
                    .setOnem2mPrimitive(onem2mNotification.getPrimitivesList())
                    .build();

            // now that we have a NotificationPrimitive, we need to send it to the Notifier
            Onem2mCoreProvider.getNotifier().publish(rc);
        }
    }
}
