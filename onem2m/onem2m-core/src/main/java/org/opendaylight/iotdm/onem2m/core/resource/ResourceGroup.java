/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.CheckAccessControlProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.RequestPrimitiveProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class ResourceGroup {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceGroup.class);
    private ResourceGroup() {}

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
                // read only
                case MEMBER_TYPE_VALIDATED:
                case CURR_NR_MEMBERS:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, key + ": read-only parameter");
                    return;
                // integer
                case MEMBER_TYPE:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof Integer)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") number expected for json key: " + key);
                            return;
                        }
                    }
                    break;

                // integer > 0
                case MAX_NR_MEMBERS:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
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
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") CREATOR must be null");
                        return;
                    } else {
                        resourceContent.getInJsonContent().remove(key);
                        JsonUtils.put(resourceContent.getInJsonContent(), CREATOR, onem2mRequest.getPrimitive(RequestPrimitive.FROM));
                    }
                    break;

                // String
                case GROUP_NAME:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
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
                        }
                    }
                    break;

                // default attributes in ResourceContent
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
        }
    }

    /**
     * When create a container, add two special names as children, one is latest, the other is oldest, and
     * the values will initially be "".  When the first contentInstance is added under the container, then
     * point the oldest and latest to this new resource, and the new contentInstance, add two new attrs,
     * prev, next with values "".  These 4 special attrs will be head, tail, prev, next for a doubly LL.
     */

    /**
     * Ensure the create/update parameters follow the rules
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void processCreateUpdateAttributes(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        String tempStr;
        Integer tempInt;

        // verify this resource can be created under the target resource
        if (onem2mRequest.isCreate) {
            String rt = onem2mRequest.getOnem2mResource().getResourceType();
            if (rt == null || !(rt.contentEquals(Onem2m.ResourceType.CSE_BASE) ||
                    rt.contentEquals(Onem2m.ResourceType.CONTAINER) ||
                    rt.contentEquals(Onem2m.ResourceType.AE))) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Cannot create Container under this resource type: " + rt);
                return;
            }
        }

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        tempStr = resourceContent.getInJsonContent().optString(CREATOR, null);
        if (tempStr != null && !onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CREATOR cannot be updated");
            return;
        }

        // 1 & WO
        String memberType = resourceContent.getInJsonContent().optString(MEMBER_TYPE, null);
        if (memberType == null && onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "MEMBERTYPE missing parameter");
            return;
        } else if (memberType != null && !onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "MEMBERTYPE cannot be updated");
            return;
        }

        // 1 & RW
        String mnm = resourceContent.getInJsonContent().optString(MAX_NR_MEMBERS, null);
        if (mnm == null && onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "MAX NUMBER OF MEMBERS missing parameter");
            return;
        }

        // 1 & List
        String mids = resourceContent.getInJsonContent().optString(MEMBERS_IDS, null);
        if (mids == null && onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "MEMBER IDS missing parameter");
            return;
        }

        // set the Cur number when created
        if (onem2mRequest.isCreate) {
            // initialize cur number of members to 0
            tempInt = resourceContent.getInJsonContent().optJSONArray(MEMBERS_IDS).length();
            JsonUtils.put(resourceContent.getInJsonContent(), CURR_NR_MEMBERS, tempInt);
        }else {
            // update the existing current number of members as the resource is being updated
            tempInt = resourceContent.getInJsonContent().optJSONArray(MEMBERS_IDS).length();
            JsonUtils.put(resourceContent.getInJsonContent(), CURR_NR_MEMBERS, tempInt);
            // todo: they are the same...
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

    /**
     * Parse the CONTENT resource representation.
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void handleCreateUpdate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        resourceContent.parse(Onem2m.ResourceTypeString.GROUP, onem2mRequest, onem2mResponse);
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
        ResourceGroup.processCreateUpdateAttributes(onem2mRequest, onem2mResponse);

    }

    private static boolean removeOldestContentInstance(String containerUri) {
        String oldestContentInstanceUri = Onem2mDb.trimURI(containerUri) + "/oldest";
        RequestPrimitiveProcessor onem2mRequest = new RequestPrimitiveProcessor();
        onem2mRequest.setPrimitive(RequestPrimitive.TO, oldestContentInstanceUri);
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();

        if (!Onem2mDb.getInstance().findResourceUsingURI(oldestContentInstanceUri, onem2mRequest, onem2mResponse)) {
            return false;
        }
        if (!Onem2mDb.getInstance().deleteResourceUsingURI(onem2mRequest, onem2mResponse)) {
            return false;
        }
        return true;
    }

    public static void checkAndFixCurrMaxRules(String containerUri) {

        RequestPrimitiveProcessor onem2mRequest = new RequestPrimitiveProcessor();
        onem2mRequest.setPrimitive(RequestPrimitive.TO, containerUri);
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();

        if (!Onem2mDb.getInstance().findResourceUsingURI(containerUri, onem2mRequest, onem2mResponse)) {
            LOG.error("Somehow the resource container is gone! : " + containerUri);
            return;
        }


        boolean stillMoreToDelete = true;
        while (stillMoreToDelete) {
            Integer maxNrInstances = onem2mRequest.getJsonResourceContent().optInt(ResourceContainer.MAX_NR_INSTANCES, -1);
            if (maxNrInstances != -1) {
                Integer currNrInstances = onem2mRequest.getJsonResourceContent().optInt(ResourceContainer.CURR_NR_INSTANCES);
                if (currNrInstances != -1) {
                    if (currNrInstances <= maxNrInstances) {
                        stillMoreToDelete = false;
                    } else {
                        if (!removeOldestContentInstance(containerUri)) {
                            stillMoreToDelete = false;
                        }
                        onem2mRequest = new RequestPrimitiveProcessor();
                        onem2mRequest.setPrimitive(RequestPrimitive.TO, containerUri);
                        onem2mResponse = new ResponsePrimitive();
                        if (!Onem2mDb.getInstance().findResourceUsingURI(containerUri, onem2mRequest, onem2mResponse)) {
                            LOG.error("Somehow the resource container is gone! : " + containerUri);
                            return;
                        }
                    }
                } else {
                    stillMoreToDelete = false;
                }
            } else {
                stillMoreToDelete = false;
            }
        }

        onem2mRequest = new RequestPrimitiveProcessor();
        onem2mRequest.setPrimitive(RequestPrimitive.TO, containerUri);
        onem2mResponse = new ResponsePrimitive();
        if (!Onem2mDb.getInstance().findResourceUsingURI(containerUri, onem2mRequest, onem2mResponse)) {
            LOG.error("Somehow the resource container is gone! : " + containerUri);
            return;
        }

        stillMoreToDelete = true;
        while (stillMoreToDelete) {
            Integer maxByteSize = onem2mRequest.getJsonResourceContent().optInt(ResourceContainer.MAX_BYTE_SIZE, -1);
            if (maxByteSize != -1) {
                Integer currByteSize = onem2mRequest.getJsonResourceContent().optInt(ResourceContainer.CURR_BYTE_SIZE);
                if (currByteSize != -1) {
                    if (currByteSize <= maxByteSize) {
                        stillMoreToDelete = false;
                    } else {
                        if (!removeOldestContentInstance(containerUri)) {
                            stillMoreToDelete = false;
                        }
                        onem2mRequest = new RequestPrimitiveProcessor();
                        onem2mRequest.setPrimitive(RequestPrimitive.TO, containerUri);
                        onem2mResponse = new ResponsePrimitive();
                        if (!Onem2mDb.getInstance().findResourceUsingURI(containerUri, onem2mRequest, onem2mResponse)) {
                            LOG.error("Somehow the resource container is gone! : " + containerUri);
                            return;
                        }
                    }
                } else {
                    stillMoreToDelete = false;
                }
            } else {
                stillMoreToDelete = false;
            }
        }
    }

    /**
     * A side effect of creating a content instance is that its parent container curr_byte_size,
     * curr_nr_instances, and state tag must also be updated.
     * @param onem2mRequest
     * @param containerResourceContent
     * @param newByteSize
     */
    public static void setCurrValuesForThisCreatedContentInstance(RequestPrimitive onem2mRequest,
                                                                  JSONObject containerResourceContent,
                                                                  Integer newByteSize) {

        Integer cni = containerResourceContent.optInt(ResourceContainer.CURR_NR_INSTANCES);
        Integer cbs = containerResourceContent.optInt(ResourceContainer.CURR_BYTE_SIZE);
        Integer st = containerResourceContent.optInt(ResourceContent.STATE_TAG);

        cni++;
        cbs += newByteSize;
        st++;

        onem2mRequest.setCurrContainerValues(cbs, cni, st);
    }

    /**
     * A side effect of deleting a content instance is that its parent container curr_byte_size,
     * curr_nr_instances, and state tag must also be updated.
     * @param onem2mRequest
     * @param containerResourceContent
     * @param delByteSize
     */
    public static void setCurrValuesForThisDeletedContentInstance(RequestPrimitive onem2mRequest,
                                                                  JSONObject containerResourceContent,
                                                                  Integer delByteSize) {

        Integer cni = containerResourceContent.optInt(ResourceContainer.CURR_NR_INSTANCES);
        Integer cbs = containerResourceContent.optInt(ResourceContainer.CURR_BYTE_SIZE);
        Integer st = containerResourceContent.optInt(ResourceContent.STATE_TAG);

        cni--;
        cbs -= delByteSize;
        st++;

        onem2mRequest.setCurrContainerValues(cbs, cni, st);
    }
}
