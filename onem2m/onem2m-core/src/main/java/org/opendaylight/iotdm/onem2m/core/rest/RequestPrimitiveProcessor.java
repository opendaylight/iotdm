/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.iotdm.onem2m.core.database.DbAttr;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContainer;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceCse;
import org.opendaylight.iotdm.onem2m.core.rest.ResourceContentProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.ResultContentProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.primitive.list.Onem2mPrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Attr;
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
        resourceContent = new ResourceContent();
    }

    /**
     *
     * @param onem2mResponse
     * @return
     */
    public boolean validatePrimitiveAttributes(ResponsePrimitive onem2mResponse)  {
        for (Onem2mPrimitive onem2mResource : this.onem2mPrimitivesList) {
            if (!RequestPrimitive.primitiveAttributes.contains(onem2mResource.getName())) {
                String shortName = RequestPrimitive.longToShortAttributes.get(onem2mResource.getName());
                if (shortName != null) {
                    // replace the primitive with the short name
                    // for now fall thru and error
                }
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "REQUEST_PRIMITIVES(" + onem2mResource.getName() + ") not valid");
                return false;
            }
        }
        return true;
    }

    /**
     * Ensure the name is OK
     * @param name
     * @return
     */
    public boolean validateResourceName(String name)  {
        if (name.contentEquals("latest") || name.contentEquals("oldest") ||
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
    public boolean validateUri(String uriString)  {
        try {
            URI toUri = new URI(uriString);
        } catch (URISyntaxException e) {
            return false;
        }
        return true;
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
     *
     * @param onem2mResponse
     */
    public void handleOperation(ResponsePrimitive onem2mResponse) {

        // make sure the attributes exist
        if (!validatePrimitiveAttributes(onem2mResponse)) {
            return;
        }

        // Use table TS0004: 7.1.1.1-1 to validate mandatory parameters

        // is there a protocol?  This is an internal option that CoAP, MQTT, HTTP, (RESTconf), and any internal
        // app MUST set.  Why ... I do not know yet :-)
        String protocol = this.getPrimitive(RequestPrimitive.PROTOCOL);
        if (protocol == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "PROTOCOL(" + RequestPrimitive.PROTOCOL + ") not specified");
            return;
        }

        // is there an operation?
        String operation = this.getPrimitive(RequestPrimitive.OPERATION);
        if (operation == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "OPERATION(" + RequestPrimitive.OPERATION + ") not specified");
            return;
        }

        // TODO: RFC 3986 ... reserved characters in a URI
        String to = getPrimitive(RequestPrimitive.TO);
        if (to == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "TO(" + RequestPrimitive.TO + ") not specified");
            return;
        } else if (!validateUri(to)) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "TO(" + RequestPrimitive.TO + ") not value URI: " + to);
            return;
        }

        String from = getPrimitive(RequestPrimitive.FROM);
        if (from == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "FROM(" + RequestPrimitive.FROM + ") not specified");
            return;
        } else if (!validateUri(from)) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "FROM(" + RequestPrimitive.FROM + ") not value URI: " + from);
            return;
        }

        // if the request had a REQUEST_IDENTIFIER, return it in the response so client can correlate
        String rqi = getPrimitive(RequestPrimitive.REQUEST_IDENTIFIER);
        if (rqi != null) {
            onem2mResponse.setPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER, rqi);
        } else {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "REQUEST_IDENTIFIER(" + RequestPrimitive.REQUEST_IDENTIFIER + ") not specified");
        }

        // ensure resource type is present only in CREATE requests
        String resourceType = getPrimitive(RequestPrimitive.RESOURCE_TYPE);
        if (resourceType == null) {
            if (operation.contentEquals(Onem2m.Operation.CREATE)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "RESOURCE_TYPE(" + RequestPrimitive.RESOURCE_TYPE + ") not specified for CREATE");
                return;
            }
        } else { // a response type was specified, then only CREATE can have one
            if (!operation.contentEquals(Onem2m.Operation.CREATE)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "RESOURCE_TYPE(" + RequestPrimitive.RESOURCE_TYPE + ") not permitted for operation: " + operation);
                return;
            }
            // resource type value will be verified later
        }

        // this is an optional parameter but we will reject unsupported values
        // only support blocking requests at this time, if not provided we default to blocking anyway
        String rt = getPrimitive(RequestPrimitive.RESPONSE_TYPE);
        if (rt != null && !rt.contentEquals(Onem2m.ResponseType.BLOCKING_REQUEST)) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NON_BLOCKING_REQUEST_NOT_SUPPORTED,
                    "Invalid response type: " + rt);
            return;
        }

        // if NAME is provided, only for CREATE
        String resourceName = this.getPrimitive((RequestPrimitive.NAME));
        if (resourceName != null) {
            if (!operation.contentEquals(Onem2m.Operation.CREATE)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "NAME(" + RequestPrimitive.NAME + ") not permitted for operation: " + operation);
                return;
            }
            if (!validateResourceName(resourceName)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INVALID_ARGUMENTS,
                        "Resource name invalid: " + resourceName);
                return;
            }
        }

        // filter criteria only valid for RETRIEVE
        String fc = getPrimitive(RequestPrimitive.FILTER_CRITERIA);
        if (fc != null) {
            if (!operation.contentEquals(Onem2m.Operation.RETRIEVE)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "FILTER_CRITERIA(" + RequestPrimitive.FILTER_CRITERIA +
                                ") not permitted for operation: " + operation);
                return;
            }
        }

        // discovery_result_type only valid for RETRIEVE
        String drt = getPrimitive(RequestPrimitive.DISCOVERY_RESULT_TYPE);
        if (drt != null) {
            if (!operation.contentEquals(Onem2m.Operation.RETRIEVE)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "DISCOVERY_RESULT_TYPE(" + RequestPrimitive.DISCOVERY_RESULT_TYPE +
                                ") not permitted for operation: " + operation);
                return;
            } else if (!(drt.contentEquals(Onem2m.DiscoveryResultType.NON_HIERARCHICAL) ||
                         drt.contentEquals(Onem2m.DiscoveryResultType.HIERARCHICAL))) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "DISCOVERY_RESULT_TYPE(" + RequestPrimitive.DISCOVERY_RESULT_TYPE +
                                ") invalid option: " + drt);
                return;
            }
        }

        // perform more specific parameter validation for the operation requested
        switch (operation) {
            case Onem2m.Operation.CREATE:
                if (!resourceType.contentEquals(Onem2m.ResourceType.CSE_BASE)) {
                    handleOperationCreate(onem2mResponse);
                } else {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                            "Cannot create a CSE Base, it must be provisioned separately!");
                }
                break;
            case Onem2m.Operation.RETRIEVE:
                handleOperationRetrieve(onem2mResponse);
                break;
            case Onem2m.Operation.UPDATE:
                handleOperationUpdate(onem2mResponse);
                break;
            case Onem2m.Operation.DELETE:
                handleOperationDelete(onem2mResponse);
                break;
            case Onem2m.Operation.NOTIFY:
                handleOperationNotify(onem2mResponse);
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_IMPLEMENTED,
                        "OPERATION(" + RequestPrimitive.OPERATION + ") NOTIFY not implemented");
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
     * @param onem2mResponse
     */
    public void handleOperationCreate(ResponsePrimitive onem2mResponse) {

        // Use table TS0004: 7.1.1.1-1 to validate CREATE specific parameters that were not handled in the
        // handleOperation

        // ensure the create has content ...
        String cf = getPrimitive(RequestPrimitive.CONTENT_FORMAT);
        if (cf == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "CONTENT_FORMAT(" + RequestPrimitive.CONTENT_FORMAT + ") not specified");
            return;
        } else if (!cf.contentEquals(Onem2m.ContentFormat.JSON) && !cf.contentEquals(Onem2m.ContentFormat.XML)) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "CONTENT_FORMAT(" + RequestPrimitive.CONTENT_FORMAT + ") not accepted (" + cf + ")");
            return;
        }
        String cn = getPrimitive(RequestPrimitive.CONTENT);
        if (cn == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "CONTENT(" + RequestPrimitive.CONTENT_FORMAT + ") not specified");
            return;
        }

        // validate result content options for create
        String rc = getPrimitive(RequestPrimitive.RESULT_CONTENT);
        if (rc != null) {
            if (!(rc.contentEquals(Onem2m.ResultContent.ATTRIBUTES) ||
                    rc.contentEquals(Onem2m.ResultContent.NOTHING) ||
                    rc.contentEquals(Onem2m.ResultContent.HIERARCHICAL_ADDRESS) ||
                    rc.contentEquals(Onem2m.ResultContent.HIERARCHICAL_ADDRESS_ATTRIBUTES))) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "RESULT_CONTENT(" + RequestPrimitive.RESULT_CONTENT + ") not accepted (" + rc + ")");
                return;
            }
        }

        // lookup the resource ... this will be the parent where the new resource will be created
        if (!Onem2mDb.getInstance().FindResourceUsingURI(this, onem2mResponse)) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_FOUND,
                    "Resource target URI not found: " + this.getPrimitive(RequestPrimitive.TO));
            return;
        }
        // the Onem2mResource is now stored in the onem2mRequest ... as it has been read in from the data store

        // if the a name is provided, ensure it is valid and unique at this hierarchical level
        String resourceName = this.getPrimitive((RequestPrimitive.NAME));
        if (resourceName != null) {
            // using the parent, see if this new resource name already exists under this parent resource
            if (Onem2mDb.getInstance().FindResourceUsingIdAndName(this.getOnem2mResource().getResourceId(), resourceName)) {
                // TS0004: 7.2.3.2
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONFLICT,
                        "Resource already exists: " + this.getPrimitive(RequestPrimitive.TO) + "/" + resourceName);
                return;
            }
            this.setResourceName(resourceName);
        }

        // process the resource specific attributes
        ResourceContentProcessor.handleCreate(this, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null) {
            return;
        }

        // now format a response based on result content desired
        ResultContentProcessor.handleCreate(this, onem2mResponse);

        // TODO: see TS0004 6.8
        // if the create was successful, ie no error has already happened, set CREATED for status code here
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) == null) {
            onem2mResponse.setPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE, Onem2m.ResponseStatusCode.CREATED);
        }
    }

    /**
     * Handle the request primitive retrieve ...
     * TODO: Strategy for error handling ... TS0004 7.1.1.2
     * @param onem2mResponse
     */
    public void handleOperationRetrieve(ResponsePrimitive onem2mResponse) {

        // Use table TS0004: 7.1.1.1-1 to validate RETRIEVE specific parameters that were not handled in the calling routine

        // if the content format is provided then it must be supported
        String cf = getPrimitive(RequestPrimitive.CONTENT_FORMAT);
        if (cf != null) {
            if (!cf.contentEquals(Onem2m.ContentFormat.JSON) && !cf.contentEquals(Onem2m.ContentFormat.XML)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "CONTENT_FORMAT(" + RequestPrimitive.CONTENT_FORMAT + ") not accepted (" + cf + ")");
                return;
            }
        }

        // validate result content options for retrieve
        String rc = getPrimitive(RequestPrimitive.RESULT_CONTENT);
        if (rc != null) {
            if (!(rc.contentEquals(Onem2m.ResultContent.ATTRIBUTES) ||
                    rc.contentEquals(Onem2m.ResultContent.ATTRIBUTES_CHILD_RESOURCES) ||
                    rc.contentEquals(Onem2m.ResultContent.CHILD_RESOURCE_REFS) ||
                    rc.contentEquals(Onem2m.ResultContent.ATTRIBUTES_CHILD_RESOURCE_REFS))) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "RESULT_CONTENT(" + RequestPrimitive.RESULT_CONTENT + ") not accepted (" + rc + ")");
                return;
            }
        }

        // return hierarchical address by default if not specified
        String drt = getPrimitive(RequestPrimitive.DISCOVERY_RESULT_TYPE);
        if (drt == null) {
            setPrimitive(RequestPrimitive.DISCOVERY_RESULT_TYPE, Onem2m.DiscoveryResultType.HIERARCHICAL);
        }

        // find the resource using the TO URI ...
        if (!Onem2mDb.getInstance().FindResourceUsingURI(this, onem2mResponse)) {

            // TODO: maybe we are trying to find the attribute, write FindResourceUsingURIAndAttribute

            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_FOUND,
                    "Resource target URI not found: " + this.getPrimitive(RequestPrimitive.TO));
            return;
        }

        // return the data according to result content and filter criteria
        ResultContentProcessor.handleRetrieve(this, onem2mResponse);

        // TODO: see TS0004 6.8
        // if FOUND, and all went well, send back OK
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) == null) {
            onem2mResponse.setPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE, Onem2m.ResponseStatusCode.OK);
        }
    }

    /**
     * Handle the request primitive delete ...
     * TODO: Strategy for error handling ... TS0004 7.1.1.2
     * @param onem2mResponse
     */
    public void handleOperationDelete(ResponsePrimitive onem2mResponse) {

        // Use table TS0004: 7.1.1.1-1 to validate DELETE specific parameters that were not handled in the calling routine

        String cf = getPrimitive(RequestPrimitive.CONTENT);
        if (cf != null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INVALID_ARGUMENTS,
                    "CONTENT(" + RequestPrimitive.CONTENT + ") not permitted");
            return;
        }

        // validate result content options
        String rc = getPrimitive(RequestPrimitive.RESULT_CONTENT);
        if (rc != null) {
            if (!(rc.contentEquals(Onem2m.ResultContent.ATTRIBUTES) ||
                    rc.contentEquals(Onem2m.ResultContent.ATTRIBUTES_CHILD_RESOURCES) ||
                    rc.contentEquals(Onem2m.ResultContent.NOTHING) ||
                    rc.contentEquals(Onem2m.ResultContent.CHILD_RESOURCE_REFS) ||
                    rc.contentEquals(Onem2m.ResultContent.ATTRIBUTES_CHILD_RESOURCE_REFS))) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "RESULT_CONTENT(" + RequestPrimitive.RESULT_CONTENT + ") not accepted (" + rc + ")");
                return;
            }
        }

        /**
         * Find the resource, fill in the response based on result content
         */
        if (Onem2mDb.getInstance().FindResourceUsingURI(this, onem2mResponse) == false) {
            // TODO: is it idempotent or not ... fail or succeed???
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_FOUND,
                    "Resource target URI not found: " + this.getPrimitive(RequestPrimitive.TO));
            return;
        }

        DbAttr parentDbAttrs = this.getDbAttrs();
        String rt = parentDbAttrs.getAttr(ResourceContent.RESOURCE_TYPE);
        if (rt != null && rt.contentEquals(Onem2m.ResourceType.CSE_BASE)) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                    "Not permitted to delete this resource: " + this.getPrimitive(RequestPrimitive.TO));
            return;
        }

        ResultContentProcessor.handleDelete(this, onem2mResponse);

        // now delete the resource from the database
        // TODO: idempotent so who cares if cannot find the resource ... is this true?
        if (Onem2mDb.getInstance().DeleteResourceUsingURI(this, onem2mResponse) == false) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR,
                    "Resource target URI data store delete error: " + this.getPrimitive(RequestPrimitive.TO));
            return;
        }

        // need to figure out notification strategy

        // TODO: see TS0004 6.8
        // if FOUND, and all went well, send back OK
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) == null) {
            onem2mResponse.setPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE, Onem2m.ResponseStatusCode.DELETED);
        }
    }

    /**
     * Handle update
     * @param onem2mResponse
     */
    public void handleOperationUpdate(ResponsePrimitive onem2mResponse) {

        // Use table TS0004: 7.1.1.1-1 to validate UPDATE specific parameters that were not handled in the calling routine

        // ensure the update has content ...
        String cf = getPrimitive(RequestPrimitive.CONTENT_FORMAT);
        if (cf == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INSUFFICIENT_ARGUMENTS, "CONTENT_FORMAT(" + RequestPrimitive.CONTENT_FORMAT + ") not specified");
            return;
        } else if (!cf.contentEquals(Onem2m.ContentFormat.JSON) && !cf.contentEquals(Onem2m.ContentFormat.XML)) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT_FORMAT(" + RequestPrimitive.CONTENT_FORMAT + ") not accepted (" + cf + ")");
            return;
        }
        String cn = getPrimitive(RequestPrimitive.CONTENT);
        if (cn == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INSUFFICIENT_ARGUMENTS, "CONTENT(" + RequestPrimitive.CONTENT_FORMAT + ") not specified");
            return;
        }

        // validate result content options
        String rc = getPrimitive(RequestPrimitive.RESULT_CONTENT);
        if (rc != null) {
            if (!(rc.contentEquals(Onem2m.ResultContent.ATTRIBUTES) ||
                    rc.contentEquals(Onem2m.ResultContent.ATTRIBUTES_CHILD_RESOURCES) ||
                    rc.contentEquals(Onem2m.ResultContent.NOTHING) ||
                    rc.contentEquals(Onem2m.ResultContent.CHILD_RESOURCE_REFS) ||
                    rc.contentEquals(Onem2m.ResultContent.ATTRIBUTES_CHILD_RESOURCE_REFS))) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "RESULT_CONTENT(" + RequestPrimitive.RESULT_CONTENT + ") not accepted (" + rc + ")");
                return;
            }
        }

        // now find the resource from the database
        if (Onem2mDb.getInstance().FindResourceUsingURI(this, onem2mResponse) == false) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_FOUND,
                    "Resource target URI not found: " + this.getPrimitive(RequestPrimitive.TO));
            return;
        }

        //RequestContentProcessor.updateResultContent(this, onem2mResponse);
        //ResultContentProcessor.handleUpdate(this, onem2mResponse);
        // need to figure out notification strategy

        // TODO: see TS0004 6.8
        // if FOUND, and all went well, send back CHANGED
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) == null) {
            onem2mResponse.setPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE, Onem2m.ResponseStatusCode.CHANGED);
        }
    }

    /**
     * Support this when we do CSE-CSE (Mcc) communication
     * @param onem2mResponse
     */
    public void handleOperationNotify(ResponsePrimitive onem2mResponse) {
        // Use table TS0004: 7.1.1.1-1 to validate NOTIFY specific parameters that were not handled in the calling routine
    }

    /**
     * Internally create a cse base based on the restconf call which is designed to be called from a provisioning
     * server or management app.
     * @param onem2mResponse
     */
    public void provisionCse(ResponsePrimitive onem2mResponse) {

        String cseId = this.getPrimitive("CSE_ID");
        if (cseId == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CSE_ID not specified!");
            return;
        }
        String cseType = this.getPrimitive("CSE_TYPE");
        if (cseType == null) {
            cseType = "IN-CSE";
        } else if (!cseType.contentEquals("IN-CSE")) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "IN-CSE is the only one supported :-(");
            return;
        }

        this.setPrimitive(RequestPrimitive.RESOURCE_TYPE, Onem2m.ResourceType.CSE_BASE);
        this.setPrimitive(RequestPrimitive.NAME, cseId);
        this.setResourceName(cseId);

        if (Onem2mDb.getInstance().FindCseByName(cseId)) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.ALREADY_EXISTS, "CSE name already exists: " + cseId);
            return;
        }

        this.setPrimitive(RequestPrimitive.CONTENT_FORMAT, Onem2m.ContentFormat.JSON);
        JSONObject j = new JSONObject();
        j.put(ResourceCse.CSE_ID, cseId);
        j.put(ResourceCse.CSE_TYPE, cseType);
        this.setPrimitive(RequestPrimitive.CONTENT, j.toString());

        // process the resource specific attributes
        ResourceContentProcessor.handleCreate(this, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null) {
            return;
        }

        // TODO: see TS0004 6.8
        // if the create was successful, ie no error has already happened, set CREATED for status code here
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) == null) {
            onem2mResponse.setPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE,
                    "Provisioned cseBase: " + cseId + " type: " + cseType);
        }
    }
}

