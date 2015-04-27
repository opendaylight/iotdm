/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.DbAttr;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Attr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceContainer {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceContainer.class);
    private ResourceContainer() {}

    // taken from CDT-container-v1_0_0.xsd / TS0004_v_1-0_1 Section 8.2.2 Short Names
    // TODO: ts0001 9.6.6-2


    public static final String CREATOR = "cr";
    public static final String MAX_NR_INSTANCES = "mni";
    public static final String MAX_BYTE_SIZE = "mbs";
    public static final String MAX_INSTANCE_AGE = "mia";
    public static final String CURR_NR_INSTANCES = "cni";
    public static final String CURR_BYTE_SIZE = "cbs";
    //public static final String LOCATION_ID = "li";
    public static final String ONTOLOGY_REF = "or";
    public static final String LATEST = "la"; // <-- container: head/tail, ci: next/prev
    public static final String OLDEST = "oldest"; // TODO: no short name defined

    // hard code set of acceptable create attributes, short and long name
    public static final Set<String> createAttributes = new HashSet<String>() {{
        // short; long
        add(ResourceContent.EXPIRATION_TIME); add("expirationTime");
        add(ResourceContent.CREATION_TIME); add("creationTime");
        add(CREATOR); add("creator");
        add(ResourceContent.LABELS); add("labels");
        add(MAX_NR_INSTANCES); add("maxNrOfInstances");
        add(MAX_BYTE_SIZE); add("maxByteSize");
        add(MAX_INSTANCE_AGE); add("maxInstanceAge");
        add(ONTOLOGY_REF); add("ontologyRef");
    }};

    // hard code set of acceptable retrieve attributes, short and long name
    public static final Set<String> retrieveAttributes = new HashSet<String>() {{
        // short; long
        add(ResourceContent.RESOURCE_TYPE); add("resourceType");
        add(ResourceContent.RESOURCE_ID); add("resourceID");
        add(ResourceContent.RESOURCE_NAME); add("resourceName");
        add(ResourceContent.PARENT_ID); add("parentID");
        add(ResourceContent.EXPIRATION_TIME); add("expirationTime");
        add(ResourceContent.CREATION_TIME); add("creationTime");
        add(ResourceContent.LAST_MODIFIED_TIME); add("lastModifiedTime");
        add(ResourceContent.LABELS); add("labels");
        add(ResourceContent.STATE_TAG); add("stateTag");
        add(MAX_NR_INSTANCES); add("maxNrOfInstances");
        add(MAX_BYTE_SIZE); add("maxByteSize");
        add(MAX_INSTANCE_AGE); add("maxInstanceAge");
        add(CURR_NR_INSTANCES); add("currNrOfInstances");
        add(CURR_BYTE_SIZE); add("currByteSize");
        add(ONTOLOGY_REF); add("ontologyRef");
    }
    };

    /**
     * This routine processes the JSON content for this resource representation.  Ideally, a json schema file would
     * be used so that each json key could be looked up in the json schema to find out what type it is, and so forth.
     * Maybe the next iteration of code, I'll create json files for each resource.
     * @param onem2mRequest
     * @param onem2mResponse
     */
    private static void processJsonCreateUpdateContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        Iterator<?> keys = resourceContent.getJsonContent().keys();
        while( keys.hasNext() ) {
            String key = (String)keys.next();

            Object o = resourceContent.getJsonContent().get(key);

            switch (key) {
                case MAX_NR_INSTANCES:
                case MAX_BYTE_SIZE:
                case MAX_INSTANCE_AGE:
                    if (!resourceContent.getJsonContent().isNull(key)) {
                        if (!(o instanceof Integer)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") number expected for json key: " + key);
                            return;
                        } else if (((Integer) o).intValue() < 0) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") integer must be non-negative: " + key);
                            return;
                        }
                        resourceContent.setDbAttr(key, o.toString());

                    } else {
                        resourceContent.setDbAttr(key, null);
                    }
                    break;

                case ONTOLOGY_REF:
                case CREATOR:
                case ResourceContent.EXPIRATION_TIME:
                    if (!resourceContent.getJsonContent().isNull(key)) {
                        if (!(o instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                            return;
                        }
                        resourceContent.setDbAttr(key, o.toString());
                    } else {
                        resourceContent.setDbAttr(key, null);
                    }
                    break;
                case ResourceContent.LABELS:
                    if (!resourceContent.getJsonContent().isNull(key)) {
                        if (!(o instanceof JSONArray)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") array expected for json key: " + key);
                            return;
                        }
                        JSONArray array = (JSONArray) o;
                        for (int i = 0; i < array.length(); i++) {
                            if (!(array.get(i) instanceof String)) {
                                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                        "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json array: " + key);
                                return;
                            }
                            //resourceContent.setDbAttr(key, array.get(i));
                        }
                    } else {
                        //resourceContent.setDbAttr(key, null);
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
     * @param onem2mRequest
     * @param onem2mResponse
     */
    public static void processCreateUpdateAttributes(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        String tempStr;
        Integer tempInt;

        // verify this resource can be created under the target resource
        if (onem2mRequest.isCreate) {
            DbAttr parentDbAttrs = onem2mRequest.getDbAttrs();
            String rt = parentDbAttrs.getAttr(ResourceContent.RESOURCE_TYPE);
            if (rt == null || !(rt.contentEquals(Onem2m.ResourceType.CSE_BASE) ||
                    rt.contentEquals(Onem2m.ResourceType.CONTAINER) ||
                    rt.contentEquals(Onem2m.ResourceType.AE))) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Cannot create Container under this resource type: " + rt);
                return;
            }
        }

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        tempStr = resourceContent.getDbAttr(CREATOR);
        if (tempStr == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CREATOR missing parameter");
            return;
        }

        // TODO: need to support updating these parameters

        // initialize state tag to 0
        tempStr = resourceContent.getDbAttr(ResourceContent.STATE_TAG);
        if (tempStr != null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "STATE_TAG read-only parameter");
            return;
        }
        tempInt = 0;
        resourceContent.setDbAttr(ResourceContent.STATE_TAG, tempInt.toString());

        // initialize currNrOfInstances to 0
        tempStr = resourceContent.getDbAttr(CURR_NR_INSTANCES);
        if (tempStr != null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CURR_NR_INSTANCES read-only parameter");
            return;
        }
        tempInt = 0;
        resourceContent.setDbAttr(CURR_NR_INSTANCES, tempInt.toString());

        // initialize currByteSize to 0
        tempStr = resourceContent.getDbAttr(CURR_BYTE_SIZE);
        if (tempStr != null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CURR_BYTE_SIZE read-only parameter");
            return;
        }
        tempInt = 0;
        resourceContent.setDbAttr(CURR_BYTE_SIZE, tempInt.toString());

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
     * Parse the CONTENT resource representation.
     * @param onem2mRequest
     * @param onem2mResponse
     */
    public static void handleCreateUpdate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        resourceContent.parse(onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;

        if (resourceContent.isJson()) {
            processJsonCreateUpdateContent(onem2mRequest, onem2mResponse);
            if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                return;
        }
        resourceContent.processCommonCreateUpdateAttributes(onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                    return;
        ResourceContainer.processCreateUpdateAttributes(onem2mRequest, onem2mResponse);

    }

    /**
     * Generate JSON for this resource
     * @param onem2mResource this resource
     * @param j JSON obj to put teh formatted json into
     */
    public static void produceJsonForResource(Onem2mResource onem2mResource, JSONObject j) {

        for (Attr attr : onem2mResource.getAttr()) {
            switch (attr.getName()) {
                case CREATOR:
                case ONTOLOGY_REF:
                    j.put(attr.getName(), attr.getValue());
                    break;
                case MAX_NR_INSTANCES:
                case MAX_BYTE_SIZE:
                case MAX_INSTANCE_AGE:
                case CURR_NR_INSTANCES:
                case CURR_BYTE_SIZE:
                    j.put(attr.getName(), Integer.valueOf(attr.getValue()));
                    break;
                default:
                    ResourceContent.produceJsonForCommonAttributes(attr, j);
                    break;
            }
        }
        for (AttrSet attrSet : onem2mResource.getAttrSet()) {
            switch (attrSet.getName()) {
                default:
                    ResourceContent.produceJsonForCommonAttributeSets(attrSet, j);
                    break;
            }
        }
    }

    /**
     * When a contentInstance is created, the container must be check to see if it OK to add this new resource.
     * For now, teh new byte size is checked against the curr byte size for the current set of content instance
     * resources in this container.  Also, the number of resources is also checked.
     *
     * Future: maxAge ...
     *
     * @param containerDbAttrs parent container of the content instance
     * @param newByteSize new object size
     * @param onem2mResponse response
     * @return validity
     */
    public static boolean validateNewContentInstance(DbAttr containerDbAttrs,
                                                     Integer newByteSize,
                                                     ResponsePrimitive onem2mResponse) {
        String tempStr;

        tempStr = containerDbAttrs.getAttr(ResourceContainer.MAX_BYTE_SIZE);
        if (tempStr != null) {
            Integer mbs = Integer.valueOf(tempStr);
            tempStr = containerDbAttrs.getAttr(ResourceContainer.CURR_BYTE_SIZE);
            if (tempStr != null) {
                Integer cbs = Integer.valueOf(tempStr);
                if (cbs + newByteSize > mbs) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                            "Cannot create ContentInstance, max bytes size exceeded: curr:" + cbs +
                                    " new: " + newByteSize + " max: " + mbs);
                    return false;
                }
            }
        }

        tempStr = containerDbAttrs.getAttr(ResourceContainer.MAX_NR_INSTANCES);
        if (tempStr != null) {
            Integer mnr = Integer.valueOf(tempStr);
            tempStr = containerDbAttrs.getAttr(ResourceContainer.CURR_NR_INSTANCES);
            if (tempStr != null) {
                Integer cnr = Integer.valueOf(tempStr);
                if (cnr + 1 > mnr) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                            "Cannot create ContentInstance, max number of instances exceeded: " + mnr);
                    return false;
                }
            }
        }
        
        return true;
    }

    /**
     * This routine processes the JSON content for this resource representation.  Ideally, a json schema file would
     * be used so that each json key could be looked up in the json schema to find out what type it is, and so forth.
     * Maybe the next iteration of code, I'll create json files for each resource.
     *
     * This routine enforces the mandatory and option parameters
     * @param onem2mRequest
     * @param onem2mResponse
     */
    private static void processJsonRetrieveContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        Iterator<?> keys = resourceContent.getJsonContent().keys();
        while( keys.hasNext() ) {
            String key = (String)keys.next();

            Object o = resourceContent.getJsonContent().get(key);

            switch (key) {

                case ONTOLOGY_REF:
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                    resourceContent.setDbAttr(key, o.toString());
                    break;

                case MAX_NR_INSTANCES:
                case MAX_BYTE_SIZE:
                case MAX_INSTANCE_AGE:
                case CURR_NR_INSTANCES:
                case CURR_BYTE_SIZE:
                    if (!(o instanceof Integer)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") integer expected for json key: " + key);
                        return;
                    }
                    resourceContent.setDbAttr(key, o.toString());
                    break;
                default:
                    if (!ResourceContent.processJsonCommonRetrieveContent(key, resourceContent, onem2mResponse)) {
                        return;
                    }
                    break;
            }
        }
    }

    /**
     * Parse the CONTENT resource representation.
     * @param onem2mRequest
     * @param onem2mResponse
     */
    public static void handleRetrieve(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        resourceContent.parse(onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;

        if (resourceContent.isJson()) {
            processJsonRetrieveContent(onem2mRequest, onem2mResponse);
            if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                return;
        }
    }
}