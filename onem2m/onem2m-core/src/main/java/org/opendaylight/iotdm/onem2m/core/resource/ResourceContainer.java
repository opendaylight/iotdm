/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.core.rest.CheckAccessControlProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.RequestPrimitiveProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceContainer extends BaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceContainer.class);

    public ResourceContainer(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        super(onem2mRequest, onem2mResponse);
    }

    // taken from CDT-container-v1_0_0.xsd / TS0004_v_1-0_1 Section 8.2.2 Short Names
    // TODO: ts0001 9.6.6-2

    public static final String CIN_BYTES_SIZE_KEY = "cin:bs";
    public static final Integer SYS_MAX_NR_INSTANCES = 8;

    public static final String CREATOR = "cr";
    public static final String MAX_NR_INSTANCES = "mni";
    public static final String MAX_BYTE_SIZE = "mbs";
    public static final String MAX_INSTANCE_AGE = "mia";
    public static final String CURR_NR_INSTANCES = "cni";
    public static final String CURR_BYTE_SIZE = "cbs";
    public static final String ONTOLOGY_REF = "or";
    public static final String LATEST = "la";
    public static final String OLDEST = "ol";
    public static final String DISABLE_RETRIEVAL = "dt";
    // todo change dt to correct name

    private void parseJsonCreateUpdateContent() {

        boolean creatorPresent = false;

        Iterator<?> keys = jsonPrimitiveContent.keys();
        while( keys.hasNext() ) {

            String key = (String)keys.next();

            Object o = jsonPrimitiveContent.opt(key);

            switch (key) {

                case CURR_BYTE_SIZE:
                case CURR_NR_INSTANCES:
                case BaseResource.STATE_TAG:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, key + ": read-only parameter");
                    return;

                case MAX_NR_INSTANCES:
                case MAX_BYTE_SIZE:
                case MAX_INSTANCE_AGE:
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

                case CREATOR:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") CREATOR must be null");
                        return;
                    }
                    creatorPresent = true;
                    break;
                case DISABLE_RETRIEVAL:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        if (!(o instanceof Boolean)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") boolean expected for json key: " + key);
                            return;
                        }
                    }
                    break;
                case ONTOLOGY_REF:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        if (!(o instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                            return;
                        }
                    }
                    break;
                case BaseResource.LABELS:
                case BaseResource.ACCESS_CONTROL_POLICY_IDS:
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

    /**
     * When create a container, add two special names as children, one is latest, the other is oldest, and
     * the values will initially be "".  When the first contentInstance is added under the container, then
     * point the oldest and latest to this new resource, and the new contentInstance, add two new attrs,
     * prev, next with values "".  These 4 special attrs will be head, tail, prev, next for a doubly LL.
     */

    public void processCreateUpdateAttributes() {

        String tempStr;

        // verify this resource can be created under the target resource
        if (onem2mRequest.isCreate) {
            Integer prt = onem2mRequest.getParentResourceType();
            if (!(prt == Onem2m.ResourceType.CSE_BASE ||
                    prt == Onem2m.ResourceType.CONTAINER ||
                    prt == Onem2m.ResourceType.AE)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Cannot create Container under this resource type: " + prt);
                return;
            }
        }

        // if update disableRetrieval to true, delete all the existing <cin>s
        if (onem2mRequest.isUpdate) {
            Boolean dt = jsonPrimitiveContent.optBoolean(DISABLE_RETRIEVAL);
            if (dt) {
                //Onem2mDb.getInstance().dumpContentInstancesForContainer(twc, trc, onem2mRequest.getPrimitive(RequestPrimitive.TO));

                JsonUtils.put(jsonPrimitiveContent, CURR_NR_INSTANCES, 0);
                JsonUtils.put(jsonPrimitiveContent, CURR_BYTE_SIZE, 0);
            }
        }
        tempStr = jsonPrimitiveContent.optString(CREATOR, null);
        if (tempStr != null && !onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CREATOR cannot be updated");
            return;
        }

        Integer mni = jsonPrimitiveContent.optInt(ResourceContainer.MAX_NR_INSTANCES, -1);
        if (mni != -1 && mni > SYS_MAX_NR_INSTANCES) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                  "MAX_NR_INSTANCES(mni) exceeds system container limit: " + SYS_MAX_NR_INSTANCES);
            return;
        } else if (mni == -1 && onem2mRequest.isCreate) {
            // set maxNumberOfInstances to 1 by default
            JsonUtils.put(jsonPrimitiveContent, ResourceContainer.MAX_NR_INSTANCES, 1);
        }

        if (onem2mRequest.isCreate) {
            // initialize state tag to 0
            JsonUtils.put(jsonPrimitiveContent, BaseResource.STATE_TAG, 0);
            JsonUtils.put(jsonPrimitiveContent, CURR_NR_INSTANCES, 0);
            JsonUtils.put(jsonPrimitiveContent, CURR_BYTE_SIZE, 0);
        } else {
            // update the existing state tag as the resource is being updated
            int stateTag = onem2mRequest.getJsonResourceContent().optInt(BaseResource.STATE_TAG);
            JsonUtils.put(jsonPrimitiveContent, BaseResource.STATE_TAG, ++stateTag);
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

        parse(Onem2m.ResourceTypeString.CONTAINER);
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
            return;

        if (isJson()) {
            parseJsonCreateUpdateContent();
            if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
                return;
        }
        //CheckAccessControlProcessor.handleCreateUpdate(trc, onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
            return;
        processCommonCreateUpdateAttributes();
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
            return;
        processCreateUpdateAttributes();

    }

    public static void checkAndFixCurrMaxRules(JSONObject containerResourceContent) {

        // maintain an array of cin ids in the container
        String cinResouceIdJsonKey = "c:" + Onem2m.ResourceType.CONTENT_INSTANCE;
        JSONArray cinResouceIdJsonArray = containerResourceContent.optJSONArray(cinResouceIdJsonKey);
        JSONArray cinByteSizeJsonArray = containerResourceContent.optJSONArray(CIN_BYTES_SIZE_KEY);

        if (cinResouceIdJsonArray != null && cinByteSizeJsonArray != null) {
            if (cinResouceIdJsonArray.length() != cinByteSizeJsonArray.length()) {
                LOG.error("checkAndFixCurrMaxRules: resId len:{}, != byteSize len: {}",
                        cinResouceIdJsonArray.length(),
                        cinByteSizeJsonArray.length());
            }
        } else {
            return; // possibly no ci's created yet
        }

        // if there is a max nr instances, remove until curr nr instances == max/sys_limit
        Integer mni = containerResourceContent.optInt(ResourceContainer.MAX_NR_INSTANCES, -1);
        if (mni == -1) {
            mni = SYS_MAX_NR_INSTANCES;
        }
        int cni = containerResourceContent.optInt(ResourceContainer.CURR_NR_INSTANCES, -1);
        while (cni != -1 && cni > mni) {
            int delByteSize = cinByteSizeJsonArray.getInt(0); // 0 is oldest, N is latest
            JsonUtils.dec(containerResourceContent, ResourceContainer.CURR_NR_INSTANCES);
            JsonUtils.decN(containerResourceContent, ResourceContainer.CURR_BYTE_SIZE, delByteSize);
            Onem2mDb.getInstance().getBGDeleteProcessor().moveResourceToDeleteParent(cinResouceIdJsonArray.getString(0));
            cinResouceIdJsonArray.remove(0);
            cinByteSizeJsonArray.remove(0);
            cni = containerResourceContent.optInt(ResourceContainer.CURR_NR_INSTANCES, -1);
        }

        // if there is a max nr instances, remove until curr nr instances == max
        Integer mbs = containerResourceContent.optInt(ResourceContainer.MAX_BYTE_SIZE, -1);
        if (mbs != -1) {
            int cbs = containerResourceContent.optInt(ResourceContainer.CURR_BYTE_SIZE, -1);
            while (cbs != -1 && cbs > mbs) {
                int delByteSize = cinByteSizeJsonArray.getInt(0); // 0 is oldest, N is latest
                JsonUtils.dec(containerResourceContent, ResourceContainer.CURR_NR_INSTANCES);
                JsonUtils.decN(containerResourceContent, ResourceContainer.CURR_BYTE_SIZE, delByteSize);
                Onem2mDb.getInstance().getBGDeleteProcessor().moveResourceToDeleteParent(cinResouceIdJsonArray.getString(0));
                cinResouceIdJsonArray.remove(0);
                cinByteSizeJsonArray.remove(0);
                cbs = containerResourceContent.optInt(ResourceContainer.CURR_BYTE_SIZE, -1);
            }
        }
    }

    // update the parent container to track the new ci, plus maintain curr byte size and curr num instances
    public static void incrementValuesForThisCreatedContentInstance(JSONObject containerResourceContent,
                                                                    Integer newByteSize,
                                                                    String resourceId) {

        JsonUtils.inc(containerResourceContent, ResourceContainer.CURR_NR_INSTANCES);
        JsonUtils.inc(containerResourceContent, ResourceContainer.STATE_TAG);
        JsonUtils.incN(containerResourceContent, ResourceContainer.CURR_BYTE_SIZE, newByteSize);

        // maintain an array of cin ids in the container
        String arrayJsonKey = "c:" + Onem2m.ResourceType.CONTENT_INSTANCE;
        JSONArray jArray = containerResourceContent.optJSONArray(arrayJsonKey);
        if (jArray == null) {
            jArray = new JSONArray();
            jArray.put(resourceId);
            containerResourceContent.put(arrayJsonKey, jArray);
        } else {
            jArray.put(jArray.length(), resourceId);
        }

        // also maintain an array of the respective byte sizes
        JSONArray jCinCbsArray = containerResourceContent.optJSONArray(CIN_BYTES_SIZE_KEY);
        if (jCinCbsArray == null) {
            jCinCbsArray = new JSONArray();
            jCinCbsArray.put(newByteSize);
            containerResourceContent.put(CIN_BYTES_SIZE_KEY, jCinCbsArray);
        } else {
            jCinCbsArray.put(jCinCbsArray.length(), newByteSize);
        }
    }

    public static void decrementValuesForThisDeletedContentInstance(JSONObject containerResourceContent,
                                                                    String cinResourceId) {

        String cinResouceIdJsonKey = "c:" + Onem2m.ResourceType.CONTENT_INSTANCE;
        JSONArray cinResouceIdJsonArray = containerResourceContent.optJSONArray(cinResouceIdJsonKey);
        JSONArray cinByteSizeJsonArray = containerResourceContent.optJSONArray(CIN_BYTES_SIZE_KEY);

        if (cinResouceIdJsonArray != null && cinByteSizeJsonArray != null) {
            if (cinResouceIdJsonArray.length() != cinByteSizeJsonArray.length()) {
                LOG.error("decrementValuesForThisDeletedContentInstance: resId len:{}, != byteSize len: {}",
                        cinResouceIdJsonArray.length(),
                        cinByteSizeJsonArray.length());
            }
        } else {
            return; // possibly no ci's created yet
        }

        for (int i = 0; i < cinResouceIdJsonArray.length(); i++) {
            if (cinResourceId.equals(cinResouceIdJsonArray.getString(i))) {
                JsonUtils.inc(containerResourceContent, ResourceContainer.STATE_TAG);
                JsonUtils.dec(containerResourceContent, ResourceContainer.CURR_NR_INSTANCES);
                JsonUtils.decN(containerResourceContent, ResourceContainer.CURR_BYTE_SIZE, cinByteSizeJsonArray.getInt(i));
                cinResouceIdJsonArray.remove(i);
                cinByteSizeJsonArray.remove(i);
                break;
            }
        }
    }

    public static String getLatestCI(JSONObject containerResourceContent) {

        String cinResouceIdJsonKey = "c:" + Onem2m.ResourceType.CONTENT_INSTANCE;
        JSONArray cinResouceIdJsonArray = containerResourceContent.optJSONArray(cinResouceIdJsonKey);

        if (cinResouceIdJsonArray != null && cinResouceIdJsonArray.length() != 0) {
            return cinResouceIdJsonArray.getString(cinResouceIdJsonArray.length() - 1);
        }
        return null;
    }

    public static String getOldestCI(JSONObject containerResourceContent) {

        String cinResouceIdJsonKey = "c:" + Onem2m.ResourceType.CONTENT_INSTANCE;
        JSONArray cinResouceIdJsonArray = containerResourceContent.optJSONArray(cinResouceIdJsonKey);

        if (cinResouceIdJsonArray != null && cinResouceIdJsonArray.length() != 0) {
            return cinResouceIdJsonArray.getString(0);
        }
        return null;
    }
}
