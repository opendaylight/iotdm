/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.client;

import java.util.Iterator;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mContainerResponse extends Onem2mResponse {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mContainerResponse.class);

    private String creator;
    private Integer maxNrInstances;
    private Integer currNrInstances;
    private Integer maxByteSize;
    private Integer currByteSize;
    private String ontologyRef;

    private Onem2mContainerResponse() { }

    public Onem2mContainerResponse(String jsonContent) {
        super(jsonContent);
        if (success) {
            success = processJsonContent();
        }
//        if (success && !Onem2m.ResourceType.CONTAINER.contentEquals(getResourceType().toString())) {
//            success = false;
//            setError("Cannot construct an CONTAINER response with resource type: " + getResourceType());
//        }
    }

    public Onem2mContainerResponse(Onem2mResponsePrimitiveClient onem2mResponse) {
        super(onem2mResponse.getContent());
        if (success) {
            success = processJsonContent();
        }
        if (success && !onem2mResponse.responseOk()) {
            success = false;
        }
    }

    public String getCreator() {
        return creator;
    }
    public Integer getMaxNrInstances() {
        return maxNrInstances;
    }
    public Integer getCurrNrInstances() {
        return currNrInstances;
    }
    public Integer getMaxByteSize() {
        return maxByteSize;
    }
    public Integer getCurrByteSize() {
        return currByteSize;
    }
    public String getOntologyRef() {
        return ontologyRef;
    }

    private boolean processJsonContent() {

//        JSONObject j = jsonContent.getJSONObject("m2m:" + Onem2m.ResourceTypeString.CONTAINER);
//        if (j == null) {
//            j = jsonContent.getJSONObject(Onem2m.ResourceTypeString.CONTAINER);
//        }
//        if (j == null) {
//            LOG.error("Expecting {} or {}", "m2m:" + Onem2m.ResourceTypeString.CONTAINER,Onem2m.ResourceTypeString.CONTAINER);
//            return false;
//        }
//        jsonContent = j;
        if (!Onem2m.ResourceTypeString.CONTAINER.contentEquals(resourceTypeString)) {
            LOG.error("Expecting {} or {}", "m2m:" + Onem2m.ResourceTypeString.CONTAINER, Onem2m.ResourceTypeString.CONTAINER);
            return false;
        }

        Iterator<?> keys = jsonContent.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();

            Object o = jsonContent.opt(key);

            switch (key) {

                case ResourceContainer.CREATOR:
                    if (!(o instanceof String)) {
                        LOG.error("String expected for json key: " + key);
                        return false;
                    }
                    this.creator = (String) o;
                    break;
                case ResourceContainer.MAX_NR_INSTANCES:
                    if (!(o instanceof Integer)) {
                        LOG.error("Integer expected for json key: " + key);
                        return false;
                    }
                    this.maxNrInstances = (Integer) o;
                    break;
                case ResourceContainer.ONTOLOGY_REF:
                    if (!(o instanceof String)) {
                        LOG.error("String expected for json key: " + key);
                        return false;
                    }
                    this.ontologyRef = (String) o;
                    break;
                case ResourceContainer.MAX_BYTE_SIZE:
                    if (!(o instanceof Integer)) {
                        LOG.error("Integer expected for json key: " + key);
                        return false;
                    }
                    this.maxByteSize = (Integer) o;
                    break;
                case ResourceContainer.CURR_BYTE_SIZE:
                    if (!(o instanceof Integer)) {
                        LOG.error("Integer expected for json key: " + key);
                        return false;
                    }
                    this.currByteSize = (Integer) o;
                    break;
                case ResourceContainer.CURR_NR_INSTANCES:
                    if (!(o instanceof Integer)) {
                        LOG.error("Integer expected for json key: " + key);
                        return false;
                    }
                    this.currNrInstances = (Integer) o;
                    break;
                default:
                    if (!super.processCommonJsonContent(key))
                        return false;
            }
        }
        return true;
    }
}

