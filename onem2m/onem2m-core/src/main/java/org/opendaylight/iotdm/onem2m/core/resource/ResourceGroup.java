/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import org.json.JSONArray;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.core.rest.CheckAccessControlProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class ResourceGroup extends BaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceGroup.class);

    public ResourceGroup(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        super(onem2mRequest, onem2mResponse);
    }

    // taken from CDT-container-v1_0_0.xsd / TS0004_v_1-0_1 Section 8.2.2 Short Names
    // TODO: ts0001 9.6.6-2


    public static final String CREATOR = "cr";
    public static final String MEMBER_TYPE = "mt";
    public static final String CURR_NR_MEMBERS = "cnm";
    public static final String MAX_NR_MEMBERS = "mnm";
    public static final String MEMBERS_IDS = "mid";
    public static final String MEMBERS_ACCESS_CONTROL_POLICY_IDS = "macp";
    public static final String MEMBER_TYPE_VALIDATED = "mtv";
    public static final String CONSISTENCY_STRATEGY = "csy";
    public static final String GROUP_NAME = "gn";
    public static final String FAN_OUT_POINT = "fopt";    // this is not an attribute
    
    private void parseJsonCreateUpdateContent() {
        
        Iterator<?> keys = jsonPrimitiveContent.keys();
        while (keys.hasNext()) {

            String key = (String) keys.next();
            
            Object o = jsonPrimitiveContent.opt(key);

            switch (key) {
                // read only
                case MEMBER_TYPE_VALIDATED:
                case CURR_NR_MEMBERS:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, key + ": read-only parameter");
                    return;
                // integer
                case MEMBER_TYPE:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        if (!(o instanceof Integer)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") number expected for json key: " + key);
                            return;
                        }
                    }
                    break;

                // integer > 0
                case MAX_NR_MEMBERS:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        if (!(o instanceof Integer)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") number expected for json key: " + key);
                            return;
                        } else if ((Integer) o < 0) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") integer must be non-negative: " + key);
                            return;
                        }
                    }
                    break;


                // special String
                case CREATOR:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") CREATOR must be null");
                        return;
                    } else {
                        jsonPrimitiveContent.remove(key);
                        JsonUtils.put(jsonPrimitiveContent, CREATOR, onem2mRequest.getPrimitiveFrom());
                    }
                    break;

                // String
                case GROUP_NAME:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        if (!(o instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                            return;
                        }
                    }
                    break;


                // list
                case MEMBERS_IDS:
                case MEMBERS_ACCESS_CONTROL_POLICY_IDS:
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
                        }
                    }
                    break;

                // default attributes in ResourceContent
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
    }

    /**
     * When create a container, add two special names as children, one is latest, the other is oldest, and
     * the values will initially be "".  When the first contentInstance is added under the container, then
     * point the oldest and latest to this new resource, and the new contentInstance, add two new attrs,
     * prev, next with values "".  These 4 special attrs will be head, tail, prev, next for a doubly LL.
     */

    public void processCreateUpdateAttributes() {

        String tempStr;
        Integer tempInt;

        // verify this resource can be created under the target resource
        if (onem2mRequest.isCreate) {
            Integer prt = onem2mRequest.getParentResourceType();
            if (!(prt == Onem2m.ResourceType.CSE_BASE ||
                    prt == Onem2m.ResourceType.CONTAINER ||
                    prt == Onem2m.ResourceType.AE)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Cannot create Group under this resource type: " + prt);
                return;
            }
        }

        tempStr = jsonPrimitiveContent.optString(CREATOR, null);
        if (tempStr != null && !onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CREATOR cannot be updated");
            return;
        }

        // 1 & WO
        Integer memberType = jsonPrimitiveContent.optInt(MEMBER_TYPE, -1);
        if (memberType != -1) {
            if (onem2mRequest.isCreate) {
                if (memberType != 0 && Onem2m.resourceTypeToString.get(memberType) == null) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "MEMBERTYPE not supported: " + memberType);
                    return;
                }
            } else {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "MEMBERTYPE cannot be updated");
                return;
            }
        } else {
            memberType = 0; // set to MIXED
            JsonUtils.put(jsonPrimitiveContent, MEMBER_TYPE, memberType);
        }

        // 1 & RW
        Integer mnm = jsonPrimitiveContent.optInt(MAX_NR_MEMBERS, -1);
        if (mnm == -1 && onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "MAX NUMBER OF MEMBERS missing parameter");
            return;
        }

        // 1 & List
        JSONArray mids = jsonPrimitiveContent.optJSONArray(MEMBERS_IDS);
        if (onem2mRequest.isCreate) {
            if (mids == null) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "MEMBER IDS missing parameter");
                return;
            }
        }
        if (mids != null) {
            if (mids.length() > mnm) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "Number OF MEMBERS exceed maximum");
                return;
            }
            // maybe we should verify that the members exist and are of the right resorue type
//            for (int i = 0; i < mids.length(); i++) {
//                String memberId = mids.getString(i);
//                if (memberId != null) {
//                    Onem2mResource onem2mResource = Onem2mDb.getInstance().getResource(trc, memberId);
//                    if (onem2mResource == null) {
//                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "Non existent member id: " + memberId);
//                        return;
//                    }
//                    if (memberType != 0) {
//                        Integer memberResourceType = Integer.valueOf(onem2mResource.getResourceType());
//                        if (memberResourceType != memberType) {
//                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
//                                    "Resource type: " + memberResourceType +
//                                            " of member: " + memberId + " does not match group member type: " + memberType);
//                            return;
//                        }
//                    }
//
//                }
//            }
            JsonUtils.put(jsonPrimitiveContent, CURR_NR_MEMBERS, mids.length());
        }

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

    public void handleCreateUpdate() {

        parse(Onem2m.ResourceTypeString.GROUP);
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
            return;

        if (isJson()) {
            parseJsonCreateUpdateContent();
            if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
                return;
        }
        //CheckAccessControlProcessor.handleCreateUpdate();
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
            return;
        processCommonCreateUpdateAttributes();
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
            return;
        processCreateUpdateAttributes();

    }
}
