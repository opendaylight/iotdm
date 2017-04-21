/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.rest.utils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.resource.BaseResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.primitive.list.Onem2mPrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class RequestPrimitive extends BasePrimitive {

    private static final Logger LOG = LoggerFactory.getLogger(RequestPrimitive.class);

    private Onem2mDb.CseBaseResourceLocator targetResourceLocator = null;

    public void setTargetResourceLocator(@Nonnull final Onem2mDb.CseBaseResourceLocator resourceLocator) {
        this.targetResourceLocator = resourceLocator;
    }

    public Onem2mDb.CseBaseResourceLocator getTargetResourceLocator() {
        return this.targetResourceLocator;
    }

    /**
     * This is the onem2m request primitive.  Parameters can be filled in by restconf, or they will be filled in by the
     * onem2m-protocols as each specific protocol is responsible for binding its internal protocol fields to this
     * protocol independent onenm2m format.  This file implements section 7.1 of TS0004.
     */

    // taken from CDT-requestPrimitive-v1_0_0.xsd / TS0004_v_1-0_1 Section 8.2.2 Short Names
    public static final String OPERATION = "op";
    public static final String TO = "to";
    public static final String FROM = "fr";
    public static final String REQUEST_IDENTIFIER = "rqi";
    public static final String RESOURCE_TYPE = "ty";
    public static final String NAME = "nm";
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
    public static final String FILTER_CRITERIA_CREATED_BEFORE = "crb";
    public static final String FILTER_CRITERIA_CREATED_AFTER = "cra";
    public static final String FILTER_CRITERIA_MODIFIED_SINCE = "ms";
    public static final String FILTER_CRITERIA_UNMODIFIED_SINCE = "us";
    public static final String FILTER_CRITERIA_STATE_TAG_SMALLER = "sts";
    public static final String FILTER_CRITERIA_STATE_TAG_BIGGER = "stb";
    public static final String FILTER_CRITERIA_LABELS = "lbl";
    public static final String FILTER_CRITERIA_RESOURCE_TYPE = "rty";
    public static final String FILTER_CRITERIA_SIZE_ABOVE = "sza";
    public static final String FILTER_CRITERIA_SIZE_BELOW = "szb";
    public static final String FILTER_CRITERIA_ATTRIBUTE = "atr";
    public static final String FILTER_CRITERIA_FILTER_USAGE = "fu";
    public static final String FILTER_CRITERIA_LIMIT = "lim";
    public static final String FILTER_CRITERIA_OFFSET = "off";
    public static final String DISCOVERY_RESULT_TYPE = "drt";
    public static final String ROLE = "rol";
    public static final String ATTRIBUTE_LIST = "atrl";

    // helper attributes
    public static final String PROTOCOL = "protocol"; // See Protocol below
    public static final String CONTENT_FORMAT = "contentFormat"; // See ContentFormat below
    public static final String NATIVEAPP_NAME = "nativeAppName"; // if Protocol is NATIVE_APP then set this parm

    // hard code set of acceptable primitive attributes, short name
    public static final Set<String> primitiveAttributes = new HashSet<String>() {{
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
        add(FILTER_CRITERIA_CREATED_BEFORE);
        add(FILTER_CRITERIA_CREATED_AFTER);
        add(FILTER_CRITERIA_MODIFIED_SINCE);
        add(FILTER_CRITERIA_UNMODIFIED_SINCE);
        add(FILTER_CRITERIA_STATE_TAG_SMALLER);
        add(FILTER_CRITERIA_STATE_TAG_BIGGER);
        add(FILTER_CRITERIA_LABELS);
        add(FILTER_CRITERIA_RESOURCE_TYPE);
        add(FILTER_CRITERIA_SIZE_ABOVE);
        add(FILTER_CRITERIA_SIZE_BELOW);
        add(FILTER_CRITERIA_FILTER_USAGE);
        add(FILTER_CRITERIA_LIMIT);
        add(FILTER_CRITERIA_OFFSET);
        add(DISCOVERY_RESULT_TYPE);
        add(PROTOCOL);
        add(CONTENT_FORMAT);
        add(NATIVEAPP_NAME);
        add(ROLE);
    }};

    public RequestPrimitive() {
        super();
    }

    protected String primitiveTo;
    public String getPrimitiveTo() { return primitiveTo; }
    public void setPrimitiveTo(String primitiveTo) { this.primitiveTo = primitiveTo; }

    protected String primitiveFrom;
    public String getPrimitiveFrom() { return primitiveFrom; }

    protected String primitiveName;
    public String getPrimitiveName() { return primitiveName; }
    public void setPrimitiveName(String primitiveName) { this.primitiveName = primitiveName; }

    protected Integer primitiveOperation = -1;
    public Integer getPrimitiveOperation() { return primitiveOperation; }

    protected Integer primitiveResourceType = -1;
    public Integer getPrimitiveResourceType() { return primitiveResourceType; }
    public void setPrimitiveResourceType(Integer primitiveResourceType) { this.primitiveResourceType = primitiveResourceType; }

    protected String primitiveContentFormat;
    public String getPrimitiveContentFormat() { return primitiveContentFormat; }
    public void setPrimitiveContentFormat(String primitiveContentFormat) { this.primitiveContentFormat = primitiveContentFormat; }

    protected String primitiveContent;
    public String getPrimitiveContent() { return primitiveContent; }
    public void setPrimitiveContent(String primitiveContent) { this.primitiveContent = primitiveContent; }

    protected String primitiveNativeAppName;
    public String getPrimitiveNativeAppName() { return primitiveNativeAppName; }
    public void setPrimitiveNativeAppName(String primitiveNativeAppName) { this.primitiveNativeAppName = primitiveNativeAppName; }

    protected Integer primitiveResponseType = -1;
    public Integer getPrimitiveResponseType() { return primitiveResponseType; }

    protected Integer primitiveResultContent = -1;
    public Integer getPrimitiveResultContent() { return primitiveResultContent; }

    protected String primitiveRequestIdentifier;
    public String getPrimitiveRequestIdentifier() { return primitiveRequestIdentifier; }

    protected String primitiveFilterCriteriaCreatedBefore;
    public String getPrimitiveFilterCriteriaCreatedBefore() { return primitiveFilterCriteriaCreatedBefore; }

    protected String primitiveFilterCriteriaCreatedAfter;
    public String getPrimitiveFilterCriteriaCreatedAfter() { return primitiveFilterCriteriaCreatedAfter; }

    protected String primitiveFilterCriteriaUnModifiedSince;
    public String getPrimitiveFilterCriteriaUnModifiedSince() { return primitiveFilterCriteriaUnModifiedSince; }

    protected String primitiveFilterCriteriaModifiedSince;
    public String getPrimitiveFilterCriteriaModifiedSince() { return primitiveFilterCriteriaModifiedSince; }

    protected Integer primitiveFilterCriteriaStateTagSmaller = -1;
    public Integer getPrimitiveFilterCriteriaStateTagSmaller() { return primitiveFilterCriteriaStateTagSmaller; }

    protected Integer primitiveFilterCriteriaStateTagBigger = -1;
    public Integer getPrimitiveFilterCriteriaStateTagBigger() { return primitiveFilterCriteriaStateTagBigger; }

    protected Integer primitiveFilterCriteriaSizeAbove = -1;
    public Integer getPrimitiveFilterCriteriaSizeAbove() { return primitiveFilterCriteriaSizeAbove; }

    protected Integer primitiveFilterCriteriaSizeBelow = -1;
    public Integer getPrimitiveFilterCriteriaSizeBelow() { return primitiveFilterCriteriaSizeBelow; }

    protected Integer primitiveFilterCriteriaLimit = -1;
    public Integer getPrimitiveFilterCriteriaLimit() { return primitiveFilterCriteriaLimit; }

    protected Integer primitiveFilterCriteriaOffset = -1;
    public Integer getPrimitiveFilterCriteriaOffset() { return primitiveFilterCriteriaOffset; }

    protected Integer primitiveFilterCriteriaFilterUsage = -1;
    public Integer getPrimitiveFilterCriteriaFilterUsage() { return primitiveFilterCriteriaFilterUsage; }

    protected Integer primitiveDiscoveryResultType = -1;
    public Integer getPrimitiveDiscoveryResultType() { return primitiveDiscoveryResultType; }
    public void setPrimitiveDiscoveryResultType(Integer primitiveDiscoveryResultType) {
        this.primitiveDiscoveryResultType = primitiveDiscoveryResultType;
    }

    protected List<String> primitiveFilterCriteriaLabels;
    public List<String> getPrimitiveFilterCriteriaLabels() { return primitiveFilterCriteriaLabels; }

    protected List<Integer> primitiveFilterCriteriaResourceTypes;
    public List<Integer> getPrimitiveFilterCriteriaResourceTypes() { return primitiveFilterCriteriaResourceTypes; }

    protected String primitiveProtocol;
    public String getPrimitiveProtocol() { return primitiveProtocol; }

    protected Object writerTransaction;
    public void setWriterTransaction(Object writerTransaction) {
        this.writerTransaction = writerTransaction;
    }
    public Object getWriterTransaction() {
        return this.writerTransaction;
    }

    public boolean isCreate;
    public boolean isUpdate;
    public boolean found_filter_criteria = false;
    /**
     * The following section is used to hold attributes for the data store.  In the case of create, when the parent
     * resource is read from the database, it is stored in the 'Onem2mResource onem2mResource' variable.
     * IN the case of read, update, and delete, when the resource is found in the database, the actual resource
     * is stored in 'Onem2mResource onem2mResource' variable.
     * When creating a new resource, the ResourceContent inJsonContent are used to "build" the attributes that will go into
     * the database.
     */
    protected String resourceId; // filled in by onem2mDb.CRUD routines, even if read hURI or nhURI
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
    public String getResourceId() {
        return this.resourceId;
    }

    protected String parentResourceId; // filled in by onem2mDb.CRUD routines, even if read hURI or nhURI
    public void setParentResourceId(String parentResourceId) {
        this.parentResourceId = parentResourceId;
    }
    public String getParentResourceId() {
        return this.parentResourceId;
    }

    // in a requestPrimitive, the RequestPrimitive.NAME is used to load into the resourceName
    protected String resourceName;
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
    public String getResourceName() {
        return this.resourceName;
    }

    // set teh paretnTarget Uri so it can be stored with the resoruce to be created
    protected String parentTargetUri;
    public void setParentTargetUri(String parentTargetUri) {
        this.parentTargetUri = parentTargetUri;
    }
    public String getParentTargetUri() {
        return this.parentTargetUri;
    }

    protected Integer resourceType;
    public void setResourceType(String resourceType) {
        this.resourceType = Integer.valueOf(resourceType);
    }
    public void setResourceType(Integer resourceType) {
        this.resourceType = resourceType;
    }
    public Integer getResourceType() {
        return this.resourceType;
    }

    protected Integer parentResourceType;
    public void setParentResourceType(String parentResourceType) {
        this.parentResourceType = Integer.valueOf(parentResourceType);
    }
    public Integer getParentResourceType() {
        return this.parentResourceType;
    }
    /*
     * For C:
     *      - parent resource before the new resource is created,
     *      - the actual (new) resource after it's created (stored in Onem2mDB)
     * For RUD:
     *      - actual resource, this is what is in the data store
     */
    protected Onem2mResource onem2mResource;
    public void setOnem2mResource(Onem2mResource onem2mResource) {
        this.onem2mResource = onem2mResource;
    }
    public Onem2mResource getOnem2mResource() {
        return this.onem2mResource;
    }

    protected Onem2mResource parentOnem2mResource;
    public void setParentOnem2mResource(Onem2mResource parentOnem2mResource) {
        this.parentOnem2mResource = parentOnem2mResource;
    }
    public Onem2mResource getParentOnem2mResource() {
        return this.parentOnem2mResource;
    }

    protected JSONObject parentJsonResourceContent;
    public void setParentJsonResourceContent(String jsonString) {
        try {
            this.parentJsonResourceContent = new JSONObject(jsonString);
        } catch (JSONException e) {
            LOG.error("Invalid JSON {}", jsonString, e);
            throw new IllegalArgumentException("Invalid JSON", e);
        }
    }
    public JSONObject getParentJsonResourceContent() {
        return this.parentJsonResourceContent;
    }

    protected BaseResource baseResource;
    public void setBaseResource(BaseResource rc) {
        this.baseResource = rc;
    }
    public BaseResource getBaseResource() {
        return this.baseResource;
    }

    private String jsonResourceContentString;
    public void setJsonResourceContentString(String v) { this.jsonResourceContentString = v; }
    public String getJsonResourceContentString() { return this.jsonResourceContentString; }

    private JSONObject jsonResourceContent;
    public void setJsonResourceContent(JSONObject jsonResourceContent) {
        this.jsonResourceContent = jsonResourceContent;
    }
    public void setJsonResourceContent(String jsonResourceContentString) {
        try {
            this.jsonResourceContent = new JSONObject(jsonResourceContentString);
        } catch (JSONException e) {
            LOG.error("Invalid JSON {}", jsonResourceContentString, e);
            throw new IllegalArgumentException("Invalid JSON", e);
        }
    }
    public JSONObject getJsonResourceContent() {
        return this.jsonResourceContent;
    }

    public boolean getHasFilterCriteria() {
        return this.found_filter_criteria;
    }

    private boolean fuDiscovery;
    public void setFUDiscovery(boolean fuDiscovery) {
        this.fuDiscovery = fuDiscovery;
    }
    public boolean getFUDiscovery() {
        return this.fuDiscovery;
    }

    private boolean parentContentHasBeenModified;
    public void setParentContentHasBeenModified(boolean parentContentHasBeenModified) {
        this.parentContentHasBeenModified = parentContentHasBeenModified;
    }
    public boolean getParentContentHasBeenModified() {
        return this.parentContentHasBeenModified;
    }

    public String getContentAttributeString(final String attrName) {
        if ((null == this.jsonResourceContent) || (! this.jsonResourceContent.has(attrName))) {
            return null;
        }

        try {
            return this.jsonResourceContent.getString(attrName);
        } catch (JSONException e) {
            LOG.trace("Failed to get attribute {}", attrName);
            return null;
        }
    }

    public Boolean getContentAttributeBoolean(final String attrName) {
        if ((null == this.jsonResourceContent) || (! this.jsonResourceContent.has(attrName))) {
            return null;
        }

        try {
            return new Boolean(this.jsonResourceContent.getBoolean(attrName));
        } catch (JSONException e) {
            LOG.trace("Failed to get attribute {}", attrName);
            return null;
        }
    }

    public String[] getContentAttributeArray(final String attrName) {
        if ((null == this.jsonResourceContent) || (! this.jsonResourceContent.has(attrName))) {
            return null;
        }

        JSONArray jsonAttrArray = this.jsonResourceContent.getJSONArray(attrName);
        ArrayList<String> attrArray = new ArrayList<>();
        for (int i =0; i < jsonAttrArray.length(); i++) {
            try {
                attrArray.add(jsonAttrArray.getString(i));
            } catch (JSONException e) {
                LOG.trace("Failed to get entry (index {}) from JSON attribute array: {}", i, jsonAttrArray.toString());
                return null;
            }
        }

        return attrArray.toArray(new String[0]);
    }

    public boolean hasContentAttributeList() {
        return nonNull(primitiveContent) && primitiveContent.contains("\"" + RequestPrimitive.ATTRIBUTE_LIST + "\":");
    }
}
