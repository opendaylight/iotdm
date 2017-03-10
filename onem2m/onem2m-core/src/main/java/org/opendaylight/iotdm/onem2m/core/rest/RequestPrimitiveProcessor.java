/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.core.resource.*;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.router.Onem2mRouterService;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.opendaylight.iotdm.onem2m.core.utils.Onem2mDateTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.primitive.list.Onem2mPrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This the service side handler for the RequestPrimitives.  When the RPC is called, this class is used to
 * processes the primitives in the request.
 */
public class RequestPrimitiveProcessor extends RequestPrimitive {

    private static final Logger LOG = LoggerFactory.getLogger(RequestPrimitiveProcessor.class);

    public RequestPrimitiveProcessor() {
        super();
    }

    public void processPrimitivesList(List<Onem2mPrimitive> onem2mPrimitivesList, ResponsePrimitive onem2mResponse) {

        /**
         * Loop thru each input parameter, do some basic validation and put into a class variable so code can direct
         * access them.
         */
        Integer i;

        for (Onem2mPrimitive onem2mPrimitive : onem2mPrimitivesList) {

            String v = onem2mPrimitive.getValue();

            switch (onem2mPrimitive.getName()) {

                case OPERATION:
                    i = convertToUInt(v);
                    if (i != -1) {
                        primitiveOperation = i;
                    } else {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "Invalid operation: " + v);
                        return;
                    }
                    break;

                case TO:
                    if (validateUri(v)) {
                        primitiveTo = v;
                    } else {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "targetURI (to) not valid URI: " + v);
                        return;
                    }
                    break;

                case FROM:
                    if (validateUri(v)) {
                        primitiveFrom = v;
                    } else {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "Origin/From not valid URI: " + v);
                        return;
                    }
                    break;

                case REQUEST_IDENTIFIER:
                    primitiveRequestIdentifier = v;
                    break;

