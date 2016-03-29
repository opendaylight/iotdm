/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.rest.utils;

import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.primitive.list.Onem2mPrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationPrimitive extends BasePrimitive {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationPrimitive.class);

    public static final String URI = "uri";
    public static final String CONTENT = "notification";

    public NotificationPrimitive() {
        super();
    }

    public void setPrimitivesList(List<Onem2mPrimitive> onem2mPrimitivesList) {
        for (Onem2mPrimitive onem2mPrimitive : onem2mPrimitivesList) {
            switch (onem2mPrimitive.getName()) {
                case NotificationPrimitive.URI:
                    setPrimitiveMany(onem2mPrimitive.getName(), onem2mPrimitive.getValue());
                    break;
                default:
                    setPrimitive(onem2mPrimitive.getName(), onem2mPrimitive.getValue());
                    break;
            }
        }
    }

    private List<String> subscriptionResourceList;
    public void setSubscriptionResourceList(List<String> srl) {
        this.subscriptionResourceList = srl;
    }
    public List<String> getSubscriptionResourceList() {
        return this.subscriptionResourceList;
    }

    // the original resourceContent used to return content based on result content requested
    protected ResourceContent resourceContent;
    public void setResourceContent(ResourceContent rc) {
        this.resourceContent = rc;
    }
    public ResourceContent getResourceContent() {
        return this.resourceContent;
    }

    protected Onem2mResource subscriptionResource;
    public void setSubscriptionResource(Onem2mResource sr) {
        this.subscriptionResource = sr;
    }
    public Onem2mResource getSubscriptionResource() {
        return this.subscriptionResource;
    }

    private JSONObject jsonSubscriptionResourceContent;
    public void setJsonSubscriptionResourceContent(String jsonubscriptionResourceContentString) {
        try {
            this.jsonSubscriptionResourceContent = new JSONObject(jsonubscriptionResourceContentString);
        } catch (JSONException e) {
            LOG.error("Invalid JSON {}", jsonubscriptionResourceContentString, e);
            throw new IllegalArgumentException("Invalid JSON", e);
        }
    }
    public JSONObject getJsonSubscriptionResourceContent() {
        return this.jsonSubscriptionResourceContent;
    }

}
