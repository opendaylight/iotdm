/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.client;

import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mRequestPrimitiveClientBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mRequestPrimitiveClient.class);

    private Onem2mRequestPrimitiveClient onem2mRequest;
    /**
     * The onem2m-protocols use this to create a new RequestPrimitive class
     */
    public Onem2mRequestPrimitiveClientBuilder() {
        onem2mRequest = new Onem2mRequestPrimitiveClient();
    }

    public Onem2mRequestPrimitiveClientBuilder setOperation(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.OPERATION, value);
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
    public Onem2mRequestPrimitiveClientBuilder setName(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.NAME, value);
        return this;
    }

    public Onem2mRequestPrimitiveClientBuilder setContent(String value) {
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

    public Onem2mRequestPrimitiveClientBuilder setFilterCriteria(String value) {
        onem2mRequest.setPrimitive(RequestPrimitive.FILTER_CRITERIA, value);
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
     * @param query
     * @return
     */
    public Boolean parseQueryStringIntoPrimitives(String query) {
        Boolean resourceTypePresent = false;
        if (query != null) {
            String[] pairs = query.split("[&]");
            for (String pair : pairs) {
                String[] param = pair.split("[=]");
                String key = null;
                String value = null;
                if (param.length > 0) {
                    key = param[0];
                    if (param.length > 1) {
                        value = param[1];
                        onem2mRequest.setPrimitive(key, value);
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
}

