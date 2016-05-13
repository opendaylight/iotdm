/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;


import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.CheckAccessControlProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.IPAddressVidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ResourceAccessControlPolicy {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceAccessControlPolicy.class);
    private ResourceAccessControlPolicy() {}

    // taken from 2.2 xsd / TS0004_v_1-0_1 Section 8.2.2 Short Names
    // TODO:
    public static final String PRIVILIEGES = "pv";
    public static final String SELF_PRIIVLIEGES = "pvs";
    public static final String ACCESS_CONTROL_RULES = "acr";
    public static final String ACCESS_CONTROL_ORIGINATORS = "acor";
    public static final String ACCESS_CONTROL_CONTEXTS = "acco";
    public static final String ACCESS_CONTROL_OPERATIONS = "acop";
    public static final String ACCESS_CONTROL_IP_ADDRESSES = "acip";
    public static final String IP_V4_ADDRESSES = "ipv4";
    public static final String IP_V6_ADDRESSES = "ipv6";
    public static final String ACCESS_CONTROL_LOCATION_REGION = "aclr";
    public static final String COUNTRY_CODE = "accc";
    public static final String CIRC_REGION = "accr";
    public static final String ACCESS_CONTROL_WINDOW = "actw";
    /**
     * This routine processes the JSON content for this resource representation.  Ideally, a json schema file would
     * be used so that each json key could be looked up in the json schema to find out what type it is, and so forth.
     * Maybe the next iteration of code, I'll create json files for each resource.
     * @param onem2mRequest
     * @param onem2mResponse
     */
    private static void parseJsonCreateUpdateContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        Iterator<?> keys = resourceContent.getInJsonContent().keys();
        while( keys.hasNext() ) {
            String key = (String)keys.next();
            resourceContent.jsonCreateKeys.add(key);
            Object s = resourceContent.getInJsonContent().opt(key);

            switch (key) {
            case PRIVILIEGES:
            case SELF_PRIIVLIEGES:
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    // privileges contains one acr, acr is a list, this is ACR layer
                    if (!(s instanceof JSONObject)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "PRIVILEGES(" + PRIVILIEGES + ") Object expected for json key: " + key);
                        return;
                    }
                    JSONObject acrObject = (JSONObject) s;
                    Iterator<?> PVkeys = acrObject.keys();
                    while (PVkeys.hasNext()){
                        String PVkey = (String)PVkeys.next();
                        Object o = acrObject.opt(PVkey);
                        switch (PVkey){
                        case ACCESS_CONTROL_RULES:
                            if (!(o instanceof JSONArray)) {
                                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                        "PRIVILEGES(" + PRIVILIEGES + ") array expected for json key: " + key);
                                return;
                            }
                            // this line below can create acp as a single string
                            JSONArray arrayRule = (JSONArray) o;
                            for (int i = 0; i < arrayRule.length(); i++){
                                JSONObject acRule = arrayRule.optJSONObject(i);
                                Iterator<?> ruleKeys = acRule.keys();
                                while(ruleKeys.hasNext()){
                                    String ruleKey = (String)ruleKeys.next();
                                    Object p = acRule.opt(ruleKey);
                                    switch (ruleKey){
                                    // what if shown more than once?
                                    case ACCESS_CONTROL_ORIGINATORS:
                                        if (!(p instanceof JSONArray)) {
                                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                    "PRIVILEGES(" + ACCESS_CONTROL_ORIGINATORS + ") array expected for json key: " + ruleKey);
                                            return;
                                        }
                                        JSONArray arrayP = (JSONArray) p;
                                        List<String> originatorsList = new ArrayList<>();
                                        for (int k = 0; k < arrayP.length(); k++) {
                                            if (!(arrayP.opt(k) instanceof String)) {
                                                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                        "PRIVILEGES(" + ACCESS_CONTROL_ORIGINATORS + ") string expected for json array: " + ruleKey);
                                                return;
                                            }
                                            originatorsList.add(arrayP.optString(k));
                                        }
                                        break;
                                    case ACCESS_CONTROL_CONTEXTS:
                                        // this is a list, this layer similar to accessControlRule
                                        if (!acRule.isNull(ruleKey)) {
                                            if (!(p instanceof JSONArray)) {
                                                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                        "PRIVILEGES(" + ACCESS_CONTROL_CONTEXTS + ") array expected for json key: " + ruleKey);
                                                return;
                                            }
                                            JSONArray arrayContext = (JSONArray) p;

                                            for (int k = 0; k < arrayContext.length(); k++){
                                                JSONObject acContext = arrayContext.optJSONObject(k);
                                                Iterator<?> contextKeys = acContext.keys();
                                                while(contextKeys.hasNext()){
                                                    String contextKey = (String)contextKeys.next();
                                                    Object q = acContext.opt(contextKey);
                                                    switch (contextKey){
                                                    // what if shown more than once?
                                                    case ACCESS_CONTROL_WINDOW:
                                                        if (!(q instanceof JSONArray)) {
                                                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                                    "PRIVILEGES(" + ACCESS_CONTROL_WINDOW + ") array expected for json key: " + contextKey);
                                                            return;
                                                        }
                                                        JSONArray arrayWindow = (JSONArray) q;
                                                        List<String> windowList = new ArrayList<>();
                                                        for (int x = 0; x < arrayWindow.length(); x++) {
                                                            if (!(arrayWindow.opt(x) instanceof String)) {
                                                                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                                        "PRIVILEGES(" + ACCESS_CONTROL_WINDOW + ") string expected for json array: " + contextKey);
                                                                return;
                                                            }
                                                            windowList.add(arrayWindow.optString(x));
                                                        }
                                                        break;
                                                    case ACCESS_CONTROL_IP_ADDRESSES:
                                                        // this is a container, this layer similar to privilege
                                                        // containes 2 type of string array, ipv4 and ipv6
                                                        if (!acContext.isNull(contextKey)) {
                                                            JSONObject acIPAddresses = (JSONObject) q;
                                                            Iterator<?> IpAddressKeys = acIPAddresses.keys();
                                                            while (IpAddressKeys.hasNext()) {
                                                                String IpAddressKey = (String) IpAddressKeys.next();
                                                                Object r = acIPAddresses.opt(IpAddressKey);
                                                                switch (IpAddressKey) {
                                                                case IP_V4_ADDRESSES:
                                                                    // assume ip-v4 must contain an array, cannot be null
                                                                    if (!(r instanceof JSONArray)) {
                                                                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                                                "PRIVILEGES(" + IP_V4_ADDRESSES+ ") array expected for json key: " + IpAddressKey);
                                                                        return;
                                                                    }
                                                                    List<String> ipv4List = new ArrayList<>();
                                                                    JSONArray ipv4Array = (JSONArray) r;
                                                                    for (int j = 0; j < ipv4Array.length();j ++){
                                                                        if (!(ipv4Array.opt(j) instanceof String)) {
                                                                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                                                    "PRIVILEGES(" + IP_V4_ADDRESSES + ") string expected for json array: " + IpAddressKey);
                                                                            return;
                                                                        } else {
                                                                            String ipv4Address = ipv4Array.optString(j);
                                                                            if (!IPAddressVidator.isIpv4Address(ipv4Address)) {
                                                                                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                                                        "PRIVILEGES("  + IP_V4_ADDRESSES+ ") : "  + ipv4Address+ " is not a valid Ipv4 address.");
                                                                                return;
                                                                            }
                                                                        }
                                                                        ipv4List.add(ipv4Array.optString(j));
                                                                    }
                                                                    break;
                                                                case IP_V6_ADDRESSES:
                                                                    // assume ip-v6 must contain an array, cannot be null
                                                                    if (!(r instanceof JSONArray)) {
                                                                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                                                "PRIVILEGES(" + IP_V6_ADDRESSES+ ") array expected for json key: " + IpAddressKey);
                                                                        return;
                                                                    }
                                                                    List<String> ipv6List = new ArrayList<>();
                                                                    JSONArray ipv6Array = (JSONArray) r;
                                                                    for (int j = 0; j < ipv6Array.length();j ++){
                                                                        if (!(ipv6Array.opt(j) instanceof String)) {
                                                                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                                                    "PRIVILEGES(" + IP_V6_ADDRESSES + ") string expected for json array: " + IpAddressKey);
                                                                            return;
                                                                        } else {
                                                                            String ipv6Address = ipv6Array.optString(j);
                                                                            if (!IPAddressVidator.isIpv6Address(ipv6Address)) {
                                                                                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                                                        "PRIVILEGES("  + IP_V6_ADDRESSES+ ") : " + ipv6Address+ " is not a valid Ipv6 address.");
                                                                                return;
                                                                            }
                                                                        }
                                                                        ipv6List.add(ipv6Array.optString(j));
                                                                    }
                                                                    break;
                                                                default:
                                                                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                                            "PRIVILEGES(" + ACCESS_CONTROL_IP_ADDRESSES  + ") attribute not recognized: " + IpAddressKey);
                                                                    return;
                                                                }
                                                            }
                                                            break;
                                                        }
                                                        break;
                                                    case ACCESS_CONTROL_LOCATION_REGION:
                                                        // this is a container, similar to IPAddresses
                                                        if (!acContext.isNull(contextKey)) {
                                                            JSONObject acLocationRegion = (JSONObject) q;
                                                            Iterator<?> LocationRegionKeys = acLocationRegion.keys();
                                                            while (LocationRegionKeys.hasNext()) {
                                                                String locationRegionKey = (String) LocationRegionKeys.next();
                                                                Object r = acLocationRegion.opt(locationRegionKey);
                                                                switch (locationRegionKey) {
                                                                case CIRC_REGION:
                                                                    // assume ip-v4 must contain an array, cannot be null
                                                                    if (!(r instanceof JSONArray)) {
                                                                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                                                "PRIVILEGES(" + CIRC_REGION+ ") array expected for json key: " + locationRegionKey);
                                                                        return;
                                                                    }
                                                                    List<String> circRegionList = new ArrayList<>();
                                                                    JSONArray circResionArray = (JSONArray) r;
                                                                    for (int j = 0; j < circResionArray.length();j ++){
                                                                        if (!(circResionArray.opt(j) instanceof String)) {
                                                                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                                                    "PRIVILEGES(" + CIRC_REGION + ") string expected for json array: " + locationRegionKey);
                                                                            return;
                                                                        }

                                                                        circRegionList.add(circResionArray.optString(j));
                                                                    }
                                                                    break;
                                                                case COUNTRY_CODE:
                                                                    // assume ip-v6 must contain an array, cannot be null
                                                                    if (!(r instanceof JSONArray)) {
                                                                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                                                "PRIVILEGES(" + COUNTRY_CODE+ ") array expected for json key: " + locationRegionKey);
                                                                        return;
                                                                    }
                                                                    List<String> countryCodeList = new ArrayList<>();
                                                                    JSONArray countryCodeArray = (JSONArray) r;
                                                                    for (int j = 0; j < countryCodeArray.length();j ++){
                                                                        if (!(countryCodeArray.opt(j) instanceof String)) {
                                                                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                                                    "PRIVILEGES(" + COUNTRY_CODE + ") string expected for json array: " + locationRegionKey);
                                                                            return;
                                                                        }
                                                                        countryCodeList.add(countryCodeArray.optString(j));
                                                                    }
                                                                    break;
                                                                default:
                                                                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                                            "PRIVILEGES(" + ACCESS_CONTROL_LOCATION_REGION + ") attribute not recognized: " + locationRegionKey);
                                                                    return;
                                                                }
                                                            }
                                                            break;
                                                        }

                                                        break;
                                                    }
                                                }

                                            }
                                        }
                                        break;
                                    case ACCESS_CONTROL_OPERATIONS:
                                        if (!resourceContent.getInJsonContent().isNull(key)) {
                                            if (!(p instanceof Integer)) {
                                                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                        "PRIVILEGES(" + ACCESS_CONTROL_OPERATIONS + ") number expected for json key: " + key);
                                                return;
                                            } else if (((Integer) p).intValue() < 0) {
                                                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                        "PRIVILEGES(" + ACCESS_CONTROL_OPERATIONS + ") integer must be non-negative: " + key);
                                                return;
                                            }
                                        }
                                        break;
                                    default:
                                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                                "PRIVILEGES(" + ACCESS_CONTROL_RULES  + ") attribute not recognized: " + ruleKey);
                                        return;
                                    }
                                }
                            }
                            break;
                        default:
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "PRIVILEGES(" + PRIVILIEGES  + ") attribute not recognized: " + PVkey);
                            return;
                        }
                    }
                }
                break;
                case ResourceContent.LABELS:
                case ResourceContent.EXPIRATION_TIME:
                case ResourceContent.RESOURCE_NAME:
                    if (!ResourceContent.parseJsonCommonCreateUpdateContent(key,
                            resourceContent,
                            onem2mResponse)) {
                        return;
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
     * Ensure the create/update parameters follow the rules
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void processCreateUpdateAttributes(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        String tempStr;
        Integer tempInt;

        // verify this resource can be created under the target resource
        if (onem2mRequest.isCreate) {
            String rt = onem2mRequest.getOnem2mResource().getResourceType();
            if (rt == null || !(rt.contentEquals(Onem2m.ResourceType.CSE_BASE) ||
                    rt.contentEquals(Onem2m.ResourceType.AE))) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Cannot create AccessControlPolicy under this resource type: " + rt);
                return;
            }
        }

        ResourceContent resourceContent = onem2mRequest.getResourceContent();


        //check mandatory privileges and selfPrivileges
        String pv = resourceContent.getInJsonContent().optString(PRIVILIEGES, null);
        if (pv == null) {
            if (onem2mRequest.isCreate) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "PRIVILEGES missing parameter");
                return;
            }
        } else {
            // todo: how to check access Control Operations and originators?
            JSONObject pvjsonObject = resourceContent.getInJsonContent().optJSONObject(PRIVILIEGES);
            if (!isContainPVMandatoryAttr(onem2mResponse, pvjsonObject)) return;
        }

        String pvs = resourceContent.getInJsonContent().optString(SELF_PRIIVLIEGES, null);
        if (pvs == null) {
            if (onem2mRequest.isCreate) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "SELF PRIVILEGES missing parameter");
                return;
            }
        } else {
            // todo: how to check access Control Operations and originators?
            JSONObject pvjsonObject = resourceContent.getInJsonContent().optJSONObject(SELF_PRIIVLIEGES);
            if (!isContainPVMandatoryAttr(onem2mResponse, pvjsonObject)) return;
        }

            // todo: Update ACP needs to check first




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
     * check the 2 mandatory attributes of pv, accessControlOriginators and accessControlOperations
     * @param onem2mResponse
     * @param pvjsonObject
     * @return
     */
    private static boolean isContainPVMandatoryAttr(ResponsePrimitive onem2mResponse, JSONObject pvjsonObject) {
        JSONArray acessControlRulesJsonArray = pvjsonObject.optJSONArray(ACCESS_CONTROL_RULES);
        for (int i = 0; i < acessControlRulesJsonArray.length(); i++) {
            JSONObject accessControlRule = acessControlRulesJsonArray.optJSONObject(i);
            String originators = accessControlRule.optString(ACCESS_CONTROL_ORIGINATORS, null);
            if (originators == null) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "ACCESS CONTROL ORIGINATOR of No." + (i+1) +" missing parameter");
                return false;
            }
            String operation = accessControlRule.optString(ACCESS_CONTROL_OPERATIONS, null);
            if (operation == null) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "ACCESS CONTROL OPERATION of No." + (i+1) +" missing parameter");
                return false;
            }
        }
        return true;
    }

    /**
     * Parse the CONTENT resource representation.
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void handleCreateUpdate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        resourceContent.parse(Onem2m.ResourceTypeString.ACCESS_CONTROL_POLICY,onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;

        if (resourceContent.isJson()) {
            parseJsonCreateUpdateContent(onem2mRequest, onem2mResponse);
            if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                return;
        }
        CheckAccessControlProcessor.handleSelfCreateUpdate(onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;
        resourceContent.processCommonCreateUpdateAttributes(onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;
        // handle the special attribtue for AccessControlPolicy
        ResourceAccessControlPolicy.processCreateUpdateAttributes(onem2mRequest, onem2mResponse);

    }

    public static void handleDefaultCreate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        resourceContent.parse(Onem2m.ResourceTypeString.ACCESS_CONTROL_POLICY, onem2mRequest, onem2mResponse);

        if (resourceContent.isJson()) {
            parseJsonCreateUpdateContent(onem2mRequest, onem2mResponse);
        }
        resourceContent.processCommonCreateUpdateAttributes(onem2mRequest, onem2mResponse);
        // handle the special attribtue for AccessControlPolicy
        ResourceAccessControlPolicy.processCreateUpdateAttributes(onem2mRequest, onem2mResponse);

    }

    // deal with the opearion numbers
    public static boolean isAllowedThisOperation(String OperationNumber, BigInteger number) {
        switch (OperationNumber){
            case Onem2m.Operation.CREATE:
                if (number.testBit(0)) return true;
                break;
            case Onem2m.Operation.RETRIEVE:
                if (number.testBit(1)) return true;
                break;
            case Onem2m.Operation.UPDATE:
                if (number.testBit(2)) return true;
                break;
            case Onem2m.Operation.DELETE:
                if (number.testBit(3)) return true;
                break;
            case Onem2m.Operation.DISCOVER:
                if (number.testBit(4)) return true;
                break;
            case Onem2m.Operation.NOTIFY:
                if (number.testBit(5)) return true;
                break;
            default:
                return false;
        }
        return false;

    }



}
