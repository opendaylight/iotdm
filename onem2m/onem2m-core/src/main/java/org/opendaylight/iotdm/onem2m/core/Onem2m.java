/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core;

import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mRequestPrimitiveInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mRequestPrimitiveInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mRequestPrimitiveOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: TS0004 8.3, and 8.4, short names are required


public class Onem2m {

    private Onem2m() {
    }
    public static final String CSE_DEFAULT_NAME = "InCSE1";
    public static final String SYS_PERF_TEST_CSE = "SYS_PERF_TEST_CSE";

    public class Operation {
        public static final String CREATE = "1";
        public static final String RETRIEVE = "2";
        public static final String UPDATE = "3";
        public static final String DELETE = "4";
        public static final String NOTIFY = "5";
    }

    // TODO: onem2m mime types: TS0004 section 6.7
    public class ContentFormat {
        public static final String JSON = "json";
        public static final String XML = "xml";
    }

    public class Protocol {
        public static final String COAP = "Coap";
        public static final String MQTT = "Mqtt";
        public static final String HTTP = "Http";
        public static final String NATIVEAPP = "NativeApp";
    }

    // TODO: ts0004 6.3.3.2.1, 8.2.4-1, why are there ENUMS and SHORT NAMES ???? WHICH ONE DO WE USE
    public class ResourceType {
        public static final String AE = "ae";
        public static final String CONTAINER = "cnt";
        public static final String CSE_BASE = "csb";
        public static final String CONTENT_INSTANCE = "cin";
        public static final String SUBSCRIPTION = "sub";
    }

    public class ResponseType { // TS0001 section 8.2.1
        public static final String NON_BLOCKING_REQUEST_SYNCH = "1";
        public static final String NON_BLOCKING_REQUEST_ASYNCH = "2";
        public static final String BLOCKING_REQUEST = "3";
    }

    public class RequestFilterCriteria { // TODO: ts0004 table 8.2.2.1
        public static final String CREATED_BEFORE = "crb";
        public static final String CREATED_AFTER = "cra";
        public static final String MODIFIED_SINCE = "ms";
        public static final String UNMODIFIED_SINCE = "us";
        public static final String STATE_TAG_SMALLER = "sts";
        public static final String STATE_TAG_BIGGER = "stb";
        public static final String EXPIRE_BEFORE = "exb";
        public static final String EXPIRE_AFTER = "exa";
        public static final String LABELS = "lbl";
        public static final String RESOURCE_TYPE = "rty";
        public static final String SIZE_ABOVE = "sza";
        public static final String SIZE_BELOW = "szb";
        public static final String CONTENT_TYPE = "cty";
        public static final String LIMIT = "lim";
        public static final String ATTRIBUTE = "atr";
        public static final String FILTER_USAGE = "fu";
    }

    // TODO: where did this list come from, not specfied in XSD but specified in TS0001 8.1.2
    public class ResultContent {
        public static final String NOTHING = "0";
        public static final String ATTRIBUTES = "1";
        public static final String HIERARCHICAL_ADDRESS = "2";
        public static final String HIERARCHICAL_ADDRESS_ATTRIBUTES = "3";
        public static final String ATTRIBUTES_CHILD_RESOURCES = "4";
        public static final String ATTRIBUTES_CHILD_RESOURCE_REFS = "5";
        public static final String CHILD_RESOURCE_REFS = "6";
        public static final String ORIGINAL_RESOURCE = "7";
    }

    // TODO: CDT-enumerationTypes
    public class DiscoveryResultType {
        public static final String HIERARCHICAL = "1";
        public static final String NON_HIERARCHICAL = "2";

    }

    // TS0004 sectiopns: 6.3.3.2.9, 6.6
    public enum ResponseStatusCode {
        ACCEPTED(1000),

        OK(2000),
        CREATED(2001),
        DELETED(2002),
        CHANGED(2004),

        BAD_REQUEST(4000),
        NOT_FOUND(4004),
        OPERATION_NOT_ALLOWED(4005),
        REQUEST_TIMEOUT(4008),
        SUBSCRIPTION_CREATOR_HAS_NO_PRIVILEGE(4101),
        CONTENTS_UNACCEPTABLE(4102),
        ACCESS_DENIED(4103),
        GROUP_REQUEST_IDENTIFIER_EXISTS(4104),
        CONFLICT(4105),

        INTERNAL_SERVER_ERROR(5000),
        NOT_IMPLEMENTED(5001),
        TARGET_NOT_REACHABLE(5103),
        NO_PRIVILEGE(5105),
        ALREADY_EXISTS(5106),
        TARGET_NOT_SUBSCRIBABLE(5203),
        SUBSCRIPTION_VERIFICATION_INITIATION_FAILED(5204),
        SUBSCRIPTION_HOST_HAS_NO_PRIVILEGE(5205),
        NON_BLOCKING_REQUEST_NOT_SUPPORTED(5206),

        EXTERNAL_OBJECT_NOT_REACHABLE(6003),
        EXTERNAL_OBJECT_NOT_FOUND(6005),
        MAX_NUMBER_OF_MEMBER_EXCEEDED(6010),
        MEMBER_TYPE_INCONSISTENT(6011),
        MGMT_SESSION_CANNOT_BE_ESTABLISHED(6020),
        MGMT_SESSION_ESTABLISHMENT_TIMEOUT(6021),
        INVALID_CMDTYPE(6022),
        INVALID_ARGUMENTS(6023),
        INSUFFICIENT_ARGUMENTS(6024),
        ALREADY_COMPLETE(6028),
        MGMT_COMMAND_NOT_CANCELLABLE(6029),
        MGMT_CONVERSION_ERROR(6025),
        MGMT_CANCELATION_FAILURE(6026);

        private int value;

        private ResponseStatusCode(int value) {
            this.value = value;

        }
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

    /**
     * Routine to allow REST clients to invoke the MDSAL RPC which will process the RequestPrimitive accessed via
     * the Onem2mService.
     * @param onem2mRequest
     * @param onem2mService
     * @return
     */
    public static ResponsePrimitive serviceOnenm2mRequest(RequestPrimitive onem2mRequest, Onem2mService onem2mService) {

        final Logger LOG = LoggerFactory.getLogger(Onem2m.class);

        ResponsePrimitive onem2mResponse;

        Onem2mRequestPrimitiveInput input = new Onem2mRequestPrimitiveInputBuilder()
                .setOnem2mPrimitive(onem2mRequest.getPrimitivesList()).build();

        try {
            RpcResult<Onem2mRequestPrimitiveOutput> rpcResult = onem2mService.onem2mRequestPrimitive(input).get();
            onem2mResponse = new ResponsePrimitive(rpcResult.getResult().getOnem2mPrimitive());
        } catch (Exception e) {
            onem2mResponse = new ResponsePrimitive();
            onem2mResponse.setRSC(ResponseStatusCode.INTERNAL_SERVER_ERROR, "RPC exception");
            LOG.error("serviceOnenm2mRequest: RPC exception");
        }

        //Onem2mDb.getInstance().dumpDataStoreToLog();

        return onem2mResponse;
    }
}

