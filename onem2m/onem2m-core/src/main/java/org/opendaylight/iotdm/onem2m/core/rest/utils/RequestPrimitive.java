/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.rest.utils;

import java.util.*;
import org.opendaylight.iotdm.onem2m.core.database.DbAttr;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.primitive.list.Onem2mPrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestPrimitive extends BasePrimitive {

    private static final Logger LOG = LoggerFactory.getLogger(RequestPrimitive.class);


    /**
     * This is the onem2m request primitive.  Parameters can be filled in by restconf, or they will be filled in by the
     * onem2m-protocols as each specific protocol is responsible for binding its internal protocol fields to this
     * protocol independent onenm2m format.  This file implements section 7.1 of TS0004.
     * TODO: need to go through the spec and verify only support local CSE, blocking requests, ...
     * Need to ensure that I do nothing to prevent forwarding to other CSE's architecturally/structurally
     */
    public RequestPrimitive() {
        super();
    }
    public RequestPrimitive(List<Onem2mPrimitive> onem2mPrimitivesList) {
        super(onem2mPrimitivesList);
    }

    // taken from CDT-requestPrimitive-v1_0_0.xsd / TS0004_v_1-0_1 Section 8.2.2 Short Names
    public static final String OPERATION = "op";
    public static final String TO = "to";
    public static final String FROM = "fr";
    public static final String REQUEST_IDENTIFIER = "rqi";
    public static final String RESOURCE_TYPE = "ty";
    public static final String NAME = "nm"; // TODO: how is this used ... primitive
    public static final String CONTENT = "pc";
    public static final String ORIGINATING_TIMESTAMP = "ot";
    public static final String REQUEST_EXPIRATION_TIMESTAMP = "rqet";
    public static final String RESULT_EXPIRATION_TIMESTAMP = "rset";
    public static final String OPERATION_EXECUTION_TIME = "oet";
    public static final String RESPONSE_TYPE = "rt";
    public static final String RESULT_PERSISTENCE = "rp";
    public static final String RESULT_CONTENT = "rcn";
    public static final String EVENT_CATEGORY = "ec";
    public static final String DELIVERY_AGGREGATION = "da";
    public static final String GROUP_REQUEST_IDENTIFIER = "gid";
    public static final String FILTER_CRITERIA = "fc";
    public static final String DISCOVERY_RESULT_TYPE = "drt";

    // helper attributes
    public static final String PROTOCOL = "protocol"; // See Protocol below
    public static final String CONTENT_FORMAT = "contentFormat"; // See ContentFormat below
    public static final String NATIVEAPP_NAME = "nativeAppName"; // if Protocol is NATIVE_APP then set this parm


    // hard code set of acceptable primitive attributes, short name
    public static final Set<String> primitiveAttributes = new HashSet<String>() {{
        // short; long
        add(OPERATION);
        add(TO);
        add(FROM);
        add(REQUEST_IDENTIFIER);
        add(RESOURCE_TYPE);
        add(NAME);
        add(CONTENT);
        add(ORIGINATING_TIMESTAMP);
        add(REQUEST_EXPIRATION_TIMESTAMP);
        add(RESULT_EXPIRATION_TIMESTAMP);
        add(OPERATION_EXECUTION_TIME);
        add(RESPONSE_TYPE);
        add(RESULT_PERSISTENCE);
        add(RESULT_CONTENT);
        add(EVENT_CATEGORY);
        add(DELIVERY_AGGREGATION);
        add(GROUP_REQUEST_IDENTIFIER);
        add(FILTER_CRITERIA);
        add(DISCOVERY_RESULT_TYPE);
        add(PROTOCOL);
        add(CONTENT_FORMAT);
        add(NATIVEAPP_NAME);
    }};

    // hard code set of long to short name
    public static final Map<String,String> longToShortAttributes = new HashMap<String,String>() {{
        // short; long
        put("operation", OPERATION);
        put("to", TO);
        put("from", FROM);
        put("requestIdentifier", REQUEST_IDENTIFIER);
        put("resourceType", RESOURCE_TYPE);
        put("name", NAME);
        put("content", CONTENT);
        put("originatingTimestamp", ORIGINATING_TIMESTAMP);
        put("requestExpirationTimestamp", REQUEST_EXPIRATION_TIMESTAMP);
        put("resultExpirationTimestamp", RESULT_EXPIRATION_TIMESTAMP);
        put("operationExecutionTime", OPERATION_EXECUTION_TIME);
        put("responseType", RESPONSE_TYPE);
        put("resultPersistence", RESULT_PERSISTENCE);
        put("resultContent", RESULT_CONTENT);
        put("eventCategory", EVENT_CATEGORY);
        put("deliveryAggregation", DELIVERY_AGGREGATION);
        put("groupRequestIdentifier", GROUP_REQUEST_IDENTIFIER);
        put("filterCriteria", FILTER_CRITERIA);
        put("discoveryResultType", DISCOVERY_RESULT_TYPE);
        put("protocol", PROTOCOL);
        put("contentFormat", CONTENT_FORMAT);
        put("nativeAppName", NATIVEAPP_NAME);
    }};

    /**
     * The following section is used to hold attributes for the data store.  In the case of create, when the parent
     * resource is read from the database, it is stored in the 'Onem2mResource onem2mResource' variable.
     * IN the case of read, update, and delete, when the resource is found in the database, the actual resource
     * is stored in 'Onem2mResource onem2mResource' variable.
     * When creating a new resource, the DbAttrs and DbAttrSets are used to "build" the attributes that will go into
     * the database.
     */
    protected String resourceId; // filled in by onem2mDb.CRUD routines, even if read hURI or nhURI
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
    public String getResourceId() {
        return this.resourceId;
    }

    // in a requestPrimitive, the RequestPrimitive.NAME is used to load into the resourceName
    protected String resourceName;
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
    public String getResourceName() {
        return this.resourceName;
    }

    // parent for C, actual resource for RUD, this is what is in the data store
    protected Onem2mResource onem2mResource;
    public void setOnem2mResource(Onem2mResource onem2mResource) {
        this.onem2mResource = onem2mResource;
    }
    public Onem2mResource getOnem2mResource() {
        return this.onem2mResource;
    }

    // parent for C, actual resource for RUD, this is what is in the data store
    protected DbAttr dbAttrs;
    public void setDbAttrs(DbAttr dbAttrs) {
        this.dbAttrs = dbAttrs;
    }
    public DbAttr getDbAttrs() {
        return this.dbAttrs;
    }

    protected ResourceContent resourceContent;
    public void setResourceContent(ResourceContent rc) {
        this.resourceContent = rc;
    }
    public ResourceContent getResourceContent() {
        return this.resourceContent;
    }

    private String requestType;
    public void setRequestType(String rt) {
        this.requestType = rt;
    }
    public String getRequestType() {
        return this.requestType;
    }

    private Set<String> validAttributes;
    public void setValidAttributes(Set<String> va) {
        this.validAttributes = va;
    }
    public Set<String> getValidAttributes() {
        return this.validAttributes;
    }
}
