/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.rest;

import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.client.Onem2mContentInstanceRequestBuilder;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.Onem2mCoreProvider;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContainer;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContentInstance;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceSubscription;
import org.opendaylight.iotdm.onem2m.core.rest.utils.NotificationPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.Onem2mDateTime;
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

    public static final String VERIFICATION_REQUEST = "vrq";
    public static final String SUBSCRIPTION_DELETION = "sud";
    public static final String SUBSCRIPTION_REFERENCE = "sur";
    public static final String CREATOR = "cr";
    public static final String SUBSCRIPTION_FORWADING_URI = "nfu";



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
            case Onem2m.NotificationContentType.RESOURCE_ID:
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

    /**
     * A. Default one
     * Update to attributes of the subscribed-to resource
     * @param onem2mRequest onem2mrequest
     */
    public static void handleEventTypeA(RequestPrimitive onem2mRequest) {
        String eventType = Onem2m.EventType.UPDATE_RESOURCE;
        String selfResourceID = onem2mRequest.getOnem2mResource().getResourceId();
        List<String> subscriptionResourceIdList = Onem2mDb.getInstance().findSelfSubscriptionID(selfResourceID, eventType);
        if (subscriptionResourceIdList.size() == 0) {
            return;
        }
        sendNotificationAccordingToType(onem2mRequest, subscriptionResourceIdList, eventType);
    }

    /**
     * B. Deletion of the  subscribed-to resource
     * @param onem2mRequest onem2mrequest
     */
    public static void handleEventTypeB(RequestPrimitive onem2mRequest) {
        String eventType = "2";
        String selfResourceID = onem2mRequest.getOnem2mResource().getResourceId();
        List<String> subscriptionResourceIdList = Onem2mDb.getInstance().findSelfSubscriptionID(selfResourceID, eventType);
        if (subscriptionResourceIdList.size() == 0) {
            return;
        }
        sendNotificationAccordingToType(onem2mRequest, subscriptionResourceIdList, eventType);
    }

    /**
     * C. Creation of a direct child of the subscribed-to resource
     * @param onem2mRequest onem2mrequest
     */
    public static void handleEventTypeC(RequestPrimitive onem2mRequest) {
        String eventType = "3";
        String selfResourceID = onem2mRequest.getOnem2mResource().getResourceId();
        List<String> subscriptionResourceIdList = Onem2mDb.getInstance().finddirectParentSubscriptionID(selfResourceID, eventType);
        if (subscriptionResourceIdList.size() == 0) {
            return;
        }
        sendNotificationAccordingToType(onem2mRequest, subscriptionResourceIdList, eventType);
    }

    /**
     * D. Deletion of a direct child of the subscribed-to resource
     * @param onem2mRequest onem2mrequest
     */
    public static void handleEventTypeD(RequestPrimitive onem2mRequest) {
        String eventType = "4";
        String selfResourceID = onem2mRequest.getOnem2mResource().getResourceId();
        List<String> subscriptionResourceIdList = Onem2mDb.getInstance().finddirectParentSubscriptionID(selfResourceID, eventType);
        if (subscriptionResourceIdList.size() == 0) {
            return;
        }
        sendNotificationAccordingToType(onem2mRequest, subscriptionResourceIdList, eventType);
    }

    /**
     * E. retrieve attempt of non-existing child
     * @param onem2mRequest onem2mrequest
     */
    public static void handleEventTypeE(RequestPrimitive onem2mRequest, List<String> subscriptionResourceIdList) {
        String eventType = Onem2m.EventType.RETRIEVE_NECHILD;
        sendNotificationAccordingToType(onem2mRequest, subscriptionResourceIdList, eventType);
    }

    /**
     * F. update of any descendents of the subscribed-to resource
     * this method could be inside each of certain operation
     * @param onem2mRequest onem2mrequest
     */

    public static void handleEvnetTypeF(RequestPrimitive onem2mRequest) {
        String selfResourceID = onem2mRequest.getOnem2mResource().getResourceId();
        List<String> subscriptionResourceIdList = Onem2mDb.getInstance().findAllAncestorsSubscriptionID(selfResourceID);
        if (subscriptionResourceIdList.size() == 0) {
            return;
        }

        sendNotificationAccordingToType(onem2mRequest, subscriptionResourceIdList, "6");
    }

    private static void sendNotificationAccordingToType(RequestPrimitive onem2mRequest, List<String> subscriptionResourceIdList, String type) {

        for (String subscriptionResourceId : subscriptionResourceIdList) {

            JSONObject notification = new JSONObject();
            JSONObject notificationEvent = new JSONObject();
            JSONObject representation;
            JSONObject operationMonitor = new JSONObject();

            NotificationPrimitive onem2mNotification = new NotificationPrimitive();

            Onem2mResource subscriptionResource = Onem2mDb.getInstance().getResource(subscriptionResourceId);
            onem2mNotification.setSubscriptionResource(subscriptionResource);
            onem2mNotification.setJsonSubscriptionResourceContent(subscriptionResource.getResourceContentJsonString());

            JSONObject subsJsonObject;
            try {
                subsJsonObject = new JSONObject(subscriptionResource.getResourceContentJsonString());
            } catch (JSONException e) {
                LOG.error("Invalid JSON {}", subscriptionResource.getResourceContentJsonString(), e);
                throw new IllegalArgumentException("Invalid JSON", e);
            }

            JSONObject changedResourceJsonObject;
            try {
                changedResourceJsonObject = new JSONObject(onem2mRequest.getOnem2mResource().getResourceContentJsonString());
            } catch (JSONException e) {
            LOG.error("Invalid JSON {}", onem2mRequest.getOnem2mResource().getResourceContentJsonString(), e);
            throw new IllegalArgumentException("Invalid JSON", e);
        }
            // todo: changedResource correct?

            /* Step 1.0	Check the eventNotificationCriteria attribute of the <subscription> resource associated with the modified resource:
            •	 If the eventNotificationCriteria attribute is set, then the Originator shall check whether the corresponding
            event matches with the event criteria. If notificationEventType is not set within the eventNotificationCriteria attribute,
            the Originator shall use the default setting of Update_of_Resource to compare against the event. If the event matches,
            go to the step 2.0. Otherwise, the Originator shall discard the corresponding event.
            •	 If the eventNotificationCriteria attribute is not configured, the Originator shall use the default setting of
                Update_of_Resource for notificationEventType and then continue with the step 2.0.

            */


            Boolean sendThisNotification = false;
            JSONObject eventnotificationcriteria = onem2mNotification.getJsonSubscriptionResourceContent().
                    optJSONObject(ResourceSubscription.EVENT_NOTIFICATION_CRITERIA);
            if (eventnotificationcriteria != null) {
                Iterator<?> encKeys = eventnotificationcriteria.keys();
                while (encKeys.hasNext()) {
                    String encKey = (String) encKeys.next();
                    Object j = eventnotificationcriteria.opt(encKey);
                    switch (encKey) {
                        case ResourceSubscription.CREATED_BEFORE:
                            String crb = (String) j;
                            String ct = changedResourceJsonObject.optString(ResourceContent.CREATION_TIME);
                            if (ct != null && Onem2mDateTime.dateCompare(ct, crb) < 0) {
                                sendThisNotification = true;
                            }
                            break;
                        case ResourceSubscription.CREATED_AFTER:
                            String cra = (String) j;
                            String ct2 = changedResourceJsonObject.optString(ResourceContent.CREATION_TIME);
                            if (ct2 != null && Onem2mDateTime.dateCompare(ct2, cra) < 0) {
                                sendThisNotification = true;
                            }
                            break;
                        case ResourceSubscription.MODIFIED_SINCE:
                            String ms = (String) j;
                            String mt = changedResourceJsonObject.optString(ResourceContent.LAST_MODIFIED_TIME);
                            if (mt != null && Onem2mDateTime.dateCompare(mt, ms) > 0) {
                                sendThisNotification = true;
                            }
                            break;
                        case ResourceSubscription.UNMODIFIED_SINCE:
                            String ums = (String) j;
                            String mt2 = changedResourceJsonObject.optString(ResourceContent.LAST_MODIFIED_TIME);
                            if (mt2 != null && Onem2mDateTime.dateCompare(mt2, ums) < 0) {
                                sendThisNotification = true;
                            }
                            break;
                        case ResourceSubscription.STATE_TAG_BIGGER:
                            String stb = (String) j;
                            Integer st = changedResourceJsonObject.optInt(ResourceContent.STATE_TAG, -1);
                            if (st != -1) {
                                if (st > Integer.valueOf(stb))
                                    sendThisNotification = true;
                            }
                            break;
                        case ResourceSubscription.STATE_TAG_SMALLER:
                            String sts = (String) j;
                            Integer st2 = changedResourceJsonObject.optInt(ResourceContent.STATE_TAG, -1);
                            if (st2 != -1) {
                                if (st2 < Integer.valueOf(sts))
                                    sendThisNotification = true;
                            }
                            break;
                        case ResourceSubscription.SIZE_ABOVE:
                            String sza = (String) j;
                            Integer cbs = changedResourceJsonObject.optInt(ResourceContainer.CURR_BYTE_SIZE, -1);
                            if (cbs != -1) {
                                if (cbs > Integer.valueOf(sza))
                                    sendThisNotification = true;
                            } else {
                                Integer cs = changedResourceJsonObject.optInt(ResourceContentInstance.CONTENT_SIZE, -1);
                                if (cs != -1) {
                                    if (cs > Integer.valueOf(sza))
                                        sendThisNotification = true;
                                }
                            }

                        case ResourceSubscription.SIZE_BELOW:
                            String szb = (String) j;
                            Integer cbs2 = changedResourceJsonObject.optInt(ResourceContainer.CURR_BYTE_SIZE, -1);
                            if (cbs2 != -1) {
                                if (cbs2 < Integer.valueOf(szb))
                                    sendThisNotification = true;
                            } else {
                                Integer cs = changedResourceJsonObject.optInt(ResourceContentInstance.CONTENT_SIZE, -1);
                                if (cs != -1) {
                                    if (cs < Integer.valueOf(szb))
                                        sendThisNotification = true;
                                }
                            }

                        case ResourceSubscription.ATTRIBUTE:
                            // todo: check whether this attributes belong to that resource?
                            // list of String

                        case OPERATION_MONITOR:
                            JSONArray om = (JSONArray) j;

                            String operation = onem2mRequest.getPrimitive(RequestPrimitive.OPERATION);
                            if (om.toString().contains(operation))
                                JsonUtils.put(operationMonitor, ORIGINATOR, onem2mRequest.getPrimitive(RequestPrimitive.FROM));

                            JsonUtils.put(operationMonitor, OPERATION, Integer.valueOf(onem2mRequest.getPrimitive(RequestPrimitive.OPERATION)));
                            JsonUtils.put(notificationEvent, OPERATION_MONITOR, operationMonitor);

                        case ResourceSubscription.NOTIFICATION_EVENT_TYPE:

                        default:
                    }
                }
                if (eventnotificationcriteria.length() == 1 ) {
                    // has only 1 attribute: notificationEventType
                    JSONArray net = eventnotificationcriteria.optJSONArray(ResourceSubscription.NOTIFICATION_EVENT_TYPE);
                    if (net !=null && net.toString().contains(type)) {
                        sendThisNotification = true;
                    }
                }
            } else {
                sendThisNotification = true;
            }

            // if does not match, break this for loop
            if (!sendThisNotification) break;

            // todo: what does notification include? See latest TS


            // Step 2.1	The Originator shall determine the type of the notification per the notificationContentType attribute.
            // The possible values of for notificationContentType attribute are 'Modified Attributes',
            // 'All Attributes', and or optionally 'ResourceID'.
            representation = produceJsonContent(onem2mRequest, onem2mNotification);
            // todo: how to check attribute ? work with eventType to determine whose attributes
            // todo: support modified attributes

            //Step 2.2	Check the notificationEventCat attribute:
            // If the notificationEventCat attribute is set, the Notify request primitive shall employ the
            // Event Category parameter as given in the notificationEventCat attribute. Then continue with the next step


            // Step 2.3 Check the latestNotify attribute:
            // If the latestNotify attribute is set, the Originator shall assign Event Category parameter of value 'latest' of
            // the notifications generated pertaining to the subscription created. Then continue with other step
            if (subsJsonObject.optBoolean(ResourceSubscription.LATEST_NOTIFY)) {
                // modify the subscription eventCat attribute to "latest" ? See TS 0001
                subsJsonObject.put(ResourceSubscription.NOTIFICATION_EVENT_CAT,"latest");
            }

            JSONArray uriArray = onem2mNotification.getJsonSubscriptionResourceContent()
                    .optJSONArray(ResourceSubscription.NOTIFICATION_URI);
            for (int i = 0; i < uriArray.length(); i++) {
                onem2mNotification.setPrimitiveMany(NotificationPrimitive.URI, uriArray.optString(i));
            }


            JsonUtils.put(notificationEvent, REPRESENTATION, representation);
            JsonUtils.put(notificationEvent, ResourceSubscription.NOTIFICATION_EVENT_TYPE, type);
            JsonUtils.put(notification, NOTIFICATION_EVENT, notificationEvent);

            String name = Onem2mDb.getInstance().getHierarchicalNameForResource(subscriptionResourceId);
            notification.put(SUBSCRIPTION_REFERENCE, name);
            onem2mNotification.setPrimitive(NotificationPrimitive.CONTENT, notification.toString());

            // copy the URI's to the notification,
            ResourceChanged rc = new ResourceChangedBuilder()
                    .setOnem2mPrimitive(onem2mNotification.getPrimitivesList())
                    .build();

            // now that we have a NotificationPrimitive, we need to send it to the Notifier
            try {
                Onem2mCoreProvider.getNotifier().putNotification(rc);
            } catch (Exception e){
                LOG.error("cannot send notification");
            }

            updateSubscription(subsJsonObject, subscriptionResourceId);

        }
    }

    /**
     * if expiration counter meets 0, delete the subscription and return false, otherwise return true
     * @param subsJsonObject subscriptionJsonObject
     * @param subscriptionResourceId subscriptionID
     * @return
     */
    private static boolean updateSubscription(JSONObject subsJsonObject, String subscriptionResourceId) {
        // todo: when is bigInteger and when is integer?
        int newNumber = subsJsonObject.optInt(ResourceSubscription.EXPIRATION_COUNTER) - 1;
        // if exc does not exist, newNumber should be -1,
        if (newNumber != -1) {
            if (newNumber == 0) {
                //todo: should we send Notification here? if so, what's the notification format
                deletetheSubscription(subscriptionResourceId);
                sendSubscriptionDeletedNotification(subsJsonObject,subscriptionResourceId);
                return false;
            }
            JsonUtils.put(subsJsonObject, ResourceSubscription.EXPIRATION_COUNTER, newNumber);
        }
        Onem2mDb.getInstance().updateSubscriptionResource(subscriptionResourceId, subsJsonObject.toString());
        return true;
    }



    private static boolean deletetheSubscription(String subscriptionResourceId) {
        // todo: what if the subscription contains schedule?
        return Onem2mDb.getInstance().deleteSubscription(subscriptionResourceId);
    }


    public static void sendSubscriptionDeletedNotification(JSONObject subsJsonObjec, String subscriptionResourceId) {
        String uri = subsJsonObjec.optString(ResourceSubscription.SUBSCRIBER_URI, null);
        if (uri == null) {
            return;
        }
        NotificationPrimitive onem2mNotification = new NotificationPrimitive();
        onem2mNotification.setPrimitiveMany(NotificationPrimitive.URI, uri);
        JSONObject notification = new JSONObject();
        String name = Onem2mDb.getInstance().getHierarchicalNameForResource(subscriptionResourceId);

        JsonUtils.put(notification, SUBSCRIPTION_DELETION, true);
        JsonUtils.put(notification, SUBSCRIPTION_REFERENCE, name);
        onem2mNotification.setPrimitive(NotificationPrimitive.CONTENT, notification.toString());

        SubscriptionDeleted sd = new SubscriptionDeletedBuilder()
                .setOnem2mPrimitive(onem2mNotification.getPrimitivesList())
                .build();

        // now that we have a NotificationPrimitive, we need to send it to the Notifier
        try {
            Onem2mCoreProvider.getNotifier().putNotification(sd);
        } catch (Exception e){
            LOG.error("cannot send notification");
        }
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
        try {
            Onem2mCoreProvider.getNotifier().putNotification(sd);
        } catch (Exception e){
            LOG.error("cannot send notification");
        }
    }

    /**
     * The results of the delete now must be put in a notification if there exists active subscriptions.
     *
     * @param onem2mRequest request
     */
    public static void handleDelete(RequestPrimitive onem2mRequest) {

        handleEvnetTypeF(onem2mRequest);
        Onem2mResource onem2mResource = onem2mRequest.getOnem2mResource();
        String resourceType = onem2mResource.getResourceType();
        if (resourceType.contentEquals(Onem2m.ResourceType.SUBSCRIPTION)) {
            handleDeleteSubscription(onem2mRequest, onem2mResource);
        } else {
            handleEventTypeB(onem2mRequest);
            handleEventTypeD(onem2mRequest);
        }
    }

    public static void handleCreate(RequestPrimitive onem2mRequest) {
        handleEvnetTypeF(onem2mRequest);
        handleEventTypeC(onem2mRequest);
    }

    public static void handleUpdate(RequestPrimitive onem2mRequest) {
        handleEvnetTypeF(onem2mRequest);
        handleEventTypeA(onem2mRequest);
    }

}
