/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.core.rest.CheckAccessControlProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class ResourceNode extends BaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceNode.class);
    public ResourceNode(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        super(onem2mRequest, onem2mResponse);
    }
    /**
     * These attributes are "specific" attributes only belong to ResourceNode.
     * Some other common attributes can be found in file "ResourceContent" at the same folder.
     */
    public static final String NODE_ID = "ni";
    public static final String HOSTED_CSEID = "hci";
    
    private void parseJsonCreateUpdateContent() {
        
        Iterator<?> keys = jsonPrimitiveContent.keys();
        while( keys.hasNext() ) {
            String key = (String)keys.next();
            
            Object o = jsonPrimitiveContent.opt(key);

            switch (key) {

                case NODE_ID:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        if (!(o instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                            return;
                        }
                    }
                    break;
                case HOSTED_CSEID:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        if (!(o instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                            return;
                        }
                    }
                    break;
                case BaseResource.LABELS:
                case BaseResource.EXPIRATION_TIME:
                case BaseResource.RESOURCE_NAME:
                    if (!parseJsonCommonCreateUpdateContent(key)) {
                        return;
                    }
                    break;

                //todo: will need to add "announceTo" "announceAttribute" later, currently we do not support that

                default:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "CONTENT(" + RequestPrimitive.CONTENT + ") attribute not recognized: " + key);
                    return;
            }
        }
    }

    public void processCreateUpdateAttributes() {


        // verify this resource can be created under the target resource
        BaseResource baseResource = onem2mRequest.getBaseResource();
        /**
         * see other resources example to see how to check more complicated case
         */
        if (onem2mRequest.isCreate) {
            // target resourceType
            Integer prt = onem2mRequest.getParentResourceType();
            if (prt != Onem2m.ResourceType.CSE_BASE) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Cannot create Node under this resource type: " + prt);
                return;
            }
        }

        /**
         * Check the mandotory attribtue's value
         */
        String ni = jsonPrimitiveContent.optString(NODE_ID, null);
        if (ni == null && onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "NODE_ID missing parameter");
            return;
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

    public void handleCreateUpdate() {

        parse(Onem2m.ResourceTypeString.NODE);
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
