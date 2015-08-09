/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.odlclient;

import java.io.ByteArrayInputStream;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OdlOnem2mHttpRequestPrimitiveBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(OdlOnem2mHttpRequestPrimitiveBuilder.class);

    private OdlOnem2mHttpRequestPrimitive onem2mRequest;
    /**
     * The onem2m-protocols use this to create a new RequestPrimitive class
     */
    public OdlOnem2mHttpRequestPrimitiveBuilder() {
        onem2mRequest = new OdlOnem2mHttpRequestPrimitive();
    }

    public OdlOnem2mHttpRequestPrimitiveBuilder setOperationCreate() {
        onem2mRequest.httpRequest.setMethod("post");
        return this;
    }
    public OdlOnem2mHttpRequestPrimitiveBuilder setOperationRetrieve() {
        onem2mRequest.httpRequest.setMethod("get");
        return this;
    }
    public OdlOnem2mHttpRequestPrimitiveBuilder setOperationUpdate() {
        onem2mRequest.httpRequest.setMethod("put");
        return this;
    }
    public OdlOnem2mHttpRequestPrimitiveBuilder setOperationDelete() {
        onem2mRequest.httpRequest.setMethod("delete");
        return this;
    }
    public OdlOnem2mHttpRequestPrimitiveBuilder setTo(String value) {
        onem2mRequest.to = value;
        return this;
    }
    public OdlOnem2mHttpRequestPrimitiveBuilder setFrom(String value) {
        onem2mRequest.httpRequest.setRequestHeader(Onem2m.HttpHeaders.X_M2M_ORIGIN, value);
        return this;
    }

    public OdlOnem2mHttpRequestPrimitiveBuilder setRequestIdentifier(String value) {
        onem2mRequest.httpRequest.setRequestHeader(Onem2m.HttpHeaders.X_M2M_RI, value);
        return this;
    }

    public OdlOnem2mHttpRequestPrimitiveBuilder setResourceType(Integer value) {
        if (!onem2mRequest.uriQueryString.contentEquals("")) {
            onem2mRequest.uriQueryString += "&";
        }
        onem2mRequest.uriQueryString += RequestPrimitive.RESOURCE_TYPE + "=" + value.toString();
        return this;
    }
    public OdlOnem2mHttpRequestPrimitiveBuilder setName(String value) {
        onem2mRequest.httpRequest.setRequestHeader(Onem2m.HttpHeaders.X_M2M_NM, value);

        return this;
    }

    public OdlOnem2mHttpRequestPrimitiveBuilder setContent(String value) {
        onem2mRequest.httpRequest.setRequestContentSource(new ByteArrayInputStream(value.getBytes()));
        onem2mRequest.httpRequest.setRequestContentType("application/json");
        return this;
    }

    public OdlOnem2mHttpRequestPrimitiveBuilder setResponseType(String value) {
        if (!onem2mRequest.uriQueryString.contentEquals("")) {
            onem2mRequest.uriQueryString += "&";
        }
        onem2mRequest.uriQueryString += RequestPrimitive.RESPONSE_TYPE + "=" + value;
        return this;
    }
    public OdlOnem2mHttpRequestPrimitiveBuilder setResultPersistence(String value) {
        if (!onem2mRequest.uriQueryString.contentEquals("")) {
            onem2mRequest.uriQueryString += "&";
        }
        onem2mRequest.uriQueryString += RequestPrimitive.RESULT_PERSISTENCE + "=" + value;
        return this;
    }

    public OdlOnem2mHttpRequestPrimitiveBuilder setResultContent(String value) {
        if (!onem2mRequest.uriQueryString.contentEquals("")) {
            onem2mRequest.uriQueryString += "&";
        }
        onem2mRequest.uriQueryString += RequestPrimitive.RESULT_CONTENT + "=" + value;
        return this;
    }
    public OdlOnem2mHttpRequestPrimitiveBuilder setDeliveryAggregation(String value) {
        if (!onem2mRequest.uriQueryString.contentEquals("")) {
            onem2mRequest.uriQueryString += "&";
        }
        onem2mRequest.uriQueryString += RequestPrimitive.DELIVERY_AGGREGATION + "=" + value;
        return this;
    }
    public OdlOnem2mHttpRequestPrimitiveBuilder setFilterCriteriaCreatedBefore(String value) {
        if (!onem2mRequest.uriQueryString.contentEquals("")) {
            onem2mRequest.uriQueryString += "&";
        }
        onem2mRequest.uriQueryString += RequestPrimitive.FILTER_CRITERIA_CREATED_BEFORE + "=" + value;
        return this;
    }
    public OdlOnem2mHttpRequestPrimitiveBuilder setFilterCriteriaCreatedAfter(String value) {
        if (!onem2mRequest.uriQueryString.contentEquals("")) {
            onem2mRequest.uriQueryString += "&";
        }
        onem2mRequest.uriQueryString += RequestPrimitive.FILTER_CRITERIA_CREATED_AFTER + "=" + value;
        return this;
    }
    public OdlOnem2mHttpRequestPrimitiveBuilder setFilterCriteriaResourceType(String value) {
        if (!onem2mRequest.uriQueryString.contentEquals("")) {
            onem2mRequest.uriQueryString += "&";
        }
        onem2mRequest.uriQueryString += RequestPrimitive.FILTER_CRITERIA_RESOURCE_TYPE + "=" + value;
        return this;
    }
    public OdlOnem2mHttpRequestPrimitiveBuilder setFilterCriteriaLabel(String value) {
        if (!onem2mRequest.uriQueryString.contentEquals("")) {
            onem2mRequest.uriQueryString += "&";
        }
        onem2mRequest.uriQueryString += RequestPrimitive.FILTER_CRITERIA_LABELS + "=" + value;
        return this;
    }
    public OdlOnem2mHttpRequestPrimitiveBuilder setFilterCriteriaFilterUsage(String value) {
        if (!onem2mRequest.uriQueryString.contentEquals("")) {
            onem2mRequest.uriQueryString += "&";
        }
        onem2mRequest.uriQueryString += RequestPrimitive.FILTER_CRITERIA_FILTER_USAGE + "=" + value;
        return this;
    }
    public OdlOnem2mHttpRequestPrimitiveBuilder setFilterCriteriaLimit(String value) {
        if (!onem2mRequest.uriQueryString.contentEquals("")) {
            onem2mRequest.uriQueryString += "&";
        }
        onem2mRequest.uriQueryString += RequestPrimitive.FILTER_CRITERIA_LIMIT + "=" + value;
        return this;
    }
    public OdlOnem2mHttpRequestPrimitiveBuilder setDiscoveryResultType(String value) {
        if (!onem2mRequest.uriQueryString.contentEquals("")) {
            onem2mRequest.uriQueryString += "&";
        }
        onem2mRequest.uriQueryString += RequestPrimitive.DISCOVERY_RESULT_TYPE + "=" + value;
        return this;
    }
    public OdlOnem2mHttpRequestPrimitive build() {
        return (onem2mRequest);
    }
}

