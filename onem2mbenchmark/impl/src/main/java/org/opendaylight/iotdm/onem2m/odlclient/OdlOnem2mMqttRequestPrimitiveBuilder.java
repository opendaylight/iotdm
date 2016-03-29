/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.odlclient;

import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OdlOnem2mMqttRequestPrimitiveBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(OdlOnem2mMqttRequestPrimitiveBuilder.class);

    private OdlOnem2mMqttRequestPrimitive onem2mRequest;
    /**
     * The onem2m-protocols use this to create a new RequestPrimitive class
     */
    public OdlOnem2mMqttRequestPrimitiveBuilder() {
        onem2mRequest = new OdlOnem2mMqttRequestPrimitive();
    }

    public OdlOnem2mMqttRequestPrimitiveBuilder setOperationCreate() {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.OPERATION, Onem2m.Operation.CREATE);
        return this;
    }
    public OdlOnem2mMqttRequestPrimitiveBuilder setOperationRetrieve() {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.OPERATION, Onem2m.Operation.RETRIEVE);
        return this;
    }
    public OdlOnem2mMqttRequestPrimitiveBuilder setOperationUpdate() {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.OPERATION, Onem2m.Operation.UPDATE);
        return this;
    }
    public OdlOnem2mMqttRequestPrimitiveBuilder setOperationDelete() {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.OPERATION, Onem2m.Operation.DELETE);
        return this;
    }
    public OdlOnem2mMqttRequestPrimitiveBuilder setTo(String value) {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.TO, value);
        return this;
    }
    public OdlOnem2mMqttRequestPrimitiveBuilder setFrom(String value) {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.FROM, value);
        return this;
    }

    public OdlOnem2mMqttRequestPrimitiveBuilder setRequestIdentifier(String value) {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.REQUEST_IDENTIFIER, value);
        return this;
    }

    public OdlOnem2mMqttRequestPrimitiveBuilder setResourceType(Integer value) {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.RESOURCE_TYPE, value);
        return this;
    }
    public OdlOnem2mMqttRequestPrimitiveBuilder setName(String value) {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.NAME, value);
        return this;
    }

    public OdlOnem2mMqttRequestPrimitiveBuilder setContent(String value) {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.CONTENT, value);
        return this;
    }

    public OdlOnem2mMqttRequestPrimitiveBuilder setResponseType(String value) {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.RESPONSE_TYPE, value);
        return this;
    }
    public OdlOnem2mMqttRequestPrimitiveBuilder setResultPersistence(String value) {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.RESULT_PERSISTENCE, value);
        return this;
    }

    public OdlOnem2mMqttRequestPrimitiveBuilder setResultContent(String value) {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.RESULT_CONTENT, value);
        return this;
    }
    public OdlOnem2mMqttRequestPrimitiveBuilder setDeliveryAggregation(String value) {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.DELIVERY_AGGREGATION, value);
        return this;
    }
    public OdlOnem2mMqttRequestPrimitiveBuilder setFilterCriteriaCreatedBefore(String value) {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.FILTER_CRITERIA_CREATED_BEFORE, value);
        return this;
    }
    public OdlOnem2mMqttRequestPrimitiveBuilder setFilterCriteriaCreatedAfter(String value) {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.FILTER_CRITERIA_CREATED_AFTER, value);
        return this;
    }
    public OdlOnem2mMqttRequestPrimitiveBuilder setFilterCriteriaResourceType(String value) {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.FILTER_CRITERIA_RESOURCE_TYPE, value);
        return this;
    }
    public OdlOnem2mMqttRequestPrimitiveBuilder setFilterCriteriaLabel(String value) {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.FILTER_CRITERIA_LABELS, value);
        return this;
    }
    public OdlOnem2mMqttRequestPrimitiveBuilder setFilterCriteriaFilterUsage(String value) {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.FILTER_CRITERIA_FILTER_USAGE, value);
        return this;
    }
    public OdlOnem2mMqttRequestPrimitiveBuilder setFilterCriteriaLimit(String value) {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.FILTER_CRITERIA_LIMIT, value);
        return this;
    }
    public OdlOnem2mMqttRequestPrimitiveBuilder setDiscoveryResultType(String value) {
        JsonUtils.put(onem2mRequest.mqttRequest, RequestPrimitive.DISCOVERY_RESULT_TYPE, value);
        return this;
    }
    public OdlOnem2mMqttRequestPrimitive build() {
        return (onem2mRequest);
    }
}

