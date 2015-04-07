/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.odlclient;

import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OdlOnem2mCoapRequestPrimitiveBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(OdlOnem2mCoapRequestPrimitiveBuilder.class);

    private OdlOnem2mCoapRequestPrimitive onem2mRequest;
    /**
     * The onem2m-protocols use this to create a new RequestPrimitive class
     */
    public OdlOnem2mCoapRequestPrimitiveBuilder() {
        onem2mRequest = new OdlOnem2mCoapRequestPrimitive();
    }

    public OdlOnem2mCoapRequestPrimitiveBuilder setOperationCreate() {
        onem2mRequest.coapRequest = Request.newPost();
        return this;
    }
    public OdlOnem2mCoapRequestPrimitiveBuilder setOperationRetrieve() {
        onem2mRequest.coapRequest = Request.newGet();
        return this;
    }
    public OdlOnem2mCoapRequestPrimitiveBuilder setOperationUpdate() {
        onem2mRequest.coapRequest = Request.newPut();
        return this;
    }
    public OdlOnem2mCoapRequestPrimitiveBuilder setOperationDelete() {
        onem2mRequest.coapRequest = Request.newDelete();
        return this;
    }
    public OdlOnem2mCoapRequestPrimitiveBuilder setTo(String value) {
        onem2mRequest.optionsSet.addURIPath(value);
        return this;
    }
    public OdlOnem2mCoapRequestPrimitiveBuilder setFrom(String value) {
        onem2mRequest.optionsSet.addOption(new Option(Onem2m.CoapOption.ONEM2M_FR, value));
        return this;
    }

    public OdlOnem2mCoapRequestPrimitiveBuilder setRequestIdentifier(String value) {
        onem2mRequest.optionsSet.addOption(new Option(Onem2m.CoapOption.ONEM2M_RQI, value));
        return this;
    }

    public OdlOnem2mCoapRequestPrimitiveBuilder setResourceType(String value) {
        if (!onem2mRequest.uriQueryString.contentEquals("")) {
            onem2mRequest.uriQueryString += "&";
        }
        onem2mRequest.uriQueryString += RequestPrimitive.RESOURCE_TYPE + "=" + value;
        return this;
    }
    public OdlOnem2mCoapRequestPrimitiveBuilder setName(String value) {
        onem2mRequest.optionsSet.addOption(new Option(Onem2m.CoapOption.ONEM2M_NM, value));
        return this;
    }

    public OdlOnem2mCoapRequestPrimitiveBuilder setContent(String value, int contentFormat) {
        onem2mRequest.coapRequest.setPayload(value, MediaTypeRegistry.APPLICATION_JSON);
        return this;
    }

    public OdlOnem2mCoapRequestPrimitiveBuilder setOriginatingTimestamp(String value) {
        onem2mRequest.optionsSet.addOption(new Option(Onem2m.CoapOption.ONEM2M_OT, value));
        return this;
    }
    public OdlOnem2mCoapRequestPrimitiveBuilder setRequestExpirationTimestamp(String value) {
        onem2mRequest.optionsSet.addOption(new Option(Onem2m.CoapOption.ONEM2M_RQET, value));

        return this;
    }
    public OdlOnem2mCoapRequestPrimitiveBuilder setResultExpirationTimestamp(String value) {
        onem2mRequest.optionsSet.addOption(new Option(Onem2m.CoapOption.ONEM2M_RSET, value));
        return this;
    }
    public OdlOnem2mCoapRequestPrimitiveBuilder setOperationExecutionTime(String value) {
        onem2mRequest.optionsSet.addOption(new Option(Onem2m.CoapOption.ONEM2M_OET, value));
        return this;
    }
    public OdlOnem2mCoapRequestPrimitiveBuilder setResponseType(String value) {
        if (!onem2mRequest.uriQueryString.contentEquals("")) {
            onem2mRequest.uriQueryString += "&";
        }
        onem2mRequest.uriQueryString += RequestPrimitive.RESPONSE_TYPE + "=" + value;
        return this;
    }
    public OdlOnem2mCoapRequestPrimitiveBuilder setResultPersistence(String value) {
        if (!onem2mRequest.uriQueryString.contentEquals("")) {
            onem2mRequest.uriQueryString += "&";
        }
        onem2mRequest.uriQueryString += RequestPrimitive.RESULT_PERSISTENCE + "=" + value;
        return this;
    }

    public OdlOnem2mCoapRequestPrimitiveBuilder setResultContent(String value) {
        if (!onem2mRequest.uriQueryString.contentEquals("")) {
            onem2mRequest.uriQueryString += "&";
        }
        onem2mRequest.uriQueryString += RequestPrimitive.RESULT_CONTENT + "=" + value;
        return this;
    }
    public OdlOnem2mCoapRequestPrimitiveBuilder setDeliveryAggregation(String value) {
        if (!onem2mRequest.uriQueryString.contentEquals("")) {
            onem2mRequest.uriQueryString += "&";
        }
        onem2mRequest.uriQueryString += RequestPrimitive.DELIVERY_AGGREGATION + "=" + value;
        return this;
    }
    public OdlOnem2mCoapRequestPrimitiveBuilder setGroupRequestIdentifier(String value) {
        onem2mRequest.optionsSet.addOption(new Option(Onem2m.CoapOption.ONEM2M_GID, value));
        return this;
    }
    public OdlOnem2mCoapRequestPrimitiveBuilder setFilterCriteria(String value) {
        if (!onem2mRequest.uriQueryString.contentEquals("")) {
            onem2mRequest.uriQueryString += "&";
        }
        onem2mRequest.uriQueryString += RequestPrimitive.FILTER_CRITERIA + "=" + value;
        return this;
    }
    public OdlOnem2mCoapRequestPrimitiveBuilder setDiscoveryResultType(String value) {
        if (!onem2mRequest.uriQueryString.contentEquals("")) {
            onem2mRequest.uriQueryString += "&";
        }
        onem2mRequest.uriQueryString += RequestPrimitive.DISCOVERY_RESULT_TYPE + "=" + value;
        return this;
    }
    public OdlOnem2mCoapRequestPrimitive build() {
        onem2mRequest.optionsSet.addURIQuery(onem2mRequest.uriQueryString);
        onem2mRequest.coapRequest.setOptions(onem2mRequest.optionsSet);
        return (onem2mRequest);
    }
}

