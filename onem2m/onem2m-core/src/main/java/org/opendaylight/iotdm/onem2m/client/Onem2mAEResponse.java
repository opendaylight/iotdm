/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.client;

import java.util.Iterator;
import org.json.JSONException;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceAE;
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

        if (!Onem2m.ResourceTypeString.AE.contentEquals(resourceTypeString)) {
            LOG.error("Expecting {} or {}", "m2m:" + Onem2m.ResourceTypeString.AE,Onem2m.ResourceTypeString.AE);
            return false;
        }

        Iterator<?> keys = jsonContent.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();

            Object o = jsonContent.opt(key);

            switch (key) {

                case ResourceAE.APP_NAME:
                    if (!(o instanceof String)) {
                        LOG.error("String expected for json key: " + key);
                        return false;
                    }
                    this.appName = (String) o;
                    break;
                case ResourceAE.REQUEST_REACHABILITY:
                    if (!(o instanceof Boolean)) {
                        LOG.error("Boolean expected for json key: " + key);
                        return false;
                    }
                    this.requestReachability = (Boolean) o;
                    break;
                case ResourceAE.AE_ID:
                    if (!(o instanceof String)) {
                        LOG.error("String expected for json key: " + key);
                        return false;
                    }
                    this.aeId = (String) o;
                    break;
                case ResourceAE.ONTOLOGY_REF:
                    if (!(o instanceof String)) {
                        LOG.error("String expected for json key: " + key);
                        return false;
                    }
                    this.ontologyRef = (String) o;
                    break;
                case ResourceAE.APP_ID:
                    if (!(o instanceof String)) {
                        LOG.error("String expected for json key: " + key);
                        return false;
                    }
                    this.appId = (String) o;
                    break;
                default:
                    if (!super.processCommonJsonContent(key))
                        return false;
            }
        }
        return true;
    }
}

