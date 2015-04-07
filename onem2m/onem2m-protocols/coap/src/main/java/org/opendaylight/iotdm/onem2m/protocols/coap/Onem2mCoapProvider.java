/*
 * Copyright(c) Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.protocols.coap;

import java.util.List;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.*;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClient;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClientBuilder;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mCoapProvider extends CoapServer implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCoapProvider.class);
    protected Onem2mService onem2mService;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        onem2mService = session.getRpcService(Onem2mService.class);
        start();
        LOG.info("Onem2mCoapProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        LOG.info("Onem2mCoapProvider Closed");
    }

    /**
     * Intercept the coap URL query.
     */
    @Override
    public Resource createRoot() {
        return new RootResource();
    }

    private class RootResource extends CoapResource {
        public RootResource() {
            super("OpenDaylight OneM2M CoAP Server");
        }

        @Override
        public Resource getChild(String name) {
            return this;
        }

        @Override
        public void handleRequest(final Exchange exchange) {
            CoAP.Code code = exchange.getRequest().getCode();
            CoapExchange coapExchange = new CoapExchange(exchange, this);
            OptionSet options = coapExchange.advanced().getRequest().getOptions();

            // onem2m needs type = CON, ACK, RST - see binding spec
            //if (exchange.getRequest().getType() != CoAP.Type.CON) {
            //   coapExchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Invalid CoAP type:" + exchange.getRequest().getType());
            //   return;
            //}
            Onem2mRequestPrimitiveClientBuilder clientBuilder = new Onem2mRequestPrimitiveClientBuilder();
            String optionValue;

            clientBuilder.setProtocol(Onem2m.Protocol.COAP);

            if (options.getContentFormat() == MediaTypeRegistry.APPLICATION_JSON) {
                clientBuilder.setContentFormat(Onem2m.ContentFormat.JSON);
            } else if (options.getContentFormat() == MediaTypeRegistry.APPLICATION_XML) {
                clientBuilder.setContentFormat(Onem2m.ContentFormat.XML);
            } else {
                clientBuilder.setContentFormat(Onem2m.ContentFormat.JSON);

                //coapExchange.respond(CoAP.ResponseCode.NOT_ACCEPTABLE, "Unknown media type: " +
                //        options.getContentFormat());
                //return;
            }

            clientBuilder.setTo(options.getURIPathString()); // To/TargetURI

            coapSetOptions(options, clientBuilder); // pull options out of coap header fields

            // according to the spec, the uri query string can contain in short form, the
            // resourceType, responseType, result persistence,  Delivery Aggregation, Result Content,
            Boolean resourceTypePresent = clientBuilder.parseQueryStringIntoPrimitives(options.getURIQueryString());
            if (resourceTypePresent && code != CoAP.Code.POST) {
                coapExchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Specifying resource type not permitted.");
                return;
            }

            // take the entire payload text and put it in the CONTENT field; it is the representation of the resource
            String cn = coapExchange.getRequestText().trim();
            if (cn != null && !cn.contentEquals("")) {
                clientBuilder.setContent(cn);
            }

            switch (code) {
                case GET:
                    clientBuilder.setOperationRetrieve();
                    break;
                case POST:
                    if (resourceTypePresent) {
                        clientBuilder.setOperationCreate();
                    } else {
                        clientBuilder.setOperationNotify();
                    }
                    break;
                case PUT:
                    clientBuilder.setOperationUpdate();
                    break;
                case DELETE:
                    clientBuilder.setOperationDelete();
                    break;
                default:
                    coapExchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Unknown code: " + code);
                    return;
            }

            Onem2mRequestPrimitiveClient onem2mRequest = clientBuilder.build();
            ResponsePrimitive onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);

            // now place the fields from the onem2m result response back in the coap fields, and send
            sendCoapResponseFromOnem2mResponse(coapExchange, onem2mRequest, onem2mResponse);

        }

        private void sendCoapResponseFromOnem2mResponse(CoapExchange exchange,
                                                        RequestPrimitive onem2mRequest,
                                                        ResponsePrimitive onem2mResponse) {

            // map the onem2m response RSC to a CoAP return code

            // the content is already in the required format ...
            String content = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
            CoAP.ResponseCode coapRSC = CoAP.ResponseCode.CREATED;
            if (content != null) {
                exchange.respond(coapRSC, content);
            } else {
                exchange.respond(coapRSC);
            }
        }

        /**
         * For each option, find the onem2m options and set the appropriate fields
         * @param options
         * @param clientBuilder
         * @return
         */
        private void coapSetOptions(OptionSet options, Onem2mRequestPrimitiveClientBuilder clientBuilder) {
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
                }
            }
        }
    }
}