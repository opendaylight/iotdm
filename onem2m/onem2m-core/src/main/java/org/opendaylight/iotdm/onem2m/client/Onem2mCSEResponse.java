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
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceCse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mCSEResponse extends Onem2mResponse {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCSEResponse.class);

    private String cseId;
    private String cseType;
    private Integer notificationCongestionPolicy;
    private Integer[] supportedResourceTypes;

    private Onem2mCSEResponse() { }

    public Onem2mCSEResponse(String jsonContent) {
        super(jsonContent);
        if (success) {
            success = processJsonContent();
        }
//        if (success && !Onem2m.ResourceType.CSE_BASE.contentEquals(getResourceType().toString())) {
//            success = false;
//            setError("Cannot construct an CSE response with resource type: " + getResourceType());
//        }
    }
    public Onem2mCSEResponse(Onem2mResponsePrimitiveClient onem2mResponse) {
        super(onem2mResponse.getContent());
        if (success) {
            success = processJsonContent();
        }
        if (success && !onem2mResponse.responseOk()) {
            success = false;
        }
    }

    public String getCseId() {
        return cseId;
    }
    public String getCseType() {
        return cseType;
    }
    public Integer getNotificationCongestionPolicy() {
        return notificationCongestionPolicy;
    }
    public Integer[] getSupportedResourceTypes() {
        return supportedResourceTypes;
    }

    private boolean processJsonContent() {
//        JSONObject j = jsonContent.getJSONObject("m2m:" + Onem2m.ResourceTypeString.CSE_BASE);
//        if (j == null) {
//            j = jsonContent.getJSONObject(Onem2m.ResourceTypeString.CSE_BASE);
//        }
//        if (j == null) {
//            LOG.error("Expecting {} or {}", "m2m:" + Onem2m.ResourceTypeString.CSE_BASE,Onem2m.ResourceTypeString.CSE_BASE);
//            return false;
//        }
//        jsonContent = j;
        if (!Onem2m.ResourceTypeString.CSE_BASE.contentEquals(resourceTypeString)) {
            LOG.error("Expecting {} or {}", "m2m:" + Onem2m.ResourceTypeString.CSE_BASE, Onem2m.ResourceTypeString.CSE_BASE);
            return false;
        }
        Iterator<?> keys = jsonContent.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();

            Object o = jsonContent.opt(key);

            switch (key) {

                case ResourceCse.CSE_ID:
                    if (!(o instanceof String)) {
                        LOG.error("String expected for json key: " + key);
                        return false;
                    }
                    this.cseId = (String) o;
                    break;
                case ResourceCse.CSE_TYPE:
                    if (!(o instanceof String)) {
                        LOG.error("String expected for json key: " + key);
                        return false;
                    }
                    this.cseType = (String) o;
                    break;
                case ResourceCse.NOTIFICATION_CONGESTION_POLICY:
                    if (!(o instanceof Integer)) {
                        LOG.error("Integer expected for json key: " + key);
                        return false;
                    }
                    this.notificationCongestionPolicy = (Integer) o;
                    break;
                case ResourceCse.SUPPORTED_RESOURCE_TYPES:
                    if (!(o instanceof JSONArray)) {
                        LOG.error("Array expected for json key: " + key);
                        return false;
                    }
                    JSONArray a = (JSONArray) o;
                    for (int i = 0; i < a.length(); i++) {
                        if (!(a.opt(i) instanceof Integer)) {
                            LOG.error("Integer expected for supported resource type: " + key);
                            return false;
                        }
                        this.supportedResourceTypes[i] = (Integer) a.opt(i);
                    }
                    break;
                default:
                    if (!super.processCommonJsonContent(key))
                        return false;
            }
        }
        return true;
    }
}

