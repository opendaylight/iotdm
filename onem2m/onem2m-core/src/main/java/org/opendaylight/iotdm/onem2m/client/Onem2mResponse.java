/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.client;

import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceAE;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.attr.set.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mResponse {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mResponse.class);

    protected JSONObject jsonContent;
    protected boolean success = true;
    private String parentId;
    private String resourceId;
    private String resourceName;
    private String creationTime;
    private String lastModifiedTime;
    private String expirationTime;
    private Integer resourceType;
    private Integer stateTag;
    private String[] labels;
    private String errorString;

    protected Onem2mResponse() {}

    public Onem2mResponse(String jsonContentString) {
        jsonContent = parseJson(jsonContentString);
        if (jsonContent == null) {
            success = false;
        }
    }

    private JSONObject parseJson(String jsonContentString) {

        JSONObject jsonContent;

        if (jsonContentString == null) {
            return null;
        }
        try {
            jsonContent = new JSONObject(jsonContentString.trim());
        } catch (JSONException e) {
            LOG.error("Parser error (" + e + ")");
            return null;
        }

        // the json should be an array of objects called "any", we support only one element in the array so pull
        // that element in the array out and place it in jsonContent
        return processJsonAnyArray(jsonContent);
    }

    private JSONObject processJsonAnyArray(JSONObject jAnyArray) {

        Iterator<?> keys = jAnyArray.keys();
        while( keys.hasNext() ) {
            String key = (String)keys.next();
            Object o = jAnyArray.get(key);

            switch (key) {

                case "any":
                    if (!(o instanceof JSONArray)) {
                        LOG.error("Array expected for json key: " + key);
                        return null;
                    }
                    JSONArray array = (JSONArray) o;
                    if (array.length() != 1) {
                        LOG.error("Too many elements: json array length: " + array.length());
                        return null;
                    }
                    if (!(array.get(0) instanceof JSONObject)) {
                        LOG.error("JSONObject expected");
                        return null;
                    }
                    return (JSONObject) array.get(0);

                default:
                    /**
                     * If the server does not respond with an "any" array, then return the JSONObject as is
                     */
                    return jAnyArray;
            }
        }
        return null;
    }

    protected boolean processCommonJsonContent(String key) {

        Object o = jsonContent.get(key);

        switch (key) {

            case ResourceContent.PARENT_ID:
                if (!(o instanceof String)) {
                    LOG.error("String expected for json key: " + key);
                    return false;
                }
                this.parentId = o.toString();
                break;
            case ResourceContent.RESOURCE_ID:
                if (!(o instanceof String)) {
                    LOG.error("String expected for json key: " + key);
                    return false;
                }
                this.resourceId = o.toString();
                break;
            case ResourceContent.RESOURCE_NAME:
                if (!(o instanceof String)) {
                    LOG.error("String expected for json key: " + key);
                    return false;
                }
                this.resourceName = o.toString();
                break;
            case ResourceContent.CREATION_TIME:
                if (!(o instanceof String)) {
                    LOG.error("String expected for json key: " + key);
                    return false;
                }
                this.creationTime = o.toString();
                break;
            case ResourceContent.EXPIRATION_TIME:
                if (!(o instanceof String)) {
                    LOG.error("String expected for json key: " + key);
                    return false;
                }
                this.expirationTime = o.toString();
                break;
            case ResourceContent.LAST_MODIFIED_TIME:
                if (!(o instanceof String)) {
                    LOG.error("String expected for json key: " + key);
                    return false;
                }
                this.lastModifiedTime = o.toString();
                break;
            case ResourceContent.RESOURCE_TYPE:
                if (!(o instanceof Integer)) {
                    LOG.error("Integer expected for json key: " + key);
                    return false;
                }
                this.resourceType = ((Integer) o).intValue();
                break;

            case ResourceContent.STATE_TAG:
                if (!(o instanceof Integer)) {
                    LOG.error("Integer expected for json key: " + key);
                    return false;
                }
                this.stateTag = ((Integer) o).intValue();
                break;

            case ResourceContent.LABELS:
                if (!(o instanceof JSONArray)) {
                    LOG.error("Array expected for json key: " + key);
                    return false;
                }
                JSONArray a = new JSONArray();
                for (int i = 0; i < a.length(); i++) {
                    if (!(a.get(i) instanceof String)) {
                        LOG.error("String expected for label: " + key);
                        return false;
                    }
                    this.labels[i] = a.get(i).toString();
                }
                break;

            case "error":
                if (!(o instanceof String)) {
                    LOG.error("String expected for json key: " + key);
                    return false;
                }
                this.errorString = o.toString();
                success = false;
                break;

            default:
                LOG.error("Unexpected json key: " + key);
                return false;
        }
        return true;
    }
    protected String getParentId() {
        return this.parentId;
    }
    public String getResourceId() {
        return this.resourceId;
    }
    public String getResourceName() {
        return this.resourceName;
    }
    public String getCreationTime() {
        return this.creationTime;
    }
    public String getExpirationTime() {
        return this.expirationTime;
    }
    public String getLastModifiedTime() {
        return this.lastModifiedTime;
    }
    public Integer getResourceType() {
        return this.resourceType;
    }
    public Integer getStateTag() {
        return this.stateTag;
    }
    public String[] getLabels() {
        return this.labels;
    }
    public String getError() {
        return this.errorString;
    }
    public void setError(String e) {
        this.errorString = e;
    }
    public boolean responseOk() {
        return success;
    }
    public JSONObject getJSONObject() {
        return this.jsonContent;
    }
}
