/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.rest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceAccessControlPolicy;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class CheckAccessControlProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(CheckAccessControlProcessor.class);

    private CheckAccessControlProcessor() {}


    public static void handleOperation(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse, String opCode) {

        //Object pc = onem2mRequest.getResourceContent().getInJsonContent();
        // any resource may contain a default acpi
        String acpi = onem2mRequest.getResourceContent().getInJsonContent().optString("acpi", null);
        if (acpi == null) {
            // dafault acpid here? otherwise never goes here
            return;
        }
        JSONArray acpiArray = onem2mRequest.getResourceContent().getInJsonContent().optJSONArray("acpi");
        if (acpiArray != null) {
            List<String> AccessControlPolicyIDList = new ArrayList<>();
            for (int i = 0; i < acpiArray.length(); i++) {
                AccessControlPolicyIDList.add(acpiArray.optString(i));
            }
            if (AccessControlPolicyIDList.size() == 0) {
                // if there is no ACPID, do nothing
                return;
            }
            // todo: check ACP type
            Boolean operation_is_allowed = false;
            for (String accessControlPolicyID : AccessControlPolicyIDList) {
                Onem2mResource accessControlPolicyResource = Onem2mDb.getInstance().getResource(accessControlPolicyID);
                if (accessControlPolicyResource != null) {
                    JSONObject jsonContent = null;
                    try {
                        jsonContent = new JSONObject(accessControlPolicyResource.getResourceContentJsonString());
                    } catch (JSONException e) {
                        throw new IllegalArgumentException("Invalid JSON", e);
                    }
                    JSONObject pvJson = jsonContent.optJSONObject(ResourceAccessControlPolicy.PRIVILIEGES);
                    JSONArray acrArray = pvJson.optJSONArray(ResourceAccessControlPolicy.ACCESS_CONTROL_RULES);
                    for (int i = 0; i < acrArray.length(); i++) {
                        JSONObject acri = acrArray.optJSONObject(i);
                        // acor and acop are mandatory
                        JSONArray acorArray = acri.optJSONArray(ResourceAccessControlPolicy.ACCESS_CONTROL_ORIGINATORS);
                        BigInteger allowedOperation = BigInteger.valueOf(acri.optInt(ResourceAccessControlPolicy.ACCESS_CONTROL_OPERATIONS));
                        // first: check whether this operation is allowed
                        String from = onem2mRequest.getPrimitive(RequestPrimitive.FROM);
                        if (acorArray.toString().contains(from)) {
                            // second : check whether the "From" IP is allowed
                            if (ResourceAccessControlPolicy.isAllowedThisOperation(opCode, allowedOperation)) {
                                operation_is_allowed = true;
                                break;
                            }
                        }
                    }
                } else {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "CONTENT(" + RequestPrimitive.CONTENT + ") ACPID : " + accessControlPolicyID + " does not exist !! ");
                    return;
                }
            }

            if (!operation_is_allowed) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "Operation : " + opCode + " not allowed. ");
                return;
            }
        }


    }

    public static void handleCreate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        handleOperation(onem2mRequest, onem2mResponse, Onem2m.Operation.CREATE);
    }

    public static void handleUpdate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        handleOperation(onem2mRequest, onem2mResponse, Onem2m.Operation.UPDATE);
    }

    public static void handleRetrieve(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        handleOperation(onem2mRequest, onem2mResponse, Onem2m.Operation.UPDATE);
    }
}
