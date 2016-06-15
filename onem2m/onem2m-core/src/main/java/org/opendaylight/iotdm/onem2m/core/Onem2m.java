/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.*;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

// TODO: TS0004 8.3, and 8.4, short names are required

public class Onem2m {

    private Onem2m() {
    }
    public static final String CSE_DEFAULT_NAME = "InCSE1";
    public static final String SYS_PERF_TEST_CSE = "SYS_PERF_TEST_CSE";

    public static final int MAX_RESOURCES = 1000000;
    public static final int MAX_TREE_WIDTH = 10000;
    public static final int MAX_TREE_LEVEL = 20;
    public static final int MAX_NR_INSTANCES_PER_CONTAINER = 20;
    public static final int MAX_DISCOVERY_LIMIT  = 1000;
    public static final boolean USE_M2M_PREFIX = true;

    public class Operation {
        public static final String CREATE = "1";
        public static final String RETRIEVE = "2";
        public static final String UPDATE = "3";
        public static final String DELETE = "4";
        public static final String NOTIFY = "5";
        public static final String DISCOVER = "6";
    }

    // TODO: onem2m mime types: TS0004 section 6.7
    public class ContentFormat {
        public static final String JSON = "json";
        public static final String XML = "xml";
    }

    public class ContentType {
        public static final String APP_VND_RES_JSON = "application/vnd.onem2m-res+json";
        public static final String APP_VND_NTFY_JSON = "application/vnd.onem2m-ntfy+json";

        public static final String APP_VND_RES_XML = "application/vnd.onem2m-res+xml";
        public static final String APP_VND_NTFY_XML = "application/vnd.onem2m-ntfy+xml";
    }

    public class CoapContentFormat {
        public static final int APP_VND_RES_XML = 10000;
        public static final int APP_VND_RES_JSON = 10001;
        public static final int APP_VND_NTFY_XML = 10002;
        public static final int APP_VND_NTFY_JSON = 10003;
    }

    public class Protocol {
        public static final String COAP = "Coap";
        public static final String MQTT = "Mqtt";
        public static final String HTTP = "Http";
        public static final String NATIVEAPP = "NativeApp";
    }

    // TODO: ts0004 6.3.3.2.1, 8.2.4-1, why are there ENUMS and SHORT NAMES ???? WHICH ONE DO WE USE
    public class ResourceType {
        public static final String AE = "2"; //"ae";
        public static final String CONTAINER = "3"; //"cnt";
        public static final String CSE_BASE = "5"; //"cb";
        public static final String CONTENT_INSTANCE = "4"; //"cin";
        public static final String SUBSCRIPTION = "23"; //"sub";
        public static final String NODE = "14"; //"nod";
        public static final String GROUP = "9"; //"grp";
        public static final String ACCESS_CONTROL_POLICY = "1"; //"acp";
        public static final String REMOTE_CSE = "16"; //"csr";
    }
    public class ResourceTypeString {
        public static final String AE = "ae";
        public static final String CONTAINER = "cnt";
        public static final String CSE_BASE = "cb";
        public static final String CONTENT_INSTANCE = "cin";
        public static final String SUBSCRIPTION = "sub";
        public static final String NODE = "nod";
        public static final String GROUP = "grp";
        public static final String ACCESS_CONTROL_POLICY = "acp";
        public static final String REMOTE_CSE = "csr";
    }

    public class CseType {
        public static final String INCSE = "IN-CSE";
        public static final String MNCSE = "MN-CSE";
    }

    // hard code set of long to short name
    public static final Map<String,String> resourceTypeToString = new HashMap<String,String>() {{
        // type; string
        put(ResourceType.AE, ResourceTypeString.AE);
        put(ResourceType.CONTAINER, ResourceTypeString.CONTAINER);
        put(ResourceType.CSE_BASE, ResourceTypeString.CSE_BASE);
        put(ResourceType.CONTENT_INSTANCE, ResourceTypeString.CONTENT_INSTANCE);
        put(ResourceType.SUBSCRIPTION, ResourceTypeString.SUBSCRIPTION);
        put(ResourceType.NODE, ResourceTypeString.NODE);
        put(ResourceType.GROUP, ResourceTypeString.GROUP);
        put(ResourceType.ACCESS_CONTROL_POLICY, ResourceTypeString.ACCESS_CONTROL_POLICY);
        put(ResourceType.REMOTE_CSE, ResourceTypeString.REMOTE_CSE);
    }};

    public class ResponseType { // TS0001 section 8.2.1
        public static final String NON_BLOCKING_REQUEST_SYNCH = "1";
        public static final String NON_BLOCKING_REQUEST_ASYNCH = "2";
        public static final String BLOCKING_REQUEST = "3";
    }

