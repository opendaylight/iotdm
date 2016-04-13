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
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceGroup;
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

        List<String> AccessControlPolicyIDList = new ArrayList<>();
        String targetURI = onem2mRequest.getPrimitive(RequestPrimitive.TO);
        // trace back acpid
        if (onem2mRequest.isCreate) {
            String acpi = onem2mRequest.getResourceContent().getInJsonContent().optString("acpi", null);
            if (acpi != null) {
                JSONArray acpiArray = onem2mRequest.getResourceContent().getInJsonContent().optJSONArray("acpi");
                for (int i = 0; i < acpiArray.length(); i++) {
                    AccessControlPolicyIDList.add(acpiArray.optString(i));
                }
                if (AccessControlPolicyIDList.size() == 0) {
                    // if there is no ACPID, do nothing
                    AccessControlPolicyIDList = getDefaultACPList(targetURI);
                }
            }
            // if create opertaion, onem2mresource in onem2mrequest is the parent resource.
            else if (onem2mRequest.getOnem2mResource().getResourceType().contentEquals(Onem2m.ResourceType.CSE_BASE)){
                AccessControlPolicyIDList = getDefaultACPList(targetURI);
            } else {
                // if target is not CSE, check whether the parent(onem2mresource) conatains acpid
                Onem2mResource parentresource = Onem2mDb.getInstance().getResource(onem2mRequest.getOnem2mResource().getParentId());
                try {
                    JSONObject jsonContent = new JSONObject(parentresource.getResourceContentJsonString());
                    while (jsonContent.optString(ResourceContent.ACCESS_CONTROL_POLICY_IDS, null) == null) {
                        if (parentresource.getResourceType().contentEquals(Onem2m.ResourceType.CSE_BASE)) {
                            String defaultACPID = Onem2mDb.getInstance().getChildResourceID(parentresource.getResourceId(),"_defaultACP");
                            String hierarchyURI = Onem2mDb.getInstance().getNonHierarchicalNameForResource(defaultACPID);
                            AccessControlPolicyIDList.add(hierarchyURI);
                            break;
                        } else {
                            parentresource = Onem2mDb.getInstance().getResource(parentresource.getParentId());
                            jsonContent = new JSONObject(parentresource.getResourceContentJsonString());
                        }
                    }
                    if (AccessControlPolicyIDList.isEmpty()) {
                        // find parent ACPID
                        JSONArray acpiArray = jsonContent.optJSONArray(ResourceContent.ACCESS_CONTROL_POLICY_IDS);
                        for (int i = 0; i < acpiArray.length(); i++) {
                            AccessControlPolicyIDList.add(acpiArray.getString(i));
                        }
                    }
                } catch (JSONException e) {
                    LOG.error("Invalid JSON {}", parentresource.getResourceContentJsonString(), e);
                    throw new IllegalArgumentException("Invalid JSON", e);
                }
            }
        } else {
            if (onem2mRequest.isUpdate && onem2mRequest.getResourceContent().getInJsonContent().optString("acpi", null) != null) {
                JSONArray acpiArray = onem2mRequest.getResourceContent().getInJsonContent().optJSONArray("acpi");
                for (int i = 0; i < acpiArray.length(); i++) {
                    AccessControlPolicyIDList.add(acpiArray.optString(i));
                }
                if (AccessControlPolicyIDList.size() == 0) {
                    // if there is no ACPID, do nothing
                    AccessControlPolicyIDList = getDefaultACPList(targetURI);
                }

            } else {
                // get self resource, onem2mresource inside request,
                Onem2mResource parentresource = onem2mRequest.getOnem2mResource();
                try {
                    JSONObject jsonContent = new JSONObject(parentresource.getResourceContentJsonString());
                    while (jsonContent.optString(ResourceContent.ACCESS_CONTROL_POLICY_IDS, null) == null) {
                        if (parentresource.getResourceType().contentEquals(Onem2m.ResourceType.CSE_BASE)) {
                            String defaultACPID = Onem2mDb.getInstance().getChildResourceID(parentresource.getResourceId(), "_defaultACP");
                            String hierarchyURI = Onem2mDb.getInstance().getNonHierarchicalNameForResource(defaultACPID);
                            AccessControlPolicyIDList.add(hierarchyURI);
                            break;
                        } else {
                            parentresource = Onem2mDb.getInstance().getResource(parentresource.getParentId());
                            jsonContent = new JSONObject(parentresource.getResourceContentJsonString());
                        }
                    }

                    if (AccessControlPolicyIDList.isEmpty()) {
                        JSONArray acpiArray = jsonContent.optJSONArray(ResourceContent.ACCESS_CONTROL_POLICY_IDS);
                        for (int i = 0; i < acpiArray.length(); i++) {
                            AccessControlPolicyIDList.add(acpiArray.optString(i));
                        }
                    }
                } catch (JSONException e) {
                    LOG.error("Invalid JSON {}", parentresource.getResourceContentJsonString(), e);
                    throw new IllegalArgumentException("Invalid JSON", e);
                }
            }
        }


        Boolean operation_is_allowed = false;
        Boolean orininator_is_allowed = false;
        String from = onem2mRequest.getPrimitive(RequestPrimitive.FROM);
        for (String accessControlPolicyID : AccessControlPolicyIDList) {
            Onem2mResource accessControlPolicyResource = Onem2mDb.getInstance().getResourceUsingURI(accessControlPolicyID);
            if (accessControlPolicyResource != null) {
                try {
                    JSONObject jsonContent = new JSONObject(accessControlPolicyResource.getResourceContentJsonString());
                    JSONObject pvJson = jsonContent.optJSONObject(ResourceAccessControlPolicy.PRIVILIEGES);
                    JSONArray acrArray = pvJson.optJSONArray(ResourceAccessControlPolicy.ACCESS_CONTROL_RULES);
                    for (int i = 0; i < acrArray.length(); i++) {
                        JSONObject acri = acrArray.optJSONObject(i);
                        // acor and acop are mandatory
                        JSONArray acorArray = acri.optJSONArray(ResourceAccessControlPolicy.ACCESS_CONTROL_ORIGINATORS);
                        BigInteger allowedOperation = BigInteger.valueOf(acri.optInt(ResourceAccessControlPolicy.ACCESS_CONTROL_OPERATIONS));
                        // first: check whether this operation is allowed
                        //todo: origin must be cseid or aeid or groupid? what about ip?
                        if (acorArray.toString().contains(from) || acorArray.toString().contains("*")) {
                            // second : check whether the "From" IP is allowed
                            orininator_is_allowed = true;
                            if (ResourceAccessControlPolicy.isAllowedThisOperation(opCode, allowedOperation)) {
                                operation_is_allowed = true;
                                break;
                            }
                        } else {
                            // check the originator ID one by one to see if it contains <Group>
                            for (int j = 0; j < acorArray.length(); j++) {
                                Onem2mResource testGroup = Onem2mDb.getInstance().getResource(acorArray.getString(j));
                                if (testGroup != null && testGroup.getResourceType().contentEquals(Onem2m.ResourceType.GROUP)) {

                                    try {
                                        JSONObject gourpjsonObj = new JSONObject(testGroup.getResourceContentJsonString());
                                        JSONArray memberIDlist = gourpjsonObj.optJSONArray(ResourceGroup.MEMBERS_IDS);
                                        if (memberIDlist.toString().contains(from)) {
                                            // second : check whether the "From" IP is allowed
                                            orininator_is_allowed = true;
                                            if (ResourceAccessControlPolicy.isAllowedThisOperation(opCode, allowedOperation)) {
                                                operation_is_allowed = true;
                                                break;
                                            }
                                        }
        //                                else {
        // current use must use AEID into the memberID
        //                                    // case: group memberID contains AE-resourceID, originator is AEID
        //                                }
                                    } catch (JSONException e) {
                                        LOG.error("Invalid JSON {}", testGroup.getResourceContentJsonString(), e);
                                        throw new IllegalArgumentException("Invalid JSON", e);
                                    }
                                }
                            }
                        }
                    }
                } catch (JSONException e) {
                    LOG.error("Invalid JSON {}", accessControlPolicyResource.getResourceContentJsonString(), e);
                    throw new IllegalArgumentException("Invalid JSON", e);
                }
            } else {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "CONTENT(" + RequestPrimitive.CONTENT + ") ACPID : " + accessControlPolicyID + " does not exist !! ");
                return;
            }
        }

        if (!orininator_is_allowed) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                    "Originator : " + from + " is invalid. ");
            return;
        }
        if (!operation_is_allowed) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                    "Operation : " + opCode + " not allowed. ");
            return;
        }

    }


    public static void handleSelfOperation(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse, String opCode) {

        Boolean operation_is_allowed = false;
        Boolean orininator_is_allowed = false;
        String from = onem2mRequest.getPrimitive(RequestPrimitive.FROM);
        JSONObject jsonContent;
        if (onem2mRequest.isCreate) {
            jsonContent = onem2mRequest.getResourceContent().getInJsonContent();
        } else {
            Onem2mResource accessControlPolicyResource = onem2mRequest.getOnem2mResource();
            try {
                jsonContent = new JSONObject(accessControlPolicyResource.getResourceContentJsonString());
            } catch (JSONException e) {
                LOG.error("Invalid JSON {}", accessControlPolicyResource.getResourceContentJsonString(), e);
                throw new IllegalArgumentException("Invalid JSON", e);
            }
        }
        JSONObject pvJson = jsonContent.optJSONObject(ResourceAccessControlPolicy.SELF_PRIIVLIEGES);
        JSONArray acrArray = pvJson.optJSONArray(ResourceAccessControlPolicy.ACCESS_CONTROL_RULES);
        for (int i = 0; i < acrArray.length(); i++) {
            JSONObject acri = acrArray.optJSONObject(i);
            // acor and acop are mandatory
            JSONArray acorArray = acri.optJSONArray(ResourceAccessControlPolicy.ACCESS_CONTROL_ORIGINATORS);
            BigInteger allowedOperation = BigInteger.valueOf(acri.optInt(ResourceAccessControlPolicy.ACCESS_CONTROL_OPERATIONS));
            // first: check whether this operation is allowed
            if (acorArray.toString().contains(from)) {
                // second : check whether the "From" IP is allowed
                orininator_is_allowed = true;
                if (ResourceAccessControlPolicy.isAllowedThisOperation(opCode, allowedOperation)) {
                    operation_is_allowed = true;
                    break;
                }
            }
        }

        if (!orininator_is_allowed) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                    "Originator : " + from + " is invalid. ");
            return;
        }
        if (!operation_is_allowed) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                    "Operation : " + opCode + " not allowed. ");
            return;
        }

    }

    public static void handleCreateUpdate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        if (onem2mRequest.isCreate) {
            handleOperation(onem2mRequest, onem2mResponse, Onem2m.Operation.CREATE);
        } else {
            handleOperation(onem2mRequest, onem2mResponse, Onem2m.Operation.UPDATE);
        }
    }

    public static void handleSelfCreateUpdate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        if (onem2mRequest.isCreate) {
            handleOperation(onem2mRequest, onem2mResponse, Onem2m.Operation.CREATE);
        } else {
            handleSelfOperation(onem2mRequest, onem2mResponse, Onem2m.Operation.UPDATE);
        }
    }

    public static void handleDelete(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        if (onem2mRequest.getOnem2mResource().getResourceType().contentEquals(Onem2m.ResourceType.ACCESS_CONTROL_POLICY)){
            handleSelfOperation(onem2mRequest, onem2mResponse, Onem2m.Operation.DELETE);
        } else {
            handleOperation(onem2mRequest, onem2mResponse, Onem2m.Operation.DELETE);
        }
    }

    public static void handleRetrieve(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        if (onem2mRequest.getOnem2mResource().getResourceType().contentEquals(Onem2m.ResourceType.ACCESS_CONTROL_POLICY)){
            handleSelfOperation(onem2mRequest, onem2mResponse, Onem2m.Operation.RETRIEVE);
        } else {
            handleOperation(onem2mRequest, onem2mResponse, Onem2m.Operation.RETRIEVE);
        }
    }

    public static List getDefaultACPList(String resourceURI) {
        List<String> AccessControlPolicyIDList = new ArrayList<>();
        String CSEid = Onem2mDb.getInstance().getCSEid(resourceURI);
        String defaultACPID = Onem2mDb.getInstance().getChildResourceID(CSEid,"_defaultACP");
        String hierarchyURI = Onem2mDb.getInstance().getNonHierarchicalNameForResource(defaultACPID);
        AccessControlPolicyIDList.add(hierarchyURI);
        return AccessControlPolicyIDList;
    }
}
