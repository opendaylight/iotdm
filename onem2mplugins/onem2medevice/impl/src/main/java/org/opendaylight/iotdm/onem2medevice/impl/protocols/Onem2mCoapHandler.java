/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2medevice.impl.protocols;

import org.eclipse.californium.core.coap.*;
import org.eclipse.californium.core.network.Exchange;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClient;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClientBuilder;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.Onem2mStats;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.plugins.*;
import org.opendaylight.iotdm.onem2m.plugins.channels.coap.IotdmPluginCoapRequest;
import org.opendaylight.iotdm.onem2m.plugins.channels.coap.IotdmPluginCoapResponse;
import org.opendaylight.iotdm.onem2medevice.impl.utils.Onem2mXmlUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author jkosmel
 * TODO: temporary copied from Onem2mCoapProvider
 */
public class Onem2mCoapHandler {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCoapHandler.class);
    private Onem2mService onem2mService;

    public Onem2mCoapHandler(Onem2mService om2mService) {
        onem2mService = om2mService;
    }

    public void handle(IotdmPluginRequest request, IotdmPluginResponse response) {
        Exchange exchange = ((IotdmPluginCoapRequest) request).getOriginalRequest();
        CoAP.Code code = exchange.getRequest().getCode();
        OptionSet options = exchange.getRequest().getOptions();

        Onem2mRequestPrimitiveClientBuilder clientBuilder = new Onem2mRequestPrimitiveClientBuilder();
        String optionValue;

        clientBuilder.setProtocol(Onem2m.Protocol.COAP);

        if (Objects.equals(request.getContentType(), "xml") || Objects.equals(request.getContentType(), "json")) {
            clientBuilder.setContentFormat(request.getContentType());
        } else {
            clientBuilder.setContentFormat(Onem2m.ContentFormat.JSON);
        }

        // Process all options
        boolean resourceTypePresent = false;
        String resourceTypeOption = null;
        String resourceTypeQuerry = null;
        for (Option opt : options.asSortedList()) {
            switch (opt.getNumber()) {
                case Onem2m.CoapOption.ONEM2M_FR:
                    clientBuilder.setFrom(opt.getStringValue());
                    break;
                case Onem2m.CoapOption.ONEM2M_RQI:
                    clientBuilder.setRequestIdentifier(opt.getStringValue());
                    break;
                case Onem2m.CoapOption.ONEM2M_NM:
                    clientBuilder.setName(opt.getStringValue());
                    break;
                case Onem2m.CoapOption.ONEM2M_OT:
                    clientBuilder.setOriginatingTimestamp(opt.getStringValue());
                    break;
                case Onem2m.CoapOption.ONEM2M_RQET:
                    clientBuilder.setRequestExpirationTimestamp(opt.getStringValue());
                    break;
                case Onem2m.CoapOption.ONEM2M_RSET:
                    clientBuilder.setResultExpirationTimestamp(opt.getStringValue());
                    break;
                case Onem2m.CoapOption.ONEM2M_OET:
                    clientBuilder.setOperationExecutionTime(opt.getStringValue());
                    break;
                case Onem2m.CoapOption.ONEM2M_EC:
                    //clientBuilder.setEventCategory(opt.getIntegerValue());
                    break;
                case Onem2m.CoapOption.ONEM2M_GID:
                    clientBuilder.setGroupRequestIdentifier(opt.getStringValue());
                    break;
                case Onem2m.CoapOption.ONEM2M_TY:
                    resourceTypeOption = String.valueOf(opt.getIntegerValue());
                    break;
                default:
                    LOG.error("Unsupported CoAP option: {}", opt.getNumber());
                    break;
            }
        }

        // according to the spec, the uri query string can contain in short form, the
        // resourceType, responseType, result persistence,  Delivery Aggregation, Result Content,
        // M3 Boolean
        resourceTypePresent = clientBuilder.parseQueryStringIntoPrimitives(options.getUriQueryString());
        if (resourceTypePresent && (null != resourceTypeOption)) {
            // Resource type is set in query string and in the option as well,
            // verify if values are equal
            resourceTypeQuerry = clientBuilder.getPrimitiveValue(RequestPrimitive.RESOURCE_TYPE);
            if (!resourceTypeOption.equals(resourceTypeQuerry)) {
                LOG.error("Invalid request received, resource type set in option ({}) and query ({}) mismatch",
                        resourceTypeOption, resourceTypeQuerry);
                ((IotdmPluginCoapResponse)response).prepareErrorResponse(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "Resource type in option and query string mismatch");
                Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_ERROR);
                return;
            }
        } else if (!resourceTypePresent && (null != resourceTypeOption)) {
            // resource type is not present in query string, use the value from option
            clientBuilder.setResourceType(resourceTypeOption);
            resourceTypePresent = true;
        }

        if (resourceTypePresent && code != CoAP.Code.POST) {
            ((IotdmPluginCoapResponse)response).prepareErrorResponse(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                                                     "Specifying resource type not permitted.");
            Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_ERROR);
            return;
        }

        // take the entire payload text and put it in the CONTENT field; it is the representation of the resource
        String cn = request.getPayLoad();
        if (cn != null && !cn.contentEquals("")) {
            clientBuilder.setPrimitiveContent(cn);
        }

        switch (code) {
            case GET:
                clientBuilder.setOperationRetrieve();
                Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_RETRIEVE);
                break;

            case POST:
                if (resourceTypePresent) {
                    clientBuilder.setOperationCreate();
                    Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_CREATE);
                } else {
                    clientBuilder.setOperationNotify();
                    Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_NOTIFY);
                }
                break;

            case PUT:
                clientBuilder.setOperationUpdate();
                Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_UPDATE);
                break;

            case DELETE:
                clientBuilder.setOperationDelete();
                Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_DELETE);
                break;

            default:
                ((IotdmPluginCoapResponse)response).prepareErrorResponse(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                                                         "Unknown code: " + code);
                Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_ERROR);
                return;
        }

        clientBuilder.setTo(options.getUriPathString()); // To/TargetURI
        // M3 clientBuilder.setTo(options.getUriPathString()); // To/TargetURI // M3

        Onem2mRequestPrimitiveClient onem2mRequest = clientBuilder.build();
        ResponsePrimitive onem2mResponse = Onem2m.serviceOnem2mRequest(onem2mRequest, onem2mService);

        if (options.hasAccept() && Onem2m.CoapContentFormat.map2String.get(options.getAccept()).contains("xml")) { //TODO: temporary solution for edevice - convert payload to xml
            onem2mResponse.setPrimitive(
                    ResponsePrimitive.CONTENT,
                    Onem2mXmlUtils.jsonResponseToXml(
                            onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT),
                            onem2mRequest.getPrimitive(RequestPrimitive.RESULT_CONTENT)
                    )
            );
            onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT_FORMAT, "xml");
        }

        // now place the fields from the onem2m result response back in the coap fields
        response.setFromResponsePrimitive(onem2mResponse);

    }
}
