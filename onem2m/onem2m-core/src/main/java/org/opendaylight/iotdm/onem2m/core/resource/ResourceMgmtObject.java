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

public class ResourceMgmtObject {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceMgmtObject.class);

    private ResourceMgmtObject() {
    }

    public static final String MGMT_DEFINITION = "mgd";
    public static final String OBJECT_ID = "obis";
    public static final String OBJECT_PATH = "obps";
    public static final String MGMT_LINK = "cmlk";
    public static final String DESCRIPTION = "dc";

    private static void parseJsonCreateUpdateContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        Iterator<?> keys = resourceContent.getInJsonContent().keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();

            resourceContent.jsonCreateKeys.add(key); // this line is new

            Object o = resourceContent.getInJsonContent().get(key);

            switch (key) {

            case MGMT_DEFINITION:
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
                break;
            case OBJECT_ID:
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
                break;
            case OBJECT_PATH:
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
                break;
            case MGMT_LINK:
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
                break;
            case DESCRIPTION:
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
                break;

            case Onem2m.Firmware.FIRMWARE_NAME:
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
                break;
            case Onem2m.Firmware.VERSION:
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
                break;
            case Onem2m.Firmware.URL:
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
                break;
            case Onem2m.Firmware.UPDATE:
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
                break;

            case ResourceContent.LABELS:
            case ResourceContent.EXPIRATION_TIME:
                if (!ResourceContent.parseJsonCommonCreateUpdateContent(key, resourceContent, onem2mResponse)) {
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

    public static void processCreateUpdateAttributes(ResourceTreeWriter twc, ResourceTreeReader trc, RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        if (onem2mRequest.isCreate) {
            String parentResourceType = onem2mRequest.getOnem2mResource().getResourceType();
            if (parentResourceType == null || !parentResourceType.contentEquals(Onem2m.ResourceType.NODE)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Cannot create MGMT OBJECT under this resource type: " + parentResourceType);
                return;
            }
        }
        // verify this resource can be created under the target resource
        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        /**
         * Check the mandotory attribtue's value
         */
        String mgd = resourceContent.getInJsonContent().optString(MGMT_DEFINITION, null);

        /**
         * Check the mandatory attribute's value
         */

        String fwr = resourceContent.getInJsonContent().optString(Onem2m.Firmware.FIRMWARE_NAME, null);

        String vr = resourceContent.getInJsonContent().optString(Onem2m.Firmware.VERSION, null);
        String ud = resourceContent.getInJsonContent().optString(Onem2m.Firmware.UPDATE, null);
        String url = resourceContent.getInJsonContent().optString(Onem2m.Firmware.URL, null);

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
            if (!Onem2mDb.getInstance().createResource(twc, trc, onem2mRequest, onem2mResponse)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR, "Cannot create in data store!");
                // TODO: what do we do now ... seems really bad ... keep stats
                return;
            }
        } else {
            if (!Onem2mDb.getInstance().updateResource(twc, trc, onem2mRequest, onem2mResponse)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR, "Cannot update the data store!");
                // TODO: what do we do now ... seems really bad ... keep stats
                return;
            }

        }
    }

    public static void handleCreateUpdate(ResourceTreeWriter twc, ResourceTreeReader trc, RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();
        /**
         * Need to add a new resource in the "Onem2m.ResourceTypeString";
         */
        resourceContent.parse(Onem2m.ResourceTypeString.MGMT_OBJECT, onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;

        if (resourceContent.isJson()) {
            parseJsonCreateUpdateContent(onem2mRequest, onem2mResponse);
            if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                return;
        }

        resourceContent.processCommonCreateUpdateAttributes(trc, onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;
        ResourceMgmtObject.processCreateUpdateAttributes(twc, trc, onem2mRequest, onem2mResponse);

    }
}

