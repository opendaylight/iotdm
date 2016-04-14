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
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.CheckAccessControlProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.opendaylight.iotdm.onem2m.core.utils.Onem2mDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceSubscription {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceSubscription.class);
    private ResourceSubscription() {}

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

    /**
     * The list<Attr> and List<AttrSet> must be filled in with the ContentPrimitive attributes
     * @param onem2mRequest onem2mrequest
     * @param onem2mResponse onem2mresponse
     */
    private static void processCreateUpdateAttributes(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        String tempStr;

        if (onem2mRequest.isCreate) {
            String rt = onem2mRequest.getOnem2mResource().getResourceType();
            if (rt == null || !(rt.contentEquals(Onem2m.ResourceType.CSE_BASE) ||
                    rt.contentEquals(Onem2m.ResourceType.CONTAINER) ||
                    rt.contentEquals(Onem2m.ResourceType.AE))) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.TARGET_NOT_SUBSCRIBABLE,
                        "Cannot create Subscription under this resource type: " + rt);
                return;
            }
        }

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        /**
         * Theoretically, if the notificationURI is different from the Originator, then special processing is
         * required. TODO: see rules in TS0004 to add this functionality
         */

        tempStr = resourceContent.getInJsonContent().optString(NOTIFICATION_CONTENT_TYPE, null);
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
            if (resourceContent.getInJsonContent().optString(NOTIFICATION_URI, null) == null) {
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

    /**
     * This routine processes the JSON content for this resource representation.  Ideally, a json schema file would
     * be used so that each json key could be looked up in the json schema to find out what type it is, and so forth.
     * Maybe the next iteration of code, I'll create json files for each resource.
     * @param onem2mRequest
     * @param onem2mResponse
     */
    private static void parseJsonCreateUpdateContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        Iterator<?> keys = resourceContent.getInJsonContent().keys();
        while( keys.hasNext() ) {

            String key = (String)keys.next();

            resourceContent.jsonCreateKeys.add(key);

            Object o = resourceContent.getInJsonContent().opt(key);

            switch (key) {

                case NOTIFICATION_CONTENT_TYPE:
                case NOTIFICATION_EVENT_CAT:
                case PENDING_NOTIFICATION:
                case EXPIRATION_COUNTER:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof Integer)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") number expected for json key: " + key);
                            return;
                        }
                    }
                    break;

                case SUBSCRIBER_URI:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
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
                    if (!resourceContent.getInJsonContent().isNull(key)) {
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
                            if (!validateUri(uri)) {
                                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                        "NOTIFICATION_URI(s) not valid URI: " + uri);
                                return;
                            }
                        }
                    }
                    break;

                case LATEST_NOTIFY:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof Boolean)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") boolean expected for json key: " + key);
                            return;
                        }
                    }
                    break;

                case EVENT_NOTIFICATION_CRITERIA:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
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
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") CREATOR must be null");
                        return;
                    }
                    break;
                case ResourceContent.LABELS:
                case ResourceContent.EXPIRATION_TIME:
                case ResourceContent.RESOURCE_NAME:
                    if (!ResourceContent.parseJsonCommonCreateUpdateContent(key,
                            resourceContent,
                            onem2mResponse)) {
                        return;
                    }
                    break;

                default:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "CONTENT(" + RequestPrimitive.CONTENT + ") attribute not recognized: " + key);
                    return;
            }
            if (resourceContent.jsonCreateKeys.contains(CREATOR)) {
                JsonUtils.put(resourceContent.getInJsonContent(), CREATOR, onem2mRequest.getPrimitive(RequestPrimitive.FROM));
            }
        }
    }

    /**
     * Parse the CONTENT resource representation.
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void handleCreateUpdate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        resourceContent.parse(Onem2m.ResourceTypeString.SUBSCRIPTION, onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;

        if (resourceContent.isJson()) {
            parseJsonCreateUpdateContent(onem2mRequest, onem2mResponse);
            if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                return;
        }
        CheckAccessControlProcessor.handleCreateUpdate(onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;
        resourceContent.processCommonCreateUpdateAttributes(onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;

        ResourceSubscription.processCreateUpdateAttributes(onem2mRequest, onem2mResponse);

    }
}