/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.client;

import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mAERequestBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mAERequestBuilder.class);

    private Onem2mAERequest aeRequest;
    private Onem2mRequestPrimitiveClientBuilder requestPrimitiveBuilder;
    private ResourceAEBuilder aeBuilder;

    public Onem2mAERequestBuilder() {
        aeRequest = new Onem2mAERequest();
        requestPrimitiveBuilder = new Onem2mRequestPrimitiveClientBuilder();
        aeBuilder = new ResourceAEBuilder();
        // set dome default parameters that all internal apps have no need to set but the core expects
        requestPrimitiveBuilder.setFrom("onem2m://Onem2mAERequest");
        requestPrimitiveBuilder.setRequestIdentifier("Onem2mAERequest-rqi");
        requestPrimitiveBuilder.setProtocol(Onem2m.Protocol.NATIVEAPP);
        requestPrimitiveBuilder.setContentFormat(Onem2m.ContentFormat.JSON);
        requestPrimitiveBuilder.setNativeAppName("Onem2mAERequest");

    }
    public Onem2mAERequestBuilder setOperationCreate() {
        requestPrimitiveBuilder.setOperationCreate();
        return this;
    }
    public Onem2mAERequestBuilder setOperationRetrieve() {
        requestPrimitiveBuilder.setOperationRetrieve();
        return this;
    }
    public Onem2mAERequestBuilder setOperationUpdate() {
        requestPrimitiveBuilder.setOperationUpdate();
        return this;
    }
    public Onem2mAERequestBuilder setOperationDelete() {
        requestPrimitiveBuilder.setOperationDelete();
        return this;
    }
    public Onem2mAERequestBuilder setOperationNotify() {
        requestPrimitiveBuilder.setOperationNotify();
        return this;
    }
    public Onem2mAERequestBuilder setTo(String value) {
        requestPrimitiveBuilder.setTo(value);
        return this;
    }
    public Onem2mAERequestBuilder setName(String value) {
        requestPrimitiveBuilder.setName(value);
        return this;
    }
    public Onem2mAERequestBuilder setOriginatingTimestamp(String value) {
        requestPrimitiveBuilder.setOriginatingTimestamp(value);
        return this;
    }
    public Onem2mAERequestBuilder setRequestExpirationTimestamp(String value) {
        requestPrimitiveBuilder.setRequestExpirationTimestamp(value);
        return this;
    }
    public Onem2mAERequestBuilder setResultExpirationTimestamp(String value) {
        requestPrimitiveBuilder.setResultExpirationTimestamp(value);
        return this;
    }
    public Onem2mAERequestBuilder setOperationExecutionTime(String value) {
        requestPrimitiveBuilder.setOperationExecutionTime(value);
        return this;
    }
    public Onem2mAERequestBuilder setResponseType(String value) {
        requestPrimitiveBuilder.setResponseType(value);
        return this;
    }
    public Onem2mAERequestBuilder setResultPersistence(String value) {
        requestPrimitiveBuilder.setResultPersistence(value);
        return this;
    }
    public Onem2mAERequestBuilder setResultContent(String value) {
        requestPrimitiveBuilder.setResultContent(value);
        return this;
    }
    public Onem2mAERequestBuilder setDeliveryAggregation(String value) {
        requestPrimitiveBuilder.setDeliveryAggregation(value);
        return this;
    }
    public Onem2mAERequestBuilder setGroupRequestIdentifier(String value) {
        requestPrimitiveBuilder.setGroupRequestIdentifier(value);
        return this;
    }
    /*
    public Onem2mAERequestBuilder setFilterCriteria(String value) {
        requestPrimitiveBuilder.setFilterCriteria(value);
        return this;
    }
    */
    public Onem2mAERequestBuilder setDiscoveryResultType(String value) {
        requestPrimitiveBuilder.setDiscoveryResultType(value);
        return this;
    }
    public Onem2mAERequestBuilder setAppName(String value) {
        aeBuilder.setAppName(value);
        return this;
    }
    public Onem2mAERequestBuilder setAppId(String value) {
        aeBuilder.setAppId(value);
        return this;
    }
    public Onem2mAERequestBuilder setAEId(String value) {
        aeBuilder.setAEId(value);
        return this;
    }
    public Onem2mAERequestBuilder setOntologyRef(String value) {
        aeBuilder.setOntologyRef(value);
        return this;
    }
    public Onem2mAERequest build() {
        String resourceAE = aeBuilder.build();
        requestPrimitiveBuilder.setContent(resourceAE);
        requestPrimitiveBuilder.setResourceType(Onem2m.ResourceType.AE);
        aeRequest.requestPrimitive = requestPrimitiveBuilder.build();
        return (aeRequest);
    }
}

