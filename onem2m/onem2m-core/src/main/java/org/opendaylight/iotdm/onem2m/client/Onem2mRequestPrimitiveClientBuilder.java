/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.client;

import static org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive.ATTRIBUTE_LIST;

import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mRequestPrimitiveClientBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mRequestPrimitiveClient.class);

    private Onem2mRequestPrimitiveClient onem2mRequest;
    protected boolean isCreate = false;
    protected boolean isDelete = false;


    public Onem2mRequestPrimitiveClientBuilder() {
        onem2mRequest = new Onem2mRequestPrimitiveClient();
    }

    public Onem2mRequestPrimitiveClientBuilder setOperationCreate() {
        String op = ((Integer)Onem2m.Operation.CREATE).toString();
        onem2mRequest.setPrimitive(RequestPrimitive.OPERATION, op);
        isCreate = true;
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setOperationRetrieve() {
        String op = ((Integer)Onem2m.Operation.RETRIEVE).toString();
        onem2mRequest.setPrimitive(RequestPrimitive.OPERATION, op);
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setOperationUpdate() {
        String op = ((Integer)Onem2m.Operation.UPDATE).toString();
        onem2mRequest.setPrimitive(RequestPrimitive.OPERATION, op);
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setOperationDelete() {
        String op = ((Integer)Onem2m.Operation.DELETE).toString();
        onem2mRequest.setPrimitive(RequestPrimitive.OPERATION, op);
        isDelete = true;
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setOperationNotify() {
        String op = ((Integer)Onem2m.Operation.NOTIFY).toString();
        onem2mRequest.setPrimitive(RequestPrimitive.OPERATION, op);
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setTo(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.TO, value);
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setFrom(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.FROM, value);
        return this;
    }

    public Onem2mRequestPrimitiveClientBuilder setRequestIdentifier(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.REQUEST_IDENTIFIER, value);
        return this;
    }

    public Onem2mRequestPrimitiveClientBuilder setResourceType(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.RESOURCE_TYPE, value);
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setResourceType(Integer value) {
        onem2mRequest.setPrimitive(RequestPrimitive.RESOURCE_TYPE, value.toString());
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setName(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.NAME, value);
        return this;
    }

    public Onem2mRequestPrimitiveClientBuilder setPrimitiveContent(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.CONTENT, value);
        return this;
    }

    public Onem2mRequestPrimitiveClientBuilder setOriginatingTimestamp(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.ORIGINATING_TIMESTAMP, value);
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setRequestExpirationTimestamp(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.REQUEST_EXPIRATION_TIMESTAMP, value);
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setResultExpirationTimestamp(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.RESULT_EXPIRATION_TIMESTAMP, value);
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setOperationExecutionTime(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.OPERATION_EXECUTION_TIME, value);
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setResponseType(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.RESPONSE_TYPE, value);
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setResultPersistence(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.RESULT_PERSISTENCE, value);
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setResultContent(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.RESULT_CONTENT, value);
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setDeliveryAggregation(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.DELIVERY_AGGREGATION, value);
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setGroupRequestIdentifier(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.GROUP_REQUEST_IDENTIFIER, value);
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setDiscoveryResultType(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.DISCOVERY_RESULT_TYPE, value);
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setProtocol(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.PROTOCOL, value);
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setContentFormat(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.CONTENT_FORMAT, value);
        return this;
    }
    public Onem2mRequestPrimitiveClientBuilder setNativeAppName(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.NATIVEAPP_NAME, value);
        return this;
    }
    /**
     * Take a coap or http query string, parse the options add them to the onem2m request primitives.  The onem2m core
     * will validate the parameters.  The return of the existence of a resource type is a convenience hack so that
     * we can discern whether the operation is a CREATE or a NOTIFY.  Need to think about how 3rd party protocols
     * will encode their options ... maybe they will simply add each option as a primitive directly, or they can
     * construct an option string ... which ever makes more sense.
     * TODO: look at MQTT to see how it encodes some of its primitives; does it use a query string?
     * @param query query string
     * @return is resource type present in the query string
     */
    public Boolean parseQueryStringIntoPrimitives(String query) {
        Boolean resourceTypePresent = false;
        if (query != null) {
            String[] pairs = query.split("[&]");
            for (String pair : pairs) {
                String[] param = pair.split("[=]");
                String key;
                String value;
                if (param.length > 0) {
                    key = param[0];
                    if (param.length > 1) {
                        value = param[1];
                        if (key.contentEquals(RequestPrimitive.FILTER_CRITERIA_LABELS) ||
                            key.contentEquals(RequestPrimitive.FILTER_CRITERIA_RESOURCE_TYPE)) {
                            onem2mRequest.setPrimitiveMany(key, value);
                        }
                        else if(key.contentEquals(ATTRIBUTE_LIST)) {
                            String[] attributes = value.split("\\+");
                            onem2mRequest.setPrimitive(RequestPrimitive.CONTENT, JsonUtils.put(new JSONObject(), ATTRIBUTE_LIST, attributes).toString());
                        }
                        else {
                            onem2mRequest.setPrimitive(key, value);
                        }
                        if (key.equalsIgnoreCase(RequestPrimitive.RESOURCE_TYPE)) { resourceTypePresent = true; }
                    }
                }
            }
        }
        return resourceTypePresent;
    }
    public Onem2mRequestPrimitiveClient build() {
        return (onem2mRequest);
    }

    public  Onem2mRequestPrimitiveClientBuilder setPrimitiveNameValue(String key, String value) {
        onem2mRequest.setPrimitive(key, value);
        return this;
    }

    public String getPrimitiveValue(String key) {
        return onem2mRequest.getPrimitive(key);
    }

    public void delPrimitive(String name) {
        onem2mRequest.delPrimitive(name);
    }
}

