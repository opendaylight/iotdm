/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.Monitor;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.database.dao.factory.DaoResourceTreeFactory;
import org.opendaylight.iotdm.onem2m.core.database.lock.ReadWriteLocker;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.TransactionManager;
import org.opendaylight.iotdm.onem2m.core.rest.RequestPrimitiveProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.router.Onem2mRouterService;
import org.opendaylight.iotdm.onem2m.plugins.Onem2mPluginManager;
import org.opendaylight.iotdm.onem2m.plugins.Onem2mPluginsDbApi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.primitive.list.Onem2mPrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.core.rev141210.DefaultCoapsConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.core.rev141210.DefaultHttpsConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.core.rev141210.Onem2mCoreRuntimeMXBean;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.core.rev141210.SecurityConfig;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class Onem2mCoreProvider implements Onem2mService, Onem2mCoreRuntimeMXBean, BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCoreProvider.class);
    private Onem2mStats stats;

    private BindingAwareBroker.RpcRegistration<Onem2mService> rpcReg;
    private DataBroker dataBroker;
    private TransactionManager transactionManager = null;

    private Onem2mDb db;
    private ResourceTreeWriter twc;
    private ResourceTreeReader trc;

    private static NotificationPublishService notifierService;
    private static Onem2mRouterService routerService;
    private static final Onem2mCoreProvider coreProvider = new Onem2mCoreProvider();

    private SecurityConfig securityConfig = null;
    private DefaultHttpsConfig defaultHttpsConfig = null;
    private DefaultCoapsConfig defaultCoapsConfig = null;

    public static Onem2mCoreProvider getInstance() {
        return coreProvider;
    }

    public void setSecurityConfig(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    public SecurityConfig getSecurityConfig() {
        return this.securityConfig;
    }

    public void setDefaultHttpsConfig(DefaultHttpsConfig keyStoreConfig) {
        this.defaultHttpsConfig = keyStoreConfig;
    }

    public DefaultHttpsConfig getDefaultHttpsConfig() {
        return this.defaultHttpsConfig;
    }

    public DefaultCoapsConfig getDefaultCoapsConfig() {
        return defaultCoapsConfig;
    }

    public void setDefaultCoapsConfig(DefaultCoapsConfig defaultCoapsConfig) {
        this.defaultCoapsConfig = defaultCoapsConfig;
    }

    public static NotificationPublishService getNotifier() {
        return Onem2mCoreProvider.notifierService;

    }

    public static final String CONFIG_STATUS_OK = "OK";
    public static final String CONFIG_STATUS_FAILED = "FAILED";

    private SecurityLevel evalSecurityLevel(SecurityLevel level) {
        if(Objects.isNull(level)) {
            throw new IllegalArgumentException("Security level is not defined");
        }
        else if (Objects.nonNull(securityConfig) && level.getIntValue() < securityConfig.getCoreSecurityLevel().getIntValue())
            return securityConfig.getCoreSecurityLevel();
        else
            return level;
    }

    /**
     * Perform session initialization
     * @param session the session
     */
    @Override
    public void onSessionInitiated(ProviderContext session) {
        this.rpcReg = session.addRpcImplementation(Onem2mService.class, this);
        this.dataBroker = session.getSALService(DataBroker.class);
        notifierService = session.getSALService(NotificationPublishService.class);
        routerService = Onem2mRouterService.getInstance();

        stats = Onem2mStats.getInstance();
        db = Onem2mDb.getInstance();
        db.initializeDatastore(dataBroker);
        Onem2mPluginManager.getInstance().handleDefaultConfigUpdate();
        LOG.info("Session Initiated");
    }

    public void registerDaoPlugin(DaoResourceTreeFactory daoResourceTreeFactory) {

        if (this.twc != null || this.trc != null) {
            LOG.error("Onem2mCoreProvider.registerDaoPlugin: new registration attempt ... not GOOD");
            return;
        }
        this.transactionManager = new TransactionManager(daoResourceTreeFactory, new ReadWriteLocker(50));
        this.twc = this.transactionManager.getDbResourceTreeWriter();
        this.trc = this.transactionManager.getTransactionReader();
        Onem2mPluginsDbApi.getInstance().registerDbReaderAndWriter(twc, trc);
    }

    private boolean isDaoPluginRegistered() {
        return this.twc != null && this.trc != null;
    }

    /**
     * Shutdown the session
     * @throws Exception general exception if bad things happen
     */
    @Override
    public void close() throws Exception {
        if (this.rpcReg != null) {
            this.rpcReg.close();
        }
        if (db != null) {
            db.close();
        }
        LOG.info("Session Closed");
    }

    private Future<RpcResult<Onem2mRequestPrimitiveOutput>> createOutputFromResponse(
                                                                           ResponsePrimitive onem2mResponse,
                                                                           List<Onem2mPrimitive> onem2mPrimitiveList) {
        onem2mPrimitiveList = onem2mResponse.getPrimitivesList();
        Onem2mRequestPrimitiveOutput output = new Onem2mRequestPrimitiveOutputBuilder()
                                                      .setOnem2mPrimitive(onem2mPrimitiveList).build();

        //LOG.info("RPC: end handle op ...");

        return RpcResultBuilder.success(output).buildFuture();
    }

    /**
     * This is the requestPrimitive RPC, it can be called from restconf directly, or it can be called from
     * onem2m-protocol-coap/http/mqtt.  Each of those onem2m-protocols have used the protocol specific bindings
     * to extract primitives from protocol specific fields and placed them in the List of Onem2mPrimitives.
     * See Onem2m.RequestPrimitive for the list of possible primitives.  For the most part, these primitives
     * are format independent and as such are strings.  There is one exception, ie the Onem2m.RequestPrimitive.CONTENT
     * parameter which has been serialized as a string by the onenm2m-protocols which was encoded in the payload
     * of those protocols.  The CONTENT is encoded by the Onem2m.RequestPrimitive.CONTENT_FORMAT=json/xml).  The
     * code that ultimately cares about the CONTENT will decode it using the appropriate content parser.
     * Based on the operation, mandatory fields have to be checked.
     * @param input the input request primitives
     * @return the response primitives
     */
    @Override
    public Future<RpcResult<Onem2mRequestPrimitiveOutput>> onem2mRequestPrimitive(Onem2mRequestPrimitiveInput input) {


        //LOG.info("RPC: begin handle op ...");
        Onem2mRequestPrimitiveOutput output = null;

        List<Onem2mPrimitive> onem2mPrimitiveList = input.getOnem2mPrimitive();
        // todo: if it is a group/fanoutpoint, new a GroupRequestPrimitiveProcessor then called a lot of single processor?
        RequestPrimitiveProcessor onem2mRequest = new RequestPrimitiveProcessor();
        ResponsePrimitive onem2mResponse = null;
        onem2mRequest.setPrimitivesList(onem2mPrimitiveList);

        if (!isDaoPluginRegistered()) {
            onem2mResponse = new ResponsePrimitive();
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR,
                    "DaoPlugin not yet registered");
            return createOutputFromResponse(onem2mResponse, onem2mPrimitiveList);
        }

        Onem2mDb.CseBaseResourceLocator resourceLocator = null;
        try {
            String nativeAppName = onem2mRequest.getPrimitive(RequestPrimitive.NATIVEAPP_NAME);
            if(nativeAppName != null && nativeAppName.equals("CSEProvisioning")) {

                Onem2mCseProvisioningInput cseInput = new Onem2mCseProvisioningInputBuilder()
                        .setOnem2mPrimitive(onem2mRequest.getPrimitivesList()).build();
                Future<RpcResult<Onem2mCseProvisioningOutput>> rpcResult = onem2mCseProvisioning(cseInput);
                try {
                    onem2mPrimitiveList = rpcResult.get().getResult().getOnem2mPrimitive();

                    output = new Onem2mRequestPrimitiveOutputBuilder()
                            .setOnem2mPrimitive(onem2mPrimitiveList).build();
                    return RpcResultBuilder.success(output).buildFuture();

                } catch (InterruptedException | ExecutionException ex) {
                    onem2mResponse = new ResponsePrimitive();
                    onem2mResponse.setPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER,
                            onem2mRequest.getPrimitive(RequestPrimitive.REQUEST_IDENTIFIER));
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR, "Cse Provisioning failed");
                    onem2mPrimitiveList = onem2mResponse.getPrimitivesList();
                    output = new Onem2mRequestPrimitiveOutputBuilder()
                            .setOnem2mPrimitive(onem2mPrimitiveList).build();
                }

                return RpcResultBuilder.success(output).buildFuture();
            }
        } catch (IllegalArgumentException ex) {
            LOG.error("Request with invalid URI passed: {}", onem2mRequest.getPrimitive(RequestPrimitive.TO));
            // rethrow the exception
            throw ex;
        }

        try {
            String to = onem2mRequest.getPrimitive(RequestPrimitive.TO);
            onem2mRequest.delPrimitive(RequestPrimitive.TO);
            onem2mRequest.setPrimitive(RequestPrimitive.TO, Onem2m.translateUriToOnem2m(to));
            resourceLocator = this.db.createResourceLocator(onem2mRequest.getPrimitive(RequestPrimitive.TO));
        } catch (IllegalArgumentException ex) {
            LOG.error("Request with invalid URI passed: {}", onem2mRequest.getPrimitive(RequestPrimitive.TO));
            // rethrow the exception
            throw ex;
        }

        // verify whether the request is correct
        SecurityLevel secLevel = evalSecurityLevel(input.getConfiguredSecurityLevel());
        if ((null != input.getSenderIdentity()) && (! input.getSenderIdentity().isEmpty())) {
            LOG.trace("Checking permissions of the authenticated request");
            onem2mResponse = checkRequestPermissionsAuth(input, resourceLocator, onem2mRequest);
        } else {
            if (secLevel == SecurityLevel.L2) {
                LOG.error("Invalid security level passed (L2) without authentication");
                onem2mResponse = new ResponsePrimitive();
                onem2mResponse.setPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER,
                        onem2mRequest.getPrimitive(RequestPrimitive.REQUEST_IDENTIFIER));
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR,
                                      "Invalid security level without authentication");
                return createOutputFromResponse(onem2mResponse, onem2mPrimitiveList);
            }
            LOG.trace("Checking permissions of the request which is not authenticated");
            onem2mResponse = checkRequestPermissionsNoAuth(secLevel, resourceLocator, onem2mRequest);
        }

        if (null != onem2mResponse) {
            // Error response has been returned by verification methods
            LOG.trace("Request permissions check failed");
            return createOutputFromResponse(onem2mResponse, onem2mPrimitiveList);
        }

        // Check if the target URI points to local resource
        if (! resourceLocator.isLocalResource()) {
            LOG.trace("Non-local resource requested by URI {}", resourceLocator.getTargetURI());

            try {
                onem2mResponse = routerService.forwardRequest(onem2mRequest, resourceLocator).get();
            } catch (InterruptedException | ExecutionException ex) {
                LOG.error("Forwarding procedure failed: {}", ex);
                onem2mResponse = new ResponsePrimitive();
                onem2mResponse.setPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER,
                        onem2mRequest.getPrimitive(RequestPrimitive.REQUEST_IDENTIFIER));
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR, "Forwarding procedure failed");
            }

        } else {
            LOG.trace("Local resource requested by URI {}", resourceLocator.getTargetURI());

            onem2mRequest.setTargetResourceLocator(resourceLocator);
            onem2mResponse = new ResponsePrimitive();

            onem2mRequest.handleOperation(twc, trc, onem2mResponse);
        }

        return createOutputFromResponse(onem2mResponse, onem2mPrimitiveList);
    }

    private ResponsePrimitive prepareAccessDeniedErrorResponse(RequestPrimitive onem2mRequest, String message) {
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();
        onem2mResponse.setPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER,
                onem2mRequest.getPrimitive(RequestPrimitive.REQUEST_IDENTIFIER));
        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.ACCESS_DENIED, message);
        return onem2mResponse;
    }

    private ResponsePrimitive prepareInternalErrorResponse(RequestPrimitive onem2mRequest, String message) {
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();
        onem2mResponse.setPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER,
                onem2mRequest.getPrimitive(RequestPrimitive.REQUEST_IDENTIFIER));
        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR, message);
        return onem2mResponse;
    }

    private ResponsePrimitive prepareBadRequestErrorResponse(RequestPrimitive onem2mRequest, String message) {
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();
        onem2mResponse.setPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER,
                onem2mRequest.getPrimitive(RequestPrimitive.REQUEST_IDENTIFIER));
        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, message);
        return onem2mResponse;
    }

    /*
     * Implements verification of requests received from non-authenticated sender.
     * Returns error response is case of verification failure. Null is returned if the request is OK.
     */
    private ResponsePrimitive checkRequestPermissionsNoAuth(SecurityLevel securityLevel,
                                                            Onem2mDb.CseBaseResourceLocator resourceLocator,
                                                            RequestPrimitive onem2mRequest) {
        String originator = onem2mRequest.getPrimitive(RequestPrimitive.FROM);
        String operation = onem2mRequest.getPrimitive(RequestPrimitive.OPERATION);
        String resourceType  = onem2mRequest.getPrimitive(RequestPrimitive.RESOURCE_TYPE);

        /*
         * Create resource locator of the originator in order to check whether the
         * originator is locally registered or not
         */
        Onem2mDb.CseBaseOriginatorLocator originLocator = null;
        try {
            originLocator = this.db.getOriginLocator(originator, null);
        } catch (IllegalArgumentException e) {
            LOG.error("Failed to create origin locator object of From value: {}, sender is not authenticated. {}",
                      originator, e);
            return prepareBadRequestErrorResponse(onem2mRequest, "Unable to use originator ID: " + originator);
        }

        // Check whether the originator is locally registered entity
        String originatorEntityType = originLocator.isRegistered(trc);

        if (resourceLocator.isLocalResource()) {
            // targeted to local resource (hosted by local cseBase)
            if (null == originatorEntityType) {
                // originator of request is not registered locally
                if (securityLevel != SecurityLevel.L0) {
                    return prepareAccessDeniedErrorResponse(onem2mRequest,
                            "Security level L1: Non registered originators not allowed");
                }
                // else process request
                return null;
            } else {
                // verify whether AE doesn't create CSE and vice versa
                switch (originatorEntityType) {
                    case Onem2m.ResourceType.AE:
                        if (operation.equals(Onem2m.Operation.CREATE) &&
                            (resourceType.equals(Onem2m.ResourceType.REMOTE_CSE))) {
                            return prepareAccessDeniedErrorResponse(onem2mRequest,
                                                                    "Attempt to register CSE on behalf of AE");
                        }
                        break;
                    case Onem2m.ResourceType.REMOTE_CSE:
                        if (operation.equals(Onem2m.Operation.CREATE) &&
                            (resourceType.equals(Onem2m.ResourceType.AE))) {
                            return prepareAccessDeniedErrorResponse(onem2mRequest,
                                                                    "Attempt to register AE on behalf of CSE");
                        }
                        break;
                    default:
                        LOG.error("Invalid entity type of registered originator entity: {}", originatorEntityType);
                        return prepareInternalErrorResponse(onem2mRequest,
                                                            "Failed to resolve request originator entity type.");
                }
            }
        } else {
            // not locally targeted, needs to be forwarded
            if ((securityLevel != SecurityLevel.L0) &&
                (null == originatorEntityType)) {
                // this is not L0 and originator is not registered
                return prepareAccessDeniedErrorResponse(onem2mRequest,
                        "Security level L1: Requests from non registered originators cannot be forwarded");
            }

            // this is L0 or the originator is registered and this is L1
            if (originLocator.isCseRelativeCtypeAeId()) {
                // this is CSE relative C-type AE-ID, can't be used as ID for other CSEs
                return prepareAccessDeniedErrorResponse(onem2mRequest,
                        "CSE relative C-type AE-ID used as request originator ID but target resource is " +
                                "not hosted locally");
            }

            if (operation.equals(Onem2m.Operation.CREATE) &&
                (resourceType.equals(Onem2m.ResourceType.AE) || resourceType.equals(Onem2m.ResourceType.REMOTE_CSE))) {
                // entity registration requests are not allowed to be forwarded
                return prepareAccessDeniedErrorResponse(onem2mRequest,
                                                        "Entity registration cannot be forwarded");
            }
        }

        // request is OK
        return null;
    }

    /*
     * Implements verification of request sent (originated or forwarded) by authenticated CSE.
     * Returns error response is case of verification failure. Null is returned if the request is OK.
     */
    private ResponsePrimitive checkRequestPermissionsAuthAsCse(
                                                     @Nonnull final String cseBaseId,
                                                     @Nonnull final String cseId,
                                                     @Nonnull final Onem2mDb.CseBaseResourceLocator resourceLocator,
                                                     @Nonnull final RequestPrimitive onem2mRequest) {
        /*
         * Algorithm parts:
         * Part1: The request is registration of CSE (create remoteCSE).
         * Part2: Validation of the From parameter.
         * Part3: End, deny CSE registration to be forwarded otherwise continue with forwarding or local processing
         */

        String originator = onem2mRequest.getPrimitive(RequestPrimitive.FROM);
        String operation = onem2mRequest.getPrimitive(RequestPrimitive.OPERATION);
        String resourceType  = onem2mRequest.getPrimitive(RequestPrimitive.RESOURCE_TYPE);

        // Create locator of the resource representing originator (not sender) of the request
        Onem2mDb.CseBaseOriginatorLocator originLocator = null;
        try {
            originLocator = this.db.getOriginLocator(originator, cseBaseId);
        } catch (IllegalArgumentException e) {
            LOG.error("Failed to create origin locator object of From value: {} and cseBaseId {}, sender is CSE. {}",
                      originator, cseBaseId, e);
        }

        if (operation.equals(Onem2m.Operation.CREATE) && resourceType.equals(Onem2m.ResourceType.AE)) {
            // AE registration cannot be forwarded nor originated by CSE
            return prepareAccessDeniedErrorResponse(onem2mRequest,
                                                    "AE registration received from CSE");
        }

        // check whether the request sender (not originator) is registered
        if (Onem2mRouterService.getInstance().hasRemoteCse(cseBaseId, cseId)) {
            /* Part2
             * Request sender CSE is registered, validate From parameter
             */

            /* check if the From parameter includes ID which identifies also
             * registrarCSE of the request originator
             */
            if (null == originLocator.getRegistrarCseId()) {
                /* check whether the From parameter includes CSE-ID of CSE
                 * which is registered
                 */
                // TODO verify FQDN
                if (Onem2mRouterService.getInstance().hasRemoteCse(cseBaseId, originLocator.getEntityId())) {
                    // From parameter is ID of registered remoteCSE
                    if (! cseId.equals(originLocator.getEntityId())) {
                        /* sender identity and originator identity mismatch
                         * request has been probably sent indirectly (through another CSE)
                         */
                        return prepareAccessDeniedErrorResponse(onem2mRequest,
                                "Originator CSE is registered but its identity does not match sender identity");
                    }

                    //continue at Part3

                } else if (originLocator.isCseRelativeCtypeAeId()) {
                    /* From parameter includes CSE-relative C-type AE-ID-Stem.
                     * It means that sender CSE forwarded request with invalid From parameter since
                     * CSE-relative C-type AE-ID-Stem cannot be used out of scope of its registrar CSE
                     */
                    return prepareAccessDeniedErrorResponse(onem2mRequest,
                            "The request received from CSE has From parameter set to CSE-relative C-type AE-ID-Stem");
                }

                // continue at Part3
            } else {
                // there's also request originator's registrarCSE CSE-ID in the form parameter

                if (Onem2mRouterService.getInstance().hasRemoteCse(cseBaseId, originLocator.getRegistrarCseId())) {
                    // the registrarCSE of the request originator is registered locally

                    if (! cseId.equals(originLocator.getRegistrarCseId())) {
                        /* The locally registered registrarCSE has different identity than sender.
                         * It means that the request has been sent indirectly.
                         */
                        return prepareAccessDeniedErrorResponse(onem2mRequest,
                                "The request received from CSE different than registrarCSE of originator but " +
                                "the registrarCSE is locally registered");
                    }

                    // continue at Part3
                }

                // continue at Part3
            }
        } else {
            /*
             * Part1
             * request sender CSE is not registered so the only operation allowed is
             * the registration of the sender CSE
             */

            if (! (operation.equals(Onem2m.Operation.CREATE) && resourceType.equals(Onem2m.ResourceType.REMOTE_CSE))) {
                // this is not CSE registration request
                return prepareAccessDeniedErrorResponse(onem2mRequest,
                                                        "Received request from CSE which is not registered");
            }

            if (! resourceLocator.isLocalResource()) {
                /* Request is not targeted locally and
                 * entity registrations cannot be forwarded
                 */
                return prepareAccessDeniedErrorResponse(onem2mRequest,
                                                        "CSE registration cannot be forwarded");
            }

            // TODO check FQDN
            if (! originLocator.getEntityId().equals(cseId)) {
                // CSE registration must be originated by the authenticated CSE
                return prepareAccessDeniedErrorResponse(onem2mRequest,
                        "CSE registration originator and authenticated CSE identity mismatch");
            }

            // process the CSE registration
            return null;
        }

        /*
         * Part3
         * Sender CSE is registered and From parameter is valid
         */
        if (operation.equals(Onem2m.Operation.CREATE) && resourceType.equals(Onem2m.ResourceType.REMOTE_CSE)) {
            // Attempt to register CSE remotely
            return prepareAccessDeniedErrorResponse(onem2mRequest, "Remote CSE registration is not allowed");
        }

        // request can be processed or forwarded
        return null;
    }

    /*
     * Implements verification of request sent by (originated by) AE.
     * Returns error response is case of verification failure. Null is returned if the request is OK.
     */
    private ResponsePrimitive checkRequestPermissionsAuthAsAe(
                                                      @Nonnull final String cseBaseId,
                                                      @Nonnull final String aeId,
                                                      @Nonnull final Onem2mDb.CseBaseResourceLocator resourceLocator,
                                                      @Nonnull final RequestPrimitive onem2mRequest) {
        String originator = onem2mRequest.getPrimitive(RequestPrimitive.FROM);
        String operation = onem2mRequest.getPrimitive(RequestPrimitive.OPERATION);
        String resourceType  = onem2mRequest.getPrimitive(RequestPrimitive.RESOURCE_TYPE);

        // Create locator of the resource representing originator (in case of AE it is also sender) of the request
        Onem2mDb.CseBaseOriginatorLocator originLocator = null;
        try {
            originLocator = this.db.getOriginLocator(originator, cseBaseId);
        } catch (IllegalArgumentException e) {
            LOG.error("Failed to create origin locator object of From value: {} and cseBaseId {}, sender is AE. {}",
                      originator, cseBaseId, e);
        }

        if (null == originLocator.isRegistered(trc)) {
            /*
             * AE is not registered so request must be targeted to locally and
             * the only operation allowed is originator (and also sender) AE registration
             */
            if (! resourceLocator.isLocalResource()) {
                // request is not targeted locally
                return prepareAccessDeniedErrorResponse(onem2mRequest,
                                                        "AE cannot send request to remoteCSE if is not registered");
            }

            if (! ((operation.equals(Onem2m.Operation.CREATE)) && resourceType.equals(Onem2m.ResourceType.AE))) {
                // this is not registration of AE
                return prepareAccessDeniedErrorResponse(onem2mRequest,
                        "The only operation allowed to non-registered AE is its own registration");
            }

            // request is AE registration, can be processed
        } else {
            /*
             * originator AE is registered
             * Identity specified in the From parameter must equal with the authenticated identity
             */
            // TODO verify FQDN
            if (! aeId.equals(originLocator.getEntityId())) {
                // AE authenticated with different identity
                return prepareAccessDeniedErrorResponse(onem2mRequest,
                        "AE authenticated with identity which is different than its registered identity");
            }

            // Check whether the target of the request is hosted locally
            if (resourceLocator.isLocalResource()) {
                // targeted locally
                if (operation.equals(Onem2m.Operation.CREATE) && resourceType.equals(Onem2m.ResourceType.REMOTE_CSE)) {
                    // AE cannot register CSE
                    return prepareAccessDeniedErrorResponse(onem2mRequest,
                            "Entity authenticated as AE attempted to register as CSE");
                }

                // don't need to check whether CSE don't try to create AE because the request
                // sender has been authenticated as AE so the local CSE must be the first hop

                // request can be processed
            } else {
                // request will be forwarded
                if (originLocator.isCseRelativeCtypeAeId()) {
                    // From parameter set to CSE-relative C-type AE-ID-Stem cannot be forwarded
                    return prepareAccessDeniedErrorResponse(onem2mRequest,
                            "Request with From parameter set to CSE-relative C-type AE-ID-Stem cannot be forwarded");
                }

                if (operation.equals(Onem2m.Operation.CREATE) &&
                    (resourceType.equals(Onem2m.ResourceType.AE) ||
                             resourceType.equals(Onem2m.ResourceType.REMOTE_CSE))) {
                    return prepareAccessDeniedErrorResponse(onem2mRequest,
                            "Entity registration cannot be forwarded");
                }

                // request can be forwarded
            }
        }

        // request is OK
        return null;
    }

    /*
     * Implements verification of requests from authenticated senders.
     * Returns error response is case of verification failure. Null is returned if the request is OK.
     */
    private ResponsePrimitive checkRequestPermissionsAuth(Onem2mRequestPrimitiveInput input,
                                                          Onem2mDb.CseBaseResourceLocator resourceLocator,
                                                          RequestPrimitive onem2mRequest) {
        /*
         * Check if mandatory attributes retrieved by authentication method are set:
         *  1. Entity type (AE or CSE)
         *  2. CSE-ID of the cseBase at which the sender is registered (useful in cases of multiple cseBase instances
         *  at the local IoTDM.
         *  3. The authenticated identity of the sender.
         */
        if (null == input.isSenderIsCse()) {
            LOG.error("Invalid request primitive input for authenticated request, isFromCse information missing");
            return prepareInternalErrorResponse(onem2mRequest, "Invalid data from authentication method");
        }

        if ((null == input.getCseBaseId()) || (input.getCseBaseId().isEmpty())) {
            LOG.error("Invalid request primitive input for authenticated request, toCseBaseId information missing");
            return prepareInternalErrorResponse(onem2mRequest, "Invalid data from authentication method");
        }

        if ((null == input.getSenderIdentity()) || (input.getSenderIdentity().isEmpty())) {
            LOG.error("Invalid request primitive input for authenticated request, " +
                      "OriginatorIdentity information is missing");
            return prepareInternalErrorResponse(onem2mRequest, "Invalid data from authentication method");
        }

        // Security level must be set otherwise it is internal error
        assert null != input.getConfiguredSecurityLevel();

        boolean isCse = input.isSenderIsCse();
        String cseBaseId = input.getCseBaseId();
        String senderIdentity = input.getSenderIdentity();

        if ((null != resourceLocator.getCseBaseCseId()) && (! cseBaseId.equals(resourceLocator.getCseBaseCseId()))) {
            LOG.error("Request sender is not authenticated for the target cseBase: target: {}, authenticated: {}",
                      resourceLocator.getCseBaseCseId(), cseBaseId);
            return prepareAccessDeniedErrorResponse(onem2mRequest,
                                                    "Operation not allowed for target cseBase: " +
                                                            resourceLocator.getCseBaseCseId());
        }

        // Call methods implementing verification for specific entity type
        if(isCse) {
            return checkRequestPermissionsAuthAsCse(cseBaseId, senderIdentity, resourceLocator, onem2mRequest);
        }

        return checkRequestPermissionsAuthAsAe(cseBaseId, senderIdentity, resourceLocator, onem2mRequest);
    }

    /**
     * This is the cse provisioning RPC, it can be called from restconf directly.  When a cse is initially configured
     * it needs to be provisioned with the parameters of the CSE.  This can be done via an RPC, or perhaps a CONFIG
     * file in the file system?
     *
     * For now the provisioning parms supported are
     * 1 CSE_ID - the name of the cse Base
     * 2 CSE_TYPE - "IN-CSE", "...
     * @param input cse parms
     * @return cse output parms
     */
    @Override
    public Future<RpcResult<Onem2mCseProvisioningOutput>> onem2mCseProvisioning(Onem2mCseProvisioningInput input) {

        LOG.info("RPC: begin handle onem2mCseProvisioning ...");

        List<Onem2mPrimitive> csePrimitiveList = input.getOnem2mPrimitive();
        RequestPrimitiveProcessor onem2mRequest = new RequestPrimitiveProcessor();
        onem2mRequest.setPrimitivesList(csePrimitiveList);
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();

        onem2mRequest.provisionCse(twc, trc, onem2mResponse);

        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE).equals(Onem2m.ResponseStatusCode.OK)) {
            RequestPrimitiveProcessor onem2mRequest1 = new RequestPrimitiveProcessor();
            onem2mRequest1.setPrimitivesList(csePrimitiveList);
            onem2mRequest1.createDefaultACP(twc, trc, onem2mResponse);
        }

        csePrimitiveList = onem2mResponse.getPrimitivesList();

        Onem2mCseProvisioningOutput output = new Onem2mCseProvisioningOutputBuilder()
                .setOnem2mPrimitive(csePrimitiveList).build();

        // TODO: modify the default acp response
        // TODO: why response return only CSE information, what about ACP?
        LOG.info("RPC: end handle onem2mCseProvisioning ...");
        return RpcResultBuilder.success(output).buildFuture();
    }

    /**
     * Cleanup the data store RPC
     * @return VOID
     */
    @Override
    public Future<RpcResult<java.lang.Void>> onem2mCleanupStore() {

        Onem2mDb.getInstance().cleanupDataStore(twc);
        Onem2mRouterService.cleanRoutingTable();
        Onem2mDb.getInstance().dumpResourceIdLog(trc, null);
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    /**
     * Dump the data store to karaf log RPC
     * @param input all or start resource
     * @return VOID
     */
    @Override
    public Future<RpcResult<java.lang.Void>> onem2mDumpResourceTree(Onem2mDumpResourceTreeInput input) {
        LOG.info("RPC: onem2mDumpResourceTree dumping ...");

        RequestPrimitiveProcessor onem2mRequest = new RequestPrimitiveProcessor();
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();

        String resourceId;
        if (input == null || input.getResourceUri() == null || input.getResourceUri().trim().contentEquals("")) {
            resourceId = null;
        } else {
            String resourceUri = input.getResourceUri().trim();
            //onem2mRequest.setPrimitive(RequestPrimitive.TO, resourceUri);
            if (!Onem2mDb.getInstance().findResourceUsingURI(trc, resourceUri, onem2mRequest, onem2mResponse)) {
                LOG.error("Cannot find resourceUri: {}", resourceUri);
                return Futures.immediateFuture(RpcResultBuilder.<Void>failed().build());
            }
            resourceId = onem2mRequest.getResourceId();
            LOG.info("Dumping resourceUri: {}, resourceId: {}", resourceUri, resourceId);
        }
        switch (input.getDumpMethod()) {
            case RAW:
                Onem2mDb.getInstance().dumpResourceIdLog(trc, resourceId);
                break;
            case HIERARCHICAL:
                Onem2mDb.getInstance().dumpHResourceIdToLog(trc, resourceId);
                break;
            default:
                LOG.error("Unknown dump method: {}", input.getDumpMethod());
                break;
        }
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    /**
     * This is a generic debug function ... it allows input and output parameters
     * @param input Onem2mDebugInput
     * @return returnRPC
     */
    @Override
    public Future<RpcResult<Onem2mDebugOutput>> onem2mDebug(Onem2mDebugInput input) {

        LOG.info("RPC: begin handle onem2mDebug ...");

        List<Onem2mPrimitive> onem2mPrimitiveList = input.getOnem2mPrimitive();
        RequestPrimitiveProcessor onem2mRequest = new RequestPrimitiveProcessor();
        onem2mRequest.setPrimitivesList(onem2mPrimitiveList);
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();

        String op = onem2mRequest.getPrimitive("op");
        if (op == null) {
            onem2mResponse.setRSC("Unknown op", "(op) not set in input list");
        } else if (op.contentEquals("stats-get")) {
            onem2mResponse.setPrimitive("stats", getOnem2mStats());
        } else {
            onem2mResponse.setRSC("unknown op", op);
        }

        onem2mPrimitiveList = onem2mResponse.getPrimitivesList();
        Onem2mDebugOutput output = new Onem2mDebugOutputBuilder()
                .setOnem2mPrimitive(onem2mPrimitiveList).build();

        LOG.info("RPC: end handle onem2mDebug ...");

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public String getOnem2mStats() {
        return stats.getStats().toString();
    }
}
