/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import java.util.Iterator;

import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceMgmtObject extends BaseResource {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceMgmtObject.class);

    public ResourceMgmtObject(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        super(onem2mRequest, onem2mResponse);
    }

    public static final String MGMT_DEFINITION = "mgd";
    public static final String OBJECT_ID = "obis";
    public static final String OBJECT_PATH = "obps";
    public static final String MGMT_LINK = "cmlk";
    public static final String DESCRIPTION = "dc";

    private void parseJsonCreateUpdateContent() {
        
        Iterator<?> keys = jsonPrimitiveContent.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            
            Object o = jsonPrimitiveContent.get(key);

            switch (key) {

            case MGMT_DEFINITION:
                if (!jsonPrimitiveContent.isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
                break;
            case OBJECT_ID:
                if (!jsonPrimitiveContent.isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
                break;
            case OBJECT_PATH:
                if (!jsonPrimitiveContent.isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
                break;
            case MGMT_LINK:
                if (!jsonPrimitiveContent.isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
                break;
            case DESCRIPTION:
                if (!jsonPrimitiveContent.isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
                break;

            case Onem2m.Firmware.FIRMWARE_NAME:
                if (!jsonPrimitiveContent.isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
                break;
            case Onem2m.Firmware.VERSION:
                if (!jsonPrimitiveContent.isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
                break;
            case Onem2m.Firmware.URL:
                if (!jsonPrimitiveContent.isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
                break;
            case Onem2m.Firmware.UPDATE:
                if (!jsonPrimitiveContent.isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
                break;

            case BaseResource.LABELS:
            case BaseResource.EXPIRATION_TIME:
                if (!parseJsonCommonCreateUpdateContent(key)) {
                    return;
                }
                break;

            // todo: will need to add "announceTo" "announceAttribute" later,
            // currently we do not support that

            default:
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                        + RequestPrimitive.CONTENT + ") attribute not recognized: " + key);
                return;
            }
        }
    }

    public void processCreateUpdateAttributes() {

        if (onem2mRequest.isCreate) {
            Integer prt = onem2mRequest.getParentResourceType();
            if (prt != Onem2m.ResourceType.NODE) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Cannot create MGMT OBJECT under this resource type: " + prt);
                return;
            }
        }

        /**
         * Check the mandotory attribtue's value
         */
        String mgd = jsonPrimitiveContent.optString(MGMT_DEFINITION, null);

        /**
         * Check the mandatory attribute's value
         */

        String fwr = jsonPrimitiveContent.optString(Onem2m.Firmware.FIRMWARE_NAME, null);

        String vr = jsonPrimitiveContent.optString(Onem2m.Firmware.VERSION, null);
        String ud = jsonPrimitiveContent.optString(Onem2m.Firmware.UPDATE, null);
        String url = jsonPrimitiveContent.optString(Onem2m.Firmware.URL, null);

        switch (mgd) {
        case Onem2m.SpecializedResource.FIRMWARE:

            if (onem2mRequest.isCreate && (vr == null || ud == null || url == null || fwr == null)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "vr,ud,url or fwr is missing ");
                return;
            }
            break;
        default:

            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "Invalid parameters");
            return;

        }

        /**
         * The resource has been filled in with any attributes that need to be
         * written to the database
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

        /**
         * Need to add a new resource in the "Onem2m.ResourceTypeString";
         */
        parse(Onem2m.ResourceTypeString.MGMT_OBJECT);
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