    public class NotificationContentType {
        public static final String MODIFIED_ATTRIBUTES = "1";
        public static final String WHOLE_RESOURCE = "2";
        public static final String RESOURCE_ID = "3";
    }

    // TODO: where did this list come from, not specified in XSD but specified in TS0001 8.1.2
    public class ResultContent {
        public static final String NOTHING = "0";
        public static final String ATTRIBUTES = "1";
        public static final String HIERARCHICAL_ADDRESS = "2";
        public static final String HIERARCHICAL_ADDRESS_ATTRIBUTES = "3";
        public static final String ATTRIBUTES_CHILD_RESOURCES = "4";
        public static final String ATTRIBUTES_CHILD_RESOURCE_REFS = "5";
        public static final String CHILD_RESOURCE_REFS = "6";
        //public static final String ORIGINAL_RESOURCE = "7";
    }

    // TODO: CDT-enumerationTypes
    public class DiscoveryResultType {
        public static final String HIERARCHICAL = "1";
        public static final String NON_HIERARCHICAL = "2";

    }

    // TODO: Filter Usage
    public class FilterUsageType {
        public static final String DISCOVERY = "1";
        public static final String CONDITIONAL_RETRIEVAL = "2";

    }

    public class EventType {
        public static final String UPDATE_RESOURCE = "1";
        public static final String RETRIEVE_NECHILD = "5";
        public static final String ANY_DESCENDENT_CHANGE = "6";
    }

    // TS0004 sections: 6.3.3.2.9, 6.6, for each new error code, add a mapping error code to CoAP and HTTP
    public class ResponseStatusCode {
        public static final String OK = "2000";
        public static final String CREATED = "2001";
        public static final String DELETED = "2002";
        public static final String CHANGED = "2004";

        public static final String BAD_REQUEST = "4000";
        public static final String NOT_FOUND = "4004";
        public static final String OPERATION_NOT_ALLOWED = "4005";
        public static final String CONTENTS_UNACCEPTABLE = "4102";
        public static final String CONFLICT = "4105";

        public static final String INTERNAL_SERVER_ERROR = "5000";
        public static final String NOT_IMPLEMENTED = "5001";
        public static final String TARGET_NOT_REACHABLE = "5103";
        public static final String ALREADY_EXISTS = "5106";
        public static final String TARGET_NOT_SUBSCRIBABLE = "5203";
        public static final String NON_BLOCKING_REQUEST_NOT_SUPPORTED = "5206";

        public static final String INVALID_ARGUMENTS = "6023";
        public static final String INSUFFICIENT_ARGUMENTS = "6024";
    }

    public class CoapOption {
        public static final int ONEM2M_FR = 256;
        public static final int ONEM2M_RQI = 257;
        public static final int ONEM2M_NM = 258;
        public static final int ONEM2M_OT = 259;
        public static final int ONEM2M_RQET = 260;
        public static final int ONEM2M_RSET = 261;
        public static final int ONEM2M_OET = 262;
        public static final int ONEM2M_RTURI = 263;
        public static final int ONEM2M_EC = 264;
        public static final int ONEM2M_RSC = 265;
        public static final int ONEM2M_GID = 266;
        public static final int ONEM2M_TY = 267;
    }

    /*
     * Definitions of CaAP Onem2m options
     * Every option has specified:
     *      - coapId: CoAP ID,
     *      - onem2mId: Onem2m Id,
     *      - valueIsString: a flag whether the value is string or integer,
     *      - max: Maximal value byte length
     */
    public static final class Onem2mCoapOptionDef {
        public final int coapId;
        public final String onem2mId;
        public final boolean valueIsString;
        public final int max;

        private Onem2mCoapOptionDef(int coapId, @Nonnull String onem2mId, boolean valueIsString, int max) {
            this.coapId = coapId;
            this.onem2mId = onem2mId;
            this.valueIsString = valueIsString;
            this.max = max;
        }
    }

    /*
     * This class stores definitions of the CoAP Onem2m options in two
     * hash maps with Onem2m parameter name as key and CoAP option ID as key
     * of the second hash map.
     */
    private static final class Onem2mCoapOptionDefinitions {
        public Map<String, Onem2mCoapOptionDef> coapOptionsMapOnem2m = new HashMap<>();
        public Map<Integer, Onem2mCoapOptionDef> coapOptionsMapCoap =  new HashMap<>();

        private void addDef(int coapId, @Nonnull String onem2mId, boolean valueIsString, int max) {
            Onem2mCoapOptionDef def = new Onem2mCoapOptionDef(coapId, onem2mId, valueIsString, max);
            coapOptionsMapCoap.put(coapId, def);
            coapOptionsMapOnem2m.put(onem2mId, def);
        }

        public Onem2mCoapOptionDefinitions() {
            initDefinitions();
        }

