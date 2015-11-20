package org.opendaylight.iotdm.onem2m.protocols.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClientBuilder;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClient;
import org.opendaylight.iotdm.onem2m.core.Onem2mStats;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.notifier.Onem2mNotifierPlugin;
import org.opendaylight.iotdm.onem2m.notifier.Onem2mNotifierService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mHttpProvider implements Onem2mNotifierPlugin, BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpProvider.class);
    protected Onem2mService onem2mService;
    private Server server;
    private final int PORT = 8282;
    private HttpClient client;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        onem2mService = session.getRpcService(Onem2mService.class);
        Onem2mNotifierService.getInstance().pluginRegistration(this);

        try {
            server.start();
            client.start();
        } catch (Exception e) {
            LOG.info("Exception: {}", e.toString());
        }
        LOG.info("Onem2mHttpProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        server.stop();
        client.stop();
        LOG.info("Onem2mHttpProvider Closed");
    }

    public Onem2mHttpProvider() {
        server = new Server(PORT);
        server.setHandler(new MyHandler());
        client = new HttpClient();

    }

    public class MyHandler extends AbstractHandler {

        /**
         * The handler for the HTTP requesst
         * @param target target
         * @param baseRequest  request
         * @param httpRequest  request
         * @param httpResponse  response
         * @throws IOException
         * @throws ServletException
         */
        @Override
        public void handle(String target, Request baseRequest,
                           HttpServletRequest httpRequest,
                           HttpServletResponse httpResponse) throws IOException, ServletException {

            Onem2mRequestPrimitiveClientBuilder clientBuilder = new Onem2mRequestPrimitiveClientBuilder();
            String headerValue;

            clientBuilder.setProtocol(Onem2m.Protocol.HTTP);

            Onem2mStats.getInstance().endpointInc(baseRequest.getRemoteAddr());
            Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS);

            String contentType = httpRequest.getContentType();
            if (contentType == null) contentType = "json";
            contentType = contentType.toLowerCase();
            if (contentType.contains("json")) {
                clientBuilder.setContentFormat(Onem2m.ContentFormat.JSON);
            } else if (contentType.contains("xml")) {
                clientBuilder.setContentFormat(Onem2m.ContentFormat.XML);
            } else {
                httpResponse.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
                httpResponse.getWriter().println("Unsupported media type: " + contentType);
                httpResponse.setContentType("text/json;charset=utf-8");
                baseRequest.setHandled(true);
                Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_ERROR);
                return;
            }

            clientBuilder.setTo(httpRequest.getRequestURI());

            // pull fields out of the headers
            headerValue = httpRequest.getHeader(Onem2m.HttpHeaders.X_M2M_ORIGIN);
            if (headerValue != null) {
                clientBuilder.setFrom(headerValue);
            }
            headerValue = httpRequest.getHeader(Onem2m.HttpHeaders.X_M2M_RI);
            if (headerValue != null) {
                clientBuilder.setRequestIdentifier(headerValue);
            }
            headerValue = httpRequest.getHeader(Onem2m.HttpHeaders.X_M2M_NM);
            if (headerValue != null) {
                clientBuilder.setName(headerValue);
            }
            headerValue = httpRequest.getHeader(Onem2m.HttpHeaders.X_M2M_GID);
            if (headerValue != null) {
                clientBuilder.setGroupRequestIdentifier(headerValue);
            }
            headerValue = httpRequest.getHeader(Onem2m.HttpHeaders.X_M2M_RTU);
            if (headerValue != null) {
                clientBuilder.setResponseType(headerValue);
            }
            headerValue = httpRequest.getHeader(Onem2m.HttpHeaders.X_M2M_OT);
            if (headerValue != null) {
                clientBuilder.setOriginatingTimestamp(headerValue);
            }

            // the contentType string can have ty=val attached to it so we should handle this case
            Boolean resourceTypePresent = false;
            String contentTypeResourceString = parseContentTypeForResourceType(contentType);
            if (contentTypeResourceString != null) {
                resourceTypePresent = clientBuilder.parseQueryStringIntoPrimitives(contentTypeResourceString);
            }
            String method = httpRequest.getMethod().toLowerCase();
            // look in query string if didnt find it in contentType header
            if (!resourceTypePresent) {
                resourceTypePresent = clientBuilder.parseQueryStringIntoPrimitives(httpRequest.getQueryString());
            } else {
                clientBuilder.parseQueryStringIntoPrimitives(httpRequest.getQueryString());
            }
            if (resourceTypePresent && !method.contentEquals("post")) {
                httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                httpResponse.getWriter().println("Specifying resource type not permitted.");
                Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_ERROR);
                return;
            }

            // take the entire payload text and put it in the CONTENT field; it is the representation of the resource
            String cn = IOUtils.toString(baseRequest.getInputStream()).trim();
            if (cn != null && !cn.contentEquals("")) {
                clientBuilder.setPrimitiveContent(cn);
            }

            switch (method) {
                case "get":
                    clientBuilder.setOperationRetrieve();
                    Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_RETRIEVE);
                    break;
                case "post":
                    if (resourceTypePresent) {
                        clientBuilder.setOperationCreate();
                        Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_CREATE);
                    } else {
                        clientBuilder.setOperationNotify();
                        Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_NOTIFY);
                    }
                    break;
                case "put":
                    clientBuilder.setOperationUpdate();
                    Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_UPDATE);
                    break;
                case "delete":
                    clientBuilder.setOperationDelete();
                    Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_DELETE);
                    break;
                default:
                    httpResponse.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
                    httpResponse.getWriter().println("Unsupported method type: " + method);
                    httpResponse.setContentType("text/json;charset=utf-8");
                    baseRequest.setHandled(true);
                    Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_ERROR);
                    return;
            }

            // invoke the service request
            Onem2mRequestPrimitiveClient onem2mRequest = clientBuilder.build();
            ResponsePrimitive onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);

            // now place the fields from the onem2m result response back in the http fields, and send
            sendHttpResponseFromOnem2mResponse(httpResponse, onem2mResponse);

            httpResponse.addHeader("Access-Control-Allow-Origin", "*");
            httpResponse.addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");

            baseRequest.setHandled(true);
        }

        private void sendHttpResponseFromOnem2mResponse(HttpServletResponse httpResponse,
                                                        ResponsePrimitive onem2mResponse) throws IOException {

            // the content is already in the required format ...
            String content = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
            String rscString = onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE);
            String rqi = onem2mResponse.getPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER);
            if (rqi != null) {
                httpResponse.setHeader(Onem2m.HttpHeaders.X_M2M_RI, rqi);
            }

            int httpRSC = mapCoreResponseToHttpResponse(httpResponse, rscString);
            if (content != null) {
                httpResponse.setStatus(httpRSC);
                httpResponse.getWriter().println(content);
            } else {
                httpResponse.setStatus(httpRSC);
            }
            if (rscString.charAt(0) =='2') {
                Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_OK);
            } else {
                Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_ERROR);
            }

            String ct = onem2mResponse.getPrimitive(ResponsePrimitive.HTTP_CONTENT_TYPE);
            if (ct != null) {
                httpResponse.setContentType(ct);
            }
            String cl = onem2mResponse.getPrimitive(ResponsePrimitive.HTTP_CONTENT_LOCATION);
            if (cl != null) {
                httpResponse.setHeader("Content-Location", cl);
            }
        }

        private int mapCoreResponseToHttpResponse(HttpServletResponse httpResponse, String rscString) {

            httpResponse.setHeader(Onem2m.HttpHeaders.X_M2M_RSC, rscString);
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
                case Onem2m.ResponseStatusCode.TARGET_NOT_SUBSCRIBABLE:
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

    // implement the Onem2mNotifierPlugin interface
    @Override
    public String getNotifierPluginName() {
        return "http";
    }

    /**
     * HTTP notifications will be set out to subscribers interested in resources from the tree where they have have hung
     * onem2m subscription resources
     * @param url where do i send this onem2m notify message
     * @param payload contents of the notification
     */
    @Override
    public void sendNotification(String url, String payload) {
        ContentExchange ex = new ContentExchange();
        ex.setURL(url);
        ex.setRequestContentSource(new ByteArrayInputStream(payload.getBytes()));
        ex.setRequestContentType(Onem2m.ContentType.APP_VND_NTFY_JSON);
        Integer cl = payload != null ?  payload.length() : 0;
        ex.setRequestHeader("Content-Length", cl.toString());
        ex.setMethod("post");
        LOG.debug("HTTP: Send notification uri: {}, payload: {}:", url, payload);
        try {
            client.send(ex);
        } catch (IOException e) {
            LOG.error("Dropping notification: uri: {}, payload: {}", url, payload);
        }
    }

    private String parseContentTypeForResourceType(String contentType) {

        String split[] = contentType.trim().split(";");
        if (split.length != 2) {
            return null;
        }
        return split[1];
    }
}
