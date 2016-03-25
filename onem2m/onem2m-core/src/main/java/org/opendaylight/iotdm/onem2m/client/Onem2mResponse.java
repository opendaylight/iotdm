/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
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
    protected String resourceTypeString = null;

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
        return processJsonResourceType(jsonContent);
    }

    private JSONObject processJsonResourceType(JSONObject j) {

        Iterator<?> keys = j.keys();
        if (keys.hasNext()) {
            String key = (String) keys.next();
            Object o = j.opt(key);
            if (!(o instanceof JSONObject)) {
                return j;
            }
            switch (key) {

                case "m2m:" + Onem2m.ResourceTypeString.AE:
                case Onem2m.ResourceTypeString.AE:
                    resourceTypeString = Onem2m.ResourceTypeString.AE;
                    return (JSONObject) o;

                case "m2m:" + Onem2m.ResourceTypeString.CONTAINER:
                case Onem2m.ResourceTypeString.CONTAINER:
                    resourceTypeString = Onem2m.ResourceTypeString.CONTAINER;
                    return (JSONObject) o;

                case "m2m:" + Onem2m.ResourceTypeString.CONTENT_INSTANCE:
                case Onem2m.ResourceTypeString.CONTENT_INSTANCE:
                    resourceTypeString = Onem2m.ResourceTypeString.CONTENT_INSTANCE;
                    return (JSONObject) o;

                case "m2m:" + Onem2m.ResourceTypeString.CSE_BASE:
                case Onem2m.ResourceTypeString.CSE_BASE:
                    resourceTypeString = Onem2m.ResourceTypeString.CSE_BASE;
                    return (JSONObject) o;

                case "m2m:" + Onem2m.ResourceTypeString.SUBSCRIPTION:
                case Onem2m.ResourceTypeString.SUBSCRIPTION:
                    resourceTypeString = Onem2m.ResourceTypeString.SUBSCRIPTION;
                    return (JSONObject) o;

                default:
                    /**
                     * If the server does not respond with a resource object,
                     */
                    return j;
            }
        }
        return null;
    }

    protected boolean processCommonJsonContent(String key) {

        Object o = jsonContent.opt(key);

        switch (key) {

            case ResourceContent.PARENT_ID:
                if (!(o instanceof String)) {
                    LOG.error("String expected for json key: " + key);
                    return false;
                }
                this.parentId = (String) o;
                break;
            case ResourceContent.RESOURCE_ID:
                if (!(o instanceof String)) {
                    LOG.error("String expected for json key: " + key);
                    return false;
                }
                this.resourceId = (String) o;
                break;
            case ResourceContent.RESOURCE_NAME:
                if (!(o instanceof String)) {
                    LOG.error("String expected for json key: " + key);
                    return false;
                }
                this.resourceName = (String) o;
                break;
            case ResourceContent.CREATION_TIME:
                if (!(o instanceof String)) {
                    LOG.error("String expected for json key: " + key);
                    return false;
                }
                this.creationTime = (String) o;
                break;
            case ResourceContent.EXPIRATION_TIME:
                if (!(o instanceof String)) {
                    LOG.error("String expected for json key: " + key);
                    return false;
                }
                this.expirationTime = (String) o;
                break;
            case ResourceContent.LAST_MODIFIED_TIME:
                if (!(o instanceof String)) {
                    LOG.error("String expected for json key: " + key);
                    return false;
                }
                this.lastModifiedTime = (String) o;
                break;
            case ResourceContent.RESOURCE_TYPE:
                if (!(o instanceof Integer)) {
                    LOG.error("Integer expected for json key: " + key);
                    return false;
                }
                this.resourceType = (Integer) o;
                break;

            case ResourceContent.STATE_TAG:
                if (!(o instanceof Integer)) {
                    LOG.error("Integer expected for json key: " + key);
                    return false;
                }
                this.stateTag = (Integer) o;
                break;

            case ResourceContent.LABELS:
                if (!(o instanceof JSONArray)) {
                    LOG.error("Array expected for json key: " + key);
                    return false;
                }
                JSONArray a = (JSONArray) o;
                for (int i = 0; i < a.length(); i++) {
                    if (!(a.opt(i) instanceof String)) {
                        LOG.error("String expected for label: " + key);
                        return false;
                    }
                    this.labels[i] = (String) a.opt(i);
                }
                break;

            case "error":
                if (!(o instanceof String)) {
                    LOG.error("String expected for json key: " + key);
                    return false;
                }
                this.errorString = (String) o;
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
