/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONObject;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.DbAttr;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResourceBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class services the AE resource CRUD operations.  It processes the onem2m request primitive.  It also
 * has to extract the AE attributes from the RequestPrimitive.CONTENT attr.  It is encoded in a string.
 * Based on the CONTENT_FORMAT, is is sent to the appropriate parser.  The Resource base class help with
 * some of this effort.
 */
public class ResourceCse {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceCse.class);
    private ResourceCse() {}

    // taken from CDT-cseBase-v1_0_0.xsd / TS0004_v_1-0_1 Section 8.2.2 Short Names
    // TODO: TS0001 9.6.3 ResourceType CSEBase

    public static final String CSE_TYPE = "cst";
    public static final String CSE_ID = "csi";
    public static final String SUPPORTED_RESOURCE_TYPES = "srt";
    public static final String NOTIFICATION_CONGESTION_POLICY = "ncp";

    // hard code set of acceptable create attributes, short and long name
    public static final Set<String> createAttributes = new HashSet<String>() {{
        // short; long
        add(ResourceContent.EXPIRATION_TIME); add("expirationTime");
        add(ResourceContent.CREATION_TIME); add("creationTime");
        add(ResourceContent.LABELS); add("labels");
        add(CSE_TYPE); add("cseType");
        add(CSE_ID); add("CSE-ID");
        add(SUPPORTED_RESOURCE_TYPES); add("supportedResourceType");
        add(NOTIFICATION_CONGESTION_POLICY); add("notificationCongestionPolicy");
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
        add(CSE_TYPE); add("cseType");
        add(CSE_ID); add("CSE-ID");
        add(SUPPORTED_RESOURCE_TYPES); add("supportedResourceType");
        add(NOTIFICATION_CONGESTION_POLICY); add("notificationCongestionPolicy");
    }
    };

    /**
     * The list<Attr> and List<AttrSet> must be filled in with the ContentPrimitive attributes
     * @param onem2mRequest
     * @param onem2mResponse
     */
    public static void handleCreate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        // TODO: validate CSE parameters
    }
}