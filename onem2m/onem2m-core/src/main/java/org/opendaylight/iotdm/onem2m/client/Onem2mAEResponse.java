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
import org.opendaylight.iotdm.onem2m.core.resource.ResourceAE;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mAEResponse extends Onem2mResponse {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mAEResponse.class);

    private String appName;
    private String appId;
    private String aeId;
    private String ontologyRef;
    private Boolean requestReachability;

    private Onem2mAEResponse() { }

    public Onem2mAEResponse(String jsonContent) {
        super(jsonContent);
        if (success) {
            success = processJsonContent();
        }
//        if (success && !Onem2m.ResourceType.AE.contentEquals(getResourceType().toString())) {
//            success = false;
//            setError("Cannot construct an AE response with resource type: " + getResourceType());
//        }
    }

    public Onem2mAEResponse(Onem2mResponsePrimitiveClient onem2mResponse) {
        super(onem2mResponse.getContent());
        if (success) {
            success = processJsonContent();
        }
        if (success && !onem2mResponse.responseOk()) {
            success = false;
        }
    }

    public String getAppName() {
        return appName;
    }
    public String getAppId() {
        return appId;
    }
    public String getAEId() {
        return aeId;
    }
    public String getOntologyRef() {
        return ontologyRef;
    }
    public Boolean getRequestReachability() {
        return requestReachability;
    }
    private boolean processJsonContent() {

        //JSONObject j = jsonContent.getJSONObject("m2m:" + Onem2m.ResourceTypeString.AE);
        //if (j == null) {
        //    j = jsonContent.getJSONObject(Onem2m.ResourceTypeString.AE);
        //}
        //if (j == null) {
        //    LOG.error("Expecting {} or {}", "m2m:" + Onem2m.ResourceTypeString.AE,Onem2m.ResourceTypeString.AE);
        //    return false;
        //}
        if (!Onem2m.ResourceTypeString.AE.contentEquals(resourceTypeString)) {
            LOG.error("Expecting {} or {}", "m2m:" + Onem2m.ResourceTypeString.AE,Onem2m.ResourceTypeString.AE);
            return false;
        }

        Iterator<?> keys = jsonContent.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();

            Object o = jsonContent.get(key);

            switch (key) {

                case ResourceAE.APP_NAME:
                    if (!(o instanceof String)) {
                        LOG.error("String expected for json key: " + key);
                        return false;
                    }
                    this.appName = o.toString();
                    break;
                case ResourceAE.REQUEST_REACHABILITY:
                    if (!(o instanceof Boolean)) {
                        LOG.error("Boolean expected for json key: " + key);
                        return false;
                    }
                    this.requestReachability = Boolean.valueOf(o.toString());
                    break;
                case ResourceAE.AE_ID:
                    if (!(o instanceof String)) {
                        LOG.error("String expected for json key: " + key);
                        return false;
                    }
                    this.appId = o.toString();
                    break;
                case ResourceAE.ONTOLOGY_REF:
                    if (!(o instanceof String)) {
                        LOG.error("String expected for json key: " + key);
                        return false;
                    }
                    this.ontologyRef = o.toString();
                    break;
                case ResourceAE.APP_ID:
                    if (!(o instanceof String)) {
                        LOG.error("String expected for json key: " + key);
                        return false;
                    }
                    this.appId = o.toString();
                    break;
                default:
                    if (!super.processCommonJsonContent(key))
                        return false;
            }
        }
        return true;
    }
}

