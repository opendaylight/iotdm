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
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.router.Onem2mRouterService;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class services the AE resource CRUD operations.  It processes the onem2m request primitive.  It also
 * has to extract the AE attributes from the RequestPrimitive.CONTENT attr.  It is encoded in a string.
 * Based on the CONTENT_FORMAT, is is sent to the appropriate parser.  The Resource base class help with
 * some of this effort.
 */
public class ResourceCse extends BaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceCse.class);
    public ResourceCse(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        super(onem2mRequest, onem2mResponse);

    }

    // taken from CDT-cseBase-v1_0_0.xsd / TS0004_v_1-0_1 Section 8.2.2 Short Names
    // TODO: TS0001 9.6.3 ResourceType CSEBase

    public static final String CSE_TYPE = "cst";
    public static final String CSE_ID = "csi";
    public static final String SUPPORTED_RESOURCE_TYPES = "srt";
    public static final String NOTIFICATION_CONGESTION_POLICY = "ncp";
    public static final String POINT_OF_ACCESS = "poa";

    private void processCreateUpdateAttributes() {

        String cseId = jsonPrimitiveContent.optString(CSE_ID, null);
        if (cseId == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CSE_ID: missing parameter");
            return;
        }

        // hard code the supported resource types
        JSONArray a = new JSONArray();
        a.put(Integer.valueOf(Onem2m.ResourceType.CSE_BASE));
        a.put(Integer.valueOf(Onem2m.ResourceType.AE));
        a.put(Integer.valueOf(Onem2m.ResourceType.CONTAINER));
        a.put(Integer.valueOf(Onem2m.ResourceType.CONTENT_INSTANCE));
        a.put(Integer.valueOf(Onem2m.ResourceType.SUBSCRIPTION));
        a.put(Integer.valueOf(Onem2m.ResourceType.GROUP));
        a.put(Integer.valueOf(Onem2m.ResourceType.NODE));
        a.put(Integer.valueOf(Onem2m.ResourceType.ACCESS_CONTROL_POLICY));
        JsonUtils.put(jsonPrimitiveContent, SUPPORTED_RESOURCE_TYPES, a);
        /**
         * The resource has been filled in with any attributes that need to be written to the database
         */
        if (onem2mRequest.isCreate) {
            if (!Onem2mDb.getInstance().createCseResource(onem2mRequest, onem2mResponse)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR, "Cannot write to data store!");
                // TODO: what do we do now ... seems really bad ... keep stats
                return;
            }
        } else {
            if (!Onem2mDb.getInstance().updateResource(onem2mRequest, onem2mResponse)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR, "Cannot write to data store!");
                // TODO: what do we do now ... seems really bad ... keep stats
                return;
            }
        }

        /*
         * Update routing table with the changes
         */
        Onem2mRouterService.getInstance().updateRoutingTable(onem2mRequest);
    }

    private void parseJsonCreateUpdateContent() {

        Iterator<?> keys = jsonPrimitiveContent.keys();

        while( keys.hasNext() ) {

            String key = (String)keys.next();

            Object o = jsonPrimitiveContent.opt(key);

            switch (key) {

                case CSE_ID:
                case CSE_TYPE:
                case BaseResource.CREATION_TIME:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        if (!(o instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                            return;
                        }
                    }
                    break;
                case BaseResource.LABELS:
//                case ResourceContent.RESOURCE_NAME:
//                    // todo: can CSE be modified?
                    if (!parseJsonCommonCreateUpdateContent(key)) {
                        return;
                    }
                    break;
                case POINT_OF_ACCESS:
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
                default:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "CONTENT(" + RequestPrimitive.CONTENT + ") attribute not recognized: " + key);
                    return;
            }
        }
    }

    public void handleCreateUpdate() {

        parse(Onem2m.ResourceTypeString.CSE_BASE);
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
            return;

        if (isJson()) {
            parseJsonCreateUpdateContent();
            if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
                return;
        }
        processCommonCreateUpdateAttributes();
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
            return;

        processCreateUpdateAttributes();

    }

}
