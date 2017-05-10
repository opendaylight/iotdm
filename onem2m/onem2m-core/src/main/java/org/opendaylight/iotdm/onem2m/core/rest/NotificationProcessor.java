/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.rest;

import static org.opendaylight.iotdm.onem2m.core.Onem2m.EventType.ANY_DESCENDANT_CHANGE;
import static org.opendaylight.iotdm.onem2m.core.Onem2m.EventType.CREATE_CHILD;
import static org.opendaylight.iotdm.onem2m.core.Onem2m.EventType.DELETE_CHILD;
import static org.opendaylight.iotdm.onem2m.core.Onem2m.EventType.DELETE_RESOURCE;
import static org.opendaylight.iotdm.onem2m.core.Onem2m.EventType.RETRIEVE_NECHILD;
import static org.opendaylight.iotdm.onem2m.core.Onem2m.EventType.UPDATE_RESOURCE;

import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.Onem2mCoreProvider;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.resource.BaseResource;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContainer;
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

    private static NotificationProcessor notificationProcessor;

    private static final Logger LOG = LoggerFactory.getLogger(NotificationProcessor.class);

    private static final Integer NUM_SUBSCRIBER_PROCESSORS = 32;
    private List<LinkedBlockingQueue<QEntry>> queueList = Lists.newArrayList();
    public enum Operation {CREATE, UPDATE, DELETE};

    private NotificationProcessor() {}

    public static NotificationProcessor getInstance() {
        if (notificationProcessor == null)
            notificationProcessor = new NotificationProcessor();
        return notificationProcessor;
    }
    /**
     * This routine looks at the notification content type and build the json representation based on its setting.
     *
     * @param onem2mRequest      the set of request primitives
     * @param onem2mNotification the set of notification primitives
     */
    private JSONObject produceJsonContent(RequestPrimitive onem2mRequest, NotificationPrimitive onem2mNotification) {

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

    private JSONObject produceJsonContentResourceReference(Onem2mResource onem2mResource) {
        return JsonUtils.put(new JSONObject(), BaseResource.RESOURCE_NAME,
                Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource));
    }

    private JSONObject produceJsonContentWholeResource(RequestPrimitive onem2mRequest,
                                                              Onem2mResource onem2mResource) {

        JSONObject j = new JSONObject();

        Integer resourceType = Integer.valueOf(onem2mResource.getResourceType());

        String name = Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource);
        JsonUtils.put(onem2mRequest.getJsonResourceContent(), BaseResource.RESOURCE_NAME, name);

        try {
            JSONObject wholeResource = new JSONObject(onem2mResource.getResourceContentJsonString());
            wholeResource.remove("c:" + Onem2m.ResourceType.SUBSCRIPTION);
            wholeResource.remove("c:" + Onem2m.ResourceType.CONTENT_INSTANCE);
            return JsonUtils.put(j, "m2m:" + Onem2m.resourceTypeToString.get(resourceType), wholeResource);
        } catch (JSONException e) {
            LOG.error("Invalid JSON {}", onem2mResource.getResourceContentJsonString(), e);
            throw new IllegalArgumentException("Invalid JSON", e);
        }
    }


    /**
     * A. Default one
     * Update to attributes of the subscribed-to resource
     *
     * @param onem2mRequest onem2mrequest
     */
    private void handleEventTypeA(RequestPrimitive onem2mRequest) {
        Onem2mDb onem2mDb = Onem2mDb.getInstance();
        List<String> subscriptionsResourceIds = onem2mDb.selfSubscriptionsResourceIds(onem2mRequest, UPDATE_RESOURCE);

        //notify also parent if it has stateTag
        if (parentHasStateTag(onem2mRequest)) {
            subscriptionsResourceIds.addAll(onem2mDb.parentSubscriptionsResourceIds(onem2mRequest, UPDATE_RESOURCE, false));
        }

        sendNotificationAccordingToType(onem2mRequest, subscriptionsResourceIds, UPDATE_RESOURCE);
    }

    /**
     * B. Deletion of the  subscribed-to resource
     *
     * @param onem2mRequest onem2mrequest
     */
    private void handleEventTypeB(RequestPrimitive onem2mRequest) {
        List<String> subscriptionsResourceIds = Onem2mDb.getInstance().selfSubscriptionsResourceIds(onem2mRequest, DELETE_RESOURCE);
        sendNotificationAccordingToType(onem2mRequest, subscriptionsResourceIds, DELETE_RESOURCE);
    }

    /**
     * C. Creation of a direct child of the subscribed-to resource
     *
     * @param onem2mRequest onem2mrequest
     */
    private void handleEventTypeC(RequestPrimitive onem2mRequest) {
        handleParentSubscriptions(onem2mRequest, true);
    }

    /**
     * D. Deletion of a direct child of the subscribed-to resource
     *
     * @param onem2mRequest onem2mrequest
     */
    private void handleEventTypeD(RequestPrimitive onem2mRequest) {
        handleParentSubscriptions(onem2mRequest, false);
    }

    /**
     * handle direct parent's subscription for create/delete child event types
     * join also UPDATE_RESOURCE subscriptions if parent has stateTag attribute
     * @param onem2mRequest onem2mRequest
     * @param isCreateChild use CREATE_CHILD if true or DELETE_CHILD otherwise
     */
    private void handleParentSubscriptions(RequestPrimitive onem2mRequest, boolean isCreateChild) {
        Map<String, List<String>> eventTypeToSubscriptionsResourceIds = new HashMap<>();
        List<String> eventTypes = Lists.newArrayList(isCreateChild ? CREATE_CHILD:DELETE_CHILD, UPDATE_RESOURCE);

        for (String eventType : eventTypes) {
            if (!eventType.equals(UPDATE_RESOURCE) || parentHasStateTag(onem2mRequest)) {
                //skip notification for subscription create - isCreateChild=true
                List<String> subscriptionsResourceIds = Onem2mDb.getInstance()
                                                       .parentSubscriptionsResourceIds(onem2mRequest, eventType, isCreateChild);
                eventTypeToSubscriptionsResourceIds.put(eventType, subscriptionsResourceIds);
            }
        }

        sendNotificationAccordingToType(onem2mRequest, eventTypeToSubscriptionsResourceIds);
    }

    /**
     * E. retrieve attempt of non-existing child
     *
     * @param onem2mRequest onem2mrequest
     */
    public void handleEventTypeE(RequestPrimitive onem2mRequest, List<String> subscriptionsResourceIds) {
        sendNotificationAccordingToType(onem2mRequest, subscriptionsResourceIds, RETRIEVE_NECHILD);
    }

    /**
     * F. update of any descendants of the subscribed-to resource
     * this method could be inside each of certain operation
     *
     * @param onem2mRequest onem2mrequest
     */

    private void handleEventTypeF(RequestPrimitive onem2mRequest) {
        List<String> subscriptionsResourceIds = Onem2mDb.getInstance().findAllAncestorsSubscriptionID(onem2mRequest);
        sendNotificationAccordingToType(onem2mRequest, subscriptionsResourceIds, ANY_DESCENDANT_CHANGE);
    }

    private boolean parentHasStateTag(RequestPrimitive onem2mRequest) {
        Integer parentResourceType = onem2mRequest.getParentResourceType();
        if (parentResourceType == null) {
            Onem2mResource onem2mParentResource = Onem2mDb.getInstance()
                                                          .findResourceUsingURI(onem2mRequest.getParentTargetUri());
            parentResourceType = Integer.valueOf(onem2mParentResource.getResourceType());
        }
        return Onem2m.stateTaggedResourceTypes.contains(parentResourceType);
    }

    private void sendNotificationAccordingToType(RequestPrimitive onem2mRequest, Map<String, List<String>> eventTypeToSubscriptionsResourceIds) {
        for (Map.Entry<String, List<String>> entry : eventTypeToSubscriptionsResourceIds.entrySet()) {
            sendNotificationAccordingToType(onem2mRequest, entry.getValue(), entry.getKey());
        }
    }

    private void sendNotificationAccordingToType(RequestPrimitive onem2mRequest, List<String> subscriptionsResourceIds, String eventType) {

        for (String subscriptionResourceId : subscriptionsResourceIds) {

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
            JSONObject eventNotificationCriteria = onem2mNotification.getJsonSubscriptionResourceContent().
                    optJSONObject(ResourceSubscription.EVENT_NOTIFICATION_CRITERIA);
            if (eventNotificationCriteria != null) {
                Iterator<?> encKeys = eventNotificationCriteria.keys();
                while (encKeys.hasNext()) {
                    String encKey = (String) encKeys.next();
                    Object j = eventNotificationCriteria.opt(encKey);
                    switch (encKey) {
                        case ResourceSubscription.CREATED_BEFORE:
                            String crb = (String) j;
                            String ct = changedResourceJsonObject.optString(BaseResource.CREATION_TIME);
                            if (ct != null && Onem2mDateTime.dateCompare(ct, crb) < 0) {
                                sendThisNotification = true;
                            }
                            break;
                        case ResourceSubscription.CREATED_AFTER:
                            String cra = (String) j;
                            String ct2 = changedResourceJsonObject.optString(BaseResource.CREATION_TIME);
                            if (ct2 != null && Onem2mDateTime.dateCompare(ct2, cra) < 0) {
                                sendThisNotification = true;
                            }
                            break;
                        case ResourceSubscription.MODIFIED_SINCE:
                            String ms = (String) j;
                            String mt = changedResourceJsonObject.optString(BaseResource.LAST_MODIFIED_TIME);
                            if (mt != null && Onem2mDateTime.dateCompare(mt, ms) > 0) {
                                sendThisNotification = true;
                            }
                            break;
                        case ResourceSubscription.UNMODIFIED_SINCE:
                            String ums = (String) j;
                            String mt2 = changedResourceJsonObject.optString(BaseResource.LAST_MODIFIED_TIME);
                            if (mt2 != null && Onem2mDateTime.dateCompare(mt2, ums) < 0) {
                                sendThisNotification = true;
                            }
                            break;
                        case ResourceSubscription.STATE_TAG_BIGGER:
                            String stb = (String) j;
                            Integer st = changedResourceJsonObject.optInt(BaseResource.STATE_TAG, -1);
                            if (st != -1) {
                                if (st > Integer.valueOf(stb))
                                    sendThisNotification = true;
                            }
                            break;
                        case ResourceSubscription.STATE_TAG_SMALLER:
                            String sts = (String) j;
                            Integer st2 = changedResourceJsonObject.optInt(BaseResource.STATE_TAG, -1);
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
                if (eventNotificationCriteria.length() == 1) {
                    // has only 1 attribute: notificationEventType
                    JSONArray net = eventNotificationCriteria.optJSONArray(ResourceSubscription.NOTIFICATION_EVENT_TYPE);
                    if (net != null && net.toString().contains(eventType)) {
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
            JsonUtils.put(notificationEvent, ResourceSubscription.NOTIFICATION_EVENT_TYPE, eventType);
            JsonUtils.put(notification, NOTIFICATION_EVENT, notificationEvent);

            String name = Onem2mDb.getInstance().getHierarchicalNameForResource(subscriptionResourceId);
            notification.put(SUBSCRIPTION_REFERENCE, name);
            onem2mNotification.setPrimitive(NotificationPrimitive.CONTENT, notification.toString());

            // Get the sender CSE-ID if set
            String senderCse = null;
            if (null != onem2mRequest.getTargetResourceLocator() &&
                null != onem2mRequest.getTargetResourceLocator().getCseBaseCseId()) {
                senderCse = onem2mRequest.getTargetResourceLocator().getCseBaseCseId();
            } else {
                LOG.warn("Unable to get sender CSEBase CSE-ID");
            }

            // copy the URI's to the notification,
            ResourceChanged rc = new ResourceChangedBuilder()
                    .setOnem2mPrimitive(onem2mNotification.getPrimitivesList())
                    .setSenderCseId(senderCse)
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
     *
     * @param subsJsonObject         subscriptionJsonObject
     * @param subscriptionResourceId subscriptionID
     * @return
     */
    private boolean updateSubscription(JSONObject subsJsonObject, String subscriptionResourceId) {
        // todo: when is bigInteger and when is integer?
        int newNumber = subsJsonObject.optInt(ResourceSubscription.EXPIRATION_COUNTER) - 1;
        // if exc does not exist, newNumber should be -1,
        if (newNumber != -1) {
            if (newNumber == 0) {
                //todo: should we send Notification here? if so, what's the notification format
//                RequestPrimitive onem2mRequest = new RequestPrimitive();
//                ResponsePrimitive onem2mResponse = new ResponsePrimitive();
//                Onem2mDb.getInstance().deleteResourceUsingURI(onem2mRequest, onem2mResponse);
                deletetheSubscription(subscriptionResourceId);
                sendSubscriptionDeletedNotification(subsJsonObject, subscriptionResourceId);
                return false;
            }
            JsonUtils.put(subsJsonObject, ResourceSubscription.EXPIRATION_COUNTER, newNumber);
        }
        Onem2mDb.getInstance().updateSubscriptionResource(subscriptionResourceId, subsJsonObject.toString());
        return true;
    }


    private boolean deletetheSubscription(String subscriptionResourceId) {
        // todo: what if the subscription contains schedule?
        return Onem2mDb.getInstance().deleteSubscription(subscriptionResourceId);
    }


    private void sendSubscriptionDeletedNotification(JSONObject subsJsonObjec, String subscriptionResourceId) {
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
     * @param onem2mRequest  request
     * @param onem2mResource CRUD operation
     */
    private void handleDeleteSubscription(RequestPrimitive onem2mRequest, Onem2mResource onem2mResource) {

        String uri = onem2mRequest.getJsonResourceContent().optString(ResourceSubscription.SUBSCRIBER_URI, null);
        if (uri == null) {
            return;
        }
        NotificationPrimitive onem2mNotification = new NotificationPrimitive();

        onem2mNotification.setPrimitiveMany(NotificationPrimitive.URI, uri);

        JSONObject notification = new JSONObject();

        String name = Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource);

        JsonUtils.put(notification, SUBSCRIPTION_DELETION, true);
        JsonUtils.put(notification, SUBSCRIPTION_REFERENCE, name);
        onem2mNotification.setPrimitive(NotificationPrimitive.CONTENT, notification.toString());

        // Get the sender CSE-ID if set
        String senderCse = null;
        if (null != onem2mRequest.getTargetResourceLocator() &&
            null != onem2mRequest.getTargetResourceLocator().getCseBaseCseId()) {
            senderCse = onem2mRequest.getTargetResourceLocator().getCseBaseCseId();
        } else {
            LOG.warn("Unable to get sender CSEBase CSE-ID");
        }

        SubscriptionDeleted sd = new SubscriptionDeletedBuilder()
                    .setOnem2mPrimitive(onem2mNotification.getPrimitivesList())
                    .setSenderCseId(senderCse)
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
     * @param onem2mRequest request
     */
    private void handleDelete(RequestPrimitive onem2mRequest) {

        handleEventTypeF(onem2mRequest);
        Onem2mResource onem2mResource = onem2mRequest.getOnem2mResource();
        Integer resourceType = onem2mRequest.getResourceType();
        if (resourceType == Onem2m.ResourceType.SUBSCRIPTION) {
            handleDeleteSubscription(onem2mRequest, onem2mResource);
        } else {
            handleEventTypeB(onem2mRequest);
            handleEventTypeD(onem2mRequest);
        }
    }

    private void handleCreate(RequestPrimitive onem2mRequest) {
        handleEventTypeF(onem2mRequest);
        handleEventTypeC(onem2mRequest);
    }

    private void handleUpdate(RequestPrimitive onem2mRequest) {
        handleEventTypeF(onem2mRequest);
        handleEventTypeA(onem2mRequest);
    }

    /**
     * A number of threads are required to process the subscriptions.  It might be prudent to ensure a resource is
     * processed in-order.  For example: if res a is added, updated, and deleted, the last op should be deleted.
     * Hopefully this is enough.  I will hash on the resId, and send it to the correct q.  There will be one
     * q per thread.
     */
    public void initThreadsAndQueuesForResourceProcessing() {

        ExecutorService executorService = Executors.newFixedThreadPool(NUM_SUBSCRIBER_PROCESSORS);

        AtomicInteger qNum = new AtomicInteger(-1);
        for (int i = 0; i < NUM_SUBSCRIBER_PROCESSORS; i++) {

            LinkedBlockingQueue<QEntry> q = new LinkedBlockingQueue<>();
            queueList.add(i, q);
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    runProcessSubscriberQ(qNum.incrementAndGet());
                }
            });
        }
    }

    private void runProcessSubscriberQ(Integer qNum) {

        Thread.currentThread().setName("subsc-proc-" + qNum);
        QEntry qEntry;

        while (true) {
            try {
                qEntry = queueList.get(qNum).take();
            } catch (Exception e) {
                LOG.error("{}", e.toString());
                qEntry = null;
            }

            if (qEntry != null) {

//                LOG.info("Denqueued notification request: opCode:{}, resId: {}, res: {}/{}",
//                        qEntry.opCode,
//                        qEntry.requestPrimitive.getResourceId(),
//                        qEntry.requestPrimitive.getParentTargetUri(),
//                        qEntry.requestPrimitive.getResourceName());

                switch (qEntry.opCode) {
                    case CREATE:
                        handleCreate(qEntry.requestPrimitive);
                        break;
                    case UPDATE:
                        handleUpdate(qEntry.requestPrimitive);

                        break;
                    case DELETE:
                        handleDelete(qEntry.requestPrimitive);
                        break;
                }
            }
        }
    }

    public void enqueueNotifierOperation(Operation opCode, RequestPrimitive requestPrimitive) {
        Integer qNum = Math.abs(requestPrimitive.getResourceId().hashCode()) % NUM_SUBSCRIBER_PROCESSORS;
        try {
            queueList.get(qNum).put(new QEntry(opCode, requestPrimitive));
        } catch (InterruptedException e) {
            LOG.error("Couldn't enqueue: opCode:{}, resId: {}, res: {}/{}, e: {}",
                    opCode, requestPrimitive.getResourceId(),
                    requestPrimitive.getParentTargetUri(),
                    requestPrimitive.getResourceName(),
                    e.toString());
        }
    }

    private class QEntry {
        protected Operation opCode;
        protected RequestPrimitive requestPrimitive;
        QEntry(Operation opCode, RequestPrimitive requestPrimitive) {
            this.opCode = opCode;
            this.requestPrimitive = requestPrimitive;
        }
    }
}
