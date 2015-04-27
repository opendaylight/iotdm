package org.opendaylight.iotdm.onem2m.protocols.http;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClientBuilder;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClient;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mHttpProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpProvider.class);
    protected Onem2mService onem2mService;
    private Server server;
    private final int PORT = 8282;

    public static final String X_M2M_ORIGIN = "X-M2M-Origin";
    public static final String X_M2M_RI = "X-M2M-RI";
    public static final String X_M2M_NM = "X-M2M-NM";
    public static final String X_M2M_GID = "X-M2M-GID";
    public static final String X_M2M_RTU = "X-M2M-RTU";
    public static final String X_M2M_OT = "X-M2M-OT";
    public static final String X_M2M_RSC = "X-M2M-RSC";

    @Override
    public void onSessionInitiated(ProviderContext session) {
        onem2mService = session.getRpcService(Onem2mService.class);
        try {
            server.start();
        } catch (Exception e) {
            LOG.info("Exception: {}", e.toString());
        }
        LOG.info("Onem2mHttpProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        LOG.info("Onem2mHttpProvider Closed");
    }

    public Onem2mHttpProvider() {
        server = new Server(PORT);
        server.setHandler(new MyHandler());
    }

    public class MyHandler extends AbstractHandler {

        @Override
        public void handle(String target, Request baseRequest,
                           HttpServletRequest httpRequest,
                           HttpServletResponse httpResponse) throws IOException, ServletException {

            Onem2mRequestPrimitiveClientBuilder clientBuilder = new Onem2mRequestPrimitiveClientBuilder();
            String headerValue;

            clientBuilder.setProtocol(Onem2m.Protocol.HTTP);

            String contentType = httpRequest.getContentType().toLowerCase();
            if (contentType.contains("json")) {
                clientBuilder.setContentFormat(Onem2m.ContentFormat.JSON);
            } else if (contentType.contains("xml")) {
                clientBuilder.setContentFormat(Onem2m.ContentFormat.XML);
            } else {
                httpResponse.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
                httpResponse.getWriter().println("Unsupported media type: " + contentType);
                httpResponse.setContentType("text/json;charset=utf-8");
                baseRequest.setHandled(true);
                return;
            }

            clientBuilder.setTo(httpRequest.getRequestURI());

            // pull fields out of the headers
            headerValue = httpRequest.getHeader(X_M2M_ORIGIN);
            if (headerValue != null) {
                clientBuilder.setFrom(headerValue);
            }
            headerValue = httpRequest.getHeader(X_M2M_RI);
            if (headerValue != null) {
                clientBuilder.setRequestIdentifier(headerValue);
            }
            headerValue = httpRequest.getHeader(X_M2M_NM);
            if (headerValue != null) {
                clientBuilder.setName(headerValue);
            }
            headerValue = httpRequest.getHeader(X_M2M_GID);
            if (headerValue != null) {
                clientBuilder.setGroupRequestIdentifier(headerValue);
            }
            headerValue = httpRequest.getHeader(X_M2M_RTU);
            if (headerValue != null) {
                clientBuilder.setResponseType(headerValue);
            }
            headerValue = httpRequest.getHeader(X_M2M_OT);
            if (headerValue != null) {
                clientBuilder.setOriginatingTimestamp(headerValue);
            }

            String method = httpRequest.getMethod().toLowerCase();
            Boolean resourceTypePresent = clientBuilder.parseQueryStringIntoPrimitives(httpRequest.getQueryString());
            if (resourceTypePresent && !method.contentEquals("post")) {
                httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                httpResponse.getWriter().println("Specifying resource type not permitted.");
                return;
            }

            // take the entire payload text and put it in the CONTENT field; it is the representation of the resource
            String cn = IOUtils.toString(baseRequest.getInputStream()).trim();
            if (cn != null && !cn.contentEquals("")) {
                clientBuilder.setContent(cn);
            }

            switch (method) {
                case "get":
                    clientBuilder.setOperationRetrieve();

                    break;
                case "post":
                    if (resourceTypePresent) {
                        clientBuilder.setOperationCreate();
                    } else {
                        clientBuilder.setOperationNotify();
                    }
                    break;
                case "put":
                    clientBuilder.setOperationUpdate();
                    break;
                case "delete":
                    clientBuilder.setOperationDelete();
                    break;
                default:
                    httpResponse.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
                    httpResponse.getWriter().println("Unsupported method type: " + method);
                    httpResponse.setContentType("text/json;charset=utf-8");
                    baseRequest.setHandled(true);
                    return;
            }

            // invoke the service request
            Onem2mRequestPrimitiveClient onem2mRequest = clientBuilder.build();
            ResponsePrimitive onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);

            // now place the fields from the onem2m result response back in the http fields, and send
            sendHttpResponseFromOnem2mResponse(httpResponse, onem2mResponse);

            httpResponse.setContentType("text/json;charset=utf-8");
            baseRequest.setHandled(true);
        }

        private void sendHttpResponseFromOnem2mResponse(HttpServletResponse httpResponse,
                                                        ResponsePrimitive onem2mResponse) throws IOException {

            // the content is already in the required format ...
            String content = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
            String rscString = onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE);
            httpResponse.setHeader(X_M2M_RI, onem2mResponse.getPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER));

            int httpRSC = mapCoreResponseToHttpResponse(httpResponse, rscString);
            if (content != null) {
                httpResponse.setStatus(httpRSC);
                httpResponse.getWriter().println(content);
            } else {
                httpResponse.setStatus(httpRSC);
            }
        }

        private int mapCoreResponseToHttpResponse(HttpServletResponse httpResponse, String rscString) {

            httpResponse.setHeader(X_M2M_RSC, rscString);
            switch (rscString) {
                case Onem2m.ResponseStatusCode.OK:
                    return HttpServletResponse.SC_OK;
                case Onem2m.ResponseStatusCode.CREATED:
                    return HttpServletResponse.SC_CREATED;
                case Onem2m.ResponseStatusCode.CHANGED:
                    return HttpServletResponse.SC_OK;
                case Onem2m.ResponseStatusCode.DELETED:
                    return HttpServletResponse.SC_OK;

                case Onem2m.ResponseStatusCode.NOT_FOUND:
                    return HttpServletResponse.SC_NOT_FOUND;
                case Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED:
                    return HttpServletResponse.SC_METHOD_NOT_ALLOWED;
                case Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE:
                    return HttpServletResponse.SC_BAD_REQUEST;
                case Onem2m.ResponseStatusCode.CONFLICT:
                    return HttpServletResponse.SC_CONFLICT;

                case Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR:
                    return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                case Onem2m.ResponseStatusCode.NOT_IMPLEMENTED:
                    return HttpServletResponse.SC_NOT_IMPLEMENTED;
                case Onem2m.ResponseStatusCode.ALREADY_EXISTS:
                    return HttpServletResponse.SC_FORBIDDEN;
                case Onem2m.ResponseStatusCode.NON_BLOCKING_REQUEST_NOT_SUPPORTED:
                    return HttpServletResponse.SC_NOT_IMPLEMENTED;

                case Onem2m.ResponseStatusCode.INVALID_ARGUMENTS:
                    return HttpServletResponse.SC_BAD_REQUEST;
                case Onem2m.ResponseStatusCode.INSUFFICIENT_ARGUMENTS:
                    return HttpServletResponse.SC_BAD_REQUEST;
            }
            return HttpServletResponse.SC_BAD_REQUEST;
        }
    }
}