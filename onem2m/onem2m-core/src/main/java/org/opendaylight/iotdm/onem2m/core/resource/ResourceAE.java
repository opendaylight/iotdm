/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.DbAttr;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.Onem2mDateTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.primitive.list.Onem2mPrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Attr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class services the AE resource CRUD operations.  It processes the onem2m request primitive.  It also
 * has to extract the AE attributes from the RequestPrimitive.CONTENT attr.  It is encoded in a string.
 * Based on the CONTENT_FORMAT, it is sent to the appropriate parser.  The PrimitiveUtils class helps with
 * some of this parsing effort.  The CRUD/N operations ensure the validation of the parameters is done.
 */
public class ResourceAE {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceAE.class);
    private ResourceAE() {}

    // TODO: TS0004_1.0.1  7.3.5 CRUD/N procedures for AE
    // TODO: TS0001_1.5.0 9.6.5 attribute definitions
    // TODO: CDT-AE-v1_0_0.xsd / TS0004_1.0.1 Section 8.2.2 Short Names

    public static final String APP_NAME = "apn";
    public static final String APP_ID = "api";
    public static final String AE_ID = "aei";
    public static final String POINT_OF_ACCESS = "apn";
    public static final String ONTOLOGY_REF = "or";
    public static final String NODE_LINK = "nl"; // do not support node resource yet

    // hard code set of acceptable create attributes, short and long name
    public static final Set<String> createAttributes = new HashSet<String>() {{
        // short; long
        add(ResourceContent.EXPIRATION_TIME); add("expirationTime");
        add(ResourceContent.CREATION_TIME); add("creationTime");
        add(ResourceContent.LABELS); add("labels");
        add(APP_NAME); add("appName");
        add(APP_ID); add("App-ID");
        add(AE_ID); add("AE-ID");
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
        add(APP_NAME); add("appName");
        add(APP_ID); add("App-ID");
        add(AE_ID); add("AE-ID");
        add(ONTOLOGY_REF); add("ontologyRef");
    }
    };

    /**
     * The list<Attr> and List<AttrSet> must be filled in with the ContentPrimitive attributes
     * @param onem2mRequest
     * @param onem2mResponse
     */
    public static void handleCreate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        /**
         * When the parentURI was located in the tree, the attr list was read, this puts it into a class
         * so that each attribute can be easily accessed
         */
        DbAttr parentDbAttrs = onem2mRequest.getDbAttrs();

        /**
         * The only resource type that can be the parent according to TS0001 9.6.1.1-1 is a cseBase
         */
        String rt = parentDbAttrs.getAttr(ResourceContent.RESOURCE_TYPE);
        if (rt == null || !rt.contentEquals(Onem2m.ResourceType.CSE_BASE)) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                    "Cannot create AE under this resource type: " + rt);
            return;
        }

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        String appId = resourceContent.getDbAttr(APP_ID);
        if (appId == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "APP_ID missing parameter");
            return;
        }

        String aeId = resourceContent.getDbAttr(AE_ID);
        if (aeId == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "AE_ID missing parameter");
            return;
        }
    }
}
