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
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContainer;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContentInstance;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mContentInstanceResponse extends Onem2mResponse {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mContainerResponse.class);

    private String content;
    private Integer contentSize;
    private String contentInfo;
    private String ontologyRef;

    private Onem2mContentInstanceResponse() { }

    public Onem2mContentInstanceResponse(String jsonContent) {
        super(jsonContent);
        if (success) {
            success = processJsonContent();
        }
        if (success && !Onem2m.ResourceType.CONTENT_INSTANCE.contentEquals(getResourceType().toString())) {
            success = false;
            setError("Cannot construct an CONTENT_INSTANCE response with resource type: " + getResourceType());
        }
    }

    public Onem2mContentInstanceResponse(Onem2mResponsePrimitiveClient onem2mResponse) {
        super(onem2mResponse.getContent());
        if (success) {
            success = processJsonContent();
        }
        if (success && !onem2mResponse.responseOk()) {
            success = false;
        }
    }

    public String getCIContent() {
        return content;
    }
    public Integer getContentSize() {
        return contentSize;
    }
    public String getContentInfo() {
        return contentInfo;
    }
    public String getOntologyRef() {
        return ontologyRef;
    }

    private boolean processJsonContent() {

        Iterator<?> keys = jsonContent.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();

            Object o = jsonContent.get(key);

            switch (key) {

                case ResourceContentInstance.CONTENT:
                    if (!(o instanceof String)) {
                        LOG.error("String expected for json key: " + key);
                        return false;
                    }
                    this.content = o.toString();
                    break;
                case ResourceContentInstance.CONTENT_INFO:
                    if (!(o instanceof String)) {
                        LOG.error("String expected for json key: " + key);
                        return false;
                    }
                    this.contentInfo = o.toString();
                    break;
                case ResourceContentInstance.CONTENT_SIZE:
                    if (!(o instanceof Integer)) {
                        LOG.error("Integer expected for json key: " + key);
                        return false;
                    }
                    this.contentSize = (Integer)(o);
                    break;
                case ResourceContainer.ONTOLOGY_REF:
                    if (!(o instanceof String)) {
                        LOG.error("String expected for json key: " + key);
                        return false;
                    }
                    this.ontologyRef = o.toString();
                    break;
                default:
                    if (!super.processCommonJsonContent(key))
                        return false;
            }
        }
        return true;
    }
}

