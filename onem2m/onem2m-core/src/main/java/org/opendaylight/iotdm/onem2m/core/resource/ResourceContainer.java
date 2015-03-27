/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.DbAttr;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
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
    public static final String LOCATION_ID = "li";
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
        add(LOCATION_ID); add("locationID");
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
        add(LOCATION_ID); add("locationID");
        add(ONTOLOGY_REF); add("ontologyRef");
    }
    };

    /**
     * When create a container, add two special names as children, one is latest, the other is oldest, and
     * the values will initially be "".  When the first contentInstance is added under the container, then
     * point the oldest and latest to this new resource, and the new contentInstance, add two new attrs,
     * prev, next with values "".  These 4 special attrs will be head, tail, prev, next for a doubly LL.
     */

    /**
     * The list<Attr> and List<AttrSet> must be filled in with the ContentPrimitive attributes
     * @param onem2mRequest
     * @param onem2mResponse
     */
    public static void handleCreate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        String tempStr;
        Integer tempInt;
        /**
         * When the parentURI was located in the tree, the attr list was read, this puts it into a class
         * so that each attribute can be easily accessed
         */
        DbAttr parentDbAttrs = onem2mRequest.getDbAttrs();

        /**
         * The only resource type that can be the parent according to TS0001 9.6.1.1-1 is a cseBase
         */
        String rt = parentDbAttrs.getAttr(ResourceContent.RESOURCE_TYPE);
        if (rt == null || !(rt.contentEquals(Onem2m.ResourceType.CSE_BASE) ||
                           rt.contentEquals(Onem2m.ResourceType.CONTAINER) ||
                           rt.contentEquals(Onem2m.ResourceType.AE))) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                    "Cannot create Container under this resource type: " + rt);
            return;
        }

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        tempStr = resourceContent.getDbAttr(CREATOR);
        if (tempStr == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CREATOR missing parameter");
            return;
        }

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

        // note for update, if updating maxNrOfInstances and the other max, then a side affect of possibly deleting
        // some contentInstances might have to occur
    }
}