                case CONTENT_FORMAT:
                    primitiveContentFormat = v;
                    if (!v.equals(Onem2m.ContentFormat.JSON)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "Unsupported content format: " + v);
                        return;
                    }
                    break;

                case RESOURCE_TYPE:
                    i = convertToUInt(v);
                    if (i != -1) {
                        primitiveResourceType = i;
                    } else {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "Invalid resource: " + v);
                        return;
                    }
                    break;

                case NAME:
                    primitiveName = v;
                    break;

                case CONTENT:
                    primitiveContent = v;
                    break;

                case RESPONSE_TYPE:
                    i = convertToUInt(v);
                    if (i != -1) {
                        primitiveResponseType = i;
                        // this is an optional parameter but we will reject unsupported values
                        // only support blocking requests at this time, if not provided we default to blocking anyway
                        if (i != Onem2m.ResponseType.BLOCKING_REQUEST) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NON_BLOCKING_REQUEST_NOT_SUPPORTED,
                                    "Invalid response type: " + v);
                            return;
                        }
                    } else {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "Invalid response type: " + v);
                        return;
                    }
                    break;

                case RESULT_CONTENT:
                    i = convertToUInt(v);
                    if (i != -1) {
                        primitiveResultContent = i;
                    } else {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "Invalid result content: " + v);
                        return;
                    }
                    break;

                case EVENT_CATEGORY:
                    break;

                case DELIVERY_AGGREGATION:
                    break;

                case FILTER_CRITERIA_CREATED_BEFORE:
                    primitiveFilterCriteriaCreatedBefore = v;
                    if (!Onem2mDateTime.isValidDateTime(v)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                "FILTER_CRITERIA_CREATED_BEFORE(" + RequestPrimitive.FILTER_CRITERIA_CREATED_BEFORE +
                                        ") not valid format: " + v);
                        return;
                    }
                    found_filter_criteria = true;
                    break;

                case FILTER_CRITERIA_CREATED_AFTER:
                    primitiveFilterCriteriaCreatedAfter = v;
                    if (!Onem2mDateTime.isValidDateTime(v)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                "FILTER_CRITERIA_CREATED_AFTER(" + RequestPrimitive.FILTER_CRITERIA_CREATED_AFTER +
                                        ") not valid format: " + v);
                        return;
                    }
                    found_filter_criteria = true;
                    break;

                case FILTER_CRITERIA_MODIFIED_SINCE:
                    primitiveFilterCriteriaModifiedSince = v;
                    if (!Onem2mDateTime.isValidDateTime(v)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                "FILTER_CRITERIA_MODIFIED_SINCE(" + RequestPrimitive.FILTER_CRITERIA_MODIFIED_SINCE +
                                        ") not valid format: " + v);
                        return;
                    }
                    found_filter_criteria = true;
                    break;

                case FILTER_CRITERIA_UNMODIFIED_SINCE:
                    primitiveFilterCriteriaUnModifiedSince = v;
                    if (!Onem2mDateTime.isValidDateTime(v)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                "FILTER_CRITERIA_UNMODIFIED_SINCE(" + RequestPrimitive.FILTER_CRITERIA_UNMODIFIED_SINCE +
                                        ") not valid format: " + v);
                        return;
                    }
                    found_filter_criteria = true;
                    break;

                case FILTER_CRITERIA_STATE_TAG_SMALLER:
                    i = convertToUInt(v);
                    if (i != -1) {
                        primitiveFilterCriteriaStateTagSmaller = i;
                    } else {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                "FILTER_CRITERIA_STATE_TAG_SMALLER(" + RequestPrimitive.FILTER_CRITERIA_STATE_TAG_SMALLER +
                                        ") not valid format: " + v);
                        return;
                    }
                    found_filter_criteria = true;
                    break;

                case FILTER_CRITERIA_STATE_TAG_BIGGER:
                    i = convertToUInt(v);
                    if (i != -1) {
                        primitiveFilterCriteriaStateTagBigger = i;
                    } else {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                "FILTER_CRITERIA_STATE_TAG_BIGGER(" + RequestPrimitive.FILTER_CRITERIA_STATE_TAG_BIGGER +
                                        ") not valid format: " + v);
                        return;
                    }
                    found_filter_criteria = true;
                    break;

                case FILTER_CRITERIA_SIZE_ABOVE:
                    i = convertToUInt(v);
                    if (i != -1) {
                        primitiveFilterCriteriaSizeAbove = i;
                    } else {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                "FILTER_CRITERIA_SIZE_ABOVE(" + RequestPrimitive.FILTER_CRITERIA_SIZE_ABOVE +
                                        ") not valid format: " + v);
                        return;
                    }
                    found_filter_criteria = true;
                    break;

                case FILTER_CRITERIA_SIZE_BELOW:
                    i = convertToUInt(v);
                    if (i != -1) {
                        primitiveFilterCriteriaSizeBelow = i;
                    } else {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                "FILTER_CRITERIA_SIZE_BELOW(" + RequestPrimitive.FILTER_CRITERIA_SIZE_BELOW +
                                        ") not valid format: " + v);
                        return;
                    }
                    found_filter_criteria = true;
                    break;

                case FILTER_CRITERIA_LABELS:
                    List<String> labels = getPrimitiveFilterCriteriaLabels();
                    if (labels == null) {
                        primitiveFilterCriteriaLabels = new ArrayList<>();
                    }
                    primitiveFilterCriteriaLabels.add(v);
                    found_filter_criteria = true;
                    break;

                case FILTER_CRITERIA_RESOURCE_TYPE:
                    i = convertToUInt(v);
                    if (i != -1) {
                        List<Integer> resourceTypes = getPrimitiveFilterCriteriaResourceTypes();
                        if (resourceTypes == null) {
                            primitiveFilterCriteriaResourceTypes = new ArrayList<>();
                        }
                        primitiveFilterCriteriaResourceTypes.add(i);
                        found_filter_criteria = true;
                    } else {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                "FILTER_CRITERIA_RESOURCE_TYPE(" + RequestPrimitive.FILTER_CRITERIA_RESOURCE_TYPE +
                                        ") not valid format: " + v);
                        return;
                    }
                    break;

                case FILTER_CRITERIA_OFFSET:
                    i = convertToUInt(v);
                    if (i != -1) {
                        primitiveFilterCriteriaOffset = i;
                    } else {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                "FILTER_CRITERIA_OFFSET(" + RequestPrimitive.FILTER_CRITERIA_OFFSET +
                                        ") not valid format: " + v);
                        return;
                    }
                    found_filter_criteria = true;
                    break;

                case FILTER_CRITERIA_LIMIT:
                    i = convertToUInt(v);
                    if (i != -1) {
                        primitiveFilterCriteriaLimit = i;
                    } else {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                "FILTER_CRITERIA_LIMIT(" + RequestPrimitive.FILTER_CRITERIA_LIMIT +
                                        ") not valid format: " + v);
                        return;
                    }
                    found_filter_criteria = true;
                    break;

                case FILTER_CRITERIA_FILTER_USAGE:
                    i = convertToUInt(v);
                    if (i != -1) {
                        primitiveFilterCriteriaFilterUsage = i;
                        if (i != Onem2m.FilterUsageType.DISCOVERY && i != Onem2m.FilterUsageType.CONDITIONAL_RETRIEVAL) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                    "FILTER_CRITERIA_FILTER_USAGE(" + RequestPrimitive.FILTER_CRITERIA_FILTER_USAGE +
                                            ") not valid value: " + v);
                            return;
                        }
                    } else {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                "FILTER_CRITERIA_FILTER_USAGE(" + RequestPrimitive.FILTER_CRITERIA_FILTER_USAGE +
                                        ") not valid format: " + v);
                        return;
                    }
                    if (primitiveFilterCriteriaFilterUsage == Onem2m.FilterUsageType.DISCOVERY) {
                        setFUDiscovery(true);
                    }
                    break;

                case DISCOVERY_RESULT_TYPE:
                    i = convertToUInt(v);
                    if (i != -1) {
                        primitiveDiscoveryResultType = i;
                        if (!(i == Onem2m.DiscoveryResultType.NON_HIERARCHICAL ||
                                i == Onem2m.DiscoveryResultType.HIERARCHICAL)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                    "DISCOVERY_RESULT_TYPE(" + RequestPrimitive.DISCOVERY_RESULT_TYPE +
                                            ") invalid option: " + v);
                            return;
                        }
                    } else {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                "DISCOVERY_RESULT_TYPE(" + RequestPrimitive.DISCOVERY_RESULT_TYPE +
                                        ") not valid format: " + v);
                        return;
                    }
                    break;

                case PROTOCOL:
                    primitiveProtocol = v;
                    switch (v) {
                        case Onem2m.Protocol.COAP:
                        case Onem2m.Protocol.HTTP:
                        case Onem2m.Protocol.MQTT:
                        case Onem2m.Protocol.NATIVEAPP:
                            break;
                        default:
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NON_BLOCKING_REQUEST_NOT_SUPPORTED,
                                    "Invalid protocol: " + v);
                            return;
                    }
                    break;

                case NATIVEAPP_NAME:
                    this.primitiveNativeAppName = v;
                    break;

                case ROLE:
                    break;

                case ORIGINATING_TIMESTAMP:
                    break;

                case "CSE_ID":
                case "CSE_TYPE":
                    break;

                default:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                            "REQUEST_PRIMITIVE(" + onem2mPrimitive.getName() + ") not valid/supported: value: " + v);
                    return;
            }
        }
    }

    /**
     * Ensure the name is OK in that it does not contain a / or it is not one of the reserved names
     * Trying to retrieve the latent content instance in a container would be tricky if one of the valid
     * names of a contentInstance can be "latest".
     * @param name
     * @return
     */
    private boolean validateResourceName(String name)  {
        if (name.contentEquals("latest") ||
                name.contentEquals(ResourceContainer.LATEST) ||
                name.contentEquals("oldest") ||
                name.contentEquals(ResourceContainer.OLDEST) ||
                name.contentEquals("0") ||
                name.contains("/")) {
            return false;
        }
        return true;
    }

    /**
     * Ensure the proposed uriString is OK, construct a URI to see if the syntax is a valid URI.
     * @param uriString
     * @return
     */
    private boolean validateUri(String uriString)  {
        try {
            URI toUri = new URI(uriString);
        } catch (URISyntaxException e) {
            return false;
        }
        return true;
    }

    private Integer convertToUInt(String intString)  {
        Integer foo;
        try {
            foo = Integer.parseInt(intString);
            if (foo < 0) return -1;
        } catch (NumberFormatException e) {
            return -1;
        }
        return foo;
    }

    public boolean hasContent() {
        if (null != this.getPrimitive(RequestPrimitive.CONTENT)) {
            return true;
        }
        return false;
    }

    /**
     * Called by the core RPC:onem2mRequestPrimitive to process the onenm2m request primitive operation.
     * The philosophy is to do as much error checking as possible up front so downstream code will not have to
     * worry if parameters that were required are in the RequestPrimitives.
     *
     * There are some common things to check here, then based on the operation, more op specific things will have
     * to be checked.
     *
     * TS0004, table 7.1.1.1-1 Request Primitive parameters
     *
     * As soon as an error has occurred at any level, the response.setRSC is called and we return.
     *
     * TODO: should I be using throw new Onem2mRequestError() so the code is more java-esque?
     * @param onem2mResponse response
     */
    public void handleOperation(ResponsePrimitive onem2mResponse) {

        // if the request had a REQUEST_IDENTIFIER, return it in the response so client can correlate
        // this must be the first statement as the rqi must be in the error response
        String rqi = getPrimitiveRequestIdentifier();
        if (rqi != null) {
            onem2mResponse.setPrimitiveRequestIdentifier(rqi);
        } else {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "REQUEST_IDENTIFIER(" + RequestPrimitive.REQUEST_IDENTIFIER + ") not specified");
            return;
        }

        // Use table TS0004: 7.1.1.1-1 to validate mandatory parameters

        String protocol = getPrimitiveProtocol();
        if (protocol == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "PROTOCOL(" + RequestPrimitive.PROTOCOL + ") not specified");
            return;
        }

        // ensure resource type is present only in CREATE requests
        Integer resourceType = getPrimitiveResourceType();
        Integer operation = getPrimitiveOperation();

        if (resourceType == -1) {
            if (operation == Onem2m.Operation.CREATE) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "RESOURCE_TYPE(" + RequestPrimitive.RESOURCE_TYPE + ") not specified for CREATE");
                return;
            }
        } else { // a resource type was specified, then only CREATE can have one
            if (operation != Onem2m.Operation.CREATE) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "RESOURCE_TYPE(" + RequestPrimitive.RESOURCE_TYPE + ") not permitted for operation: " + operation);
                return;
            }
            // resource type value will be verified later
        }

        String from = getPrimitiveFrom();
        if (from == null && resourceType != Onem2m.ResourceType.AE) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "FROM(" + RequestPrimitive.FROM + ") not specified");
            return;
        }

        // discovery_result_type only valid for RETRIEVE
        Integer drt = getPrimitiveDiscoveryResultType();
        if (drt != -1) {
            if (operation != Onem2m.Operation.RETRIEVE) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "DISCOVERY_RESULT_TYPE(" + RequestPrimitive.DISCOVERY_RESULT_TYPE +
                                ") not permitted for operation: " + operation);
                return;
            }
        }

        switch (operation) {
            case Onem2m.Operation.CREATE:
                setWriterTransaction(Onem2mDb.getInstance().startWriteTransaction());
                try {
                    if (! hasContent()) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                              "Mandatory resource content is missing!");
                        break;
                    }

                    if (resourceType != Onem2m.ResourceType.CSE_BASE || protocol.equals(Onem2m.Protocol.NATIVEAPP)) {
                        handleOperationCreate(onem2mResponse);
                    } else {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                "Cannot create a CSE Base, it must be provisioned separately!");
                    }
                } finally {
                    if (!Onem2mDb.getInstance().endWriteTransaction(getWriterTransaction())) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR,
                                "Database transaction failed!");
                    }
                }
                break;
            case Onem2m.Operation.RETRIEVE:
                handleOperationRetrieve(onem2mResponse);
                break;
            case Onem2m.Operation.UPDATE:
                try {
                    if (! hasContent()) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                              "Mandatory resource content is missing!");
                        break;
                    }

                    handleOperationUpdate(onem2mResponse);
                } finally {
                }
                break;
            case Onem2m.Operation.DELETE:
                try {
                    handleOperationDelete(onem2mResponse);
                } finally {
                }
                break;
            case Onem2m.Operation.NOTIFY:
                handleOperationNotify(onem2mResponse);
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_IMPLEMENTED,
                        "OPERATION(" + RequestPrimitive.OPERATION + ") NOTIFY not implemented");
                LOG.warn("Received NOTIFY operation but handling is not implemented, from: {}.",
                         getPrimitiveFrom());
                break;
            default:
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "OPERATION(" + RequestPrimitive.OPERATION + ") not valid: " + operation);
        }


        // TODO: at this point we could support returning the optional TO/FROM/OT/RET/EC but we will wait
    }

    /**
     * Handle the request primitive create ...
     * TODO: Strategy for error handling ... TS0004 7.1.1.2
     * @param onem2mResponse response
     */
    public void handleOperationCreate(ResponsePrimitive onem2mResponse) {

        // Use table TS0004: 7.1.1.1-1 to validate CREATE specific parameters that were not handled in the
        // handleOperation

        // ensure the create has content ...
        String cf = getPrimitiveContentFormat();
        if (cf == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "CONTENT_FORMAT(" + RequestPrimitive.CONTENT_FORMAT + ") not specified");
            return;
        }

        String cn = getPrimitiveContent();
        if (cn == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "CONTENT(" + RequestPrimitive.CONTENT_FORMAT + ") not specified");
            return;
        }

        // validate result content options for create
        Integer rc = getPrimitiveResultContent();
        if (rc != -1) {
            switch (rc) {
                case Onem2m.ResultContent.ATTRIBUTES:
                case Onem2m.ResultContent.NOTHING:
                case Onem2m.ResultContent.HIERARCHICAL_ADDRESS:
                case Onem2m.ResultContent.HIERARCHICAL_ADDRESS_ATTRIBUTES:
                    break;
                default:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "RESULT_CONTENT(" + RequestPrimitive.RESULT_CONTENT + ") not accepted (" + rc + ")");
                    return;

            }
        }

        // lookup the resource ... this will be the parent where the new resource will be created
        Integer resourceType = getPrimitiveResourceType();
        this.setResourceType(resourceType);

        if (resourceType != Onem2m.ResourceType.CSE_BASE) {

            String to = getPrimitiveTo();

            Onem2mResource onem2mParentResource = Onem2mDb.getInstance().findResourceUsingURI(to);
            if (onem2mParentResource == null) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_FOUND,
                        "Resource target URI not found: " + to);
                return;
            }

            // cache some important information from the parent resource as it has been read from the database
            this.setParentResourceId(onem2mParentResource.getResourceId());
            this.setParentOnem2mResource(onem2mParentResource);
            this.setParentJsonResourceContent(onem2mParentResource.getResourceContentJsonString());
            this.setParentResourceType(onem2mParentResource.getResourceType());

            // set the new hierarchical parent for this new resource based on the parent
            this.setParentTargetUri(onem2mParentResource.getParentTargetUri() + "/" + onem2mParentResource.getName());

            String resourceName = this.getPrimitiveName();

            // if the a name is provided, ensure it is valid and unique at this hierarchical level
            if (resourceName != null) {
                // using the parent, see if this new resource name already exists under this parent resource
                if (Onem2mDb.getInstance().findChildFromParentAndChildName(
                        onem2mParentResource.getResourceId(), resourceName) != null) {
                    // TS0004: 7.2.3.2
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONFLICT,
                            "Resource already exists: " + getPrimitiveTo() + "/" + resourceName);
                    return;
                }
                this.setResourceName(resourceName);
            }
        } else {
            String resourceName = this.getPrimitiveName();
            if (resourceName == null) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CSE name not specified");
                return;
            }
            if (Onem2mDb.getInstance().findCseByName(resourceName)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONFLICT, "CSE name already exists: " + resourceName);
                return;
            }
            this.setResourceName(resourceName);
        }

        // prevent multiple writers to the parent, as state will be updated
        RequestLocker.getInstance().LockResource(this.getParentResourceId());
        try {
            ResourceContentProcessor.handleCreate(this, onem2mResponse, this.getTargetResourceLocator());
            if (onem2mResponse.getPrimitiveResponseStatusCode() != null) {
                return;
            }
        } finally {
            RequestLocker.getInstance().UnlockResource(this.getParentResourceId());
        }

        // now format a response based on result content desired
        ResultContentProcessor.handleCreate(this, onem2mResponse);

        // now process common notifications type F
        NotificationProcessor.getInstance().enqueueNotifierOperation(NotificationProcessor.Operation.CREATE, this);

        // TODO: see TS0004 6.8
        // if the create was successful, ie no error has already happened, set CREATED for status code here
        if (onem2mResponse.getPrimitiveResponseStatusCode() == null) {
            onem2mResponse.setPrimitiveResponseStatusCode(Onem2m.ResponseStatusCode.CREATED);
        }
    }

    /**
     * Handle the request primitive retrieve ...
     * TODO: Strategy for error handling ... TS0004 7.1.1.2
     * @param onem2mResponse response
     */
    public void handleOperationRetrieve(ResponsePrimitive onem2mResponse) {

        // Use table TS0004: 7.1.1.1-1 to validate RETRIEVE specific parameters that were not handled in the calling routine

        // validate result content options for retrieve
        Integer rc = getPrimitiveResultContent();
        if (rc != -1) {
            switch (rc) {
                case Onem2m.ResultContent.ATTRIBUTES:
                case Onem2m.ResultContent.ATTRIBUTES_CHILD_RESOURCES:
                case Onem2m.ResultContent.CHILD_RESOURCE_REFS:
                case Onem2m.ResultContent.ATTRIBUTES_CHILD_RESOURCE_REFS:
                    break;
                default:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "RESULT_CONTENT(" + RequestPrimitive.RESULT_CONTENT + ") not accepted (" + rc + ")");
                    return;

            }
        }

        // return hierarchical address by default if not specified
        Integer drt = getPrimitiveDiscoveryResultType();
        if (drt == -1) {
            setPrimitiveDiscoveryResultType(Onem2m.DiscoveryResultType.HIERARCHICAL);
        }

        // find the resource using the TO URI ...
        String to = getPrimitiveTo();
        Onem2mResource onem2mResource = Onem2mDb.getInstance().findResourceUsingURI(to);
        if (onem2mResource == null) {

            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_FOUND,
                    "Resource target URI not found: " + to);
            return;
        }

        this.setResourceId(onem2mResource.getResourceId());
        this.setOnem2mResource(onem2mResource);
        this.setJsonResourceContent(onem2mResource.getResourceContentJsonString());
        this.setResourceType(onem2mResource.getResourceType());

        this.setParentTargetUri(onem2mResource.getParentTargetUri());
        this.setResourceName(onem2mResource.getName());

        // check parent Container disableRetrieval attribute
        Integer rt = this.getResourceType();
        if (rt == Onem2m.ResourceType.CONTENT_INSTANCE) {
            String parentID = this.getOnem2mResource().getParentId();
            Onem2mResource parentResource = Onem2mDb.getInstance().getResource(parentID);
            JSONObject parentJsonObject = new JSONObject(parentResource.getResourceContentJsonString());
            if (parentJsonObject.optBoolean(ResourceContainer.DISABLE_RETRIEVAL)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Parent Container's disableRetrieval is set to true, cannot delete this resource: " + getPrimitiveTo());
                return;
            }
        }


        //CheckAccessControlProcessor.handleRetrieve(this, onem2mResponse);
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null) {
            return;
        }
        // process the resource specific attributes, for stats only
        ResourceContentProcessor.handleRetrieve(this, onem2mResponse);
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null) {
            return;
        }

        // return the data according to result content and filter criteria
        ResultContentProcessor.handleRetrieve(this, onem2mResponse);
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null) {
            return;
        }
        // TODO: see TS0004 6.8
        // if FOUND, and all went well, send back OK
        if (onem2mResponse.getPrimitiveResponseStatusCode() == null) {
            onem2mResponse.setPrimitiveResponseStatusCode(Onem2m.ResponseStatusCode.OK);
        }
    }

    /**
     * Handle the request primitive delete ...
     * TODO: Strategy for error handling ... TS0004 7.1.1.2
     * @param onem2mResponse response
     */
    public void handleOperationDelete(ResponsePrimitive onem2mResponse) {

        // Use table TS0004: 7.1.1.1-1 to validate DELETE specific parameters that were not handled in the calling routine

        String cf = getPrimitiveContent();
        if (cf != null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INVALID_ARGUMENTS,
                    "CONTENT(" + RequestPrimitive.CONTENT + ") not permitted");
            return;
        }

        Integer rc = getPrimitiveResultContent();
        if (rc != -1) {
            switch (rc) {
                case Onem2m.ResultContent.ATTRIBUTES:
                case Onem2m.ResultContent.NOTHING:
                case Onem2m.ResultContent.CHILD_RESOURCE_REFS:
                case Onem2m.ResultContent.ATTRIBUTES_CHILD_RESOURCE_REFS:
                    break;
                default:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "RESULT_CONTENT(" + RequestPrimitive.RESULT_CONTENT + ") not accepted (" + rc + ")");
                    return;

            }
        }
        /**
         * Find the resource, fill in the response based on result content
         */
        String to = this.getPrimitiveTo();
        Onem2mResource onem2mResource = Onem2mDb.getInstance().findResourceUsingURI(to);
        if (onem2mResource == null) {
            // TODO: is it idempotent or not ... fail or succeed???
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_FOUND,
                    "Resource target URI not found: " + to);
            return;
        }

        this.setResourceId(onem2mResource.getResourceId());
        this.setOnem2mResource(onem2mResource);
        this.setJsonResourceContent(onem2mResource.getResourceContentJsonString());
        this.setResourceType(onem2mResource.getResourceType());

        this.setParentTargetUri(onem2mResource.getParentTargetUri());
        this.setResourceName(onem2mResource.getName());

        String protocol = getPrimitiveProtocol();
        Integer rt = this.getResourceType();
        if (rt == Onem2m.ResourceType.CSE_BASE && !protocol.equals(Onem2m.Protocol.NATIVEAPP)) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                    "Not permitted to delete this resource: " + this.getPrimitiveTo());
            return;
        }

        // check parent Container disableRetrieval attribute
        if (rt == Onem2m.ResourceType.CONTENT_INSTANCE) {
            String parentID = this.getOnem2mResource().getParentId();
            Onem2mResource parentResource = Onem2mDb.getInstance().getResource(parentID);
            JSONObject parentJsonObject = new JSONObject(parentResource.getResourceContentJsonString());
            if (parentJsonObject.optBoolean(ResourceContainer.DISABLE_RETRIEVAL)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Parent Container's disableRetrieval is set to true, cannot delete this resource: " + this.getPrimitiveTo());
                return;
            }
        }

        //CheckAccessControlProcessor.handleDelete(this, onem2mResponse);
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null) {
            return;
        }

        // just used for stats
        ResourceContentProcessor.handleDelete(this, onem2mResponse);
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null) {
            return;
        }

        ResultContentProcessor.handleDelete(this, onem2mResponse);
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null) {
            return;
        }

        NotificationProcessor.getInstance().enqueueNotifierOperation(NotificationProcessor.Operation.DELETE, this);

        Onem2mRouterService.getInstance().updateRoutingTable(this);

        // now delete the resource from the database
        RequestLocker.getInstance().LockResource(this.getResourceId());
        try {
            // TODO: idempotent so who cares if cannot find the resource ... is this true?
            if (Onem2mDb.getInstance().pseudoDeleteOnem2mResource(onem2mResource) == false) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR,
                        "Resource target URI data store delete error: " + this.getPrimitiveTo());
                return;
            }
        } finally {
            RequestLocker.getInstance().UnlockResource(this.getResourceId());
        }

        // TODO: see TS0004 6.8
        // if FOUND, and all went well, send back OK
        if (onem2mResponse.getPrimitiveResponseStatusCode() == null) {
            onem2mResponse.setPrimitiveResponseStatusCode(Onem2m.ResponseStatusCode.DELETED);
        }
    }

    /**
     * Handle update
     * @param onem2mResponse response
     */
    public void handleOperationUpdate(ResponsePrimitive onem2mResponse) {

        // Use table TS0004: 7.1.1.1-1 to validate UPDATE specific parameters that were not handled in the calling routine

        // ensure the update has content ...
        String cf = getPrimitiveContentFormat();
        if (cf == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INSUFFICIENT_ARGUMENTS, "CONTENT_FORMAT(" + RequestPrimitive.CONTENT_FORMAT + ") not specified");
            return;
        } else if (cf != Onem2m.ContentFormat.JSON && cf != Onem2m.ContentFormat.XML) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT_FORMAT(" + RequestPrimitive.CONTENT_FORMAT + ") not accepted (" + cf + ")");
            return;
        }
        String cn = getPrimitiveContent();
        if (cn == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INSUFFICIENT_ARGUMENTS, "CONTENT(" + RequestPrimitive.CONTENT_FORMAT + ") not specified");
            return;
        }

        // validate result content options for Update
        Integer rc = getPrimitiveResultContent();
        if (rc != -1) {
            switch (rc) {
                case Onem2m.ResultContent.ATTRIBUTES:
                case Onem2m.ResultContent.NOTHING:
                case Onem2m.ResultContent.CHILD_RESOURCE_REFS:
                case Onem2m.ResultContent.ATTRIBUTES_CHILD_RESOURCE_REFS:
                    break;
                default:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "RESULT_CONTENT(" + RequestPrimitive.RESULT_CONTENT + ") not accepted (" + rc + ")");
                    return;
            }
        }

        String to = this.getPrimitiveTo();
        Onem2mResource onem2mResource = Onem2mDb.getInstance().findResourceUsingURI(to);
        if (onem2mResource == null) {
            // TODO: is it idempotent or not ... fail or succeed???
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_FOUND,
                    "Resource target URI not found: " + to);
            return;
        }

        this.setResourceId(onem2mResource.getResourceId());
        this.setOnem2mResource(onem2mResource);
        this.setJsonResourceContent(onem2mResource.getResourceContentJsonString());
        this.setResourceType(onem2mResource.getResourceType());

        this.setParentTargetUri(onem2mResource.getParentTargetUri());
        this.setResourceName(onem2mResource.getName());

        // cannot update contentInstance resources so check resource type
        Integer rt = this.getResourceType();
        if (rt == Onem2m.ResourceType.CONTENT_INSTANCE) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                    "Not permitted to update content instance: " + this.getPrimitiveTo());
            return;
        }

        RequestLocker.getInstance().LockResource(resourceId);
        try {
            ResourceContentProcessor.handleUpdate(this, onem2mResponse);
            if (onem2mResponse.getPrimitiveResponseStatusCode() != null) {
                return;
            }
        } finally {
            RequestLocker.getInstance().UnlockResource(resourceId);
        }

        ResultContentProcessor.handleUpdate(this, onem2mResponse);
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null) {
            return;
        }

        NotificationProcessor.getInstance().enqueueNotifierOperation(NotificationProcessor.Operation.UPDATE, this);

        // TODO: see TS0004 6.8
        // if FOUND, and all went well, send back CHANGED
        if (onem2mResponse.getPrimitiveResponseStatusCode() == null) {
            onem2mResponse.setPrimitiveResponseStatusCode(Onem2m.ResponseStatusCode.CHANGED);
        }
    }

    /**
     * Support this when we do CSE-CSE (Mcc) communication
     * @param onem2mResponse response
     */
    public void handleOperationNotify(ResponsePrimitive onem2mResponse) {
        // Use table TS0004: 7.1.1.1-1 to validate NOTIFY specific parameters that were not handled in the calling routine
    }

    /**
     * Internally create a cse base based on the restconf call which is designed to be called from a provisioning
     * server or management app.
     * @param onem2mResponse response
     */
    public void provisionCse(ResponsePrimitive onem2mResponse) {

        try {
            String cseId = this.getPrimitive("CSE_ID");
            if (cseId == null) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CSE_ID not specified!");
                return;
            }
            String cseType = this.getPrimitive("CSE_TYPE");
            if (cseType == null) {
                cseType = Onem2m.CseType.INCSE;
            } else if (!cseType.contentEquals(Onem2m.CseType.INCSE)) { //TODO: what is the difference between CSE types
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "IN-CSE is the only one supported :-(");
                return;
            }
            String iotdmInstanceIdString = this.getPrimitive("CSE_INSTANCE_ID");
            if (iotdmInstanceIdString != null) {
                Integer iotdmInstanceId;
                try {
                    iotdmInstanceId = Integer.valueOf(iotdmInstanceIdString);
                } catch (NumberFormatException e) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CSE_ID not specified!");
                    return;
                }
                Onem2mDb.getInstance().setIotdmInstanceId(iotdmInstanceId);
            }
            this.setPrimitiveResourceType(Onem2m.ResourceType.CSE_BASE);
            this.setPrimitiveName(cseId);
            /* NOTE: The cseID is used as resourceName for the CSE_BASE. These attributes are used as part of
             * SP-relative and absolute structured URIs.
             * Processing of URIs expects that these attributes are equal so the change needs to be reflected also
             * in the processing of URIs.
             */
            this.setResourceName(cseId);

            if (Onem2mDb.getInstance().findCseByName(cseId)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.ALREADY_EXISTS, "CSE name already exists: " + cseId);
                return;
            }

            this.setPrimitiveContentFormat(Onem2m.ContentFormat.JSON);
            this.setResourceType(Onem2m.ResourceType.CSE_BASE);

            JSONObject jCse = new JSONObject();
            JsonUtils.put(jCse, ResourceCse.CSE_ID, cseId);
            JsonUtils.put(jCse, ResourceCse.CSE_TYPE, cseType);
            JSONObject j = new JSONObject();
            JsonUtils.put(j, "m2m:" + Onem2m.ResourceTypeString.CSE_BASE, jCse);
            this.setPrimitiveContent(j.toString());
            this.setParentTargetUri("");

            // process the resource specific attributes
            ResourceContentProcessor.handleCreate(this, onem2mResponse, null);
            if (onem2mResponse.getPrimitiveResponseStatusCode() != null) {
                return;
            }

            ResultContentProcessor.handleCreate(this, onem2mResponse);

            // TODO: see TS0004 6.8
            // if the create was successful, ie no error has already happened, set CREATED for status code here
            if (onem2mResponse.getPrimitiveResponseStatusCode() == null) {
//                onem2mResponse.setPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE,
//                        "Provisioned cseBase: " + cseId + " type: " + cseType);
                onem2mResponse.setPrimitiveResponseStatusCode(Onem2m.ResponseStatusCode.OK);
            }
        } finally {
        }
    }

    public void createDefaultACP(List<Onem2mPrimitive> csePrimitiveList,
                                 ResponsePrimitive onem2mResponse) {

        try {
            String cseId = null;

            for (Onem2mPrimitive csePrimitive : csePrimitiveList) {
                String k = csePrimitive.getName();
                String v = csePrimitive.getValue();

                switch (k) {
                    case "CSE_ID":
                        cseId = v;
                        break;
                }
            }

            String cseURI = cseId; // without any slash means CSE-relative

            this.setResourceName("_defaultACP");
            this.setPrimitiveResourceType(Onem2m.ResourceType.ACCESS_CONTROL_POLICY);
            this.setResourceType(Onem2m.ResourceType.ACCESS_CONTROL_POLICY);
            this.setPrimitiveTo(cseURI);
            this.setPrimitiveContentFormat(Onem2m.ContentFormat.JSON);
            this.isCreate = true;

            // store the parent resource into onem2mresource.
            Onem2mResource onem2mParentResource = Onem2mDb.getInstance().findResourceUsingURI(cseURI);
            if (onem2mParentResource == null) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_FOUND,
                        "Resource target URI not found: " + cseURI);
                return;
            }

            this.setParentResourceId(onem2mParentResource.getResourceId());
            this.setParentOnem2mResource(onem2mParentResource);
            this.setParentJsonResourceContent(onem2mParentResource.getResourceContentJsonString());
            this.setParentResourceType(onem2mParentResource.getResourceType());

            // set the new hierarchical parent for this new resource based on the parent
            this.setParentTargetUri(onem2mParentResource.getParentTargetUri() + "/" + onem2mParentResource.getName());

            this.setPrimitiveContent("{\n" +
                    "\n" +
                    "    \"m2m:acp\":{\n" +
//                    "      \"et\": \"" + exptime +"\",\n" +
                    "      \"pv\":\n" +
                    "        {\"acr\":[{\n" +
                    "              \n" +
                    "          \"acor\" : [\"*\"],\n" +
                    "          \"acop\":63\n" +
                    "              \n" +
                    "        },\n" +
                    "        ]},\n" +
                    "      \"pvs\":\n" +
                    "        {\"acr\":[{\n" +
                    "              \n" +
                    "          \"acor\" : [\"admin\"],\n" +
                    "          \"acop\":63\n" +
                    "              \n" +
                    "        },\n" +
                    "        ]}\n" +
                    "       \n" +
                    "    }\n" +
                    "  \n" +
                    "}");



            ResourceAccessControlPolicy.handleDefaultCreate(this, onem2mResponse);

            // if the create was successful, ie no error has already happened, set CREATED for status code here
            if (onem2mResponse.getPrimitiveContent() == null) {
                onem2mResponse.setPrimitiveContent("Provisioned default ACP for " + cseId + ", name: " + "_deaultACP");
            }

        } finally {
        }
    }
}

