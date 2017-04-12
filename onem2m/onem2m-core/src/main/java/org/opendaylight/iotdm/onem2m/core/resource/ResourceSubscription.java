/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.CheckAccessControlProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.opendaylight.iotdm.onem2m.core.utils.Onem2mDateTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceSubscription extends BaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceSubscription.class);
    public ResourceSubscription(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        super(onem2mRequest, onem2mResponse);
    }
    // taken from CDT-contentInstance-v1_0_0.xsd / TS0004_v_1-0_1 Section 8.2.2 Short Names
    // TODO: ts0001 9.6.7-1

    public static final String NOTIFICATION_URI = "nu";
    public static final String NOTIFICATION_CONTENT_TYPE = "nct";
    public static final String NOTIFICATION_EVENT_CAT = "nec";
    public static final String SUBSCRIBER_URI = "su";
    public static final String PENDING_NOTIFICATION = "pn";
    public static final String LATEST_NOTIFY = "ln";
    public static final String EXPIRATION_COUNTER = "exc";
    public static final String EVENT_NOTIFICATION_CRITERIA = "enc";
    public static final String GROUP_ID = "gpi";
    public static final String NOTIFICATION_FORWARDING_URI = "nfu";
    public static final String BATCH_NOTIFY = "bn";
    public static final String RATE_LIMIT = "rl";
    public static final String PRE_SUBSCRIPTION_NOTIFY = "psn";
    public static final String NOTIFICATION_STORAGE_PRIORITY = "nsp";


    public static final String CREATED_BEFORE = "crb";
    public static final String CREATED_AFTER = "cra";
    public static final String MODIFIED_SINCE = "ms";
    public static final String UNMODIFIED_SINCE = "us";
    public static final String STATE_TAG_SMALLER = "sts";
    public static final String STATE_TAG_BIGGER = "stb";

    //public static final String EXPIRE_BEFORE = "exb";
    //public static final String EXPIRE_AFTER = "exa";
    //public static final String MISSING_DATA ="";

    public static final String SIZE_ABOVE = "sza";
    public static final String SIZE_BELOW = "szb";
    public static final String ATTRIBUTE = "atr";
    public static final String NOTIFICATION_EVENT_TYPE = "net";
    public static final String OPERATION_MONITOR = "om";
    public static final String CREATOR = "cr";

    // TODO: need to add a section or new file to handle event notification criteria

    private static boolean validateUri(String uriString)  {
        try {
            URI toUri = new URI(uriString);
        } catch (URISyntaxException e) {
            return false;
        }
        return true;
    }

    private void processCreateUpdateAttributes() {

        String tempStr;

        if (onem2mRequest.isCreate) {
            Integer prt = onem2mRequest.getParentResourceType();
            if (!(prt == Onem2m.ResourceType.CSE_BASE ||
                    prt == Onem2m.ResourceType.CONTAINER ||
                    prt == Onem2m.ResourceType.AE)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.TARGET_NOT_SUBSCRIBABLE,
                        "Cannot create Subscription under this resource type: " + prt);
                return;
            }
        }

        /**
         * Theoretically, if the notificationURI is different from the Originator, then special processing is
         * required. TODO: see rules in TS0004 to add this functionality
         */

        tempStr = jsonPrimitiveContent.optString(NOTIFICATION_CONTENT_TYPE, null);
        if (tempStr != null) {
            if (!tempStr.contentEquals(Onem2m.NotificationContentType.MODIFIED_ATTRIBUTES) &&
                    !tempStr.contentEquals(Onem2m.NotificationContentType.WHOLE_RESOURCE) &&
                    !tempStr.contentEquals(Onem2m.NotificationContentType.RESOURCE_ID)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "NOTIFICATION_CONTENT_TYPE not valid: " + tempStr);
                return;
            }
        }


        if (onem2mRequest.isCreate) {
            if (jsonPrimitiveContent.optString(NOTIFICATION_URI, null) == null) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "NOTIFICATION_URI missing! " );
                return;
            }
        }

        /**
         * The resource has been filled in with any attributes that need to be written to the database
         */
        if (onem2mRequest.isCreate) {
            if (!Onem2mDb.getInstance().createResource(onem2mRequest, onem2mResponse)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR, "Cannot create in data store!");
                // TODO: what do we do now ... seems really bad ... keep stats
                return;
            }
        } else {
            if (!Onem2mDb.getInstance().updateResource(onem2mRequest, onem2mResponse)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR, "Cannot update the data store!");
                // TODO: what do we do now ... seems really bad ... keep stats
                return;
            }
        }
    }

    private void parseJsonCreateUpdateContent() {

        boolean creatorPresent = false;

        Iterator<?> keys = jsonPrimitiveContent.keys();
        while( keys.hasNext() ) {

            String key = (String)keys.next();

            Object o = jsonPrimitiveContent.opt(key);

            switch (key) {

                case NOTIFICATION_CONTENT_TYPE:
                case NOTIFICATION_EVENT_CAT:
                case PENDING_NOTIFICATION:
                case EXPIRATION_COUNTER:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        if (!(o instanceof Integer)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") number expected for json key: " + key);
                            return;
                        }
                    }
                    break;

                case SUBSCRIBER_URI:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        if (!(o instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                            return;
                        }
                        String subUri = (String) o;
                        if (!validateUri(subUri)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                    "SUBSCRIBER_URI not valid URI: " + subUri);
                            return;
                        }
                    }
                    break;

                case NOTIFICATION_URI:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        if (!(o instanceof JSONArray)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") array expected for json key: " + key);
                            return;
                        }
                        JSONArray array = (JSONArray) o;
                        for (int i = 0; i < array.length(); i++) {
                            if (!(array.opt(i) instanceof String)) {
                                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                        "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json array: " + key);
                                return;
                            }
                            String uri = (String) array.opt(i);

                            // Check as URL because schema must be specified in order to identify protocol
                            if (!Onem2m.isValidUriScheme(uri)) {
                                // given uri can be resourceId or it's invalid
                                Onem2mResource resource = Onem2mDb.getInstance().findResourceUsingURI(uri);
                                if(Objects.isNull(resource)) {
                                    LOG.error("Given URL or resourceId is not valid: {}", uri);
                                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                            "NOTIFICATION_URI can't be resolved neither as valid uri nor as resourceId: " + uri);
                                    return;
                                } else if (Integer.valueOf(resource.getResourceType())
                                                  .equals(Onem2m.ResourceType.CSE_BASE) || //not supporting using CSE_BASE poa for notifications now
                                        !Onem2m.pointOfAccessedResourceTypes.contains(Integer.valueOf(resource.getResourceType()))) {
                                    LOG.error("Given resourceId doesn't match a resource with pointOfAccess attribute: {}", uri);
                                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                            "NOTIFICATION_URI resourceId doesn't match a resource with pointOfAccess attribute: " + uri);
                                    return;
                                }
                            }
                        }
                    }
                    break;

                case LATEST_NOTIFY:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        if (!(o instanceof Boolean)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") boolean expected for json key: " + key);
                            return;
                        }
                    }
                    break;

                case EVENT_NOTIFICATION_CRITERIA:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        if (!(o instanceof JSONObject)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") Object expected for json key: " + key);
                            return;
                        }
                        JSONObject acrObject = (JSONObject) o;
                        Iterator<?> encKeys = acrObject.keys();
                        while (encKeys.hasNext()) {
                            String encKey = (String) encKeys.next();
                            Object j = acrObject.opt(encKey);
                            switch (encKey) {
                                case CREATED_BEFORE:
                                case CREATED_AFTER:
                                case MODIFIED_SINCE:
                                case UNMODIFIED_SINCE:
                                    if (!(j instanceof String)) {
                                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                "CONTENT(" + EVENT_NOTIFICATION_CRITERIA + ") string expected for json key: " + encKey);
                                        return;
                                    }
                                    if (!Onem2mDateTime.isValidDateTime((String)j)) {
                                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                "CONTENT(" + EVENT_NOTIFICATION_CRITERIA + ") valid time format expected for json key: " + encKey);
                                        return;
                                    }
                                    break;
                                case STATE_TAG_BIGGER:
                                case STATE_TAG_SMALLER:
                                case SIZE_ABOVE:
                                case SIZE_BELOW:
                                    if (!(j instanceof Integer)) {
                                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                "CONTENT(" + EVENT_NOTIFICATION_CRITERIA + ") number expected for json key: " + encKey);
                                        return;
                                    }
                                    break;

                                case ATTRIBUTE:
                                    // todo: check whether this attributes belong to that resource?
                                    // list of String
                                    if (!(j instanceof JSONArray)) {
                                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                "CONTENT(" + EVENT_NOTIFICATION_CRITERIA + ") array expected for json key: " + encKey);
                                        return;
                                    }
                                    JSONArray array = (JSONArray) j;
                                    for (int i = 0; i < array.length(); i++) {
                                        if (!(array.get(i) instanceof String)) {
                                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                    "EVENT_NOTIFICATION_CRITERIA(" + EVENT_NOTIFICATION_CRITERIA+ ") String expected for json array: " + encKey);
                                            return;
                                        }
                                    }
                                    break;

                                case OPERATION_MONITOR:
                                    // list of operation, should be among 1,2,3,4
                                    if (!(j instanceof JSONArray)) {
                                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                "CONTENT(" + EVENT_NOTIFICATION_CRITERIA + ") array expected for json key: " + encKey);
                                        return;
                                    }
                                    JSONArray array1 = (JSONArray) j;
                                    for (int i = 0; i < array1.length(); i++) {
                                        if (!(array1.get(i) instanceof Integer)) {
                                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                    "EVENT_NOTIFICATION_CRITERIA(" + EVENT_NOTIFICATION_CRITERIA+ ") Integer expected for json array: " + encKey);
                                            return;
                                        } else {
                                            // should be among 1,2,3,4,5
                                            int net = (Integer)array1.get(i);
                                            if (net <= 0 || net >= 6 ) {
                                                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                        "EVENT_NOTIFICATION_CRITERIA(" + EVENT_NOTIFICATION_CRITERIA+ ") Only Integer 1,2,3,4,5 expected for json array: " + encKey);
                                                return;
                                            }
                                        }
                                    }
                                    break;
                                case NOTIFICATION_EVENT_TYPE:
                                    // list of Integer
                                    if (!(j instanceof JSONArray)) {
                                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                "CONTENT(" + EVENT_NOTIFICATION_CRITERIA + ") array expected for json key: " + encKey);
                                        return;
                                    }
                                    JSONArray array2 = (JSONArray) j;
                                    for (int i = 0; i < array2.length(); i++) {
                                        if (!(array2.get(i) instanceof Integer)) {
                                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                    "EVENT_NOTIFICATION_CRITERIA(" + EVENT_NOTIFICATION_CRITERIA+ ") Integer expected for json array: " + encKey);
                                            return;
                                        } else {
                                            // should be among 1,2,3,4,5,6
                                            int net = (Integer)array2.get(i);
                                            if (net <= 0 || net >= 7 ) {
                                                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                        "EVENT_NOTIFICATION_CRITERIA(" + EVENT_NOTIFICATION_CRITERIA+ ") Only Integer 1,2,3,4,5 expected for json array: " + encKey);
                                                return;
                                            }
                                        }
                                    }
                                    break;
                                default:
                                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                            "CONTENT(" + EVENT_NOTIFICATION_CRITERIA + ") attribute not recognized: " + encKey);
                                    return;
                            }
                        }
                    }
                    break;
                case CREATOR:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") CREATOR must be null");
                        return;
                    }
                    creatorPresent = true;
                    break;
                case BaseResource.LABELS:
                case BaseResource.EXPIRATION_TIME:
                case BaseResource.RESOURCE_NAME:
                    if (!parseJsonCommonCreateUpdateContent(key)) {
                        return;
                    }
                    break;

                default:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "CONTENT(" + RequestPrimitive.CONTENT + ") attribute not recognized: " + key);
                    return;
            }
        }
        if (creatorPresent) {
            JsonUtils.put(jsonPrimitiveContent, CREATOR, onem2mRequest.getPrimitiveFrom());
        }
    }

    public static void modifyParentForSubscriptionCreation(JSONObject parentJsonContent, String resourceId) {
        // in the parent, maintain a list of subscriptions for fast access
        String arrayJsonKey = "c:" + Onem2m.ResourceType.SUBSCRIPTION;
        JSONArray jArray = parentJsonContent.optJSONArray(arrayJsonKey);
        if (jArray == null) {
            jArray = new JSONArray();
            jArray.put(resourceId);
            parentJsonContent.put(arrayJsonKey, jArray);
        } else {
            jArray.put(jArray.length(), resourceId);
        }
    }

    public static void modifyParentForSubscriptionDeletion(JSONObject parentJsonContent, String resourceId) {
        incrementParentStateTagIfPresent(parentJsonContent);

        String resouceIdJsonKey = "c:" + Onem2m.ResourceType.SUBSCRIPTION;
        JSONArray resouceIdJsonArray = parentJsonContent.optJSONArray(resouceIdJsonKey);

        if (resouceIdJsonArray != null) {
            for (int i = 0; i <= resouceIdJsonArray.length(); i++) {
                if (resourceId.equals(resouceIdJsonArray.getString(i))) {
                    resouceIdJsonArray.remove(i);
                    break;
                }
            }
        }
    }

    public void handleCreateUpdate() {

        parse(Onem2m.ResourceTypeString.SUBSCRIPTION);
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
            return;

        if (isJson()) {
            parseJsonCreateUpdateContent();
            if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
                return;
        }
        CheckAccessControlProcessor.handleCreateUpdate(onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
            return;
        processCommonCreateUpdateAttributes();
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
            return;

        processCreateUpdateAttributes();
    }
}
