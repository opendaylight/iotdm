/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.*;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        public static final String CSE_BASE = "5"; //"csb";
        public static final String CONTENT_INSTANCE = "4"; //"cin";
        public static final String SUBSCRIPTION = "23"; //"sub";
        public static final String NODE = "14"; //"nod"
        public static final String GROUP = "9";
        public static final String ACCESS_CONTROL_POLICY = "1";
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
            LOG.error("serviceOnenm2mRequest: RPC exception");
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
}