        private void initDefinitions() {
            addDef(CoapOption.ONEM2M_FR, RequestPrimitive.FROM, true, 255);
            addDef(CoapOption.ONEM2M_RQI, RequestPrimitive.REQUEST_IDENTIFIER, true, 255);
            addDef(CoapOption.ONEM2M_OT,  RequestPrimitive.ORIGINATING_TIMESTAMP, true, 15);
            addDef(CoapOption.ONEM2M_RQET, RequestPrimitive.REQUEST_EXPIRATION_TIMESTAMP, true, 15);
            addDef(CoapOption.ONEM2M_RSET, RequestPrimitive.RESULT_EXPIRATION_TIMESTAMP, true, 15);
            addDef(CoapOption.ONEM2M_OET, RequestPrimitive.OPERATION_EXECUTION_TIME, true, 15);
            // TODO don't have support for the RTURI
            // addDef(CoapOption.ONEM2M_RTURI, RequestPrimitive., true, 255);
            addDef(CoapOption.ONEM2M_EC, RequestPrimitive.EVENT_CATEGORY, false, 0xFF);
            addDef(CoapOption.ONEM2M_RSC, ResponsePrimitive.RESPONSE_STATUS_CODE, false, 0xFFFF);
            addDef(CoapOption.ONEM2M_GID, RequestPrimitive.GROUP_REQUEST_IDENTIFIER, true, 255);
            addDef(CoapOption.ONEM2M_TY, RequestPrimitive.RESOURCE_TYPE, false, 0xFFFF);
        }
    }

    private static final Onem2mCoapOptionDefinitions coapOptDefs = new Onem2mCoapOptionDefinitions();

    /**
     * Returns definition of CoAP option identified by Onem2m primitive
     * parameter name.
     * @param onem2mParameterName
     * @return
     */
    public static Onem2mCoapOptionDef getCoapOptionOnem2m(@Nonnull String onem2mParameterName) {
        return coapOptDefs.coapOptionsMapOnem2m.get(onem2mParameterName);
    }

    /**
     * Returns definition of CoAP option identfied by CoAP option ID.
     * @param coapOptionId
     * @return
     */
    public static Onem2mCoapOptionDef getCoapOptionCoap(int coapOptionId) {
        return coapOptDefs.coapOptionsMapCoap.get(coapOptionId);
    }

    public class HttpHeaders {
        public static final String X_M2M_ORIGIN = "X-M2M-Origin";
        public static final String X_M2M_RI = "X-M2M-RI";
        public static final String X_M2M_NM = "X-M2M-NM";
        public static final String X_M2M_GID = "X-M2M-GID";
        public static final String X_M2M_RTU = "X-M2M-RTU";
        public static final String X_M2M_OT = "X-M2M-OT";
        public static final String X_M2M_RSC = "X-M2M-RSC";
    }


    public class MqttMessageType {
        public static final String RESPONSE = "resp";
        public static final String REQUEST = "req";
    }

    public class MqttOptions {
        public static final int QOS1 = 1;
        public static final boolean RETAINED = true;
    }

    // set of primitive attributes which are mapped as query-string field in HTTP and CoAP
    public static final Set<String> queryStringParameters = new HashSet<String>() {{
//        add(RequestPrimitive.OPERATION);
//        add(RequestPrimitive.TO);
//        add(RequestPrimitive.FROM);
//        add(RequestPrimitive.REQUEST_IDENTIFIER);
        add(RequestPrimitive.RESOURCE_TYPE);
//        add(RequestPrimitive.NAME);
//        add(RequestPrimitive.CONTENT);
//        add(RequestPrimitive.ORIGINATING_TIMESTAMP);
//        add(RequestPrimitive.REQUEST_EXPIRATION_TIMESTAMP);
//        add(RequestPrimitive.RESULT_EXPIRATION_TIMESTAMP);
//        add(RequestPrimitive.OPERATION_EXECUTION_TIME);
        add(RequestPrimitive.RESPONSE_TYPE);
        add(RequestPrimitive.RESULT_PERSISTENCE);
        add(RequestPrimitive.RESULT_CONTENT);
//        add(RequestPrimitive.EVENT_CATEGORY);
        add(RequestPrimitive.DELIVERY_AGGREGATION);
//        add(RequestPrimitive.GROUP_REQUEST_IDENTIFIER);
//        add(RequestPrimitive.FILTER_CRITERIA);
        add(RequestPrimitive.FILTER_CRITERIA_CREATED_BEFORE);
        add(RequestPrimitive.FILTER_CRITERIA_CREATED_AFTER);
        add(RequestPrimitive.FILTER_CRITERIA_MODIFIED_SINCE);
        add(RequestPrimitive.FILTER_CRITERIA_UNMODIFIED_SINCE);
        add(RequestPrimitive.FILTER_CRITERIA_STATE_TAG_SMALLER);
        add(RequestPrimitive.FILTER_CRITERIA_STATE_TAG_BIGGER);
        add(RequestPrimitive.FILTER_CRITERIA_LABELS);
        add(RequestPrimitive.FILTER_CRITERIA_RESOURCE_TYPE);
        add(RequestPrimitive.FILTER_CRITERIA_SIZE_ABOVE);
        add(RequestPrimitive.FILTER_CRITERIA_SIZE_BELOW);
        add(RequestPrimitive.FILTER_CRITERIA_FILTER_USAGE);
        add(RequestPrimitive.FILTER_CRITERIA_LIMIT);
        add(RequestPrimitive.FILTER_CRITERIA_OFFSET);
        add(RequestPrimitive.DISCOVERY_RESULT_TYPE);
//        add(RequestPrimitive.PROTOCOL);
//        add(RequestPrimitive.CONTENT_FORMAT);
//        add(RequestPrimitive.NATIVEAPP_NAME);
//        add(RequestPrimitive.ROLE);
    }};

    /**
     * Routine to allow REST clients to invoke the MDSAL RPC which will process the RequestPrimitive accessed via
     * the Onem2mService.
     * @param onem2mRequest request
     * @param onem2mService response
     * @return the response primitives
     */
    public static ResponsePrimitive serviceOnenm2mRequest(RequestPrimitive onem2mRequest, Onem2mService onem2mService) {

        final Logger LOG = LoggerFactory.getLogger(Onem2m.class);

        ResponsePrimitive onem2mResponse;

        Onem2mRequestPrimitiveInput input = new Onem2mRequestPrimitiveInputBuilder()
                .setOnem2mPrimitive(onem2mRequest.getPrimitivesList()).build();

        try {
            RpcResult<Onem2mRequestPrimitiveOutput> rpcResult = onem2mService.onem2mRequestPrimitive(input).get();
            onem2mResponse = new ResponsePrimitive();
            onem2mResponse.setPrimitivesList(rpcResult.getResult().getOnem2mPrimitive());
        } catch (Exception e) {
            onem2mResponse = new ResponsePrimitive();
            onem2mResponse.setRSC(ResponseStatusCode.INTERNAL_SERVER_ERROR, "RPC exception:" + e.toString());
            LOG.error("serviceOnenm2mRequest: RPC exception: msg: {}, cause: {}", e.getMessage(), e.getCause());
        }

        return onem2mResponse;
    }

    /**
     * Enable REST clients to provision Cse's
     * @param onem2mRequest request
     * @param onem2mService response
     * @return the response primitives
     */
    public static ResponsePrimitive serviceCseProvisioning(RequestPrimitive onem2mRequest, Onem2mService onem2mService) {

        final Logger LOG = LoggerFactory.getLogger(Onem2m.class);

        ResponsePrimitive onem2mResponse;

        Onem2mCseProvisioningInput input = new Onem2mCseProvisioningInputBuilder()
                .setOnem2mPrimitive(onem2mRequest.getPrimitivesList()).build();

        try {
            RpcResult<Onem2mCseProvisioningOutput> rpcResult = onem2mService.onem2mCseProvisioning(input).get();
            onem2mResponse = new ResponsePrimitive();
            onem2mResponse.setPrimitivesList(rpcResult.getResult().getOnem2mPrimitive());
        } catch (Exception e) {
            onem2mResponse = new ResponsePrimitive();
            onem2mResponse.setRSC(ResponseStatusCode.INTERNAL_SERVER_ERROR, "RPC exception:" + e.toString());
            LOG.error("serviceOnenm2mRequest: RPC exception");
        }

        return onem2mResponse;
    }

    /**
     * Translates URI string from the protocol format into Onem2m format.
     * @param protocolUri
     * @return
     */
    public static String translateUriToOnem2m(@Nonnull String protocolUri) {
        protocolUri = Onem2mDb.trimURI(protocolUri);

        if (-1 != protocolUri.indexOf("~/")) {
            return protocolUri.replaceFirst("~/", "/");
        }

        if (-1 != protocolUri.indexOf("_/")) {
            return protocolUri.replaceFirst("_/", "//");
        }

        if (protocolUri.startsWith("/")) {
            return protocolUri.substring(1);
        }

        return protocolUri;
    }

    /**
     * Translates URI from Onem2m format into protocol format.
     * @param onem2mUri
     * @return
     */
    public static String translateUriFromOnem2m(@Nonnull String onem2mUri) {
        if (onem2mUri.startsWith("//")) {
            return onem2mUri.replaceFirst("//", "/_/");
        }

        if (onem2mUri.startsWith("/")) {
            return onem2mUri.replaceFirst("/", "/~/");
        }

        return "/" + onem2mUri;
    }
}

